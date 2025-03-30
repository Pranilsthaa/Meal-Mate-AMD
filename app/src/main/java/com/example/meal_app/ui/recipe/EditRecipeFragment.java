package com.example.meal_app.ui.recipe;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.DocumentSnapshot;
import java.util.List;
import java.util.Map;
import androidx.navigation.Navigation;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.LinearLayout;
import android.widget.ArrayAdapter;
import com.example.meal_app.R;

public class EditRecipeFragment extends AddRecipeFragment {
    private String recipeId;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Get recipe ID from arguments
        if (getArguments() != null) {
            String recipeId = getArguments().getString("recipeId");
            if (recipeId != null) {
                loadRecipe(recipeId);
            }
        }

        // Update title to "Edit Recipe"
        TextView titleText = view.findViewById(R.id.titleText);
        if (titleText != null) {
            titleText.setText("Edit Recipe");
        }
    }

    private void loadRecipe(String recipeId) {
        getDb().collection("recipes").document(recipeId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Load basic recipe data
                        getRecipeNameInput().setText(documentSnapshot.getString("name"));
                        getDescriptionInput().setText(documentSnapshot.getString("description"));
                        getCookingTimeInput().setText(String.valueOf(documentSnapshot.getLong("cookingTime")));
                        getServingsInput().setText(String.valueOf(documentSnapshot.getLong("servings")));

                        // Load category
                        String category = documentSnapshot.getString("category");
                        if (category != null) {
                            int position = getCategoryAdapter().getPosition(category);
                            if (position != -1) {
                                getCategorySpinner().setSelection(position);
                            }
                        }

                        // Load image
                        String imageUrl = documentSnapshot.getString("imageUrl");
                        if (imageUrl != null) {
                            loadImageFromUrl(imageUrl);
                        }

                        // Load ingredients
                        List<Map<String, Object>> ingredients = (List<Map<String, Object>>) documentSnapshot.get("ingredients");
                        if (ingredients != null) {
                            for (Map<String, Object> ingredient : ingredients) {
                                addIngredientViewWithData(ingredient);
                            }
                        }

                        // Load instructions
                        List<String> instructions = (List<String>) documentSnapshot.get("instructions");
                        if (instructions != null) {
                            getInstructionsList().clear();
                            getInstructionsList().addAll(instructions);
                            // Update instruction views
                            for (String instruction : instructions) {
                                addInstructionStepWithData(instruction);
                            }
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    showError("Failed to load recipe: " + e.getMessage());
                });
    }

    private void addIngredientViewWithData(Map<String, Object> ingredient) {
        View ingredientView = getLayoutInflater().inflate(R.layout.item_ingredient_input, null);
        
        // Set the ingredient data
        EditText nameInput = ingredientView.findViewById(R.id.ingredientNameInput);
        EditText quantityInput = ingredientView.findViewById(R.id.quantityInput);
        EditText priceInput = ingredientView.findViewById(R.id.priceInput);
        Spinner unitSpinner = ingredientView.findViewById(R.id.unitSpinner);
        Spinner typeSpinner = ingredientView.findViewById(R.id.typeSpinner);

        // Set values
        nameInput.setText((String) ingredient.get("name"));
        Object quantity = ingredient.get("quantity");
        quantityInput.setText(quantity != null ? String.valueOf(quantity) : "");
        Object price = ingredient.get("price");
        priceInput.setText(price != null ? String.valueOf(price) : "");

        // Setup unit spinner
        ArrayAdapter<CharSequence> unitAdapter = ArrayAdapter.createFromResource(requireContext(),
                R.array.ingredient_units, android.R.layout.simple_spinner_item);
        unitAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        unitSpinner.setAdapter(unitAdapter);
        String unit = (String) ingredient.get("unit");
        if (unit != null) {
            int unitPosition = unitAdapter.getPosition(unit);
            if (unitPosition != -1) {
                unitSpinner.setSelection(unitPosition);
            }
        }

        // Setup type spinner
        ArrayAdapter<CharSequence> typeAdapter = ArrayAdapter.createFromResource(requireContext(),
                R.array.ingredient_types, android.R.layout.simple_spinner_item);
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        typeSpinner.setAdapter(typeAdapter);
        String type = (String) ingredient.get("type");
        if (type != null) {
            int typePosition = typeAdapter.getPosition(type);
            if (typePosition != -1) {
                typeSpinner.setSelection(typePosition);
            }
        }

        // Add to container
        LinearLayout container = requireView().findViewById(R.id.ingredientsContainer);
        container.addView(ingredientView);
    }

    private void addInstructionStepWithData(String instruction) {
        View stepView = getLayoutInflater().inflate(R.layout.item_instruction_step, null);
        
        // Set the instruction data
        EditText stepInput = stepView.findViewById(R.id.stepInput);
        stepInput.setText(instruction);

        // Add to container
        LinearLayout container = requireView().findViewById(R.id.instructionsContainer);
        container.addView(stepView);
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