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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.example.meal_app.utils.SessionManager;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding; // View binding for accessing UI elements
    private FirebaseFirestore db; // Firestore database instance
    private SessionManager sessionManager; // Session manager for handling user sessions
    private static final String TAG = "LoginActivity"; // Tag for logging
    private FirebaseAuth mAuth; // Firebase Authentication instance

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Set status bar color to gradient_start
        getWindow().setStatusBarColor(getResources().getColor(R.color.gradient_start));

        // Initialize Firestore, SessionManager, and FirebaseAuth
        db = FirebaseFirestore.getInstance();
        sessionManager = new SessionManager(this);
        mAuth = FirebaseAuth.getInstance();

        // Check if user is already logged in
        if (sessionManager.isLoggedIn()) {
            // Redirect to MainActivity if logged in
            startActivity(new Intent(LoginActivity.this, MainActivity.class));
            finish();
            return;
        }

        // Hide the action bar
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        // Set up click listeners and touch listener
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
                    // Hide the keyboard and clear focus from the current view
                    InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(focusedView.getWindowToken(), 0);
                    focusedView.clearFocus();
                }
            }
            return false;
        });
    }

    private void setupClickListeners() {
        // Set up login button click listener
        binding.loginButton.setOnClickListener(v -> handleLogin());

        // Set up signup text click listener to navigate to SignupActivity
        binding.signupText.setOnClickListener(v -> {
            startActivity(new Intent(this, SignupActivity.class));
            finish();
        });
    }

    private void resetLoginButton() {
        // Reset login button to its default state
        binding.loginButton.setEnabled(true);
        binding.loginButton.setText("Log In");
    }

    private void handleLogin(String email, String password) {
        // Disable login button and show loading state
        binding.loginButton.setEnabled(false);
        binding.loginButton.setText("Logging in...");
        binding.loginError.setVisibility(View.GONE);

        // Authenticate user with Firebase Authentication
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Authentication successful
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            // Fetch user details from Firestore
                            db.collection("users").document(user.getUid())
                                    .get()
                                    .addOnSuccessListener(documentSnapshot -> {
                                        if (documentSnapshot.exists()) {
                                            // Retrieve user details
                                            String name = documentSnapshot.getString("name");
                                            String address = documentSnapshot.getString("address");
                                            String phone = documentSnapshot.getString("phone");

                                            // Create a login session
                                            sessionManager.createLoginSession(
                                                    user.getUid(),
                                                    email,
                                                    name != null ? name : "User",
                                                    address != null ? address : "Not set",
                                                    phone != null ? phone : "Not set");

                                            // Navigate to MainActivity
                                            startActivity(new Intent(LoginActivity.this, MainActivity.class));
                                            finish();
                                        } else {
                                            // User data not found in Firestore
                                            binding.loginError.setText("User data not found. Please sign up.");
                                            binding.loginError.setVisibility(View.VISIBLE);
                                            resetLoginButton();
                                        }
                                    })
                                    .addOnFailureListener(e -> {
                                        // Handle Firestore error
                                        binding.loginError.setText("Error: " + e.getMessage());
                                        binding.loginError.setVisibility(View.VISIBLE);
                                        resetLoginButton();
                                    });
                        }
                    } else {
                        // Authentication failed
                        String errorMessage = task.getException() != null ? task.getException().getMessage()
                                : "Authentication failed";
                        binding.loginError.setText(errorMessage);
                        binding.loginError.setVisibility(View.VISIBLE);
                        resetLoginButton();
                    }
                });
    }

    private void handleLogin() {
        // Get email and password input from the user
        String email = binding.usernameInput.getText().toString().trim();
        String password = binding.passwordInput.getText().toString().trim();

        // Validate email and password input
        if (email.isEmpty()) {
            binding.loginError.setText("Please enter your email");
            binding.loginError.setVisibility(View.VISIBLE);
            return;
        }

        if (password.isEmpty()) {
            binding.loginError.setText("Please enter your password");
            binding.loginError.setVisibility(View.VISIBLE);
            return;
        }

        // Basic email format validation
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.loginError.setText("Please enter a valid email address");
            binding.loginError.setVisibility(View.VISIBLE);
            return;
        }

        // Proceed with login
        handleLogin(email, password);
    }
}
