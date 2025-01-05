package com.hospital.lifelinkhospitals.model;

import com.google.gson.annotations.SerializedName;
import java.time.LocalDate;

public class InsuranceResponse {
    @SerializedName("id")
    private String id;
    
    @SerializedName("insuranceProviderName")
    private String insuranceProviderName;
    
    @SerializedName("policyNumber")
    private String policyNumber;
    
    @SerializedName("groupNumber")
    private String groupNumber;
    
    @SerializedName("insuranceType")
    private String insuranceType;
    
    @SerializedName("policyHolderName")
    private String policyHolderName;
    
    @SerializedName("relationshipToPolicyHolder")
    private String relationshipToPolicyHolder;
    
    @SerializedName("startDate")
    private LocalDate startDate;
    
    @SerializedName("endDate")
    private LocalDate endDate;
    
    @SerializedName("planType")
    private String planType;
    
    @SerializedName("coversEmergencyService")
    private boolean coversEmergencyService;
    
    @SerializedName("coversAmbulanceService")
    private boolean coversAmbulanceService;
    
    @SerializedName("createdAt")
    private String createdAt;
    
    @SerializedName("lastUpdatedAt")
    private String lastUpdatedAt;

    // Add getters for all fields
    public String getId() { return id; }
    public String getInsuranceProviderName() { return insuranceProviderName; }
    public String getPolicyNumber() { return policyNumber; }
    public String getGroupNumber() { return groupNumber; }
    public String getInsuranceType() { return insuranceType; }
    public String getPolicyHolderName() { return policyHolderName; }
    public String getRelationshipToPolicyHolder() { return relationshipToPolicyHolder; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
    public String getPlanType() { return planType; }
    public boolean isCoversEmergencyService() { return coversEmergencyService; }
    public boolean isCoversAmbulanceService() { return coversAmbulanceService; }
    public String getCreatedAt() { return createdAt; }
    public String getLastUpdatedAt() { return lastUpdatedAt; }
} 