package com.example.meal_app.ui.recipe;

import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.LinearLayout;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.navigation.Navigation;
import com.bumptech.glide.Glide;
import com.example.meal_app.R;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.DocumentSnapshot;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class EditRecipeFragment extends AddRecipeFragment {
    private String recipeId;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Get recipe ID from arguments
        if (getArguments() != null) {
            recipeId = getArguments().getString("recipeId");
            if (recipeId != null) {
                loadRecipe(recipeId);
            }
        }

        // Update title to "Edit Recipe"
        TextView titleText = view.findViewById(R.id.titleText);
        if (titleText != null) {
            titleText.setText("Edit Recipe");
        }

        // Setup image upload functionality
        View imageUploadCard = view.findViewById(R.id.imageUploadCard);
        ImageButton removeImageButton = view.findViewById(R.id.removeImageButton);

        if (imageUploadCard != null) {
            imageUploadCard.setOnClickListener(v -> openImagePicker());
        }

        if (removeImageButton != null) {
            removeImageButton.setOnClickListener(v -> {
                setSelectedImageUri(null);
                if (getView() != null) {
                    View uploadPrompt = getView().findViewById(R.id.uploadPromptLayout);
                    if (uploadPrompt != null) {
                        uploadPrompt.setVisibility(View.VISIBLE);
                    }
                    // Clear the image view
                    android.widget.ImageView imageView = getView().findViewById(R.id.recipeImageView);
                    if (imageView != null) {
                        imageView.setImageDrawable(null);
                    }
                }
            });
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
                            // Hide upload prompt since we have an image
                            if (getView() != null) {
                                View uploadPrompt = getView().findViewById(R.id.uploadPromptLayout);
                                if (uploadPrompt != null) {
                                    uploadPrompt.setVisibility(View.GONE);
                                }
                            }
                        }

                        // Clear existing ingredients container
                        LinearLayout ingredientsContainer = requireView().findViewById(R.id.ingredientsContainer);
                        if (ingredientsContainer != null) {
                            ingredientsContainer.removeAllViews();
                        }

                        // Load ingredients
                        List<Map<String, Object>> ingredients = (List<Map<String, Object>>) documentSnapshot.get("ingredients");
                        if (ingredients != null) {
                            for (Map<String, Object> ingredient : ingredients) {
                                addIngredientViewWithData(ingredient);
                            }
                        } else {
                            // Add at least one empty ingredient view
                            addIngredientView(requireView());
                        }

                        // Clear existing instructions container
                        LinearLayout instructionsContainer = requireView().findViewById(R.id.instructionsContainer);
                        if (instructionsContainer != null) {
                            instructionsContainer.removeAllViews();
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
                        } else {
                            // Add at least one empty instruction step
                            addInstructionStep(requireView());
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

        // Setup remove button
        ImageButton removeButton = ingredientView.findViewById(R.id.removeIngredientButton);
        removeButton.setOnClickListener(v -> {
            ((ViewGroup) ingredientView.getParent()).removeView(ingredientView);
            ingredientViews.remove(ingredientView);
            updateTotalPrice();
        });

        // Setup price change listener
        priceInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                updateTotalPrice();
            }
        });

        // Add margin to the ingredient view
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, 32); // 32dp bottom margin
        ingredientView.setLayoutParams(params);

        // Add to container and track the view
        LinearLayout container = requireView().findViewById(R.id.ingredientsContainer);
        container.addView(ingredientView);
        ingredientViews.add(ingredientView);
    }

    private void addInstructionStepWithData(String instruction) {
        View stepView = getLayoutInflater().inflate(R.layout.item_instruction_step, null);
        
        // Set the instruction data
        EditText stepInput = stepView.findViewById(R.id.stepInput);
        stepInput.setText(instruction);

        // Set step number
        TextView stepNumber = stepView.findViewById(R.id.stepNumberText);
        stepNumber.setText(String.format("%d.", instructionViews.size() + 1));

        // Setup remove button
        ImageButton removeButton = stepView.findViewById(R.id.removeStepButton);
        removeButton.setOnClickListener(v -> {
            ((ViewGroup) stepView.getParent()).removeView(stepView);
            instructionViews.remove(stepView);
            updateStepNumbers();
        });

        // Add margin to the step view
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, 32); // 32dp bottom margin
        stepView.setLayoutParams(params);

        // Add to container and track the view
        LinearLayout container = requireView().findViewById(R.id.instructionsContainer);
        container.addView(stepView);
        instructionViews.add(stepView);
    }

    private void updateTotalPrice() {
        double total = 0.0;
        for (View ingredientView : getIngredientViews()) {
            EditText priceInput = ingredientView.findViewById(R.id.priceInput);
            String priceStr = priceInput.getText().toString().trim();
            if (!priceStr.isEmpty()) {
                try {
                    total += Double.parseDouble(priceStr);
                } catch (NumberFormatException e) {
                    // Skip invalid numbers
                }
            }
        }
        TextView totalPriceText = requireView().findViewById(R.id.totalPriceText);
        if (totalPriceText != null) {
            totalPriceText.setText(String.format("Total: NRS %.2f", total));
        }
    }

    private void updateStepNumbers() {
        int stepNumber = 1;
        for (View stepView : getInstructionViews()) {
            TextView stepNumberText = stepView.findViewById(R.id.stepNumberText);
            stepNumberText.setText(String.format("%d.", stepNumber++));
        }
    }

    // Getter methods to access parent class lists
    private List<View> getIngredientViews() {
        return ingredientViews;
    }

    private List<View> getInstructionViews() {
        return instructionViews;
    }

    @Override
    protected void saveRecipe() {
        if (!validateInputs()) return;

        Map<String, Object> recipeData = createRecipeData();
        
        // If we have a new image selected, upload it first
        if (getSelectedImageUri() != null) {
            try {
                String base64Image = getBase64FromUri(getSelectedImageUri());
                uploadImageToImgBB(base64Image, recipeData);
            } catch (IOException e) {
                showError("Failed to process image: " + e.getMessage());
                return;
            }
        } else {
            // No new image selected, preserve the existing image URL
            getDb().collection("recipes").document(recipeId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String existingImageUrl = documentSnapshot.getString("imageUrl");
                        if (existingImageUrl != null) {
                            recipeData.put("imageUrl", existingImageUrl);
                        }
                        updateRecipeInFirestore(recipeData);
                    }
                })
                .addOnFailureListener(e -> showError("Error retrieving existing recipe: " + e.getMessage()));
        }
    }

    protected void updateRecipeInFirestore(Map<String, Object> recipeData) {
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