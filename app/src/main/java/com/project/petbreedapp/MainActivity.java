package com.project.petbreedapp;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.media.Image;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;

public class MainActivity extends AppCompatActivity {

    private static final int CAMERA_PIC_REQUEST = 1337;
    private static final int SELECT_PICTURE = 200;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ImageButton buttonOpenCam = (ImageButton) findViewById(R.id.btnOpenCamera);
        buttonOpenCam.setOnClickListener(op);
        ImageButton buttonUpload = (ImageButton) findViewById(R.id.btnUploadPhoto);
        buttonUpload.setOnClickListener(op);
    }

    View.OnClickListener op = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if(view.getId() == R.id.btnOpenCamera){
                Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(cameraIntent, CAMERA_PIC_REQUEST);
            }
            else if(view.getId() == R.id.btnUploadPhoto){
                imageChooser();
            }

        }
    };

    private void imageChooser() {
        Intent i = new Intent();
        i.setType("image/*");
        i.setAction(Intent.ACTION_GET_CONTENT);

        startActivityForResult(Intent.createChooser(i, "Select Picture"), SELECT_PICTURE);
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CAMERA_PIC_REQUEST) {
            Bitmap image = (Bitmap) data.getExtras().get("data"); //sets imageview as the bitmapoadPhoto);
            ImageView imageview = (ImageView) findViewById(R.id.ivShowImage);
            imageview.setImageBitmap(image);
        }
        else if(requestCode == SELECT_PICTURE){
            Uri selectedImageUri = data.getData();
            if (null != selectedImageUri) {
                ImageView imageview = (ImageView) findViewById(R.id.ivShowImage);
                imageview.setImageURI(selectedImageUri);
            }
        }
    }

}