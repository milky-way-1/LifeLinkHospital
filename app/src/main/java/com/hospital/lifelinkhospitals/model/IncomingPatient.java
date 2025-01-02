package com.hospital.lifelinkhospitals.model;

import com.google.gson.annotations.SerializedName;

public class IncomingPatient {
    @SerializedName("id")
    private String id;

    @SerializedName("userId")
    private String userId;

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }


}
