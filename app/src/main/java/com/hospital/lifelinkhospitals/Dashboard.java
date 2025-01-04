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
        // Initialize layouts
        registeredHospitalLayout = findViewById(R.id.registeredHospitalLayout);
        unregisteredHospitalLayout = findViewById(R.id.unregisteredHospitalLayout);
        hospitalStatusLoading = findViewById(R.id.hospitalStatusLoading);
        emptyStateLayout = findViewById(R.id.emptyStateLayout);

        // Initialize text views
        hospitalNameText = findViewById(R.id.hospitalNameText);
        hospitalAddressText = findViewById(R.id.hospitalAddressText);
        availableBedsText = findViewById(R.id.availableBedsText);

        // Initialize buttons
        registerHospitalButton = findViewById(R.id.registerHospitalButton);
        registerHospitalButton.setOnClickListener(v ->
                startActivity(new Intent(this, HospitalRegistrationActivity.class))
        );

        // Initialize RecyclerView
        incomingPatientsRecyclerView = findViewById(R.id.incomingPatientsRecyclerView);

        // Initialize SwipeRefreshLayout
        swipeRefresh = findViewById(R.id.swipeRefresh);
        swipeRefresh.setOnRefreshListener(this::refreshData);
        swipeRefresh.setColorSchemeResources(R.color.primary);
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
        loadIncomingPatients();
    }

    private void loadHospitalData() {
        showLoading();
        String token = "Bearer " + sessionManager.getToken();
        String userId = sessionManager.getUserId();

        if (userId == null || token == null) {
            showUnregisteredState();
            finishLoading();
            return;
        }

        RetrofitClient.getInstance()
                .getApiService()
                .getHospitalId(token, userId)
                .enqueue(new Callback<Hospital>() {
                    @Override
                    public void onResponse(Call<Hospital> call, Response<Hospital> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            Hospital hospital = response.body();
                            updateHospitalInfo(hospital);
                            loadIncomingPatients();
                        } else {
                            handleError(response.code());
                        }
                        finishLoading();
                    }

                    @Override
                    public void onFailure(Call<Hospital> call, Throwable t) {
                        showError("Network error: " + t.getMessage());
                        finishLoading();
                    }
                });
    }

    private void loadIncomingPatients() {
        String token = "Bearer " + sessionManager.getToken();
        String hospitalId = sessionManager.getUserId();

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
        if (hospital == null) return;

        runOnUiThread(() -> {
            registeredHospitalLayout.setVisibility(View.VISIBLE);
            unregisteredHospitalLayout.setVisibility(View.GONE);

            hospitalNameText.setText(hospital.getName());
            hospitalAddressText.setText(hospital.getAddress());
            availableBedsText.setText(String.format("Available Beds: %d",
                    hospital.getAvailableBeds()));
        });
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

    private void handleError(int code) {
        switch (code) {
            case 401:
                showError("Session expired. Please login again.");
                // Handle logout
                break;
            case 404:
                showUnregisteredState();
                break;
            default:
                showError("Error: " + code);
                break;
        }
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

    private void showUnregisteredState() {
        runOnUiThread(() -> {
            hospitalStatusLoading.setVisibility(View.GONE);
            registeredHospitalLayout.setVisibility(View.GONE);
            unregisteredHospitalLayout.setVisibility(View.VISIBLE);
        });
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