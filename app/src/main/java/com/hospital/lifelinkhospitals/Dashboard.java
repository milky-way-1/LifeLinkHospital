package com.hospital.lifelinkhospitals;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;


import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.hospital.lifelinkhospitals.Util.SessionManager;
import com.hospital.lifelinkhospitals.api.ApiService;
import com.hospital.lifelinkhospitals.api.RetrofitClient;
import com.hospital.lifelinkhospitals.model.Hospital;
import com.hospital.lifelinkhospitals.model.IncomingPatient;


import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.button.MaterialButton;
import com.hospital.lifelinkhospitals.Util.SessionManager;
import com.hospital.lifelinkhospitals.api.RetrofitClient;
import com.hospital.lifelinkhospitals.model.Hospital;
import com.hospital.lifelinkhospitals.model.IncomingPatient;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class Dashboard extends AppCompatActivity implements IncomingPatientsAdapter.OnPatientClickListener {
    private View registeredHospitalLayout;
    private View unregisteredHospitalLayout;
    private ProgressBar hospitalStatusLoading;
    private TextView hospitalNameText;
    private TextView hospitalAddressText;
    private TextView availableBedsText;
    private MaterialButton registerHospitalButton;
    private RecyclerView incomingPatientsRecyclerView;
    private View emptyStateLayout;
    private SwipeRefreshLayout swipeRefresh;

    private SessionManager sessionManager;
    private IncomingPatientsAdapter patientsAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        initializeViews();
        setupServices();
        setupRecyclerView();
        loadHospitalData();
    }

    private void initializeViews() {
        try {
            Toast.makeText(this, "1. Starting view initialization", Toast.LENGTH_SHORT).show();

            // Initialize layouts
            registeredHospitalLayout = findViewById(R.id.registeredHospitalLayout);
            unregisteredHospitalLayout = findViewById(R.id.unregisteredHospitalLayout);
            hospitalStatusLoading = findViewById(R.id.hospitalStatusLoading);
            emptyStateLayout = findViewById(R.id.emptyStateLayout);

            // Initialize text views - Note these are inside registeredHospitalLayout
            if (registeredHospitalLayout != null) {
                hospitalNameText = registeredHospitalLayout.findViewById(R.id.hospitalNameText);
                hospitalAddressText = registeredHospitalLayout.findViewById(R.id.hospitalAddressText);
                availableBedsText = registeredHospitalLayout.findViewById(R.id.availableBedsText);
            }

            // Initialize button - Note this is inside unregisteredHospitalLayout
            if (unregisteredHospitalLayout != null) {
                registerHospitalButton = unregisteredHospitalLayout.findViewById(R.id.registerHospitalButton);
                if (registerHospitalButton != null) {
                    registerHospitalButton.setOnClickListener(v -> {
                        Intent intent = new Intent(Dashboard.this, HospitalRegistrationActivity.class);
                        startActivity(intent);
                    });
                }
            }

            // Initialize RecyclerView and SwipeRefreshLayout
            swipeRefresh = findViewById(R.id.swipeRefresh);
            if (swipeRefresh != null) {
                swipeRefresh.setOnRefreshListener(this::refreshData);
                swipeRefresh.setColorSchemeResources(R.color.primary);
            }

            incomingPatientsRecyclerView = findViewById(R.id.incomingPatientsRecyclerView);

            Toast.makeText(this, "2. View initialization complete", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            Toast.makeText(this, "Error initializing views: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void setupServices() {
        sessionManager = new SessionManager(this);
    }

    private void setupRecyclerView() {
        patientsAdapter = new IncomingPatientsAdapter(this);
        incomingPatientsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        incomingPatientsRecyclerView.setAdapter(patientsAdapter);
    }

    private void refreshData() {
        loadHospitalData();
        String hospitalId = sessionManager.getHospitalId();
        if(hospitalId != null) loadIncomingPatients(hospitalId);
    }

    private void loadHospitalData() {
        try {
            showLoading();
            String token = sessionManager.getToken();
            String userId = sessionManager.getUserId();

            Toast.makeText(this, "1. Starting loadHospitalData", Toast.LENGTH_SHORT).show();

            if (userId == null || token == null) {
                showUnregisteredState();
                finishLoading();
                Toast.makeText(this, "2. No token or userId", Toast.LENGTH_SHORT).show();
                return;
            }

            // Debug info
            Toast.makeText(this, "3. UserID: " + userId, Toast.LENGTH_SHORT).show();
            Toast.makeText(this, "4. Token exists: " + (token != null), Toast.LENGTH_SHORT).show();

            // Ensure token format
            String authToken = token.startsWith("Bearer ") ? token : "Bearer " + token;

            RetrofitClient.getInstance()
                    .getApiService()
                    .getHospitalId(userId, authToken)
                    .enqueue(new Callback<Hospital>() {
                        @Override
                        public void onResponse(Call<Hospital> call, Response<Hospital> response) {
                            try {
                                Toast.makeText(Dashboard.this,
                                        "5. Response code: " + response.code(), Toast.LENGTH_SHORT).show();

                                if (response.isSuccessful() && response.body() != null) {
                                    Hospital hospital = response.body();

                                    // Debug hospital data
                                    String debugInfo = String.format(
                                            "Hospital: %s\nAddress: %s\nBeds: %d",
                                            hospital.getHospitalName(),
                                            hospital.getAddress(),
                                            hospital.getTotalBeds()
                                    );
                                    Toast.makeText(Dashboard.this, debugInfo, Toast.LENGTH_LONG).show();

                                    // Save hospital ID
                                    sessionManager.updateHospitalData(hospital.getId());

                                    // Update UI
                                    updateHospitalInfo(hospital);
                                    loadIncomingPatients(hospital.getId());
                                } else {
                                    String errorBody = "";
                                    try {
                                        if (response.errorBody() != null) {
                                            errorBody = response.errorBody().string();
                                        }
                                    } catch (IOException e) {
                                        errorBody = "Could not read error body";
                                    }

                                    final String error = String.format(
                                            "Error %d: %s",
                                            response.code(),
                                            errorBody
                                    );

                                    runOnUiThread(() -> {
                                        Toast.makeText(Dashboard.this, error, Toast.LENGTH_LONG).show();

                                        if (response.code() == 404) {
                                            showUnregisteredState();
                                        } else if (response.code() == 401) {
                                            // Only logout for unauthorized
                                            sessionManager.logout();
                                        }
                                    });
                                }
                            } catch (Exception e) {
                                String error = "Error processing response: " + e.getMessage();
                                Toast.makeText(Dashboard.this, error, Toast.LENGTH_LONG).show();
                                e.printStackTrace(); // Log the stack trace
                            } finally {
                                finishLoading();
                            }
                        }

                        @Override
                        public void onFailure(Call<Hospital> call, Throwable t) {
                            String error = "Network error: " + t.getMessage();
                            Toast.makeText(Dashboard.this, error, Toast.LENGTH_LONG).show();
                            t.printStackTrace(); // Log the stack trace
                            showUnregisteredState();
                            finishLoading();
                        }
                    });

        } catch (Exception e) {
            String error = "Fatal error: " + e.getMessage();
            Toast.makeText(this, error, Toast.LENGTH_LONG).show();
            e.printStackTrace(); // Log the stack trace
            finishLoading();
        }
    }

    private void debugViewStates() {
        String states = String.format(
                "View States:\nLoading: %s\nRegistered: %s\nUnregistered: %s\nEmpty: %s",
                hospitalStatusLoading != null ? hospitalStatusLoading.getVisibility() : "null",
                registeredHospitalLayout != null ? registeredHospitalLayout.getVisibility() : "null",
                unregisteredHospitalLayout != null ? unregisteredHospitalLayout.getVisibility() : "null",
                emptyStateLayout != null ? emptyStateLayout.getVisibility() : "null"
        );
        Toast.makeText(this, states, Toast.LENGTH_LONG).show();
    }

    private void loadIncomingPatients(String hospitalId) {
        String token = "Bearer " + sessionManager.getToken();

        if (hospitalId == null || token == null) {
            showError("Session expired. Please login again.");
            return;
        }

        RetrofitClient.getInstance()
                .getApiService()
                .getHospitalBookings(token, hospitalId)
                .enqueue(new Callback<List<IncomingPatient>>() {
                    @Override
                    public void onResponse(Call<List<IncomingPatient>> call,
                                           Response<List<IncomingPatient>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            updateIncomingPatientsList(response.body());
                        } else {
                            handleError(response.code());
                            updateIncomingPatientsList(new ArrayList<>());
                        }
                    }

                    @Override
                    public void onFailure(Call<List<IncomingPatient>> call, Throwable t) {
                        showError("Network error: " + t.getMessage());
                        updateIncomingPatientsList(new ArrayList<>());
                    }
                });
    }

    private void updateHospitalInfo(Hospital hospital) {
        try {
            if (hospital == null) {
                Toast.makeText(this, "Hospital data is null", Toast.LENGTH_SHORT).show();
                return;
            }

            runOnUiThread(() -> {
                try {
                    // Update visibility
                    if (hospitalStatusLoading != null) {
                        hospitalStatusLoading.setVisibility(View.GONE);
                    }
                    if (registeredHospitalLayout != null) {
                        registeredHospitalLayout.setVisibility(View.VISIBLE);
                    }
                    if (unregisteredHospitalLayout != null) {
                        unregisteredHospitalLayout.setVisibility(View.GONE);
                    }

                    // Update text views
                    if (hospitalNameText != null) {
                        hospitalNameText.setText(hospital.getHospitalName());
                    } else {
                        Toast.makeText(this, "Hospital name view is null", Toast.LENGTH_SHORT).show();
                    }

                    if (hospitalAddressText != null) {
                        hospitalAddressText.setText(hospital.getAddress());
                    } else {
                        Toast.makeText(this, "Hospital address view is null", Toast.LENGTH_SHORT).show();
                    }

                    if (availableBedsText != null) {
                        availableBedsText.setText(String.format("Available Beds: %d",
                                hospital.getTotalBeds()));
                    } else {
                        Toast.makeText(this, "Available beds view is null", Toast.LENGTH_SHORT).show();
                    }

                    // Enable SwipeRefreshLayout
                    if (swipeRefresh != null) {
                        swipeRefresh.setEnabled(true);
                    }

                } catch (Exception e) {
                    Toast.makeText(Dashboard.this,
                            "Error updating UI: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                }
            });
        } catch (Exception e) {
            Toast.makeText(this,
                    "Error in updateHospitalInfo: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
        }
    }

    private void updateIncomingPatientsList(List<IncomingPatient> patients) {
        runOnUiThread(() -> {
            boolean isEmpty = patients == null || patients.isEmpty();
            emptyStateLayout.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
            incomingPatientsRecyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);

            if (!isEmpty) {
                patientsAdapter.updatePatients(patients);
            }
        });
    }


    private void showLoading() {
        runOnUiThread(() -> {
            hospitalStatusLoading.setVisibility(View.VISIBLE);
            registeredHospitalLayout.setVisibility(View.GONE);
            unregisteredHospitalLayout.setVisibility(View.GONE);
        });
    }

    private void finishLoading() {
        runOnUiThread(() -> {
            hospitalStatusLoading.setVisibility(View.GONE);
            swipeRefresh.setRefreshing(false);
        });
    }

    private void handleError(int code) {
        try {
            switch (code) {
                case 401:
                    Toast.makeText(this, "Unauthorized - Please login again", Toast.LENGTH_LONG).show();
                    sessionManager.logout();
                    break;
                case 404:
                    Toast.makeText(this, "Hospital not found", Toast.LENGTH_LONG).show();
                    showUnregisteredState();
                    break;
                default:
                    Toast.makeText(this, "Error code: " + code, Toast.LENGTH_LONG).show();
                    break;
            }
        } catch (Exception e) {
            Toast.makeText(this,
                    "Error handling error code: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
        }
    }
    private void showUnregisteredState() {
        try {
            runOnUiThread(() -> {
                try {
                    if (hospitalStatusLoading != null) {
                        hospitalStatusLoading.setVisibility(View.GONE);
                    }
                    if (registeredHospitalLayout != null) {
                        registeredHospitalLayout.setVisibility(View.GONE);
                    }
                    if (unregisteredHospitalLayout != null) {
                        unregisteredHospitalLayout.setVisibility(View.VISIBLE);
                    }
                    if (swipeRefresh != null) {
                        swipeRefresh.setEnabled(false);
                    }
                } catch (Exception e) {
                    Toast.makeText(Dashboard.this,
                            "Error updating UI state: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                }
            });
        } catch (Exception e) {
            Toast.makeText(this,
                    "Error in showUnregisteredState: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
        }
    }

    private void showError(String message) {
        runOnUiThread(() ->
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        );
    }

    @Override
    public void onPatientClick(IncomingPatient patient) {
        Intent intent = new Intent(this, PatientDetailsActivity.class);
        intent.putExtra("patient_id", patient.getId());
        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshData();
    }
}