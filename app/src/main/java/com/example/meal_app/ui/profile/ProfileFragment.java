package com.example.meal_app.ui.profile;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import com.example.meal_app.R;
import com.example.meal_app.utils.SessionManager;
import com.example.meal_app.LoginActivity;

public class ProfileFragment extends Fragment {

    private SessionManager sessionManager;

    public View onCreateView(@NonNull LayoutInflater inflater,
            ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_profile, container, false);

        sessionManager = new SessionManager(requireContext());

        TextView userName = root.findViewById(R.id.userName);
        TextView userEmail = root.findViewById(R.id.userEmail);
        TextView userAddress = root.findViewById(R.id.userAddress);
        TextView userPhone = root.findViewById(R.id.userPhone);
        Button logoutButton = root.findViewById(R.id.logoutButton);

        // Set user details
        userName.setText("Name: " + sessionManager.getUserName());
        userEmail.setText("Email: " + sessionManager.getUserEmail());
        userAddress.setText("Address: " + sessionManager.getUserAddress());
        userPhone.setText("Phone: " + sessionManager.getUserPhone());

        // Logout button functionality
        logoutButton.setOnClickListener(v -> {
            sessionManager.logout();
            startActivity(new Intent(getActivity(), LoginActivity.class));
            getActivity().finish();
        });

        return root;
    }
}