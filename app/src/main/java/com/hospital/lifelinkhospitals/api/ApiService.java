package com.hospital.lifelinkhospitals.api;


import com.hospital.lifelinkhospitals.model.JwtResponse;
import com.hospital.lifelinkhospitals.model.LoginRequest;
import com.hospital.lifelinkhospitals.model.MessageResponse;
import com.hospital.lifelinkhospitals.model.SignupRequest;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface ApiService {
    @POST("api/auth/login")
    Call<JwtResponse> login(@Body LoginRequest request);

    @POST("api/auth/signup")
    Call<MessageResponse> signup(@Body SignupRequest request);
}