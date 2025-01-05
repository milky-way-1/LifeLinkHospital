package com.hospital.lifelinkhospitals;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;


import android.content.Intent;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.hospital.lifelinkhospitals.Util.SessionManager;
import com.hospital.lifelinkhospitals.api.RetrofitClient;
import com.hospital.lifelinkhospitals.model.Hospital;
import com.hospital.lifelinkhospitals.model.IncomingPatient;
import com.hospital.lifelinkhospitals.model.InsuranceResponse;
import com.hospital.lifelinkhospitals.model.PatientResponse;


import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;


public class Dashboard extends AppCompatActivity implements IncomingPatientsAdapter.OnPatientClickListener {
    private LinearLayout registeredHospitalLayout;
    private LinearLayout unregisteredHospitalLayout;
    private ProgressBar hospitalStatusLoading;
    private LinearLayout emptyStateLayout;
    private TextView hospitalNameText;
    private TextView hospitalAddressText;
    private TextView availableBedsText;
    private SwipeRefreshLayout swipeRefresh;
    private RecyclerView incomingPatientsRecyclerView;
    private MaterialButton registerHospitalButton;
    private SessionManager sessionManager;
    private IncomingPatientsAdapter patientsAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        initializeViews();
        setupSwipeRefresh();
        setupRecyclerView();

        // Initial data load
        loadHospitalData();
    }

    private void initializeViews() {
        // Initialize SessionManager first
        sessionManager = new SessionManager(this);

        // Initialize all views
        registeredHospitalLayout = findViewById(R.id.registeredHospitalLayout);
        unregisteredHospitalLayout = findViewById(R.id.unregisteredHospitalLayout);
        hospitalStatusLoading = findViewById(R.id.hospitalStatusLoading);
        emptyStateLayout = findViewById(R.id.emptyStateLayout);
        swipeRefresh = findViewById(R.id.swipeRefresh);
        incomingPatientsRecyclerView = findViewById(R.id.incomingPatientsRecyclerView);

        // Initialize TextViews inside registeredHospitalLayout
        hospitalNameText = findViewById(R.id.hospitalNameText);
        hospitalAddressText = findViewById(R.id.hospitalAddressText);
        availableBedsText = findViewById(R.id.availableBedsText);

        // Initialize and setup register button
        registerHospitalButton = findViewById(R.id.registerHospitalButton);
        if (registerHospitalButton != null) {
            registerHospitalButton.setOnClickListener(v -> {
                Intent intent = new Intent(Dashboard.this, HospitalRegistrationActivity.class);
                startActivity(intent);
            });
        }
    }

    private void setupSwipeRefresh() {
        if (swipeRefresh != null) {
            swipeRefresh.setOnRefreshListener(this::refreshData);
            swipeRefresh.setColorSchemeResources(R.color.primary);
        }
    }

    private void setupRecyclerView() {
        patientsAdapter = new IncomingPatientsAdapter(this);
        incomingPatientsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        incomingPatientsRecyclerView.setAdapter(patientsAdapter);
    }

    private void loadHospitalData() {
        try {
            showLoading();
            String token = sessionManager.getToken();
            String userId = sessionManager.getUserId();

            Toast.makeText(this, "Loading data... UserID: " + userId, Toast.LENGTH_SHORT).show();

            if (userId == null || token == null) {
                Toast.makeText(this, "Missing credentials", Toast.LENGTH_SHORT).show();
                showUnregisteredState();
                finishLoading();
                return;
            }

            // Ensure token format
            String authToken = token.startsWith("Bearer ") ? token : "Bearer " + token;

            RetrofitClient.getInstance()
                    .getApiService()
                    .getHospitalId(userId, authToken)
                    .enqueue(new Callback<Hospital>() {
                        @Override
                        public void onResponse(Call<Hospital> call, Response<Hospital> response) {
                            Toast.makeText(Dashboard.this,
                                    "Response code: " + response.code(), Toast.LENGTH_SHORT).show();

                            if (response.isSuccessful() && response.body() != null) {
                                Hospital hospital = response.body();
                                Toast.makeText(Dashboard.this,
                                        "Hospital received: " + hospital.getHospitalName(),
                                        Toast.LENGTH_SHORT).show();
                                updateHospitalInfo(hospital);
                                loadIncomingPatients(hospital.getId());
                            } else {
                                Toast.makeText(Dashboard.this,
                                        "API Error: " + response.code(),
                                        Toast.LENGTH_SHORT).show();
                                showUnregisteredState();
                            }
                            finishLoading();
                        }

                        @Override
                        public void onFailure(Call<Hospital> call, Throwable t) {
                            Toast.makeText(Dashboard.this,
                                    "Network error: " + t.getMessage(),
                                    Toast.LENGTH_SHORT).show();
                            showUnregisteredState();
                            finishLoading();
                        }
                    });

        } catch (Exception e) {
            Toast.makeText(this, "Fatal error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            showUnregisteredState();
            finishLoading();
        }
    }

    private void loadIncomingPatients(String hospitalId) {
        String token = sessionManager.getToken();
        if (token == null) return;

        String authToken = token.startsWith("Bearer ") ? token : "Bearer " + token;

        showLoading();
        RetrofitClient.getInstance()
                .getApiService()
                .getIncomingPatients(authToken, hospitalId)
                .enqueue(new Callback<List<IncomingPatient>>() {
                    @Override
                    public void onResponse(Call<List<IncomingPatient>> call, Response<List<IncomingPatient>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            List<IncomingPatient> patients = response.body();
                            // First update the list with basic info
                            updatePatientsList(patients);
                            // Then load details for each patient
                            for (IncomingPatient patient : patients) {
                                loadPatientDetails(patient, authToken);
                            }
                        } else {
                            Toast.makeText(Dashboard.this, 
                                "Failed to load patients: " + response.code(), 
                                Toast.LENGTH_SHORT).show();
                            showEmptyState();
                        }
                        finishLoading();
                    }

                    @Override
                    public void onFailure(Call<List<IncomingPatient>> call, Throwable t) {
                        Toast.makeText(Dashboard.this, 
                            "Error: " + t.getMessage(), 
                            Toast.LENGTH_SHORT).show();
                        showEmptyState();
                        finishLoading();
                    }
                });
    }

    private void loadPatientDetails(IncomingPatient patient, String authToken) {
        // Load patient details
        RetrofitClient.getInstance()
                .getApiService()
                .getPatientDetailsByUserId(authToken, patient.getUserId())
                .enqueue(new Callback<PatientResponse>() {
                    @Override
                    public void onResponse(Call<PatientResponse> call, Response<PatientResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            patient.setPatientDetails(response.body());
                            // Update the adapter to reflect the new data
                            patientsAdapter.notifyDataSetChanged();
                            
                            // Load insurance details
                            loadPatientInsurance(patient, authToken);
                        }
                    }

                    @Override
                    public void onFailure(Call<PatientResponse> call, Throwable t) {
                        Toast.makeText(Dashboard.this,
                                "Error loading patient details: " + t.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadPatientInsurance(IncomingPatient patient, String authToken) {
        RetrofitClient.getInstance()
                .getApiService()
                .getInsurancesByUserId(authToken, patient.getUserId())
                .enqueue(new Callback<List<InsuranceResponse>>() {
                    @Override
                    public void onResponse(Call<List<InsuranceResponse>> call,
                                         Response<List<InsuranceResponse>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            patient.setInsurances(response.body());
                            // Update the adapter to reflect the new data
                            patientsAdapter.notifyDataSetChanged();
                        }
                    }

                    @Override
                    public void onFailure(Call<List<InsuranceResponse>> call, Throwable t) {
                        Toast.makeText(Dashboard.this,
                                "Error loading insurance details: " + t.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void updatePatientsList(List<IncomingPatient> patients) {
        runOnUiThread(() -> {
            if (patients.isEmpty()) {
                showEmptyState();
            } else {
                hideEmptyState();
                patientsAdapter.setPatients(patients);
            }
        });
    }

    private void updateHospitalInfo(Hospital hospital) {
        if (hospital == null) {
            Toast.makeText(this, "Hospital object is null", Toast.LENGTH_SHORT).show();
            showUnregisteredState();
            return;
        }

        runOnUiThread(() -> {
            try {
                Toast.makeText(this,
                        "Updating UI with hospital: " + hospital.getHospitalName(),
                        Toast.LENGTH_SHORT).show();

                sessionManager.updateHospitalData(hospital.getId());

                if (registeredHospitalLayout == null || hospitalNameText == null ||
                        hospitalAddressText == null || availableBedsText == null) {
                    Toast.makeText(this, "One or more views are null", Toast.LENGTH_LONG).show();
                    return;
                }

                registeredHospitalLayout.setVisibility(View.VISIBLE);
                unregisteredHospitalLayout.setVisibility(View.GONE);
                hospitalStatusLoading.setVisibility(View.GONE);

                hospitalNameText.setText(hospital.getHospitalName());
                hospitalAddressText.setText(hospital.getAddress());
                availableBedsText.setText(String.format("Available Beds: %d", hospital.getTotalBeds()));

                if (swipeRefresh != null) {
                    swipeRefresh.setEnabled(true);
                }

                loadIncomingPatients(hospital.getId());

            } catch (Exception e) {
                Toast.makeText(this,
                        "Error updating UI: " + e.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showUnregisteredState() {
        runOnUiThread(() -> {
            try {
                registeredHospitalLayout.setVisibility(View.GONE);
                unregisteredHospitalLayout.setVisibility(View.VISIBLE);
                hospitalStatusLoading.setVisibility(View.GONE);
                emptyStateLayout.setVisibility(View.GONE);

                if (swipeRefresh != null) {
                    swipeRefresh.setEnabled(false);
                }
            } catch (Exception e) {
                Log.e("Dashboard", "Error showing unregistered state", e);
            }
        });
    }

    private void showEmptyState() {
        runOnUiThread(() -> {
            emptyStateLayout.setVisibility(View.VISIBLE);
            incomingPatientsRecyclerView.setVisibility(View.GONE);
        });
    }

    private void hideEmptyState() {
        runOnUiThread(() -> {
            emptyStateLayout.setVisibility(View.GONE);
            incomingPatientsRecyclerView.setVisibility(View.VISIBLE);
        });
    }

    private void showLoading() {
        runOnUiThread(() -> {
            hospitalStatusLoading.setVisibility(View.VISIBLE);
            registeredHospitalLayout.setVisibility(View.GONE);
            unregisteredHospitalLayout.setVisibility(View.GONE);
            emptyStateLayout.setVisibility(View.GONE);
        });
    }

    private void finishLoading() {
        runOnUiThread(() -> {
            hospitalStatusLoading.setVisibility(View.GONE);
            if (swipeRefresh != null) {
                swipeRefresh.setRefreshing(false);
            }
        });
    }

    private void refreshData() {
        if (swipeRefresh != null) {
            swipeRefresh.setRefreshing(true);
        }
        loadHospitalData();
    }

    private void logViewStates() {
        Log.d("Dashboard", String.format(
                "View States:\n" +
                        "Loading: %s\n" +
                        "Registered: %s\n" +
                        "Unregistered: %s\n" +
                        "Empty: %s",
                hospitalStatusLoading.getVisibility() == View.VISIBLE ? "Visible" : "Gone",
                registeredHospitalLayout.getVisibility() == View.VISIBLE ? "Visible" : "Gone",
                unregisteredHospitalLayout.getVisibility() == View.VISIBLE ? "Visible" : "Gone",
                emptyStateLayout.getVisibility() == View.VISIBLE ? "Visible" : "Gone"
        ));
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (sessionManager.isSessionValid()) {
            refreshData();
        } else {
            showUnregisteredState();
        }
    }

    @Override
    public void onPatientClick(IncomingPatient patient) {
        Intent intent = new Intent(this, PatientDetailsActivity.class);
        intent.putExtra("userId", patient.getUserId());
        startActivity(intent);
    }
}