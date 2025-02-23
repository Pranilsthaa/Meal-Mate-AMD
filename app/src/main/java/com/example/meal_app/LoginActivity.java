package com.example.meal_app;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.meal_app.databinding.ActivityLoginBinding;
import com.google.firebase.firestore.FirebaseFirestore;

import com.example.meal_app.utils.SessionManager;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private FirebaseFirestore db;
    private SessionManager sessionManager;
    private static final String TAG = "LoginActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = FirebaseFirestore.getInstance();
        sessionManager = new SessionManager(this);

        // Check if user is already logged in
        if (sessionManager.isLoggedIn()) {
            startActivity(new Intent(LoginActivity.this, MainActivity.class));
            finish();
            return;
        }

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        setupClickListeners();
        setupTouchListener();
    }

    private void setupTouchListener() {
        // Set up touch listener for non-text box views to hide keyboard
        View mainContainer = binding.getRoot();
        mainContainer.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                View focusedView = getCurrentFocus();
                if (focusedView != null) {
                    InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(focusedView.getWindowToken(), 0);
                    focusedView.clearFocus();
                }
            }
            return false;
        });
    }

    private void setupClickListeners() {
        binding.loginButton.setOnClickListener(v -> handleLogin());
        binding.signupText.setOnClickListener(v -> {
            startActivity(new Intent(this, SignupActivity.class));
            finish();
        });
    }

    private void handleLogin() {
        String email = binding.usernameInput.getText().toString().trim();
        String password = binding.passwordInput.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        binding.loginButton.setEnabled(false);
        binding.loginButton.setText("Logging in...");

        String userId = email.replace("@", "_").replace(".", "_");

        db.collection("user_detail")
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String storedPassword = documentSnapshot.getString("password");
                        if (password.equals(storedPassword)) {
                            // Create login session
                            sessionManager.createLoginSession(userId, email);

                            Toast.makeText(LoginActivity.this,
                                    "Login successful!",
                                    Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(LoginActivity.this, MainActivity.class));
                            finish();
                        } else {
                            Toast.makeText(LoginActivity.this,
                                    "Invalid password",
                                    Toast.LENGTH_SHORT).show();
                            binding.loginButton.setEnabled(true);
                            binding.loginButton.setText("Log In");
                        }
                    } else {
                        Toast.makeText(LoginActivity.this,
                                "User not found",
                                Toast.LENGTH_SHORT).show();
                        binding.loginButton.setEnabled(true);
                        binding.loginButton.setText("Log In");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error checking user credentials", e);
                    Toast.makeText(LoginActivity.this,
                            "Error: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                    binding.loginButton.setEnabled(true);
                    binding.loginButton.setText("Log In");
                });
    }

    private void handleSignup() {
        // TODO: Implement signup logic or navigation
        Toast.makeText(this, "Sign up clicked", Toast.LENGTH_SHORT).show();
    }
}