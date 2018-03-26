package microp.android;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import android.support.annotation.NonNull;
import android.net.*;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.*;
import java.io.*;

public class MainActivity extends AppCompatActivity {
    FirebaseStorage storage = FirebaseStorage.getInstance();
    // Create a storage reference from our app
    StorageReference storageRef = storage.getReference();
    StorageReference ourFileRef = storageRef.child("boardData/testfile.mp3");
    // Create file metadata including the content type
    StorageMetadata metadata = new StorageMetadata.Builder()
            .setContentType("audio/mpeg")
            .build();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        try {// because it throws an exception
            uploadResource();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void uploadResource() throws IOException {
// the text we will upload
        String someTextToUpload = "Hellghggfhgfghfgfho world! \nthis is to test uploading data";
//convert the text to bytes
        byte[] file = someTextToUpload.getBytes();
// Now we need to use the UploadTask class to upload to our cloud
        UploadTask uploadTask = ourFileRef.putBytes(file, metadata);
        uploadTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
// Handle unsuccessful uploads
                System.out.println("Failed");
            }
        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>(){
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
// taskSnapshot.getMetadata() contains file metadata such as size, content-type, and download Uri downloadUrl = taskSnapshot.getDownloadUrl();
                System.out.println("Success");
            }
        });
    }
}
