package com.hospital.lifelinkhospitals.model;

import com.google.gson.annotations.SerializedName;

public class IncomingPatient {
    @SerializedName("id")
    private String id;

    @SerializedName("name")
    private String name;

    @SerializedName("status")
    private String status;

    @SerializedName("estimatedTimeInMinutes")
    private int estimatedTimeInMinutes;

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public int getEstimatedTimeInMinutes() { return estimatedTimeInMinutes; }
    public void setEstimatedTimeInMinutes(int estimatedTimeInMinutes) {
        this.estimatedTimeInMinutes = estimatedTimeInMinutes;
    }
}
