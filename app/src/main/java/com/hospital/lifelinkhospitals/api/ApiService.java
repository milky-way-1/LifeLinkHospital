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
import retrofit2.http.Query;

public interface ApiService {
    @POST("api/auth/login")
    Call<JwtResponse> login(@Body LoginRequest request);

    @POST("api/auth/signup")
    Call<MessageResponse> signup(@Body SignupRequest request);

    @GET("api/hospital/{hospitalId}")
    Call<Hospital> getHospitalDetails(
            @Header("Authorization") String token,
            @Path("hospitalId") String hospitalId);

    @GET("api/hospital/{hospitalId}/incoming-patients")
    Call<List<IncomingPatient>> getIncomingPatients(
            @Header("Authorization") String token,
            @Path("hospitalId") String hospitalId);

    @POST("api/hospital/register")
    Call<ApiResponse<Hospital>> registerHospital(
            @Header("Authorization") String token,
            @Body HospitalRegistrationRequest request);



}
