package com.example.meal_app.ui.recipe;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.annotation.NonNull;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.DocumentSnapshot;
import java.util.List;
import java.util.Map;
import androidx.navigation.Navigation;

public class EditRecipeFragment extends AddRecipeFragment {
    private String recipeId;

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Get recipe ID from arguments
        if (getArguments() != null) {
            recipeId = getArguments().getString("recipeId");
            if (recipeId != null) {
                loadRecipeDetails();
            }
        }

        // Update title to "Edit Recipe"
        TextView titleText = view.findViewById(com.example.meal_app.R.id.titleText);
        if (titleText != null) {
            titleText.setText("Edit Recipe");
        }
    }

    private void loadRecipeDetails() {
        getDb().collection("recipes").document(recipeId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        populateFields(documentSnapshot);
                    }
                })
                .addOnFailureListener(e -> {
                    if (getView() != null) {
                        showError("Error loading recipe: " + e.getMessage());
                    }
                });
    }

    private void populateFields(DocumentSnapshot document) {
        // Populate recipe name
        getRecipeNameInput().setText(document.getString("name"));
        
        // Populate description
        getDescriptionInput().setText(document.getString("description"));
        
        // Populate cooking time
        Long cookingTime = document.getLong("cookingTime");
        if (cookingTime != null) {
            getCookingTimeInput().setText(String.valueOf(cookingTime));
        }

        // Populate servings
        Long servings = document.getLong("servings");
        if (servings != null) {
            getServingsInput().setText(String.valueOf(servings));
        }

        // Populate category
        String category = document.getString("category");
        if (category != null) {
            int position = getCategoryAdapter().getPosition(category);
            if (position >= 0) {
                getCategorySpinner().setSelection(position);
            }
        }

        // Populate ingredients
        List<Map<String, Object>> ingredients = (List<Map<String, Object>>) document.get("ingredients");
        if (ingredients != null) {
            getIngredientsList().clear();
            getIngredientsList().addAll(ingredients);
        }

        // Populate instructions
        List<String> instructions = (List<String>) document.get("instructions");
        if (instructions != null) {
            getInstructionsList().clear();
            getInstructionsList().addAll(instructions);
        }

        // Load image if exists
        String imageUrl = document.getString("imageUrl");
        if (imageUrl != null && !imageUrl.isEmpty()) {
            loadImageFromUrl(imageUrl);
        }
    }

    @Override
    protected void saveRecipe() {
        if (!validateInputs()) return;

        Map<String, Object> recipeData = createRecipeData();
        
        // Update existing recipe instead of creating new one
        getDb().collection("recipes").document(recipeId)
                .update(recipeData)
                .addOnSuccessListener(aVoid -> {
                    if (getView() != null) {
                        Snackbar.make(getView(), "Recipe updated successfully", Snackbar.LENGTH_LONG).show();
                        Navigation.findNavController(requireView()).navigateUp();
                    }
                })
                .addOnFailureListener(e -> {
                    if (getView() != null) {
                        showError("Error updating recipe: " + e.getMessage());
                    }
                });
    }
} 