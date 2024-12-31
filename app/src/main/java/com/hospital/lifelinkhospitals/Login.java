package com.hospital.lifelinkhospitals;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.hospital.lifelinkhospitals.Util.SessionManager;

public class Login extends AppCompatActivity {

    private TextInputEditText emailInput;
    private TextInputEditText passwordInput;
    private MaterialButton loginButton;
    private View signupText;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        sessionManager = new SessionManager(this);
        initializeViews();
        setupClickListeners();
    }

    private void initializeViews() {
        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        loginButton = findViewById(R.id.loginButton);
        signupText = findViewById(R.id.signupText);
    }

    private void setupClickListeners() {
        loginButton.setOnClickListener(v -> attemptLogin());
        signupText.setOnClickListener(v -> {
            startActivity(new Intent(Login.this, Signup.class));
        });
    }

    private void attemptLogin() {
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show loading state
        loginButton.setEnabled(false);
        loginButton.setText("Logging in...");

        // TODO: Implement your login API call here
        // For demonstration, we'll just simulate a delay
        new android.os.Handler().postDelayed(() -> {
            // On successful login:
            sessionManager.saveUserSession(
                    "dummy_token",  // token
                    email,         // email
                    "user_123",    // userId
                    "John Doe"     // userName
            );
            startActivity(new Intent(Login.this, Dashboard.class));
            finish();

            // On failure:
            // loginButton.setEnabled(true);
            // loginButton.setText("Login");
            // Toast.makeText(this, "Login failed", Toast.LENGTH_SHORT).show();
        }, 2000);
    }
}