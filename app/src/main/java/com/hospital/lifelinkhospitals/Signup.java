package com.hospital.lifelinkhospitals;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.hospital.lifelinkhospitals.Util.SessionManager;

public class Signup extends AppCompatActivity {

    private TextInputEditText fullNameInput;
    private TextInputEditText emailInput;
    private TextInputEditText passwordInput;
    private MaterialButton signupButton;
    private View loginText;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        sessionManager = new SessionManager(this);
        initializeViews();
        setupClickListeners();
    }

    private void initializeViews() {
        fullNameInput = findViewById(R.id.fullNameInput);
        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        signupButton = findViewById(R.id.signupButton);
        loginText = findViewById(R.id.loginText);
    }

    private void setupClickListeners() {
        signupButton.setOnClickListener(v -> attemptSignup());
        loginText.setOnClickListener(v -> {
            startActivity(new Intent(Signup.this, Login.class));
            finish();
        });
    }

    private void attemptSignup() {
        String fullName = fullNameInput.getText().toString().trim();
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();

        if (!validateInputs(fullName, email, password)) {
            return;
        }

        // Show loading state
        signupButton.setEnabled(false);
        signupButton.setText("Creating Account...");

        // TODO: Implement your signup API call here
        // For demonstration, we'll just simulate a delay
        new android.os.Handler().postDelayed(() -> {
            // On successful signup:
            sessionManager.saveUserSession(
                    "dummy_token",  // token
                    email,         // email
                    "user_" + System.currentTimeMillis(),  // userId
                    fullName      // userName
            );
            startActivity(new Intent(Signup.this, Dashboard.class));
            finish();

            // On failure:
            // signupButton.setEnabled(true);
            // signupButton.setText("Sign Up");
            // Toast.makeText(this, "Signup failed", Toast.LENGTH_SHORT).show();
        }, 2000);
    }

    private boolean validateInputs(String fullName, String email, String password) {
        if (fullName.isEmpty()) {
            fullNameInput.setError("Name is required");
            fullNameInput.requestFocus();
            return false;
        }

        if (email.isEmpty()) {
            emailInput.setError("Email is required");
            emailInput.requestFocus();
            return false;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailInput.setError("Please enter a valid email");
            emailInput.requestFocus();
            return false;
        }

        if (password.isEmpty()) {
            passwordInput.setError("Password is required");
            passwordInput.requestFocus();
            return false;
        }

        if (password.length() < 6) {
            passwordInput.setError("Password must be at least 6 characters");
            passwordInput.requestFocus();
            return false;
        }

        return true;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        startActivity(new Intent(Signup.this, Login.class));
        finish();
    }
}