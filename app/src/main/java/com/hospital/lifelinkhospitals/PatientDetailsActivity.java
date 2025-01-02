package com.hospital.lifelinkhospitals;


import android.app.AlertDialog;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.hospital.lifelinkhospitals.Util.SessionManager;
import com.hospital.lifelinkhospitals.api.RetrofitClient;
import com.hospital.lifelinkhospitals.model.BloodRequest;
import com.hospital.lifelinkhospitals.model.PatientDetails;


import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PatientDetailsActivity extends AppCompatActivity {
    private TextView nameText, ageText, genderText, bloodTypeText, heightText, weightText;
    private TextView organDonorText;
    private RecyclerView emergencyContactsRecycler, pastSurgeriesRecycler, medicationsRecycler;
    private ChipGroup medicalHistoryChips, allergiesChips, dietaryRestrictionsChips, culturalConsiderationsChips;
    private Button requestBloodButton;
    private ProgressBar progressBar;
    private SessionManager sessionManager;

    private EmergencyContactAdapter emergencyContactAdapter;
    private PastSurgeryAdapter pastSurgeryAdapter;
    private MedicationAdapter medicationAdapter;
    private PatientDetails patientDetails;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_patient_details);

        // Enable back button in action bar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Patient Details");
        }

        sessionManager = new SessionManager(this);
        initViews();
        setupRecyclerViews();

        String patientId = getIntent().getStringExtra("patient_id");
        if (patientId != null) {
            fetchPatientDetails(patientId);
        } else {
            showError("Patient ID not found");
            finish();
        }
    }

    private void initViews() {
        // Basic Information
        nameText = findViewById(R.id.patientName);
        ageText = findViewById(R.id.patientAge);
        genderText = findViewById(R.id.patientGender);
        bloodTypeText = findViewById(R.id.patientBloodType);
        heightText = findViewById(R.id.patientHeight);
        weightText = findViewById(R.id.patientWeight);
        organDonorText = findViewById(R.id.organDonorText);

        // RecyclerViews
        emergencyContactsRecycler = findViewById(R.id.emergencyContactsRecycler);
        pastSurgeriesRecycler = findViewById(R.id.pastSurgeriesRecycler);
        medicationsRecycler = findViewById(R.id.medicationsRecycler);

        // ChipGroups
        medicalHistoryChips = findViewById(R.id.medicalHistoryChips);
        allergiesChips = findViewById(R.id.allergiesChips);
        dietaryRestrictionsChips = findViewById(R.id.dietaryRestrictionsChips);
        culturalConsiderationsChips = findViewById(R.id.culturalConsiderationsChips);

        // Button and ProgressBar
        requestBloodButton = findViewById(R.id.requestBloodButton);
        progressBar = findViewById(R.id.progressBar);

        requestBloodButton.setOnClickListener(v -> showBloodRequestDialog());
    }

    private void setupRecyclerViews() {
        // Emergency Contacts RecyclerView
        emergencyContactAdapter = new EmergencyContactAdapter();
        emergencyContactsRecycler.setAdapter(emergencyContactAdapter);
        emergencyContactsRecycler.setLayoutManager(new LinearLayoutManager(this));
        emergencyContactsRecycler.setNestedScrollingEnabled(false);

        // Past Surgeries RecyclerView
        pastSurgeryAdapter = new PastSurgeryAdapter();
        pastSurgeriesRecycler.setAdapter(pastSurgeryAdapter);
        pastSurgeriesRecycler.setLayoutManager(new LinearLayoutManager(this));
        pastSurgeriesRecycler.setNestedScrollingEnabled(false);

        // Medications RecyclerView
        medicationAdapter = new MedicationAdapter();
        medicationsRecycler.setAdapter(medicationAdapter);
        medicationsRecycler.setLayoutManager(new LinearLayoutManager(this));
        medicationsRecycler.setNestedScrollingEnabled(false);
    }

    private void fetchPatientDetails(String patientId) {
        showLoading(true);
        String token = "Bearer " + sessionManager.getToken();

        RetrofitClient.getInstance()
                .getApiService()
                .getPatientDetails(token, patientId)
                .enqueue(new Callback<PatientDetails>() {
                    @Override
                    public void onResponse(Call<PatientDetails> call, Response<PatientDetails> response) {
                        showLoading(false);
                        if (response.isSuccessful() && response.body() != null) {
                            patientDetails = response.body();
                            updateUI(patientDetails);
                        } else {
                            showError("Failed to fetch patient details: " +
                                    (response.errorBody() != null ? response.errorBody().toString() : "Unknown error"));
                        }
                    }

                    @Override
                    public void onFailure(Call<PatientDetails> call, Throwable t) {
                        showLoading(false);
                        showError("Network error: " + t.getMessage());
                    }
                });
    }

    private void updateUI(PatientDetails details) {
        try {
            // Basic Information
            nameText.setText(details.getFullName());
            ageText.setText(getString(R.string.age_format, details.getAge()));
            genderText.setText(details.getGender().toString());
            bloodTypeText.setText(details.getBloodType().toString());
            heightText.setText(getString(R.string.height_format, details.getHeight()));
            weightText.setText(getString(R.string.weight_format, details.getWeight()));
            organDonorText.setText(getString(R.string.organ_donor_format,
                    details.isOrganDonor() ? "Yes" : "No"));

            // Emergency Contacts
            if (details.getEmergencyContacts() != null && !details.getEmergencyContacts().isEmpty()) {
                emergencyContactsRecycler.setVisibility(View.VISIBLE);
                emergencyContactAdapter.setContacts(details.getEmergencyContacts());
            } else {
                emergencyContactsRecycler.setVisibility(View.GONE);
            }

            // Medical History
            if (details.getMedicalHistory() != null && !details.getMedicalHistory().isEmpty()) {
                updateChipGroup(medicalHistoryChips, details.getMedicalHistory());
                medicalHistoryChips.setVisibility(View.VISIBLE);
            } else {
                medicalHistoryChips.setVisibility(View.GONE);
            }

            // Past Surgeries
            if (details.getPastSurgeries() != null && !details.getPastSurgeries().isEmpty()) {
                pastSurgeriesRecycler.setVisibility(View.VISIBLE);
                pastSurgeryAdapter.setSurgeries(details.getPastSurgeries());
            } else {
                pastSurgeriesRecycler.setVisibility(View.GONE);
            }

            // Current Medications
            if (details.getCurrentMedications() != null && !details.getCurrentMedications().isEmpty()) {
                medicationsRecycler.setVisibility(View.VISIBLE);
                medicationAdapter.setMedications(details.getCurrentMedications());
            } else {
                medicationsRecycler.setVisibility(View.GONE);
            }

            // Allergies
            if (details.getAllergies() != null && !details.getAllergies().isEmpty()) {
                updateChipGroup(allergiesChips, details.getAllergies());
                allergiesChips.setVisibility(View.VISIBLE);
            } else {
                allergiesChips.setVisibility(View.GONE);
            }

            // Dietary Restrictions
            if (details.getDietaryRestrictions() != null && !details.getDietaryRestrictions().isEmpty()) {
                updateChipGroup(dietaryRestrictionsChips, details.getDietaryRestrictions());
                dietaryRestrictionsChips.setVisibility(View.VISIBLE);
            } else {
                dietaryRestrictionsChips.setVisibility(View.GONE);
            }

            // Cultural Considerations
            if (details.getCulturalConsiderations() != null && !details.getCulturalConsiderations().isEmpty()) {
                updateChipGroup(culturalConsiderationsChips, details.getCulturalConsiderations());
                culturalConsiderationsChips.setVisibility(View.VISIBLE);
            } else {
                culturalConsiderationsChips.setVisibility(View.GONE);
            }
        } catch (Exception e) {
            showError("Error updating UI: " + e.getMessage());
        }
    }

    private void updateChipGroup(ChipGroup chipGroup, List<String> items) {
        chipGroup.removeAllViews();
        for (String item : items) {
            Chip chip = new Chip(this);
            chip.setText(item);
            chip.setClickable(false);
            chip.setCheckable(false);
            chipGroup.addView(chip);
        }
    }

    private void showBloodRequestDialog() {
        if (patientDetails == null) {
            showError("Patient details not available");
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Blood Request")
                .setMessage("Do you want to create a blood request for " +
                        patientDetails.getFullName() + " (Blood Type: " +
                        patientDetails.getBloodType() + ")?")
                .setPositiveButton("Yes", (dialog, which) -> createBloodRequest())
                .setNegativeButton("No", null)
                .show();
    }

    private void createBloodRequest() {
        showLoading(true);
        String token = "Bearer " + sessionManager.getToken();

        BloodRequest requestDTO = new BloodRequest(
                sessionManager.getUserId(),
                patientDetails.getId(),
                patientDetails.getBloodType(),
                "PENDING"
        );

        RetrofitClient.getInstance()
                .getApiService()
                .createBloodRequest(token, requestDTO)
                .enqueue(new Callback<BloodRequest>() {
                    @Override
                    public void onResponse(Call<BloodRequest> call, Response<BloodRequest> response) {
                        showLoading(false);
                        if (response.isSuccessful()) {
                            showSuccess("Blood request created successfully");
                        } else {
                            showError("Failed to create blood request: " +
                                    (response.errorBody() != null ? response.errorBody().toString() : "Unknown error"));
                        }
                    }

                    @Override
                    public void onFailure(Call<BloodRequest> call, Throwable t) {
                        showLoading(false);
                        showError("Network error: " + t.getMessage());
                    }
                });
    }

    private void showLoading(boolean show) {
        if (progressBar != null) {
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        if (requestBloodButton != null) {
            requestBloodButton.setEnabled(!show);
        }
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private void showSuccess(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        setResult(RESULT_OK);
        finish();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}