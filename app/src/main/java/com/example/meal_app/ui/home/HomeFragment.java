package com.example.meal_app.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.core.content.ContextCompat;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import com.example.meal_app.R;
import com.example.meal_app.databinding.FragmentHomeBinding;
import com.example.meal_app.utils.SessionManager;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
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
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        sessionManager = new SessionManager(requireContext());

        // Set welcome message
        String userEmail = sessionManager.getUserEmail();
        String userName = userEmail != null ? userEmail.split("@")[0] : "User";
        binding.welcomeText.setText("Welcome, " + userName);

        // Initialize RecyclerViews
        RecyclerView quickAccessRecyclerView = binding.quickAccessRecyclerView;
        quickAccessRecyclerView.setLayoutManager(
                new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        // TODO: Set adapter for quick access items

        RecyclerView groceryListRecyclerView = binding.groceryListRecyclerView;
        groceryListRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        // TODO: Set adapter for grocery list items

        // Setup FAB click listener
        binding.addFab.setOnClickListener(view -> {
            NavController navController = Navigation.findNavController(requireActivity(),
                    R.id.nav_host_fragment_activity_main);
            navController.navigate(R.id.navigation_add_recipe);
        });

        binding.profileButton.setOnClickListener(view -> {
            NavController navController = Navigation.findNavController(requireActivity(),
                    R.id.nav_host_fragment_activity_main);
            navController.navigate(R.id.navigation_profile);
        });

        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}