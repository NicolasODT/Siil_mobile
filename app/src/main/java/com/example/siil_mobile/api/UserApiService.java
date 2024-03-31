package com.example.siil_mobile.api;

import com.example.siil_mobile.model.JwtRequest;
import com.example.siil_mobile.model.JwtResponse;

import retrofit2.http.POST;
import retrofit2.Call;
import retrofit2.http.Body;

public interface UserApiService {
    @POST("/api/utilisateurs/authenticate")
    Call<JwtResponse> authenticate(@Body JwtRequest authRequest);
}
