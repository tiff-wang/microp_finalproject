/*

Functions Endpoint: https://us-central1-microp-g2.cloudfunctions.net/<function_name>


*/

const priv = require('./private');
const functions = require('firebase-functions');
const mkdirp = require('mkdirp-promise');
const gcs = require('@google-cloud/storage')();
const spawn = require('child-process-promise').spawn;
const path = require('path');
const os = require('os');

 // Create and Deploy Your First Cloud Functions
 // https://firebase.google.com/docs/functions/write-firebase-functions

exports.helloWorld = functions.https.onRequest((request, response) => {
 	response.send("Hello from Firebase!");
});

// exports.pitchandroll = functions.https.onRequest((request, response) => {
// 	var plotly = require('plotly')(priv.username, priv.apiKey);

// 	var pitch = {
// 	  x: [1, 2, 3, 4],
// 	  y: [2, 15, 13, 17],
// 	  name: 'Pitch'
// 	};

// 	var roll = {
// 	  x: [1, 2, 3, 4],
// 	  y: [3, 12, 14, 18],
// 	  name: 'Roll'
// 	};

// 	var data = [pitch, roll];
// 	var graphOptions = {title:"Pitch and Roll", filename: "date-axes", fileopt: "overwrite"};

// 	plotly.plot(data, graphOptions, (err, msg) => {
// 	    if(err){
// 	    	response.send("Pitch and roll plotting failed: " + err)
// 	    }
// 	    console.log("Successfully plotted: " + msg)
// 	    response.send("Successfully plotted: " + msg)
// 	});
// });

exports.uploadRec = functions.storage.object().onChange((event) => {
  const object = event.data;
  const contentType = object.contentType;
  const file = gcs.bucket(object.bucket).file(object.name);
  const fileName = path.basename(object.name);

 // Exit if this is a move or deletion event.
  if (object.resourceState === 'not_exists') {
    console.log('This is a deletion event.');
    return null;
  }

  if (fileName.startsWith('proc_')){
  	console.log('Already processed file');
    return null;
  }

  // Exit if this is triggered on a file that is not an audio.
  else if (contentType.startsWith('text/plain')) {
    accGraph(object.name, object.bucket, object.metadata)
    return console.log('Plotting pitch and roll')
  } 

  else if (contentType.startsWith('image/')){
    return console.log('graph uploaded')
  }

  else if (contentType.startsWith('audio/')){
    // add voice recognition 
    return console.log('audio file')
  }

  return console.log('ended with not a file of interest');
});


function accGraph(filePath, bucketName, metadata) {
  let downloadUrl = "https://firebasestorage.googleapis.com/v0/b/" + bucketName + "/o/" + filePath + "?alt=media&token=" + metadata.firebaseStorageDownloadTokens

  // const url = "https://firebasestorage.googleapis.com/v0/b/microp-g2.appspot.com/o/boardData%2Ftestfisdfle%20(1).txt?alt=media&token=4df767d9-a74b-46fa-a83f-42055f715a8e"
    const https = require('https')
    const plotly = require('plotly')

    https.get(downloadUrl, (res) => {
        res.setEncoding('utf8');
        res.on('data', (data) => {
            console.log('text file parsing start')

            var plotly = require('plotly')(priv.username, priv.apiKey);
            var array = data.split("\n").slice(0, -1)
            var X = array[0].split(" ").slice(0, -1).map(Number)
            var Y = array[1].split(" ").slice(0, -1).map(Number)
            var Z = array[2].split(" ").slice(0, -1).map(Number)

            var pitch_array = []
            var roll_array = []

            for(var i = 0 ; i < X.length; i ++){
                pitch_array.push(Math.atan(-1 * X[i] / Z[i]))
                roll_array.push(Math.atan(Y[i]/Math.sqrt(X[i] * X[i] + Z[i] * Z[i])))
            }

            var x_array = Array.apply(null, {length: pitch_array.length}).map(Number.call, Number)
             var pitch = {
               x: pitch_array,
               y: x_array,
               name: 'Pitch'
             };

             var roll = {
               x: roll_array, 
               y: x_array,
               name: 'Roll'
             };

             var graph_data = [pitch, roll];
             var graphOptions = {title:"Pitch and Roll", filename: "date-axes", fileopt: "overwrite"};

             var figure = {'data': graph_data}

             plotly.getImage(figure, graphOptions, (err, imageStream) => {
                 if(err){
                     console.log("Pitch and roll plotting failed")
                 }

                console.log('starting upload')
                const fs = require('fs')

                var bucket = gcs.bucket(bucketName)
                const tempLocalFile = path.join(os.tmpdir(), 'graph.png');
                const tempLocalDir = path.dirname(tempLocalFile);

                return mkdirp(tempLocalDir).then(() => {
                    console.log('Temporary directory has been created', tempLocalDir);
                    // Download file from bucket.
                    var fileStream = fs.createWriteStream(tempLocalFile);
                    imageStream.pipe(fileStream);
                    console.log('The file has been downloaded to', tempLocalFile);
                    // Uploading the Blurred image.
                    return bucket.upload(tempLocalFile, {
                      destination: 'graph.png',
                      metadata: {metadata: 'image/png'}, // Keeping custom metadata.
                    });
                  }).then(() => {
                    console.log('Image uploaded to Storage at', filePath);
                    fs.unlinkSync(tempLocalFile);
                    return console.log('Deleted local file', filePath);
                 });
             });
        });
    });
}


exports.syncRecognizeGCS = functions.https.onRequest((req, res) => {
  // Imports the Google Cloud client library
  const speech = require('@google-cloud/speech');

  // Creates a client
  const client = new speech.SpeechClient();


  const gcsUri = 'gs://microp-g2.appspot.com/test-online.raw';
  const encoding = 'LINEAR16';
  const sampleRateHertz = 8000;
  const languageCode = 'en-US';

  const config = {
    encoding: encoding,
    sampleRateHertz: sampleRateHertz,
    languageCode: languageCode,
  };
  const audio = {
    uri: gcsUri,
  };

  const request = {
    config: config,
    audio: audio,
  };

  // Detects speech in the audio file
  client
    .recognize(request)
    .then(data => {
      const response = data[0];
      const transcription = response.results
        .map(result => result.alternatives[0].transcript)
        .join('\n');
      console.log('Transcription: ', transcription)
      return res.send('Transcription: ', transcription);
    })
    .catch(err => {
      console.error('ERROR:', err);
    });
})