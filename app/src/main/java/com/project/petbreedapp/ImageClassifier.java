package com.project.petbreedapp;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.os.Trace;
import android.util.Log;

import org.tensorflow.lite.Interpreter;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

public class ImageClassifier {

    private Interpreter tflite;
    private List<String> labels;

    // Change these values according to your model
    private static final int INPUT_SIZE = 224;
    private static final int PIXEL_SIZE = 3;
    private static final int IMAGE_MEAN = 0;
    private static final float IMAGE_STD = 255.0f;
    private static final int NUM_CLASSES = 37;

    public ImageClassifier(AssetManager assetManager, String modelPath, String labelPath) throws IOException {
        MappedByteBuffer tfliteModel = loadModelFile(assetManager, modelPath);
        tflite = new Interpreter(tfliteModel);

        labels = loadLabelList(assetManager, labelPath);
    }

    public List<Recognition> classifyImage(Bitmap bitmap) {
        if (tflite == null) {
            Log.d("Error:", "model not instantiated");
            return null;
        }

        ByteBuffer imgData = convertBitmapToByteBuffer(bitmap);
        float[][] result = new float[1][NUM_CLASSES];

        Trace.beginSection("runInference");
        tflite.run(imgData, result);
        Trace.endSection();

        return getTopKLabels(result);
    }

    private MappedByteBuffer loadModelFile(AssetManager assetManager, String modelPath) throws IOException {
        AssetFileDescriptor fileDescriptor = assetManager.openFd(modelPath);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    private List<String> loadLabelList(AssetManager assetManager, String labelPath) throws IOException {
        List<String> labelList = new ArrayList<>();
        BufferedReader reader = new BufferedReader(new InputStreamReader(assetManager.open(labelPath)));
        String line;
        while ((line = reader.readLine()) != null) {
            labelList.add(line);
        }
        reader.close();
        return labelList;
    }

    private ByteBuffer convertBitmapToByteBuffer(Bitmap bitmap) {
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(4 * INPUT_SIZE * INPUT_SIZE * PIXEL_SIZE);
        byteBuffer.order(ByteOrder.nativeOrder());
        int[] intValues = new int[INPUT_SIZE * INPUT_SIZE];

        Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, true);
        resizedBitmap.getPixels(intValues, 0, resizedBitmap.getWidth(), 0, 0, resizedBitmap.getWidth(), resizedBitmap.getHeight());

        int pixel = 0;
        for (int i = 0; i < INPUT_SIZE; ++i) {
            for (int j = 0; j < INPUT_SIZE; ++j) {
                final int val = intValues[pixel++];
                // Normalize values to [0, 1] and adjust the mean and std
                byteBuffer.putFloat((val & 0xFF) / 255.0f);
                byteBuffer.putFloat(((val >> 8) & 0xFF) / 255.0f);
                byteBuffer.putFloat(((val >> 16) & 0xFF) / 255.0f);
            }
        }

        return byteBuffer;
    }



    private List<Recognition> getTopKLabels(float[][] labelProbArray) {
        List<Recognition> recognitions = new ArrayList<>();
        for (int i = 0; i < labels.size(); ++i) {
            recognitions.add(new Recognition(labels.get(i), labelProbArray[0][i]));
        }
        return recognitions;
    }

    public void close() {
        if (tflite != null) {
            tflite.close();
            tflite = null;
        }
    }
}
