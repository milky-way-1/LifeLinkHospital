package com.hospital.lifelinkhospitals;


import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;


import com.hospital.lifelinkhospitals.model.IncomingPatient;

import java.util.List;

public class IncomingPatientsAdapter extends RecyclerView.Adapter<IncomingPatientsAdapter.PatientViewHolder> {
    private List<IncomingPatient> patients;

    public IncomingPatientsAdapter(List<IncomingPatient> patients) {
        this.patients = patients;
    }

    @NonNull
    @Override
    public PatientViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_incoming_patient, parent, false);
        return new PatientViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PatientViewHolder holder, int position) {
        IncomingPatient patient = patients.get(position);
        holder.bind(patient);
    }

    @Override
    public int getItemCount() {
        return patients.size();
    }

    public void updatePatients(List<IncomingPatient> newPatients) {
        this.patients = newPatients;
        notifyDataSetChanged();
    }

    static class PatientViewHolder extends RecyclerView.ViewHolder {
        private final TextView patientNameText;
        private final TextView etaText;
        private final TextView statusText;

        public PatientViewHolder(@NonNull View itemView) {
            super(itemView);
            patientNameText = itemView.findViewById(R.id.patientNameText);
            etaText = itemView.findViewById(R.id.etaText);
            statusText = itemView.findViewById(R.id.statusText);
        }

        public void bind(IncomingPatient patient) {
            patientNameText.setText(patient.getName());
            etaText.setText(String.format("ETA: %d mins", patient.getEstimatedTimeInMinutes()));
            statusText.setText(patient.getStatus());
        }
    }
}
