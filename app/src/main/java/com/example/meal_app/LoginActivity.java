package com.example.meal_app;

import android.content.Intent;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.meal_app.databinding.ActivityLoginBinding;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

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
        String username = binding.usernameInput.getText().toString();
        String password = binding.passwordInput.getText().toString();

        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, username, Toast.LENGTH_SHORT).show();

        // For now, just proceed to main activity
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    private void handleSignup() {
        // TODO: Implement signup logic or navigation
        Toast.makeText(this, "Sign up clicked", Toast.LENGTH_SHORT).show();
    }
}