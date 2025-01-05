package com.hospital.lifelinkhospitals;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.hospital.lifelinkhospitals.adapters.EmergencyContactsAdapter;
import com.hospital.lifelinkhospitals.adapters.MedicationsAdapter;
import com.hospital.lifelinkhospitals.adapters.PastSurgeriesAdapter;
import com.hospital.lifelinkhospitals.adapters.InsuranceAdapter;
import com.hospital.lifelinkhospitals.api.RetrofitClient;
import com.hospital.lifelinkhospitals.model.PatientResponse;
import com.hospital.lifelinkhospitals.model.InsuranceResponse;
import com.hospital.lifelinkhospitals.Util.SessionManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.util.List;

public class PatientDetailsActivity extends AppCompatActivity {
    private TextView patientName, patientAge, patientGender, patientBloodType;
    private TextView allergiesText, dietaryRestrictionsText;
    private TextView organDonorText, culturalConsiderationsText;
    
    private RecyclerView emergencyContactsRecyclerView;
    private RecyclerView medicationsRecyclerView;
    private RecyclerView pastSurgeriesRecyclerView;
    private RecyclerView insuranceRecyclerView;
    
    private EmergencyContactsAdapter emergencyContactsAdapter;
    private MedicationsAdapter medicationsAdapter;
    private PastSurgeriesAdapter pastSurgeriesAdapter;
    private InsuranceAdapter insuranceAdapter;
    
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_patient_details);

        initializeViews();
        setupRecyclerViews();

        String userId = getIntent().getStringExtra("userId");
        if (userId != null) {
            loadPatientDetails(userId);
        } else {
            Toast.makeText(this, "User ID not provided", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void initializeViews() {
        sessionManager = new SessionManager(this);
        
        // Initialize TextViews
        patientName = findViewById(R.id.patientName);
        patientAge = findViewById(R.id.patientAge);
        patientGender = findViewById(R.id.patientGender);
        patientBloodType = findViewById(R.id.patientBloodType);
        allergiesText = findViewById(R.id.allergiesText);
        dietaryRestrictionsText = findViewById(R.id.dietaryRestrictionsText);
        organDonorText = findViewById(R.id.organDonorText);
        culturalConsiderationsText = findViewById(R.id.culturalConsiderationsText);
        
        // Initialize RecyclerViews
        emergencyContactsRecyclerView = findViewById(R.id.emergencyContactsRecyclerView);
        medicationsRecyclerView = findViewById(R.id.medicationsRecyclerView);
        pastSurgeriesRecyclerView = findViewById(R.id.pastSurgeriesRecyclerView);
        insuranceRecyclerView = findViewById(R.id.insuranceRecyclerView);

        insuranceAdapter = new InsuranceAdapter();
        insuranceRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        insuranceRecyclerView.setAdapter(insuranceAdapter);
    }

    private void setupRecyclerViews() {
        // Setup Emergency Contacts RecyclerView
        emergencyContactsAdapter = new EmergencyContactsAdapter();
        emergencyContactsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        emergencyContactsRecyclerView.setAdapter(emergencyContactsAdapter);

        // Setup Medications RecyclerView
        medicationsAdapter = new MedicationsAdapter();
        medicationsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        medicationsRecyclerView.setAdapter(medicationsAdapter);

        // Setup Past Surgeries RecyclerView
        pastSurgeriesAdapter = new PastSurgeriesAdapter();
        pastSurgeriesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        pastSurgeriesRecyclerView.setAdapter(pastSurgeriesAdapter);
    }

    private void loadPatientDetails(String userId) {
        String token = sessionManager.getToken();
        if (token == null) {
            Toast.makeText(this, "Authentication token not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String authToken = token.startsWith("Bearer ") ? token : "Bearer " + token;

        // Load patient details
        RetrofitClient.getInstance()
                .getApiService()
                .getPatientDetailsByUserId(authToken, userId)
                .enqueue(new Callback<PatientResponse>() {
                    @Override
                    public void onResponse(Call<PatientResponse> call, Response<PatientResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            updatePatientUI(response.body());
                            // Load insurance details after patient details are loaded
                            loadInsuranceDetails(userId, authToken);
                        } else {
                            Toast.makeText(PatientDetailsActivity.this,
                                    "Failed to load patient details: " + response.code(),
                                    Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<PatientResponse> call, Throwable t) {
                        Toast.makeText(PatientDetailsActivity.this,
                                "Error loading patient details: " + t.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadInsuranceDetails(String userId, String authToken) {
        RetrofitClient.getInstance()
                .getApiService()
                .getInsurancesByUserId(authToken, userId)
                .enqueue(new Callback<List<InsuranceResponse>>() {
                    @Override
                    public void onResponse(Call<List<InsuranceResponse>> call,
                                         Response<List<InsuranceResponse>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            updateInsuranceUI(response.body());
                        } else {
                            Toast.makeText(PatientDetailsActivity.this,
                                    "Failed to load insurance details: " + response.code(),
                                    Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<List<InsuranceResponse>> call, Throwable t) {
                        Toast.makeText(PatientDetailsActivity.this,
                                "Error loading insurance details: " + t.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void updateInsuranceUI(List<InsuranceResponse> insurances) {
        if (insurances != null && !insurances.isEmpty()) {
            insuranceAdapter.setInsurances(insurances);
            insuranceRecyclerView.setVisibility(View.VISIBLE);
        } else {
            insuranceRecyclerView.setVisibility(View.GONE);
            // Show "No insurance information available" message if you have a TextView for it
            TextView noInsuranceText = findViewById(R.id.noInsuranceText);
            if (noInsuranceText != null) {
                noInsuranceText.setVisibility(View.VISIBLE);
            }
        }
    }

    private void updatePatientUI(PatientResponse patient) {
        // Update basic information
        patientName.setText(patient.getFullName());
        patientAge.setText("Age: " + patient.getAge());
        patientGender.setText("Gender: " + patient.getGender());
        patientBloodType.setText("Blood Type: " + patient.getBloodType());

        // Update lists
        if (patient.getEmergencyContacts() != null) {
            emergencyContactsAdapter.setContacts(patient.getEmergencyContacts());
        }

        if (patient.getCurrentMedications() != null) {
            medicationsAdapter.setMedications(patient.getCurrentMedications());
        }

        if (patient.getPastSurgeries() != null) {
            pastSurgeriesAdapter.setSurgeries(patient.getPastSurgeries());
        }

        // Update additional information
        if (patient.getAllergies() != null && !patient.getAllergies().isEmpty()) {
            allergiesText.setText("Allergies:\n• " + TextUtils.join("\n• ", patient.getAllergies()));
        } else {
            allergiesText.setText("No known allergies");
        }

        if (patient.getDietaryRestrictions() != null && !patient.getDietaryRestrictions().isEmpty()) {
            dietaryRestrictionsText.setText("Dietary Restrictions:\n• " + 
                    TextUtils.join("\n• ", patient.getDietaryRestrictions()));
        } else {
            dietaryRestrictionsText.setText("No dietary restrictions");
        }

        organDonorText.setText("Organ Donor: " + (patient.isOrganDonor() ? "Yes" : "No"));

        if (patient.getCulturalConsiderations() != null && !patient.getCulturalConsiderations().isEmpty()) {
            culturalConsiderationsText.setText("Cultural Considerations:\n• " + 
                    TextUtils.join("\n• ", patient.getCulturalConsiderations()));
        } else {
            culturalConsiderationsText.setText("No cultural considerations specified");
        }
    }
}