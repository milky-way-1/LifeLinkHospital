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
import com.hospital.lifelinkhospitals.adapters.IncomingPatientsAdapter;
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
    private RecyclerView patientsRecyclerView;
    private IncomingPatientsAdapter patientsAdapter;
    private ProgressBar progressBar;
    private View emptyStateLayout;
    private SwipeRefreshLayout swipeRefresh;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);
        
        initializeViews();
        sessionManager = new SessionManager(this);

        // First get the hospital ID
        String userId = sessionManager.getUserId(); // Make sure you have this method in SessionManager
        if (userId != null) {
            getHospitalId(userId);
        } else {
            Toast.makeText(this, "No User ID found!", Toast.LENGTH_LONG).show();
            showEmptyState();
        }
    }

    private void initializeViews() {
        // Initialize views
        patientsRecyclerView = findViewById(R.id.patientsRecyclerView);
        progressBar = findViewById(R.id.progressBar);
        emptyStateLayout = findViewById(R.id.emptyStateLayout);
        swipeRefresh = findViewById(R.id.swipeRefresh);

        // Setup RecyclerView
        patientsAdapter = new IncomingPatientsAdapter(this);
        patientsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        patientsRecyclerView.setAdapter(patientsAdapter);

        // Setup SwipeRefreshLayout
        swipeRefresh.setOnRefreshListener(() -> {
            String hospitalId = sessionManager.getHospitalId();
            if (hospitalId != null) {
                loadIncomingPatients(hospitalId);
            }
        });

        Toast.makeText(this, "Views initialized", Toast.LENGTH_SHORT).show();
    }

    private void getHospitalId(String userId) {
        String token = sessionManager.getToken();
        if (token == null) {
            Toast.makeText(this, "No token found!", Toast.LENGTH_LONG).show();
            showEmptyState();
            return;
        }

        String authToken = token.startsWith("Bearer ") ? token : "Bearer " + token;
        showLoading();

        RetrofitClient.getInstance()
                .getApiService()
                .getHospitalId(userId, authToken)
                .enqueue(new Callback<Hospital>() {
                    @Override
                    public void onResponse(Call<Hospital> call, Response<Hospital> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            Hospital hospital = response.body();
                            String hospitalId = hospital.getId();
                            Toast.makeText(Dashboard.this, 
                                "Got Hospital ID: " + hospitalId, 
                                Toast.LENGTH_SHORT).show();
                            
                            // Save hospital ID to session
                            sessionManager.saveHospitalId(hospitalId);
                            
                            // Now load the patients
                            loadIncomingPatients(hospitalId);
                        } else {
                            Toast.makeText(Dashboard.this, 
                                "Failed to get hospital ID. Code: " + response.code(), 
                                Toast.LENGTH_LONG).show();
                            showEmptyState();
                            finishLoading();
                        }
                    }

                    @Override
                    public void onFailure(Call<Hospital> call, Throwable t) {
                        Toast.makeText(Dashboard.this, 
                            "Error getting hospital ID: " + t.getMessage(), 
                            Toast.LENGTH_LONG).show();
                        showEmptyState();
                        finishLoading();
                    }
                });
    }

    private void loadIncomingPatients(String hospitalId) {
        String token = sessionManager.getToken();
        if (token == null) {
            Toast.makeText(this, "No token found!", Toast.LENGTH_LONG).show();
            showEmptyState();
            return;
        }

        String authToken = token.startsWith("Bearer ") ? token : "Bearer " + token;
        Toast.makeText(this, "Token: " + authToken.substring(0, 20) + "...", Toast.LENGTH_SHORT).show();
        Toast.makeText(this, "Hospital ID: " + hospitalId, Toast.LENGTH_SHORT).show();

        showLoading();
        RetrofitClient.getInstance()
                .getApiService()
                .getIncomingPatients(authToken, hospitalId)
                .enqueue(new Callback<List<IncomingPatient>>() {
                    @Override
                    public void onResponse(Call<List<IncomingPatient>> call, Response<List<IncomingPatient>> response) {
                        // Print the raw response for debugging
                        try {
                            if (response.errorBody() != null) {
                                Toast.makeText(Dashboard.this, 
                                    "Error body: " + response.errorBody().string(), 
                                    Toast.LENGTH_LONG).show();
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        Toast.makeText(Dashboard.this, 
                            "Response code: " + response.code(), 
                            Toast.LENGTH_SHORT).show();

                        if (response.isSuccessful() && response.body() != null) {
                            List<IncomingPatient> patients = response.body();
                            
                            if (patients.isEmpty()) {
                                Toast.makeText(Dashboard.this, 
                                    "No patients in response", 
                                    Toast.LENGTH_LONG).show();
                                showEmptyState();
                            } else {
                                Toast.makeText(Dashboard.this, 
                                    "Received " + patients.size() + " patients", 
                                    Toast.LENGTH_SHORT).show();
                                hideEmptyState();
                                patientsAdapter.setPatients(patients);
                                for (IncomingPatient patient : patients) {
                                    loadPatientDetails(patient, authToken);
                                }
                            }
                        } else {
                            Toast.makeText(Dashboard.this, 
                                "API Error: " + response.code() + 
                                " - " + response.message(), 
                                Toast.LENGTH_LONG).show();
                            showEmptyState();
                        }
                        finishLoading();
                    }

                    @Override
                    public void onFailure(Call<List<IncomingPatient>> call, Throwable t) {
                        Toast.makeText(Dashboard.this, 
                            "Network Error: " + t.getMessage(), 
                            Toast.LENGTH_LONG).show();
                        t.printStackTrace(); // Print stack trace to logcat
                        showEmptyState();
                        finishLoading();
                    }
                });
    }

    private void showLoading() {
        runOnUiThread(() -> {
            if (progressBar != null) {
                progressBar.setVisibility(View.VISIBLE);
            }
            if (patientsRecyclerView != null) {
                patientsRecyclerView.setVisibility(View.GONE);
            }
            if (emptyStateLayout != null) {
                emptyStateLayout.setVisibility(View.GONE);
            }
        });
    }

    private void finishLoading() {
        runOnUiThread(() -> {
            if (progressBar != null) {
                progressBar.setVisibility(View.GONE);
            }
            if (swipeRefresh != null) {
                swipeRefresh.setRefreshing(false);
            }
        });
    }

    private void showEmptyState() {
        runOnUiThread(() -> {
            if (progressBar != null) {
                progressBar.setVisibility(View.GONE);
            }
            if (emptyStateLayout != null) {
                emptyStateLayout.setVisibility(View.VISIBLE);
            }
            if (patientsRecyclerView != null) {
                patientsRecyclerView.setVisibility(View.GONE);
            }
        });
    }

    private void hideEmptyState() {
        runOnUiThread(() -> {
            if (progressBar != null) {
                progressBar.setVisibility(View.GONE);
            }
            if (emptyStateLayout != null) {
                emptyStateLayout.setVisibility(View.GONE);
            }
            if (patientsRecyclerView != null) {
                patientsRecyclerView.setVisibility(View.VISIBLE);
            }
        });
    }

    // Add method to check API endpoint
    private void checkApiEndpoint() {
        String token = sessionManager.getToken();
        String hospitalId = sessionManager.getHospitalId();
        
        if (token != null && hospitalId != null) {
            Call<List<IncomingPatient>> call = RetrofitClient.getInstance()
                    .getApiService()
                    .getIncomingPatients("Bearer " + token, hospitalId);
            
            Toast.makeText(this, 
                "API URL: " + call.request().url(), 
                Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onPatientClick(IncomingPatient patient) {
        Toast.makeText(this, "Clicked on patient: " + patient.getId(), Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(this, PatientDetailsActivity.class);
        intent.putExtra("userId", patient.getUserId());
        startActivity(intent);
    }

    private void loadPatientDetails(IncomingPatient patient, String authToken) {
        RetrofitClient.getInstance()
                .getApiService()
                .getPatientDetailsByUserId(authToken, patient.getUserId())
                .enqueue(new Callback<PatientResponse>() {
                    @Override
                    public void onResponse(Call<PatientResponse> call, Response<PatientResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            Toast.makeText(Dashboard.this, 
                                "Loaded details for patient: " + patient.getUserId(), 
                                Toast.LENGTH_SHORT).show();
                            patient.setPatientDetails(response.body());
                            patientsAdapter.notifyDataSetChanged();
                            
                            // Load insurance details after patient details
                            loadPatientInsurance(patient, authToken);
                        } else {
                            Toast.makeText(Dashboard.this, 
                                "Failed to load patient details. Code: " + response.code(), 
                                Toast.LENGTH_SHORT).show();
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
                            Toast.makeText(Dashboard.this, 
                                "Loaded insurance for patient: " + patient.getUserId(), 
                                Toast.LENGTH_SHORT).show();
                            patient.setInsurances(response.body());
                            patientsAdapter.notifyDataSetChanged();
                        } else {
                            Toast.makeText(Dashboard.this, 
                                "Failed to load insurance. Code: " + response.code(), 
                                Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<List<InsuranceResponse>> call, Throwable t) {
                        Toast.makeText(Dashboard.this, 
                            "Error loading insurance: " + t.getMessage(), 
                            Toast.LENGTH_SHORT).show();
                    }
                });
    }
}