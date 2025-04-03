package com.example.meal_app.ui.dashboard;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.meal_app.databinding.FragmentDashboardBinding;

public class DashboardFragment extends Fragment {

    // Binding object for the fragment's layout
    private FragmentDashboardBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
            ViewGroup container, Bundle savedInstanceState) {
        // Obtain the ViewModel for this fragment
        DashboardViewModel dashboardViewModel
                = new ViewModelProvider(this).get(DashboardViewModel.class);

        // Inflate the layout for this fragment using View Binding
        binding = FragmentDashboardBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // Get a reference to the TextView in the layout
        final TextView textView = binding.textDashboard;

        // Observe the LiveData from the ViewModel and update the TextView when data changes
        dashboardViewModel.getText().observe(getViewLifecycleOwner(), textView::setText);

        // Return the root view of the fragment
        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Set the binding object to null to avoid memory leaks
        binding = null;
    }
}
