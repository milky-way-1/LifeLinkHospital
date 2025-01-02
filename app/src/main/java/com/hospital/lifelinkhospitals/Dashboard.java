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


import java.util.ArrayList;
import java.util.List;

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

    private SessionManager sessionManager;
    private com.hospital.lifelinkhospitals.IncomingPatientsAdapter patientsAdapter;
    private static final int POLLING_INTERVAL = 10000; // 10 seconds
    private Handler bookingCheckHandler;
    private Runnable bookingCheckRunnable;
    private boolean isPollingActive = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        initializeViews();
        setupServices();
        setupRecyclerView();
        setupBookingPolling();
        loadHospitalData();
    }

    private void initializeViews() {
        registeredHospitalLayout = findViewById(R.id.registeredHospitalLayout);
        unregisteredHospitalLayout = findViewById(R.id.unregisteredHospitalLayout);
        hospitalStatusLoading = findViewById(R.id.hospitalStatusLoading);
        hospitalNameText = findViewById(R.id.hospitalNameText);
        hospitalAddressText = findViewById(R.id.hospitalAddressText);
        availableBedsText = findViewById(R.id.availableBedsText);
        registerHospitalButton = findViewById(R.id.registerHospitalButton);
        incomingPatientsRecyclerView = findViewById(R.id.incomingPatientsRecyclerView);
        emptyStateLayout = findViewById(R.id.emptyStateLayout);

        registerHospitalButton.setOnClickListener(v ->
                startActivity(new Intent(this, HospitalRegistrationActivity.class))
        );
    }

    private void setupServices() {
        sessionManager = new SessionManager(this);
    }

    private void setupRecyclerView() {
        patientsAdapter = new IncomingPatientsAdapter(this); // Pass the click listener
        incomingPatientsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        incomingPatientsRecyclerView.setAdapter(patientsAdapter);
    }

    private void setupBookingPolling() {
        bookingCheckHandler = new Handler(Looper.getMainLooper());
        bookingCheckRunnable = new Runnable() {
            @Override
            public void run() {
                if (isPollingActive) {
                    checkForNewBookings();
                    bookingCheckHandler.postDelayed(this, POLLING_INTERVAL);
                }
            }
        };
    }

    private void checkForNewBookings() {
        String hospitalId = sessionManager.getUserId();
        String token = "Bearer " + sessionManager.getToken();

        if (hospitalId == null || token == null) {
            stopPolling();
            return;
        }

        RetrofitClient.getInstance()
                .getApiService()
                .getHospitalBookings(token, hospitalId)
                .enqueue(new Callback<List<IncomingPatient>>() {
                    @Override
                    public void onResponse(Call<List<IncomingPatient>> call, Response<List<IncomingPatient>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            updateIncomingPatientsList(response.body());
                        } else {
                            Log.e("Dashboard", "Failed to fetch bookings: " + response.code());
                        }
                    }

                    @Override
                    public void onFailure(Call<List<IncomingPatient>> call, Throwable t) {
                        Log.e("Dashboard", "Network error while fetching bookings", t);
                    }
                });
    }

    private void loadHospitalData() {
        showLoading();
        String hospitalId = sessionManager.getUserId();
        String token = "Bearer " + sessionManager.getToken();

        if (hospitalId == null) {
            showUnregisteredState();
            return;
        }

        RetrofitClient.getInstance()
                .getApiService()
                .getHospitalDetails(token, hospitalId)
                .enqueue(new Callback<Hospital>() {
                    @Override
                    public void onResponse(Call<Hospital> call, Response<Hospital> response) {
                        hideLoading();
                        if (response.isSuccessful() && response.body() != null) {
                            updateHospitalInfo(response.body());
                            loadIncomingPatients();
                        } else {
                            if (response.code() == 404) {
                                showUnregisteredState();
                            } else {
                                showError("Failed to load hospital details");
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<Hospital> call, Throwable t) {
                        hideLoading();
                        showError("Network error: " + t.getMessage());
                    }
                });
    }

    @Override
    public void onPatientClick(IncomingPatient patient) {
        Intent intent = new Intent(this, PatientDetailsActivity.class);
        intent.putExtra("patient_id", patient.getId());
        startActivity(intent);
    }

    private void startPolling() {
        if (!isPollingActive) {
            isPollingActive = true;
            bookingCheckHandler.post(bookingCheckRunnable);
            Log.d("Dashboard", "Started polling for new bookings");
        }
    }

    private void stopPolling() {
        isPollingActive = false;
        if (bookingCheckHandler != null) {
            bookingCheckHandler.removeCallbacks(bookingCheckRunnable);
            Log.d("Dashboard", "Stopped polling for new bookings");
        }
    }

    private void updateHospitalInfo(Hospital hospital) {
        registeredHospitalLayout.setVisibility(View.VISIBLE);
        unregisteredHospitalLayout.setVisibility(View.GONE);

        hospitalNameText.setText(hospital.getName());
        hospitalAddressText.setText(hospital.getAddress());
        availableBedsText.setText(String.format("Available Beds: %d", hospital.getAvailableBeds()));

        startPolling();
    }

    private void updateIncomingPatientsList(List<IncomingPatient> patients) {
        if (patients == null || patients.isEmpty()) {
            emptyStateLayout.setVisibility(View.VISIBLE);
            incomingPatientsRecyclerView.setVisibility(View.GONE);
        } else {
            emptyStateLayout.setVisibility(View.GONE);
            incomingPatientsRecyclerView.setVisibility(View.VISIBLE);
            patientsAdapter.updatePatients(patients);
        }
    }

    private void showLoading() {
        hospitalStatusLoading.setVisibility(View.VISIBLE);
        registeredHospitalLayout.setVisibility(View.GONE);
        unregisteredHospitalLayout.setVisibility(View.GONE);
    }

    private void hideLoading() {
        hospitalStatusLoading.setVisibility(View.GONE);
    }

    private void showUnregisteredState() {
        hideLoading();
        registeredHospitalLayout.setVisibility(View.GONE);
        unregisteredHospitalLayout.setVisibility(View.VISIBLE);
        stopPolling();
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadHospitalData();
        startPolling();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopPolling();
    }

    @Override
    protected void onDestroy() {
        stopPolling();
        super.onDestroy();
    }
    private void loadIncomingPatients() {
        String hospitalId = sessionManager.getUserId();
        String token = "Bearer " + sessionManager.getToken();

        if (hospitalId == null || token == null) {
            showError("Session expired. Please login again.");
            return;
        }

        RetrofitClient.getInstance()
                .getApiService()
                .getIncomingPatients(token, hospitalId)
                .enqueue(new Callback<List<IncomingPatient>>() {
                    @Override
                    public void onResponse(Call<List<IncomingPatient>> call, Response<List<IncomingPatient>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            updateIncomingPatientsList(response.body());
                        } else {
                            Log.e("Dashboard", "Failed to load incoming patients. Code: " + response.code());
                            if (response.code() == 401) {
                                showError("Session expired. Please login again.");
                                // Optionally: Handle logout or session expiry
                            } else {
                                showError("Failed to load incoming patients");
                            }
                            // Show empty state when there's an error
                            updateIncomingPatientsList(null);
                        }
                    }

                    @Override
                    public void onFailure(Call<List<IncomingPatient>> call, Throwable t) {
                        Log.e("Dashboard", "Network error loading incoming patients", t);
                        showError("Network error: " + t.getMessage());
                        // Show empty state when there's a network error
                        updateIncomingPatientsList(null);
                    }
                });
    }
}