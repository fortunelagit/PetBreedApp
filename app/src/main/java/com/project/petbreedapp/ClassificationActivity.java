package com.project.petbreedapp;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;


import org.w3c.dom.Text;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;



public class ClassificationActivity extends AppCompatActivity {

    private static final int CAMERA_PIC_REQUEST = 1337;
    private static final int SELECT_PICTURE = 200;

    private ApiService apiService;

    private ImageClassifier imageClassifier;

    private ActivityResultLauncher<Intent> cameraLauncher;
    private ActivityResultLauncher<Intent> galleryLauncher;


    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.classification_activity);

        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build();


        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://fortla-56503.portmap.io:56503/breed-api/") // Replace with your base URL
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        apiService = retrofit.create(ApiService.class);

        ImageButton buttonOpenCam = (ImageButton) findViewById(R.id.btnOpenCamera);
        buttonOpenCam.setOnClickListener(op);
        ImageButton buttonUpload = (ImageButton) findViewById(R.id.btnUploadPhoto);
        buttonUpload.setOnClickListener(op);

        // Initialize the ImageClassifier in the onCreate method
        try {
            imageClassifier = new ImageClassifier(getAssets(), "model.tflite", "labels.txt");
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Initialize the ActivityResultLauncher for the camera
        cameraLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        handleCameraResult(result.getData());
                    }
                });

        // Initialize the ActivityResultLauncher for the gallery
        galleryLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        handleGalleryResult(result.getData());
                    }
                });


        // Find views
        progressBar = findViewById(R.id.pbLoading);

        // Set initial visibility
        progressBar.setVisibility(View.GONE);

        FloatingActionButton button = findViewById(R.id.btnRefresh);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ClassificationActivity.this, ClassificationActivity.class);
                startActivity(intent);
            }
        });

    }



    View.OnClickListener op = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if(view.getId() == R.id.btnOpenCamera){
                captureImage();
            }
            else if(view.getId() == R.id.btnUploadPhoto){
                imageChooser();
            }

        }
    };

    private void captureImage() {
        Intent i = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        cameraLauncher.launch(i);
    }

    private void imageChooser() {
        Intent i = new Intent();
        i.setType("image/*");
        i.setAction(Intent.ACTION_GET_CONTENT);
        galleryLauncher.launch(Intent.createChooser(i, "Select Picture"));
    }

    private void handleCameraResult(Intent data) {
        Bundle extras = data.getExtras();
        if (extras != null) {
            Bitmap image = (Bitmap) extras.get("data");

            ImageView imageview = (ImageView) findViewById(R.id.ivShowImage);
            imageview.setImageBitmap(image);

            // Save the captured image to the gallery
            saveImageToGallery(image);
            // Convert bitmap to file
            File imageFile = bitmapToFile(image);

            // Upload the file using Retrofit
            uploadFile(imageFile);
        }
    }

    private void handleGalleryResult(Intent data) {
        Uri selectedImageUri = data.getData();
        if (selectedImageUri != null) {
                ImageView imageview = (ImageView) findViewById(R.id.ivShowImage);
                imageview.setImageURI(selectedImageUri);

                // Convert URI to file
                File imageFile = uriToFile(selectedImageUri);
                // Upload the file using Retrofit
                uploadFile(imageFile);
            }
    }
//
//    private void captureImage() {
//        Intent i = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
//
//        startActivityForResult(i, CAMERA_PIC_REQUEST);
//    }
//
//    private void imageChooser() {
//        Intent i = new Intent();
//        i.setType("image/*");
//        i.setAction(Intent.ACTION_GET_CONTENT);
//
//        startActivityForResult(Intent.createChooser(i, "Select Picture"), SELECT_PICTURE);
//    }
//
//    @Override
//    protected void onDestroy() {
//        // Close the ImageClassifier when the activity is destroyed
//        if (imageClassifier != null) {
//            imageClassifier.close();
//        }
//
//        super.onDestroy();
//    }
//
//    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//        super.onActivityResult(requestCode, resultCode, data);
//        if (requestCode == CAMERA_PIC_REQUEST) {
//            Bitmap image = (Bitmap) data.getExtras().get("data");
//
//            // Classify the image using the initialized ImageClassifier
////            if (imageClassifier != null) {
////                List<Recognition> results = imageClassifier.classifyImage(image);
////
////                // Do something with the results, e.g., display in a TextView or log
////                for (Recognition result : results) {
////                    Log.d("ImageClassifier", "Label: " + result.getLabel() + ", Confidence: " + result.getConfidence());
////                }
////            }
//
//            ImageView imageview = (ImageView) findViewById(R.id.ivShowImage);
//            imageview.setImageBitmap(image);
//
//            // Save the captured image to the gallery
//            saveImageToGallery(image);
//            // Convert bitmap to file
//            File imageFile = bitmapToFile(image);
//
//            // Upload the file using Retrofit
//            uploadFile(imageFile);
//        }
//        else if(requestCode == SELECT_PICTURE){
//            Uri selectedImageUri = data.getData();
//            if (null != selectedImageUri) {
//
//                ImageView imageview = (ImageView) findViewById(R.id.ivShowImage);
//                imageview.setImageURI(selectedImageUri);
//
//                // Convert URI to file
//                File imageFile = uriToFile(selectedImageUri);
//                // Upload the file using Retrofit
//                uploadFile(imageFile);
//            }
//        }
//    }

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

        progressBar.setVisibility(View.VISIBLE);

        RequestBody requestFile = RequestBody.create(MediaType.parse("image/*"), file);
        MultipartBody.Part body = MultipartBody.Part.createFormData("file", file.getName(), requestFile);

        Call<ResponseBody> call = apiService.uploadImage(body);

        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    try {
                        String jsonResponse = response.body().string();

                        // Parse the response as a JsonElement
                        try {
                            JsonElement jsonElement = new Gson().fromJson(jsonResponse, JsonElement.class);

                            if (jsonElement.isJsonObject()) {
                                // Handle JSON object
                                JsonObject jsonObject = jsonElement.getAsJsonObject();
                                setPredictionResult(String.valueOf(jsonObject));
                            } else {
                                // Handle JSON primitive (e.g., string, number)
                                JsonPrimitive jsonPrimitive = jsonElement.getAsJsonPrimitive();
                                setPredictionResult(String.valueOf(jsonPrimitive));
                            }

                            Toast.makeText(ClassificationActivity.this, "Success", Toast.LENGTH_SHORT).show();

                        } catch (JsonSyntaxException e) {
                            // Handle the case where the response is not valid JSON
                            e.printStackTrace();
                            Toast.makeText(ClassificationActivity.this, "Invalid JSON response", Toast.LENGTH_SHORT).show();
                        }

                    } catch (IOException e) {
                        e.printStackTrace();
                        // Handle IOException (e.g., network error, failed to read response)
                        Toast.makeText(ClassificationActivity.this, "Error reading response", Toast.LENGTH_SHORT).show();
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
                    Toast.makeText(ClassificationActivity.this, "Upload failed. Check logs for details.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                // Handle failure
                String errorMessage = "Network error: " + t.getMessage();
                Toast.makeText(ClassificationActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                t.printStackTrace(); // Log the error to the console for debugging
                progressBar.setVisibility(View.GONE);
            }

        });
    }

    private void setView(TextView tv, JsonElement je){
        if (je != null && je.isJsonPrimitive()) {
            // Handle JSON primitive (e.g., string, number)
            String predictionValue = je.getAsString();
            tv.setText(predictionValue);

        } else {
            // Handle the case where "prediction" is not a valid JSON primitive
            Toast.makeText(this, "Invalid 'prediction' value", Toast.LENGTH_SHORT).show();
        }
    }

    private void setArraytoList(ListView lv, JsonElement je) {
        JsonArray jsonArray = je.getAsJsonArray();

        List<String> predictionsList = new ArrayList<>();

        for (JsonElement element : jsonArray) {
            predictionsList.add(element.getAsString());
        }

        ProbabilityAdapter adapter = new ProbabilityAdapter(this, predictionsList);
        lv.setAdapter(adapter);
    }


    private void setPredictionResult(String jsonResponse) {
        try {
            // Parse JSON using Gson
            Gson gson = new Gson();
            JsonObject jsonObject = gson.fromJson(jsonResponse, JsonObject.class);

            // Extract specific values based on keys
            JsonElement predictionElement = jsonObject.get("prediction");
            JsonElement predictionValues = jsonObject.get("values");
            JsonElement predictionTorchValues = jsonObject.get("torch_values");


            TextView tv0 = findViewById(R.id.tvText1);
            tv0.setText("According to us, above is identified as ");

            setView(findViewById(R.id.tvResult), predictionElement);

            TextView tv1 = findViewById(R.id.tvList1);
            tv1.setText("With a confidence score as follows:");

            setArraytoList(findViewById(R.id.lvPrediction), predictionValues);


            TextView tv2 = findViewById(R.id.tvList2);
            tv2.setText("But it could also be a ");
            setArraytoList(findViewById(R.id.lvTorchPrediction), predictionTorchValues);

            progressBar.setVisibility(View.GONE);

            TextView  tv = findViewById(R.id.tvInstruct);
            tv.setText("");
        } catch (JsonSyntaxException e) {
            // Handle the case where the response is not valid JSON
            e.printStackTrace();
            Toast.makeText(this, "Invalid JSON response", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            // Handle other exceptions
            e.printStackTrace();
            Toast.makeText(this, "Error processing response", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.isConnected();
        }
        return false;
    }




}