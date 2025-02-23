package com.example.meal_app;

import android.content.Intent;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.meal_app.databinding.ActivitySignupBinding;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

import android.util.Log;

public class SignupActivity extends AppCompatActivity {

    private ActivitySignupBinding binding;
    private FirebaseFirestore db;
    private static final String TAG = "SignupActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySignupBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Initialize Firestore with debug logging
        db = FirebaseFirestore.getInstance();
        FirebaseFirestore.setLoggingEnabled(true); // Enable debug logging
        Log.d(TAG, "Firestore initialized");

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        setupClickListeners();
        setupTouchListener();
    }

    private void setupTouchListener() {
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
        binding.signupButton.setOnClickListener(v -> handleSignup());
        binding.loginText.setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }

    private void handleSignup() {
        String name = binding.nameInput.getText().toString().trim();
        String email = binding.emailInput.getText().toString().trim();
        String password = binding.passwordInput.getText().toString().trim();
        String address = binding.addressInput.getText().toString().trim();
        String phone = binding.phoneInput.getText().toString().trim();

        // Validate all fields
        if (name.isEmpty() || email.isEmpty() || password.isEmpty()
                || address.isEmpty() || phone.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validate email format
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Please enter a valid email address", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validate phone number
        if (phone.length() != 10) {
            Toast.makeText(this, "Please enter a valid 10-digit phone number", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show confirmation dialog
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Confirm Account Creation")
                .setMessage("Are you sure you want to create an account with these details?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    createAccount(name, email, password, address, phone);
                })
                .setNegativeButton("No", null)
                .show();
    }

    private void createAccount(String name, String email, String password,
            String address, String phone) {
        // Create user data map
        Map<String, Object> user = new HashMap<>();
        user.put("name", name);
        user.put("email", email);
        user.put("password", password);
        user.put("address", address);
        user.put("phone", phone);

        // Show loading state
        binding.signupButton.setEnabled(false);
        binding.signupButton.setText("Creating Account...");

        // Generate a unique document ID
        String userId = email.replace("@", "_").replace(".", "_");

        // Add user to Firestore with specific document ID
        db.collection("user_detail")
                .document(userId) // Use specific document ID instead of auto-generated
                .set(user) // Use set instead of add
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "User document created with ID: " + userId);
                    Toast.makeText(SignupActivity.this,
                            "Account created successfully! Please login.",
                            Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(SignupActivity.this, LoginActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error creating user document", e);
                    binding.signupButton.setEnabled(true);
                    binding.signupButton.setText("Sign Up");
                    Toast.makeText(SignupActivity.this,
                            "Error: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }
}