package com.example.siil_mobile;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Header;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

public class DashboardActivity extends AppCompatActivity {
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final String TAG = "DashboardActivity";
    private Retrofit retrofit;
    private UserApiService userApiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        retrofit = new Retrofit.Builder()
                .baseUrl("http://192.168.1.125:8080/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        userApiService = retrofit.create(UserApiService.class);

        Button takePhotoButton = findViewById(R.id.button_take_photo);
        takePhotoButton.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(DashboardActivity.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(DashboardActivity.this, new String[]{Manifest.permission.CAMERA}, REQUEST_IMAGE_CAPTURE);
            } else {
                dispatchTakePictureIntent();
            }
        });
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
    }

    @SuppressLint("MissingSuperCall")
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            Bitmap imageBitmap = (Bitmap) extras.get("data");
            File imageFile = saveImageToFile(imageBitmap);

            SharedPreferences prefs = getSharedPreferences("userPrefs", MODE_PRIVATE);
            String jwtToken = prefs.getString("jwtToken", "");
            Log.d(TAG, "Retrieved JWT Token: " + jwtToken);

            if (jwtToken.isEmpty()) {
                Log.e(TAG, "JWT Token is empty. Cannot proceed with image upload.");
                return;
            }

            RequestBody reqFile = RequestBody.create(MediaType.parse("image/jpeg"), imageFile);
            MultipartBody.Part body = MultipartBody.Part.createFormData("image", imageFile.getName(), reqFile);
            Log.d(TAG, "Image captured. Preparing to upload...");
            // Utilisation du JWT token dans la requête
            Call<ApiResponse> call = userApiService.uploadImage("Bearer " + jwtToken, body);

            call.enqueue(new Callback<ApiResponse>() {
                @Override
                public void onResponse(@NonNull Call<ApiResponse> call, @NonNull Response<ApiResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        Log.d(TAG, "Upload success: " + response.body().getMessage());
                        Toast.makeText(DashboardActivity.this, "Image uploaded!", Toast.LENGTH_SHORT).show();
                    } else {
                        try {
                            if (response.errorBody() != null) {
                                Log.e(TAG, "Error body: " + response.errorBody().string());
                            } else {
                                Log.e(TAG, "Error body is null");
                            }
                        } catch (IOException e) {
                            Log.e(TAG, "Exception when reading error body", e);
                        }
                        Toast.makeText(DashboardActivity.this, "Upload failed: " + response.code(), Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<ApiResponse> call, Throwable t) {
                    Log.e(TAG, "Upload failed: " + t.getMessage(), t);
                    Toast.makeText(DashboardActivity.this, "Upload failed: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private File saveImageToFile(Bitmap bitmap) {
        File photoFile = null;
        try {
            photoFile = createImageFile();
            try (FileOutputStream fos = new FileOutputStream(photoFile)) {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            }
        } catch (IOException ex) {
            Log.e(TAG, "Error writing bitmap", ex);
        }
        return photoFile;
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return File.createTempFile(
                imageFileName,
                ".jpg",
                storageDir
        );
    }

    public interface UserApiService {
        @Multipart
        @POST("/api/images/upload")
        Call<ApiResponse> uploadImage(@Header("Authorization") String authToken, @Part MultipartBody.Part file);
    }


    public static class ApiResponse {
        private String message;

        // Getter et setter pour message si nécessaire
        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_IMAGE_CAPTURE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                dispatchTakePictureIntent();
            } else {
                Toast.makeText(this, "Camera permission is required to use camera", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
