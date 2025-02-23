package com.example.meal_app;

import android.content.Intent;
import android.os.Bundle;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.meal_app.databinding.ActivityMainBinding;
import com.example.meal_app.utils.SessionManager;

import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        sessionManager = new SessionManager(this);

        // Check if user is logged in
        if (!sessionManager.isLoggedIn()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        // Setup bottom navigation
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);

        // Setup AppBarConfiguration
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_home, R.id.navigation_meals,
                R.id.navigation_grocery, R.id.navigation_profile)
                .build();

        NavigationUI.setupWithNavController(binding.navView, navController);

        // Set bottom navigation colors
        binding.navView.setItemIconTintList(
                ContextCompat.getColorStateList(this, R.color.bottom_nav_color));
        binding.navView.setItemTextColor(
                ContextCompat.getColorStateList(this, R.color.bottom_nav_color));
    }

    // Add this method to handle logout
    private void handleLogout() {
        sessionManager.logout();
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }
}