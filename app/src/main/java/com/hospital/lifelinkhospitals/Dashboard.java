package com.hospital.lifelinkhospitals;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;


import android.content.Intent;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
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
            startActivity(new Intent(this, com.hospital.lifelinkhospitals.activities.HospitalRegistrationActivity.class));
        });
    }

    private void setupServices() {
        apiService = RetrofitClient.getInstance().getApiService();
        sessionManager = new SessionManager(this);
    }

    private void setupRecyclerView() {
        patientsAdapter = new IncomingPatientsAdapter(new ArrayList<>());
        incomingPatientsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        incomingPatientsRecyclerView.setAdapter(patientsAdapter);
    }

    private void loadHospitalData() {
        showLoading();
        String hospitalId = sessionManager.getUserId();

        if (hospitalId == null) {
            showUnregisteredState();
            return;
        }

        apiService.getHospitalDetails(hospitalId).enqueue(new Callback<Hospital>() {
            @Override
            public void onResponse(Call<Hospital> call, Response<Hospital> response) {
                hideLoading();
                if (response.isSuccessful() && response.body() != null) {
                    updateHospitalInfo(response.body());
                    loadIncomingPatients();
                } else {
                    showError("Failed to load hospital details");
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
        apiService.getIncomingPatients(hospitalId).enqueue(new Callback<List<IncomingPatient>>() {
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

    private void showUnregisteredState() {
        hideLoading();
        registeredHospitalLayout.setVisibility(View.GONE);
        unregisteredHospitalLayout.setVisibility(View.VISIBLE);
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
}