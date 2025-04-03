package com.example.meal_app;

import android.content.Intent;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.widget.EditText;
import android.view.WindowManager;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.meal_app.databinding.ActivityMainBinding;
import com.example.meal_app.utils.SessionManager;

import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private SessionManager sessionManager;
    private NavController navController;
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    // Define the menu order as a class constant
    private static final int[] MENU_ORDER = {
        R.id.navigation_home,
        R.id.navigation_meals,
        R.id.navigation_grocery
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set window soft input mode to adjust nothing, keeping bottom nav fixed
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        // Set status bar color to black
        getWindow().setStatusBarColor(getResources().getColor(R.color.black));

        // Initialize Firebase instances
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
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

        // Setup bottom navigation with animations
        binding.navView.setOnItemSelectedListener(item -> {
            if (item.getItemId() == navController.getCurrentDestination().getId()) {
                return false; // Don't navigate if we're already on this destination
            }

            // Get current and target destinations
            int currentId = navController.getCurrentDestination().getId();
            int targetId = item.getItemId();

            // Create NavOptions with fade animations
            androidx.navigation.NavOptions navOptions = new androidx.navigation.NavOptions.Builder()
                    .setLaunchSingleTop(true)
                    .setEnterAnim(R.anim.fade_in_nav)
                    .setExitAnim(R.anim.fade_out_nav)
                    .setPopEnterAnim(R.anim.fade_in_nav)
                    .setPopExitAnim(R.anim.fade_out_nav)
                    .build();

            // Navigate with animations
            try {
                navController.navigate(targetId, null, navOptions);
                return true;
            } catch (Exception e) {
                return false;
            }
        });

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
    }

    private boolean shouldSlideLeft(int currentId, int targetId) {
        // Find positions in the order using optimized search
        int currentPos = -1;
        int targetPos = -1;
        for (int i = 0; i < MENU_ORDER.length && (currentPos == -1 || targetPos == -1); i++) {
            if (MENU_ORDER[i] == currentId) {
                currentPos = i;
            }
            if (MENU_ORDER[i] == targetId) {
                targetPos = i;
            }
        }

        // Check if both current and target positions are valid and if the target is to the left of the current
        return currentPos != -1 && targetPos != -1 && targetPos < currentPos;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        // Get the currently focused view
        View view = getCurrentFocus();
        if (view != null && (ev.getAction() == MotionEvent.ACTION_UP || ev.getAction() == MotionEvent.ACTION_MOVE)
                && view instanceof EditText && !view.getClass().getName().startsWith("android.webkit.")) {
            // Get the screen coordinates of the view
            int[] scrcoords = new int[2];
            view.getLocationOnScreen(scrcoords);
            float x = ev.getRawX() + view.getLeft() - scrcoords[0];
            float y = ev.getRawY() + view.getTop() - scrcoords[1];
            // Check if the touch event is outside the bounds of the EditText
            if (x < view.getLeft() || x > view.getRight() || y < view.getTop() || y > view.getBottom()) {
                // Hide the keyboard if the touch is outside the EditText
                hideKeyboard(view);
            }
        }
        // Pass the event to the superclass for default handling
        return super.dispatchTouchEvent(ev);
    }

    private void hideKeyboard(View view) {
        // Get the InputMethodManager service
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            // Hide the soft keyboard and clear focus from the view
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
