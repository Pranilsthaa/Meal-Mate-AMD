package com.example.meal_app;

import android.content.Intent;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.widget.EditText;

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
    private NavController navController;

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
        navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);

        // Setup AppBarConfiguration
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_home, R.id.navigation_meals,
                R.id.navigation_grocery)
                .build();

        NavigationUI.setupWithNavController(binding.navView, navController);

        // Set bottom navigation colors
        binding.navView.setItemIconTintList(
                ContextCompat.getColorStateList(this, R.color.bottom_nav_color));
        binding.navView.setItemTextColor(
                ContextCompat.getColorStateList(this, R.color.bottom_nav_color));

        // Handle navigation changes
        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            // Keep Home tab selected when on Profile screen
            if (destination.getId() == R.id.navigation_profile) {
                binding.navView.getMenu().findItem(R.id.navigation_home).setChecked(true);
            }
        });

        // Handle navigation without reloading data
        binding.navView.setOnItemSelectedListener(item -> {
            navController.navigate(item.getItemId());
            return true;
        });
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        View view = getCurrentFocus();
        if (view != null && (ev.getAction() == MotionEvent.ACTION_UP || ev.getAction() == MotionEvent.ACTION_MOVE) &&
                view instanceof EditText && !view.getClass().getName().startsWith("android.webkit.")) {
            int[] scrcoords = new int[2];
            view.getLocationOnScreen(scrcoords);
            float x = ev.getRawX() + view.getLeft() - scrcoords[0];
            float y = ev.getRawY() + view.getTop() - scrcoords[1];
            if (x < view.getLeft() || x > view.getRight() || y < view.getTop() || y > view.getBottom()) {
                hideKeyboard(view);
            }
        }
        return super.dispatchTouchEvent(ev);
    }

    private void hideKeyboard(View view) {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            view.clearFocus();
        }
    }

    @Override
    public void onBackPressed() {
        if (navController.getCurrentDestination().getId() == R.id.navigation_profile) {
            navController.navigate(R.id.navigation_home);
        } else {
            super.onBackPressed();
        }
    }

    // Add this method to handle logout
    private void handleLogout() {
        sessionManager.logout();
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }
}