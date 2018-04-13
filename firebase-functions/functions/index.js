/*

Functions Endpoint: https://us-central1-microp-g2.cloudfunctions.net/<function_name>


*/

const priv = require('./private');
const functions = require('firebase-functions');

 // Create and Deploy Your First Cloud Functions
 // https://firebase.google.com/docs/functions/write-firebase-functions

exports.helloWorld = functions.https.onRequest((request, response) => {
 	response.send("Hello from Firebase!");
});

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


