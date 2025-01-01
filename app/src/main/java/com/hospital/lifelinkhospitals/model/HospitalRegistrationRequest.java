package com.hospital.lifelinkhospitals.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;
import java.util.ArrayList;

public class HospitalRegistrationRequest {

    @SerializedName("hospital_name")
    private String hospitalName;

    @SerializedName("hospital_type")
    private String hospitalType;

    @SerializedName("license_number")
    private String licenseNumber;

    @SerializedName("year_established")
    private String yearEstablished;

    @SerializedName("phone_number")
    private String phoneNumber;

    @SerializedName("address")
    private String address;

    @SerializedName("city")
    private String city;

    @SerializedName("state")
    private String state;

    @SerializedName("pin_code")
    private String pinCode;

    @SerializedName("latitude")
    private double latitude;

    @SerializedName("longitude")
    private double longitude;

    @SerializedName("total_beds")
    private int totalBeds;

    @SerializedName("icu_beds")
    private int icuBeds;

    @SerializedName("emergency_beds")
    private int emergencyBeds;

    @SerializedName("has_ambulance_service")
    private boolean hasAmbulanceService;

    @SerializedName("has_emergency_service")
    private boolean hasEmergencyService;

    @SerializedName("departments")
    private List<String> departments;

    public HospitalRegistrationRequest() {
        departments = new ArrayList<>();
    }

    // Builder pattern for easy object creation
    public static class Builder {
        private HospitalRegistrationRequest request;

        public Builder() {
            request = new HospitalRegistrationRequest();
        }

        public Builder hospitalName(String hospitalName) {
            request.hospitalName = hospitalName;
            return this;
        }

        public Builder hospitalType(String hospitalType) {
            request.hospitalType = hospitalType;
            return this;
        }

        public Builder licenseNumber(String licenseNumber) {
            request.licenseNumber = licenseNumber;
            return this;
        }

        public Builder yearEstablished(String yearEstablished) {
            request.yearEstablished = yearEstablished;
            return this;
        }

        public Builder phoneNumber(String phoneNumber) {
            request.phoneNumber = phoneNumber;
            return this;
        }

        public Builder address(String address) {
            request.address = address;
            return this;
        }

        public Builder city(String city) {
            request.city = city;
            return this;
        }

        public Builder state(String state) {
            request.state = state;
            return this;
        }

        public Builder pinCode(String pinCode) {
            request.pinCode = pinCode;
            return this;
        }

        public Builder location(double latitude, double longitude) {
            request.latitude = latitude;
            request.longitude = longitude;
            return this;
        }

        public Builder beds(int total, int icu, int emergency) {
            request.totalBeds = total;
            request.icuBeds = icu;
            request.emergencyBeds = emergency;
            return this;
        }

        public Builder services(boolean hasAmbulance, boolean hasEmergency) {
            request.hasAmbulanceService = hasAmbulance;
            request.hasEmergencyService = hasEmergency;
            return this;
        }

        public Builder departments(List<String> departments) {
            request.departments = departments;
            return this;
        }

        public HospitalRegistrationRequest build() {
            return request;
        }
    }
}