package com.example.combinedpls;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.combinedpls.ml.Model;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.tensorflow.lite.DataType;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class DietFragment extends Fragment {

    private ImageView imageView;
    private TextView resultTextView;
    private TextView confidenceTextView;
    private Button takePictureButton;
    private EditText foodNameEditText;
    private TextView foodDetailsTextView;

    private final int IMAGE_CAPTURE_REQUEST_CODE = 1;
    private final int CAMERA_PERMISSION_REQUEST_CODE = 101;

    private Model model;
    private int imageSize = 224;
    private String foodName = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_diet, container, false);

        imageView = view.findViewById(R.id.imageView);
        resultTextView = view.findViewById(R.id.result);
        confidenceTextView = view.findViewById(R.id.confidence);
        takePictureButton = view.findViewById(R.id.button);
        foodDetailsTextView = view.findViewById(R.id.textViewResult);

        try {
            model = Model.newInstance(requireContext());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        takePictureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                    openCamera();
                } else {
                    ActivityCompat.requestPermissions(requireActivity(), new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST_CODE);
                }
            }
        });

        return view;
    }

    private void openCamera() {
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(cameraIntent, IMAGE_CAPTURE_REQUEST_CODE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == IMAGE_CAPTURE_REQUEST_CODE && resultCode == getActivity().RESULT_OK && data != null) {
            Bitmap imageBitmap = (Bitmap) data.getExtras().get("data");
            imageView.setImageBitmap(imageBitmap);
            classifyImage(imageBitmap);
        }
    }

    private void classifyImage(Bitmap image) {
        // Resize image to match model input size
        image = ThumbnailUtils.extractThumbnail(image, imageSize, imageSize);

        // Convert image to ByteBuffer
        ByteBuffer byteBuffer = convertBitmapToByteBuffer(image);

        // Create input TensorBuffer
        TensorBuffer inputFeature0 = TensorBuffer.createFixedSize(new int[]{1, imageSize, imageSize, 3}, DataType.FLOAT32);
        inputFeature0.loadBuffer(byteBuffer);

        // Run inference
        Model.Outputs outputs = model.process(inputFeature0);
        TensorBuffer outputFeature0 = outputs.getOutputFeature0AsTensorBuffer();

        // Post-process inference results
        float[] confidences = outputFeature0.getFloatArray();
        int maxPos = 0;
        float maxConfidence = 0;
        for (int i = 0; i < confidences.length; i++) {
            if (confidences[i] > maxConfidence) {
                maxConfidence = confidences[i];
                maxPos = i;
            }
        }

        // Display results
        String[] classes = {"Idly", "Dosa", "Vadai", "Puri", "Parotta", "Apple", "Banana", "Carrot", "Orange", "Watermelon", "Fish fry", "half boiled egg", "Biriyani", "White rice", "Lemon rice", "Noodles", "Sambar", "Milk", "Laddu", "Choclate cake"};
        resultTextView.setText(classes[maxPos]);
        foodName = classes[maxPos];
        String confidenceText = "";
        for (int i = 0; i < classes.length; i++) {
            confidenceText += String.format("%s: %.1f%%\n", classes[i], confidences[i] * 100);
        }
        confidenceTextView.setText(confidenceText);
        fetchFoodData(foodName);
    }

    private ByteBuffer convertBitmapToByteBuffer(Bitmap bitmap) {
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(4 * imageSize * imageSize * 3);
        byteBuffer.order(ByteOrder.nativeOrder());
        int[] intValues = new int[imageSize * imageSize];
        bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
        int pixel = 0;
        for (int i = 0; i < imageSize; i++) {
            for (int j = 0; j < imageSize; j++) {
                int val = intValues[pixel++];
                byteBuffer.putFloat(((val >> 16) & 0xFF) * (1.f / 255.f));
                byteBuffer.putFloat(((val >> 8) & 0xFF) * (1.f / 255.f));
                byteBuffer.putFloat((val & 0xFF) * (1.f / 255.f));
            }
        }
        return byteBuffer;
    }

    private void fetchFoodData(String foodName) {
        try {
            String json = loadJSONFromAsset("food_content.json");
            if (json != null) {
                JSONObject jsonObject = new JSONObject(json);
                JSONArray foodsArray = jsonObject.getJSONArray("foods");

                for (int i = 0; i < foodsArray.length(); i++) {
                    JSONObject foodObject = foodsArray.getJSONObject(i);
                    String name = foodObject.getString("name");
                    if (name.equalsIgnoreCase(foodName)) {
                        String calories = foodObject.getString("calories");
                        String protein = foodObject.getString("protein");
                        String carbs = foodObject.getString("carbs");
                        String fat = foodObject.getString("fat");

                        String foodDetails = "Name: " + name + "\n"
                                + "Calories: " + calories + "\n"
                                + "Protein: " + protein + "\n"
                                + "Carbs: " + carbs + "\n"
                                + "Fat: " + fat;

                        foodDetailsTextView.setText(foodDetails);
                        return;
                    }
                }
                foodDetailsTextView.setText("Food not found");
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private String loadJSONFromAsset(String fileName) {
        String json = null;
        try {
            InputStream is = requireActivity().getAssets().open(fileName);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            json = new String(buffer, "UTF-8");
        } catch (IOException e) {
            e.printStackTrace();
        }
        return json;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera();
            } else {
                Toast.makeText(requireContext(), "Camera permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        model.close();
    }

}