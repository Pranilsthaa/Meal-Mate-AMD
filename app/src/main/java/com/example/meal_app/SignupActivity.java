package com.example.meal_app;

import android.content.Intent;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.meal_app.databinding.ActivitySignupBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

import android.util.Log;

public class SignupActivity extends AppCompatActivity {

    private ActivitySignupBinding binding;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private static final String TAG = "SignupActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySignupBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Set status bar color to gradient_start
        getWindow().setStatusBarColor(getResources().getColor(R.color.gradient_start));

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        setupTouchListener();
        setupClickListeners();
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
        String email = binding.emailInput.getText().toString().trim();
        String password = binding.passwordInput.getText().toString().trim();
        String name = binding.nameInput.getText().toString().trim();
        String phone = binding.phoneInput.getText().toString().trim();
        String address = binding.addressInput.getText().toString().trim();

        // Input validation
        if (email.isEmpty() || password.isEmpty() || name.isEmpty()) {
            binding.signupError.setText("Please fill in all required fields");
            binding.signupError.setVisibility(View.VISIBLE);
            return;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.signupError.setText("Please enter a valid email address");
            binding.signupError.setVisibility(View.VISIBLE);
            return;
        }

        if (password.length() < 6) {
            binding.signupError.setText("Password must be at least 6 characters");
            binding.signupError.setVisibility(View.VISIBLE);
            return;
        }

        binding.signupButton.setEnabled(false);
        binding.signupButton.setText("Creating Account...");
        binding.signupError.setVisibility(View.GONE);

        // Create user with email and password
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        String userId = mAuth.getCurrentUser().getUid();

                        // Create user document in Firestore
                        Map<String, Object> user = new HashMap<>();
                        user.put("name", name);
                        user.put("email", email);
                        user.put("phone", phone.isEmpty() ? "Not set" : phone);
                        user.put("address", address.isEmpty() ? "Not set" : address);

                        db.collection("users").document(userId)
                                .set(user)
                                .addOnSuccessListener(aVoid -> {
                                    Log.d(TAG, "User document created");
                                    startActivity(new Intent(SignupActivity.this, LoginActivity.class));
                                    finish();
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Error creating user document", e);
                                    binding.signupError.setText("Error creating account: " + e.getMessage());
                                    binding.signupError.setVisibility(View.VISIBLE);
                                    binding.signupButton.setEnabled(true);
                                    binding.signupButton.setText("Sign Up");
                                });
                    } else {
                        Log.e(TAG, "createUserWithEmail:failure", task.getException());
                        binding.signupError.setText("Signup failed: " + task.getException().getMessage());
                        binding.signupError.setVisibility(View.VISIBLE);
                        binding.signupButton.setEnabled(true);
                        binding.signupButton.setText("Sign Up");
                    }
                });
    }
}