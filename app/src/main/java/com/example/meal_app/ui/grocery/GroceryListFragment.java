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
    private FragmentGroceryListBinding binding;
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentGroceryListBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // Load grocery list
        if (auth.getCurrentUser() != null) {
            loadGroceryList();
        }

        return root;
    }

    private void loadGroceryList() {
        String userId = auth.getCurrentUser().getUid();
        Log.d("GroceryListFragment", "Loading grocery list for userId: " + userId);

        db.collection("groceryLists").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    Log.d("GroceryListFragment", "Got grocery list document. Exists: " + documentSnapshot.exists());
                    if (documentSnapshot.exists()) {
                        Map<String, Object> groceryList = documentSnapshot.getData();
                        Log.d("GroceryListFragment", "Grocery list data: " + groceryList);
                        
                        if (groceryList != null && !groceryList.isEmpty()) {
                            List<Ingredient> ingredients = new ArrayList<>();
                            for (Map.Entry<String, Object> entry : groceryList.entrySet()) {
                                try {
                                    Map<String, Object> ingredientData = (Map<String, Object>) entry.getValue();
                                    Log.d("GroceryListFragment", "Processing ingredient: " + ingredientData);
                                    
                                    Ingredient ingredient = new Ingredient();
                                    ingredient.setName((String) ingredientData.get("name"));
                                    ingredient.setCategory((String) ingredientData.get("category"));
                                    ingredient.setPrice(((Number) ingredientData.get("price")).doubleValue());
                                    ingredient.setQuantity(((Number) ingredientData.get("quantity")).doubleValue());
                                    ingredient.setUnit((String) ingredientData.get("unit"));
                                    ingredient.setBought((Boolean) ingredientData.get("bought"));
                                    ingredient.setSelectedForSms((Boolean) ingredientData.get("selectedForSms"));
                                    
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

        // Create and set adapter
        if (binding.groceryRecyclerView != null) {
            Log.d("GroceryListFragment", "Setting up RecyclerView");
            binding.groceryRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
            GroceryListAdapter adapter = new GroceryListAdapter(categorizedIngredients, this);
            binding.groceryRecyclerView.setAdapter(adapter);
            
            // Hide empty state and show RecyclerView
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
            binding.emptyStateLayout.setVisibility(View.VISIBLE);
            binding.groceryRecyclerView.setVisibility(View.GONE);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d("GroceryListFragment", "onResume called");
        if (auth.getCurrentUser() != null) {
            loadGroceryList();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onIngredientClick(Ingredient ingredient) {
        // Handle ingredient click
    }

    @Override
    public void onCheckboxClick(Ingredient ingredient, boolean isChecked) {
        // Update the ingredient's bought status in Firestore
        String userId = auth.getCurrentUser().getUid();
        db.collection("groceryLists").document(userId)
                .update(ingredient.getName() + ".bought", isChecked)
                .addOnSuccessListener(aVoid -> Log.d("GroceryListFragment", "Updated bought status for " + ingredient.getName()))
                .addOnFailureListener(e -> Log.e("GroceryListFragment", "Error updating bought status", e));
    }

    @Override
    public void onSmsCheckboxClick(Ingredient ingredient, boolean isChecked) {
        // Update the ingredient's SMS selection status in Firestore
        String userId = auth.getCurrentUser().getUid();
        db.collection("groceryLists").document(userId)
                .update(ingredient.getName() + ".selectedForSms", isChecked)
                .addOnSuccessListener(aVoid -> Log.d("GroceryListFragment", "Updated SMS selection for " + ingredient.getName()))
                .addOnFailureListener(e -> Log.e("GroceryListFragment", "Error updating SMS selection", e));
    }
} 