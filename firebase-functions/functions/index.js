'use strict';

const functions = require('firebase-functions');
const gcs = require('@google-cloud/storage')();
const path = require('path');
const os = require('os');
const fs = require('fs');
const https = require('https')
const uuid = require("uuid");
const needle = require("needle");
const querystring = require("querystring");


/**
 * When an image is uploaded we check if it is flagged as Adult or Violence by the Cloud Vision
 * API and if it is we blur it using ImageMagick.
 */
exports.recognizeNumber = functions.storage.object().onChange((event) => {
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
	const raw = "https://firebasestorage.googleapis.com/v0/b/microp-g2.appspot.com/o/test.raw?alt=media&token=b7c52973-2405-4683-962e-ab41083d93f5"
	const wav = "https://firebasestorage.googleapis.com/v0/b/microp-g2.appspot.com/o/test-3.wav?alt=media&token=4220d350-4032-4e39-997c-c3bb5989e1ae"

	https.get(wav, response => {
		console.log("file downloaded")
		let subscriptionKey = '6e27195a4b144e6c8da5e3b3f9a63e43';
		let params = {
			'scenarios': 'ulm',
			'locale': 'en-US',
			'device.os': '-',
			'version': '3.0',
            'appid': 'D4D52672-91D7-4C74-8AD8-42B1D98141A5',
			'format': 'json',
			'requestid': uuid.v4(),
			'instanceid': uuid.v4()
		};
		let endpoint = 'https://speech.platform.bing.com/recognize?' + querystring.stringify(params)

		let options = {
				headers: {
					'Transfer-Encoding': 'chunked',
					'Ocp-Apim-Subscription-Key': subscriptionKey,
					'Content-Type': 'audio/wav; codec="audio/pcm"; samplerate=16000'
				}
		};
		needle.post(endpoint, response, options, (err, res, body) => {
			console.log("query recognition")
			if (err) {
				return console.log(err);
			}
			if (res.statusCode !== 200) {
				return console.log(new Error(`Wrong status code ${res.statusCode} in Bing Speech API / synthesize`));
			}
			return console.log(body);
		});
				
	});


	// const tempLocalFile = path.join(os.tmpdir(), filePath);
	// const tempLocalDir = path.dirname(tempLocalFile);
	// const bucket = gcs.bucket(bucketName);

	
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