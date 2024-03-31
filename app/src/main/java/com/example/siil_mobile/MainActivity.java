package com.example.siil_mobile;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.siil_mobile.api.UserApiService;
import com.example.siil_mobile.model.JwtRequest;
import com.example.siil_mobile.model.JwtResponse;

import java.io.IOException;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private UserApiService userApiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://192.168.1.125:8080/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        userApiService = retrofit.create(UserApiService.class);

        Button buttonLogin = findViewById(R.id.buttonLogin);
        buttonLogin.setOnClickListener(view -> {
            EditText editTextEmail = findViewById(R.id.editTextEmail);
            EditText editTextPassword = findViewById(R.id.editTextPassword);
            String email = editTextEmail.getText().toString();
            String password = editTextPassword.getText().toString();
            performLogin(email, password);
        });
    }

    private void performLogin(String email, String password) {
        JwtRequest request = new JwtRequest();
        request.setUsername(email);
        request.setPassword(password);

        Log.d(TAG, "Attempting login...");

        userApiService.authenticate(request).enqueue(new Callback<JwtResponse>() {
            @Override
            public void onResponse(@NonNull Call<JwtResponse> call, @NonNull Response<JwtResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String jwtToken = response.body().getJwtToken();
                    Log.d(TAG, "Login success. JWT Token: " + jwtToken);

                    SharedPreferences.Editor editor = getSharedPreferences("userPrefs", MODE_PRIVATE).edit();
                    editor.putString("jwtToken", jwtToken);
                    editor.apply();

                    Log.d(TAG, "JWT Token saved successfully.");

                    Intent intent = new Intent(MainActivity.this, DashboardActivity.class);
                    startActivity(intent);
                } else {
                    String errorBody = null;
                    try {
                        errorBody = response.errorBody() != null ? response.errorBody().string() : "null";
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    Log.d(TAG, "Error body: " + errorBody);
                    Toast.makeText(MainActivity.this, "Login failed: " + errorBody, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<JwtResponse> call, @NonNull Throwable t) {
                Log.e(TAG, "Login request failed", t);
                Toast.makeText(MainActivity.this, "Login failed: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}
