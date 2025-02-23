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

    private ActivityLoginBinding binding;
    private FirebaseFirestore db;
    private SessionManager sessionManager;
    private static final String TAG = "LoginActivity";
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = FirebaseFirestore.getInstance();
        sessionManager = new SessionManager(this);
        mAuth = FirebaseAuth.getInstance();

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

    private void resetLoginButton() {
        binding.loginButton.setEnabled(true);
        binding.loginButton.setText("Log In");
    }

    private void handleLogin(String email, String password) {
        binding.loginButton.setEnabled(false);
        binding.loginButton.setText("Logging in...");
        binding.loginError.setVisibility(View.GONE);

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            // Get user details from Firestore
                            db.collection("users").document(user.getUid())
                                    .get()
                                    .addOnSuccessListener(documentSnapshot -> {
                                        if (documentSnapshot.exists()) {
                                            String name = documentSnapshot.getString("name");
                                            String address = documentSnapshot.getString("address");
                                            String phone = documentSnapshot.getString("phone");

                                            sessionManager.createLoginSession(
                                                    user.getUid(),
                                                    email,
                                                    name != null ? name : "User",
                                                    address != null ? address : "Not set",
                                                    phone != null ? phone : "Not set");

                                            startActivity(new Intent(LoginActivity.this, MainActivity.class));
                                            finish();
                                        } else {
                                            binding.loginError.setText("User data not found. Please sign up.");
                                            binding.loginError.setVisibility(View.VISIBLE);
                                            resetLoginButton();
                                        }
                                    })
                                    .addOnFailureListener(e -> {
                                        binding.loginError.setText("Error: " + e.getMessage());
                                        binding.loginError.setVisibility(View.VISIBLE);
                                        resetLoginButton();
                                    });
                        }
                    } else {
                        String errorMessage = task.getException() != null ? task.getException().getMessage()
                                : "Authentication failed";
                        binding.loginError.setText(errorMessage);
                        binding.loginError.setVisibility(View.VISIBLE);
                        resetLoginButton();
                    }
                });
    }

    private void handleLogin() {
        String email = binding.usernameInput.getText().toString().trim();
        String password = binding.passwordInput.getText().toString().trim();

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

        handleLogin(email, password);
    }
}