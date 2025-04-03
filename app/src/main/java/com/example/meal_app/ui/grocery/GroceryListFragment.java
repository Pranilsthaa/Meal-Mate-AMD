package com.example.meal_app.ui.grocery;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.meal_app.R;
import com.example.meal_app.adapter.GroceryListAdapter;
import com.example.meal_app.databinding.FragmentGroceryListBinding;
import com.example.meal_app.model.Ingredient;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GroceryListFragment extends Fragment implements GroceryListAdapter.OnIngredientClickListener {

    private FragmentGroceryListBinding binding; // View binding for the fragment
    private FirebaseFirestore db; // Firestore database instance
    private FirebaseAuth auth; // Firebase authentication instance

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout using view binding
        binding = FragmentGroceryListBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // Initialize Firebase instances
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // Load the grocery list if the user is logged in
        if (auth.getCurrentUser() != null) {
            loadGroceryList();
        }

        return root;
    }

    private void loadGroceryList() {
        // Get the current user's ID
        String userId = auth.getCurrentUser().getUid();
        Log.d("GroceryListFragment", "Loading grocery list for userId: " + userId);

        // Fetch the grocery list document from Firestore
        db.collection("groceryLists").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    Log.d("GroceryListFragment", "Got grocery list document. Exists: " + documentSnapshot.exists());
                    if (documentSnapshot.exists()) {
                        // Parse the grocery list data
                        Map<String, Object> groceryList = documentSnapshot.getData();
                        Log.d("GroceryListFragment", "Grocery list data: " + groceryList);

                        if (groceryList != null && !groceryList.isEmpty()) {
                            List<Ingredient> ingredients = new ArrayList<>();
                            for (Map.Entry<String, Object> entry : groceryList.entrySet()) {
                                try {
                                    // Parse each ingredient entry
                                    Map<String, Object> ingredientData = (Map<String, Object>) entry.getValue();
                                    if (ingredientData == null) {
                                        continue; // Skip null entries
                                    }
                                    Log.d("GroceryListFragment", "Processing ingredient: " + ingredientData);

                                    // Create an Ingredient object and populate its fields
                                    Ingredient ingredient = new Ingredient();
                                    ingredient.setName((String) ingredientData.get("name"));
                                    ingredient.setCategory((String) ingredientData.get("category"));

                                    // Handle numeric values with null checks
                                    Object quantityObj = ingredientData.get("quantity");
                                    if (quantityObj instanceof Number) {
                                        ingredient.setQuantity(((Number) quantityObj).doubleValue());
                                    } else {
                                        ingredient.setQuantity(1.0); // Default value
                                    }

                                    Object priceObj = ingredientData.get("price");
                                    if (priceObj instanceof Number) {
                                        ingredient.setPrice(((Number) priceObj).doubleValue());
                                    } else {
                                        ingredient.setPrice(0.0); // Default value
                                    }

                                    ingredient.setUnit((String) ingredientData.get("unit"));

                                    // Handle boolean values with null checks
                                    Object boughtObj = ingredientData.get("bought");
                                    ingredient.setBought(boughtObj instanceof Boolean ? (Boolean) boughtObj : false);

                                    Object selectedForSmsObj = ingredientData.get("selectedForSms");
                                    ingredient.setSelectedForSms(selectedForSmsObj instanceof Boolean ? (Boolean) selectedForSmsObj : false);

                                    Log.d("GroceryListFragment", "Created ingredient object: " + ingredient);
                                    ingredients.add(ingredient);
                                } catch (Exception e) {
                                    Log.e("GroceryListFragment", "Error processing ingredient: " + entry.getKey(), e);
                                }
                            }

                            Log.d("GroceryListFragment", "Total ingredients loaded: " + ingredients.size());
                            updateGroceryList(ingredients);
                        } else {
                            Log.d("GroceryListFragment", "Grocery list is empty");
                            showEmptyState();
                        }
                    } else {
                        Log.d("GroceryListFragment", "No grocery list document found");
                        showEmptyState();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("GroceryListFragment", "Error loading grocery list", e);
                    if (getView() != null) {
                        Snackbar.make(getView(), "Error loading grocery list: " + e.getMessage(), Snackbar.LENGTH_LONG).show();
                    }
                });
    }

    private void updateGroceryList(List<Ingredient> ingredients) {
        Log.d("GroceryListFragment", "Updating grocery list with " + ingredients.size() + " ingredients");

        // Group ingredients by category
        Map<String, List<Ingredient>> categorizedIngredients = new HashMap<>();
        for (Ingredient ingredient : ingredients) {
            String category = ingredient.getCategory();
            if (!categorizedIngredients.containsKey(category)) {
                categorizedIngredients.put(category, new ArrayList<>());
            }
            categorizedIngredients.get(category).add(ingredient);
        }

        Log.d("GroceryListFragment", "Categories found: " + categorizedIngredients.keySet());

        // Create and set the RecyclerView adapter
        if (binding.groceryRecyclerView != null) {
            Log.d("GroceryListFragment", "Setting up RecyclerView");
            binding.groceryRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
            GroceryListAdapter adapter = new GroceryListAdapter(categorizedIngredients, this);
            binding.groceryRecyclerView.setAdapter(adapter);

            // Hide the empty state and show the RecyclerView
            binding.emptyStateLayout.setVisibility(View.GONE);
            binding.groceryRecyclerView.setVisibility(View.VISIBLE);

            Log.d("GroceryListFragment", "RecyclerView setup complete");
        } else {
            Log.e("GroceryListFragment", "groceryRecyclerView is null");
        }
    }

    private void showEmptyState() {
        Log.d("GroceryListFragment", "Showing empty state");
        if (binding != null) {
            // Show the empty state layout and hide the RecyclerView
            binding.emptyStateLayout.setVisibility(View.VISIBLE);
            binding.groceryRecyclerView.setVisibility(View.GONE);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d("GroceryListFragment", "onResume called");
        // Reload the grocery list when the fragment resumes
        if (auth.getCurrentUser() != null) {
            loadGroceryList();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Clear the binding reference to avoid memory leaks
        binding = null;
    }

    @Override
    public void onIngredientClick(Ingredient ingredient) {
        // Handle ingredient click (e.g., show details or edit)
    }

    @Override
    public void onCheckboxClick(Ingredient ingredient, boolean isChecked) {
        // Update the ingredient's "bought" status in Firestore
        String userId = auth.getCurrentUser().getUid();
        db.collection("groceryLists").document(userId)
                .update(ingredient.getName() + ".bought", isChecked)
                .addOnSuccessListener(aVoid -> Log.d("GroceryListFragment", "Updated bought status for " + ingredient.getName()))
                .addOnFailureListener(e -> Log.e("GroceryListFragment", "Error updating bought status", e));
    }

    @Override
    public void onSmsCheckboxClick(Ingredient ingredient, boolean isChecked) {
        // Update the ingredient's "selectedForSms" status in Firestore
        String userId = auth.getCurrentUser().getUid();
        db.collection("groceryLists").document(userId)
                .update(ingredient.getName() + ".selectedForSms", isChecked)
                .addOnSuccessListener(aVoid -> Log.d("GroceryListFragment", "Updated SMS selection for " + ingredient.getName()))
                .addOnFailureListener(e -> Log.e("GroceryListFragment", "Error updating SMS selection", e));
    }
}
