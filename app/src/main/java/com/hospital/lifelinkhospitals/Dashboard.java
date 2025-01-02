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

public class Dashboard extends AppCompatActivity {
    private View registeredHospitalLayout;
    private View unregisteredHospitalLayout;
    private ProgressBar hospitalStatusLoading;
    private TextView hospitalNameText;
    private TextView hospitalAddressText;
    private TextView availableBedsText;
    private MaterialButton registerHospitalButton;
    private RecyclerView incomingPatientsRecyclerView;
    private View emptyStateLayout;

    private ApiService apiService;
    private SessionManager sessionManager;
    private IncomingPatientsAdapter patientsAdapter;
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

        registerHospitalButton.setOnClickListener(v -> {
            startActivity(new Intent(this, HospitalRegistrationActivity.class));
        });
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

    private void setupServices() {
        apiService = RetrofitClient.getInstance().getApiService();
        sessionManager = new SessionManager(this);
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

    private void setupRecyclerView() {
        patientsAdapter = new IncomingPatientsAdapter(new ArrayList<>());
        incomingPatientsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        incomingPatientsRecyclerView.setAdapter(patientsAdapter);
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

    private void loadIncomingPatients() {
        String hospitalId = sessionManager.getUserId();
        String token = "Bearer " + sessionManager.getToken();

        RetrofitClient.getInstance()
                .getApiService()
                .getIncomingPatients(token, hospitalId)
                .enqueue(new Callback<List<IncomingPatient>>() {
                    @Override
                    public void onResponse(Call<List<IncomingPatient>> call, Response<List<IncomingPatient>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            updateIncomingPatientsList(response.body());
                        }
                    }

                    @Override
                    public void onFailure(Call<List<IncomingPatient>> call, Throwable t) {
                        showError("Failed to load incoming patients");
                    }
                });
    }

    private void updateHospitalInfo(Hospital hospital) {
        registeredHospitalLayout.setVisibility(View.VISIBLE);
        unregisteredHospitalLayout.setVisibility(View.GONE);

        hospitalNameText.setText(hospital.getName());
        hospitalAddressText.setText(hospital.getAddress());
        availableBedsText.setText(String.format("Available Beds: %d", hospital.getAvailableBeds()));

        // Start polling only if hospital is registered
        startPolling();
    }

    private void showUnregisteredState() {
        hideLoading();
        registeredHospitalLayout.setVisibility(View.GONE);
        unregisteredHospitalLayout.setVisibility(View.VISIBLE);
        stopPolling(); // Stop polling if hospital is not registered
    }

    private void updateIncomingPatientsList(List<IncomingPatient> patients) {
        if (patients.isEmpty()) {
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
}