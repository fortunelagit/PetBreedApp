package com.project.petbreedapp;

import androidx.appcompat.app.AppCompatActivity;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.media.Image;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import org.tensorflow.lite.task.vision.classifier.Classifications;
import org.tensorflow.lite.task.vision.classifier.ImageClassifier;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

//import java.io.BufferedReader;
//import java.io.ByteArrayOutputStream;
//import java.io.DataOutputStream;
//import java.io.IOException;
//import java.io.InputStream;
//import java.io.InputStreamReader;
//import java.net.HttpURLConnection;
//import java.net.URL;
//import android.provider.MediaStore;
//import android.util.Base64;
//import android.widget.TextView;
//import android.widget.Toast;


public class MainActivity extends AppCompatActivity {

    private static final int CAMERA_PIC_REQUEST = 1337;
    private static final int SELECT_PICTURE = 200;

    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://10.0.2.2:5000") // Replace with your base URL
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        apiService = retrofit.create(ApiService.class);

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
            Bitmap image = (Bitmap) data.getExtras().get("data");

            ImageView imageview = (ImageView) findViewById(R.id.ivShowImage);
            imageview.setImageBitmap(image);

            // Save the captured image to the gallery
            saveImageToGallery(image);
//            // Convert bitmap to file
//            File imageFile = bitmapToFile(image);
//
//            // Upload the file using Retrofit
//            uploadFile(imageFile);
        }
        else if(requestCode == SELECT_PICTURE){
            Uri selectedImageUri = data.getData();
            if (null != selectedImageUri) {

                ImageView imageview = (ImageView) findViewById(R.id.ivShowImage);
                imageview.setImageURI(selectedImageUri);

                // Convert URI to file
                File imageFile = uriToFile(selectedImageUri);
                // Upload the file using Retrofit
                uploadFile(imageFile);
            }
        }
    }

//    private void classifyImage(Bitmap image){
//
//    }

    private File bitmapToFile(Bitmap bitmap) {
        try {
            File file = new File(getCacheDir(), "image.jpg");
            file.createNewFile();

            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream);
            byte[] bitmapData = byteArrayOutputStream.toByteArray();

            FileOutputStream fileOutputStream = new FileOutputStream(file);
            fileOutputStream.write(bitmapData);
            fileOutputStream.flush();
            fileOutputStream.close();

            return file;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private File uriToFile(Uri uri) {
        String[] projection = {MediaStore.Images.Media.DATA};
        Cursor cursor = getContentResolver().query(uri, projection, null, null, null);
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        String filePath = cursor.getString(column_index);
        cursor.close();
        return new File(filePath);
    }

    private void saveImageToGallery(Bitmap bitmap) {
        // Use MediaStore API to insert the image into the device's MediaStore
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        Uri uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

        try {
            OutputStream outputStream = getContentResolver().openOutputStream(uri);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
            if (outputStream != null) {
                outputStream.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void uploadFile(File file) {

        if (!isNetworkAvailable()) {
            Toast.makeText(this, "No internet connection", Toast.LENGTH_SHORT).show();
            return;
        }
        RequestBody requestFile = RequestBody.create(MediaType.parse("image/*"), file);
        MultipartBody.Part body = MultipartBody.Part.createFormData("file", file.getName(), requestFile);

        Call<ResponseBody> call = apiService.uploadImage(body);

        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    // Handle success
                    try {
                        String jsonResponse = response.body().string();
                        // Process the response as needed

                        setPredictionResult(jsonResponse);

                        Toast.makeText(MainActivity.this, "Success", Toast.LENGTH_SHORT).show();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    // Handle error
                    String errorBody = "";
                    try {
                        errorBody = response.errorBody().string();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    Log.e("Upload Error", "Code: " + response.code() + ", Message: " + response.message() + ", Body: " + errorBody);
                    Toast.makeText(MainActivity.this, "Upload failed. Check logs for details.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                // Handle failure
                String errorMessage = "Network error: " + t.getMessage();
                Toast.makeText(MainActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                t.printStackTrace(); // Log the error to the console for debugging
            }

        });
    }

    private void setPredictionResult(String jsonResponse){

        // Parse JSON using Gson
        Gson gson = new Gson();
        JsonObject jsonObject = gson.fromJson(jsonResponse, JsonObject.class);

        // Extract specific values based on keys
        String predictionValue = jsonObject.get("prediction").getAsString();

        // Display specific information in a TextView
        String displayText = predictionValue;
        TextView result = findViewById(R.id.tvResult);
        result.setText(displayText);
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.isConnected();
        }
        return false;
    }
//    private String bitmapToBase64(Bitmap bitmap) {
//        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
//        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream);
//        byte[] byteArray = byteArrayOutputStream.toByteArray();
//        return Base64.encodeToString(byteArray, Base64.DEFAULT);
//    }
//
//    private void sendImageToServer(final String imageBase64) {
//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                try {
//                    URL url = new URL("http://127.0.0.1:5000");
//                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
//
//                    connection.setRequestMethod("POST");
//                    connection.setDoOutput(true);
//
//                    DataOutputStream outputStream = new DataOutputStream(connection.getOutputStream());
//                    outputStream.writeBytes("file=" + imageBase64);
//                    outputStream.flush();
//                    outputStream.close();
//
//                    int responseCode = connection.getResponseCode();
//
//                    if (responseCode == HttpURLConnection.HTTP_OK) {
//                        Toast.makeText(getBaseContext(), "Success", Toast.LENGTH_SHORT).show();
//                        InputStream inputStream = connection.getInputStream();
//
//                        String response = convertInputStreamToString(inputStream);
//
//                        // Process the response string as needed
//                        processResponse(response);
//                    } else {
//                        Toast.makeText(getBaseContext(), "Failed", Toast.LENGTH_SHORT).show();
//                        TextView result = (TextView) findViewById(R.id.tvResult);
//                        result.setText("FAILED");
//                    }
//
//                    connection.disconnect();
//                } catch (Exception e) {
//                    e.printStackTrace();
//                    TextView result = (TextView) findViewById(R.id.tvResult);
//                    result.setText("CATCH");
//                }
//            }
//        }).start();
//    }
//
//    private String convertInputStreamToString(InputStream inputStream) throws IOException {
//        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
//        StringBuilder stringBuilder = new StringBuilder();
//        String line;
//
//        while ((line = bufferedReader.readLine()) != null) {
//            stringBuilder.append(line).append("\n");
//        }
//
//        bufferedReader.close();
//        return stringBuilder.toString();
//    }
//
//    private void processResponse(String response) {
//        // Add your logic to process the response string here
//        // For example, you can parse JSON or perform other actions based on the server's response
//        // This method will be called after successfully receiving the response from the server
//
//
//        TextView result = (TextView) findViewById(R.id.tvResult);
//        result.setText(response);
//    }

}