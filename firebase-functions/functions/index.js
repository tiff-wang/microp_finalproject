'use strict';

const functions = require('firebase-functions');
const gcs = require('@google-cloud/storage')();
const path = require('path');
const os = require('os');
const fs = require('fs');
const https = require('https')
const { BingSpeechClient, VoiceRecognitionResponse } = require('bingspeech-api-client');


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

  return recognize(object.name, object.bucket, object.metadata);
});


function recognize(filePath, bucketName, metadata) {
  // let downloadUrl = "https://firebasestorage.googleapis.com/v0/b/" + bucketName + "/o/" + filePath + "?alt=media&token=" + metadata.firebaseStorageDownloadTokens

  let downloadUrl = "https://firebasestorage.googleapis.com/v0/b/microp-g2.appspot.com/o/test-3.wav?alt=media&token=b271de9f-a8dc-4e0f-b9f9-e6cb31320c59"

  console.log(downloadUrl)
  var request = https.get("https://firebasestorage.googleapis.com/v0/b/microp-g2.appspot.com/o/test-3.wav?alt=media&token=b271de9f-a8dc-4e0f-b9f9-e6cb31320c59", (response) => {
    console.log("hiiiiiiiiiii")
    console.log(response)
    let subscriptionKey = '062461c0e7f5456abcc4791b559907f2';
    let client = new BingSpeechClient(subscriptionKey);
    client.recognizeStream(response).then(response => {
      return console.log(response.results[0].name)
    }).catch(err => 
      console.log(err)
    );
  });



  // const tempLocalFile = path.join(os.tmpdir(), filePath);
  // const tempLocalDir = path.dirname(tempLocalFile);
  // const bucket = gcs.bucket(bucketName);
  // const client = new speech.SpeechClient();

  
  // Create the temp directory where the storage file will be downloaded.
  // return mkdirp(tempLocalDir).then(() => {
  //   console.log('Temporary directory has been created', tempLocalDir);
  //   // Download file from bucket.
  //   return bucket.file(filePath).download({destination: tempLocalFile});
  // }).then(() => {
  //   console.log('The file has been downloaded to', tempLocalFile);
  //   // Uploading the Blurred image.
  //   filePath = 'proc_' + filePath;
  //   return bucket.upload(tempLocalFile, {
  //     destination: filePath,
  //     metadata: {metadata: metadata}, // Keeping custom metadata.
  //   });
  // }).then(() => {
  //   filePath = 'proc_' + filePath;
  //   console.log('Image uploaded to Storage at', filePath);
  //   fs.unlinkSync(tempLocalFile);
  //   return console.log('Deleted local file', filePath);
  // });
}