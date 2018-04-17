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

// returns response "Hello from Firebase" on HTTP request --> test
exports.helloWorld = functions.https.onRequest((request, response) => {
 	response.send("Hello from Firebase!");
});


// return graph url on HTTP request . 
exports.pitchandroll = functions.https.onRequest((request, response) => {
	var plotly = require('plotly')(priv.username, priv.apiKey);

	var pitch = {
	  x: [1, 2, 3, 4],
	  y: [2, 15, 13, 17],
	  name: 'Pitch'
	};

	var roll = {
	  x: [1, 2, 3, 4],
	  y: [3, 12, 14, 18],
	  name: 'Roll'
	};

	var data = [pitch, roll];
	var graphOptions = {title:"Pitch and Roll", filename: "date-axes", fileopt: "overwrite"};

	plotly.plot(data, graphOptions, (err, msg) => {
	    if(err){
	    	response.send("Pitch and roll plotting failed: " + err)
	    }
	    console.log("Successfully plotted: " + msg)
	    response.send("Successfully plotted: " + msg)
	});
});


// uploadRec triggered on storage change event (delete, upload, rename etc)
// calls different functions based on type of files uploaded
exports.uploadRec = functions.storage.object().onChange((event) => {
  const object = event.data;
  const contentType = object.contentType;
  const file = gcs.bucket(object.bucket).file(object.name);
  const fileName = path.basename(object.name);

  console.log(object)

 // Exit if this is a move or deletion event.
  if (object.resourceState === 'not_exists') {
    console.log('This is a deletion event.');
    return null;
  }

  if (path.basename(object.name).startsWith('proc_')){
  	console.log('Already processed file');
    return null;
  }

  else if (contentType.startsWith('text/plain')) {
    accGraph(object.name, object.bucket, object.metadata)
    console.log('Plotting pitch and roll');
    return null;
  } 

  else if (contentType.startsWith('image/')){
    return console.log('graph uploaded')
  }

  else if (object.name.endsWith('.raw')){
    // add voice recognition 
    voiceRec(object.name, object.bucket, object.metadata)
    return console.log('audio file')
  }

  // exit without processing the file if not audio or text
  console.log('ended with not a file of interest');
  return null;
});


// read acc data from text file and uses plotly to graph the pitch and roll
// graph saved back onto firebase under "graph.png"
function accGraph(filePath, bucketName, metadata) {
  let downloadUrl = "https://firebasestorage.googleapis.com/v0/b/" + bucketName + "/o/" + filePath + "?alt=media&token=" + metadata.firebaseStorageDownloadTokens
    const https = require('https')

    // get text file content
    https.get(downloadUrl, (res) => {
        res.setEncoding('utf8');
        res.on('data', (data) => {
            console.log('text file parsing start')
            var plotly = require('plotly')(priv.username, priv.apiKey);

            // parse the text file 
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
               x: x_array,
               y: pitch_array,
               name: 'Pitch'
             };

             var roll = {
               x: x_array, 
               y: roll_array,
               name: 'Roll'
             };

             var graph_data = [pitch, roll];
             var graphOptions = {title:"Pitch and Roll", filename: "date-axes", fileopt: "overwrite"};

             var figure = {'data': graph_data}

             // get graph from plotly
             plotly.getImage(figure, graphOptions, (err, imageStream) => {
                 if(err){
                     console.log("Pitch and roll plotting failed")
                 }

                console.log('starting upload')
                const fs = require('fs')

                var bucket = gcs.bucket(bucketName)
                const tempLocalFile = path.join(os.tmpdir(), 'graph.png');
                const tempLocalDir = path.dirname(tempLocalFile);

                // save graph on firebase
                return mkdirp(tempLocalDir).then(() => {
                    console.log('Temporary directory has been created', tempLocalDir);
                    var fileStream = fs.createWriteStream(tempLocalFile);
                    imageStream.pipe(fileStream);
                    console.log('The file has been downloaded to', tempLocalFile);
                    return bucket.upload(tempLocalFile, {
                      destination: 'graph.png',
                      metadata: {metadata: 'image/png'}, 
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


// Voice recognition using google speech API 
function voiceRec(filePath, bucketName, metadata){
  // Imports the Google Cloud client library
  const speech = require('@google-cloud/speech');

  // Creates a client
  const client = new speech.SpeechClient();


  const gcsUri = 'gs://' + bucketName + '/' + filePath;
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

  console.log('sending data to Google Speech API')

  // Detects speech in the audio file
  client
    .recognize(request)
    .then(data => {
      const response = data[0];
      const transcription = response.results
        .map(result => result.alternatives[0].transcript)
        .join('\n');
      return console.log('Transcription: ', transcription);
    })
    .catch(err => {
      console.error('ERROR:', err);
    });
}
