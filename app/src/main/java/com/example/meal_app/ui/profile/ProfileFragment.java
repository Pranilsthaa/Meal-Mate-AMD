package com.example.meal_app.ui.profile;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.ImageButton;
import android.widget.Button;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.core.content.ContextCompat;
import com.example.meal_app.R;
import com.example.meal_app.utils.SessionManager;
import com.example.meal_app.LoginActivity;

public class ProfileFragment extends Fragment {

    private SessionManager sessionManager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Set status bar color
        if (getActivity() != null) {
            Window window = getActivity().getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(ContextCompat.getColor(getActivity(), R.color.black));
        }
    }

    public View onCreateView(@NonNull LayoutInflater inflater,
            ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_profile, container, false);

        sessionManager = new SessionManager(requireContext());

        // Initialize views
        TextView userNameDisplay = root.findViewById(R.id.userNameDisplay);
        TextView userEmailDisplay = root.findViewById(R.id.userEmailDisplay);
        TextView userPhone = root.findViewById(R.id.userPhone);
        TextView userAddress = root.findViewById(R.id.userAddress);
        Button logoutButton = root.findViewById(R.id.logoutButton);
        ImageButton backButton = root.findViewById(R.id.backButton);

        // Set user details
        userNameDisplay.setText(sessionManager.getUserName());
        userEmailDisplay.setText(sessionManager.getUserEmail());
        userPhone.setText(sessionManager.getUserPhone());
        userAddress.setText(sessionManager.getUserAddress());

        // Back button functionality
        backButton.setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().onBackPressed();
            }
        });

        // Logout button functionality
        logoutButton.setOnClickListener(v -> {
            sessionManager.logout();
            startActivity(new Intent(getActivity(), LoginActivity.class));
            if (getActivity() != null) {
                getActivity().finish();
            }
        });

        return root;
    }
}