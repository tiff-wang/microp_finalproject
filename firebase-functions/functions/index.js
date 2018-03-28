'use strict';

const functions = require('firebase-functions');
const mkdirp = require('mkdirp-promise');
const gcs = require('@google-cloud/storage')();
const speech = require('@google-cloud/speech');
const spawn = require('child-process-promise').spawn;
const path = require('path');
const os = require('os');
const fs = require('fs');


/**
 * When an image is uploaded we check if it is flagged as Adult or Violence by the Cloud Vision
 * API and if it is we blur it using ImageMagick.
 */
exports.number = functions.storage.object().onChange((event) => {
  const object = event.data;
  const contentType = object.contentType;
  const file = gcs.bucket(object.bucket).file(object.name);
  const fileName = path.basename(object.name);

  // Exit if this is a move or deletion event.
  if (object.resourceState === 'not_exists') {
    console.log('This is a deletion event.');
    return null;
  }

  // Exit if this is triggered on a file that is not an audio.
  if (!contentType.startsWith('audio/')) {
    console.log('This is not an audio.');
    return null;
  }

  if (fileName.startsWith('proc_')){
  	console.log('Already processed file');
    return null;
  }

  return test(object.name, object.bucket, object.metadata);
});


function recognize(filePath, bucketName, metadata) {
  const tempLocalFile = path.join(os.tmpdir(), filePath);
  const tempLocalDir = path.dirname(tempLocalFile);
  const bucket = gcs.bucket(bucketName);
  const client = new speech.SpeechClient();

  const config = {
    encoding: 'LINEAR16',
    sampleRateHertz: '16000',
    languageCode: 'en-US',
  };
  const audio = {
    uri: "gs://" + bucketName + "/" + filePath,
  };

  const request = {
    config: config,
    audio: audio,
  };
  // Create the temp directory where the storage file will be downloaded.
  return mkdirp(tempLocalDir).then(() => {
    console.log('Temporary directory has been created', tempLocalDir);
    // Download file from bucket.
    return bucket.file(filePath).download({destination: tempLocalFile});
  }).then(() => {
    console.log('The file has been downloaded to', tempLocalFile);
    // Uploading the Blurred image.
    filePath = 'proc_' + filePath;
    return bucket.upload(tempLocalFile, {
      destination: filePath,
      metadata: {metadata: metadata}, // Keeping custom metadata.
    });
  }).then(() => {
    filePath = 'proc_' + filePath;
    console.log('Image uploaded to Storage at', filePath);
    fs.unlinkSync(tempLocalFile);
    return console.log('Deleted local file', filePath);
  });
}

function test(filePath, bucketName, metadata){
	const speech = require('@google-cloud/speech');
	const fs = require('fs');

	// Creates a client
	const client = new speech.SpeechClient();

	// The name of the audio file to transcribe
	const fileName = './resources/audio.raw';

	// Reads a local audio file and converts it to base64
	const file = fs.readFileSync(fileName);
	const audioBytes = file.toString('base64');

	// The audio file's encoding, sample rate in hertz, and BCP-47 language code
	const audio = {
	  "uri": "gs://" + bucketName + "/" + filePath
	};
	const config = {
    "encoding": "LINEAR16",
    "sampleRateHertz": 16000,
    "languageCode": "en-US"
	};
	const request = {
	  audio: audio,
	  config: config,
	};

	// Detects speech in the audio file
	client
	  .recognize(request)
	  .then(data => {
	    const response = data[0];
	    const transcription = response.results
	      .map(result => result.alternatives[0].transcript)
	      .join('\n');
	    console.log(`Transcription: ${transcription}`);

	    return transcription
	  })
	  .catch(err => {
	    console.error('ERROR:', err);
	  });

	return null;
}