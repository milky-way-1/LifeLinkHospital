package com.hospital.lifelinkhospitals;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.hospital.lifelinkhospitals.R;
import com.hospital.lifelinkhospitals.model.IncomingPatient;

import java.util.ArrayList;
import java.util.List;

public class IncomingPatientsAdapter extends RecyclerView.Adapter<IncomingPatientsAdapter.ViewHolder> {
    private List<IncomingPatient> patients = new ArrayList<>();
    private final OnPatientClickListener listener;

    public interface OnPatientClickListener {
        void onPatientClick(IncomingPatient patient);
    }

    public IncomingPatientsAdapter(OnPatientClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_incoming_patient, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        IncomingPatient patient = patients.get(position);
        holder.bind(patient, listener);
    }

    @Override
    public int getItemCount() {
        return patients.size();
    }

    public void updatePatients(List<IncomingPatient> newPatients) {
        this.patients = newPatients != null ? newPatients : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void addPatient(IncomingPatient patient) {
        if (patient != null) {
            this.patients.add(0, patient);
            notifyItemInserted(0);
        }
    }

    public void removePatient(String patientId) {
        for (int i = 0; i < patients.size(); i++) {
            if (patients.get(i).getId().equals(patientId)) {
                patients.remove(i);
                notifyItemRemoved(i);
                break;
            }
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final CardView cardView;
        private final TextView idText;
        private final TextView userIdText;

        ViewHolder(View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.patientCard);
            idText = itemView.findViewById(R.id.patientId);
            userIdText = itemView.findViewById(R.id.patientUserId);
        }

        void bind(IncomingPatient patient, OnPatientClickListener listener) {
            idText.setText("ID: " + patient.getId());
            userIdText.setText("User ID: " + patient.getUserId());

            cardView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onPatientClick(patient);
                }
            });
        }
    }
}