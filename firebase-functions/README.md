### Firebase Functions 

Node.js project for functions running on the firebase server. These functions are triggered by events such as file upload, delete, change. 

You will need the firebase cli to deploy new functions. Once you clone the repo, navigate to 
```
cd firebase-functions/functions
``` 

Install the node modules using: 
```
npm install
```

and then install the firebase cli using: 
```
npm install -g firebase-tools
```

You will then be able to modify the functions/index.js file. Deploy the new function using: 
```
firebase deploy
```


##### Current functions

###### - numbers: triggered on storage change event 
This function creates a copy of the file and stores it back into firebase storage as 'proc_<filename>'
TODO: implement Google Cloud Speech API and save a text file containing the recognized words on firebase. 


