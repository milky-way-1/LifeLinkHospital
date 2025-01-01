package com.hospital.lifelinkhospitals.api;


import com.hospital.lifelinkhospitals.model.ApiResponse;
import com.hospital.lifelinkhospitals.model.Hospital;
import com.hospital.lifelinkhospitals.model.HospitalRegistrationRequest;
import com.hospital.lifelinkhospitals.model.IncomingPatient;
import com.hospital.lifelinkhospitals.model.JwtResponse;
import com.hospital.lifelinkhospitals.model.LoginRequest;
import com.hospital.lifelinkhospitals.model.MessageResponse;
import com.hospital.lifelinkhospitals.model.SignupRequest;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface ApiService {
    @POST("api/auth/login")
    Call<JwtResponse> login(@Body LoginRequest request);

    @POST("api/auth/signup")
    Call<MessageResponse> signup(@Body SignupRequest request);

    @GET("api/hospitals/{hospitalId}")
    Call<Hospital> getHospitalDetails(@Path("hospitalId") String hospitalId);

    @GET("api/hospitals/{hospitalId}/incoming-patients")
    Call<List<IncomingPatient>> getIncomingPatients(@Path("hospitalId") String hospitalId);

    @POST("api/hospitals/register")
    Call<ApiResponse<Hospital>> registerHospital(
            @Header("Authorization") String token,
            @Body HospitalRegistrationRequest request);

}
