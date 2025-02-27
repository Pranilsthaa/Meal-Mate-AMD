package com.example.meal_app.ui.recipe;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.meal_app.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RecipeDetailsFragment extends Fragment {
    private FirebaseFirestore db;
    private String recipeId;
    private View root;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        root = inflater.inflate(R.layout.fragment_recipe_details, container, false);
        db = FirebaseFirestore.getInstance();

        // Get recipe ID from arguments
        if (getArguments() != null) {
            recipeId = getArguments().getString("recipeId");
            // Check if navigating from meals screen
            boolean isFromMeals = getArguments().getBoolean("fromMeals", true);

            // Hide "Add to Meal Plan" button if coming from meals screen
            Button addToMealPlanButton = root.findViewById(R.id.addToMealPlanButton);
            if (isFromMeals) {
                addToMealPlanButton.setVisibility(View.GONE);
            }
        }

        // Setup back button
        ImageButton backButton = root.findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().onBackPressed();
            }
        });

        if (recipeId != null) {
            loadRecipeDetails();
        }

        return root;
    }

    private void loadRecipeDetails() {
        db.collection("recipes").document(recipeId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        updateUI(documentSnapshot);
                    }
                })
                .addOnFailureListener(e -> {
                    Snackbar.make(root, "Error loading recipe: " + e.getMessage(), Snackbar.LENGTH_LONG).show();
                });
    }

    private void updateUI(DocumentSnapshot document) {
        if (document == null || !document.exists()) {
            handleError("Recipe not found");
            return;
        }

        // Update recipe image
        ImageView recipeImage = root.findViewById(R.id.recipeImage);
        String imageUrl = document.getString("imageUrl");
        if (imageUrl != null && !imageUrl.isEmpty()) {
            Glide.with(this)
                    .load(imageUrl)
                    .centerCrop()
                    .into(recipeImage);
        }

        // Update basic info
        TextView nameText = root.findViewById(R.id.recipeName);
        TextView descriptionText = root.findViewById(R.id.recipeDescription);
        TextView categoryText = root.findViewById(R.id.recipeCategory);
        TextView cookingTimeText = root.findViewById(R.id.cookingTime);
        TextView servingsText = root.findViewById(R.id.servings);
        TextView totalPriceText = root.findViewById(R.id.totalPrice);

        nameText.setText(document.getString("name"));
        descriptionText.setText(document.getString("description"));
        categoryText.setText(document.getString("category"));

        // Format cooking time
        Long cookingTime = document.getLong("cookingTime");
        if (cookingTime != null) {
            cookingTimeText.setText(cookingTime + " min");
        }

        // Format servings
        Long servings = document.getLong("servings");
        if (servings != null) {
            servingsText.setText(servings + " servings");
        }

        // Format price in NRS
        Double totalPrice = document.getDouble("totalPrice");
        if (totalPrice != null) {
            totalPriceText.setText("NRS " + String.format("%.2f", totalPrice));
        }

        // Setup ingredients list
        RecyclerView ingredientsRecyclerView = root.findViewById(R.id.ingredientsRecyclerView);
        List<Map<String, Object>> ingredients = (List<Map<String, Object>>) document.get("ingredients");
        if (ingredients != null) {
            ingredientsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
            ingredientsRecyclerView.setAdapter(new IngredientsAdapter(ingredients));
        }

        // Setup instructions list
        RecyclerView instructionsRecyclerView = root.findViewById(R.id.instructionsRecyclerView);
        List<String> instructions = (List<String>) document.get("instructions");
        if (instructions != null) {
            instructionsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
            instructionsRecyclerView.setAdapter(new InstructionsAdapter(instructions));
        }
    }

    private void showDaySelectionDialog() {
        String[] days = { "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday" };
        new android.app.AlertDialog.Builder(requireContext())
                .setTitle("Select Day")
                .setItems(days, (dialog, which) -> {
                    assignRecipeToDay(days[which]);
                })
                .show();
    }

    private void assignRecipeToDay(String day) {
        if (recipeId == null)
            return;

        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Reference to the meal_plans collection
        db.collection("meal_plans")
                .whereEqualTo("userId", userId)
                .whereEqualTo("day", day)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        // Update existing meal plan
                        DocumentSnapshot mealPlan = queryDocumentSnapshots.getDocuments().get(0);
                        List<String> recipeIds = (List<String>) mealPlan.get("recipeIds");
                        if (recipeIds == null) {
                            recipeIds = new ArrayList<>();
                        }
                        if (!recipeIds.contains(recipeId)) {
                            recipeIds.add(recipeId);
                            mealPlan.getReference().update("recipeIds", recipeIds)
                                    .addOnSuccessListener(aVoid -> {
                                        Snackbar.make(root, "Recipe added to " + day, Snackbar.LENGTH_SHORT).show();
                                        if (getActivity() != null) {
                                            getActivity().onBackPressed();
                                        }
                                    })
                                    .addOnFailureListener(
                                            e -> handleError("Failed to update meal plan: " + e.getMessage()));
                        } else {
                            Snackbar.make(root, "Recipe already added to " + day, Snackbar.LENGTH_SHORT).show();
                        }
                    } else {
                        // Create new meal plan
                        Map<String, Object> mealPlan = new HashMap<>();
                        mealPlan.put("userId", userId);
                        mealPlan.put("day", day);
                        mealPlan.put("recipeIds", Arrays.asList(recipeId));

                        db.collection("meal_plans")
                                .add(mealPlan)
                                .addOnSuccessListener(documentReference -> {
                                    Snackbar.make(root, "Recipe added to " + day, Snackbar.LENGTH_SHORT).show();
                                    if (getActivity() != null) {
                                        getActivity().onBackPressed();
                                    }
                                })
                                .addOnFailureListener(
                                        e -> handleError("Failed to create meal plan: " + e.getMessage()));
                    }
                })
                .addOnFailureListener(e -> handleError("Failed to check meal plan: " + e.getMessage()));
    }

    private void handleError(String message) {
        if (getActivity() != null && root != null) {
            Snackbar.make(root, message, Snackbar.LENGTH_LONG).show();
        }
    }

    // Ingredients Adapter
    private static class IngredientsAdapter extends RecyclerView.Adapter<IngredientsAdapter.ViewHolder> {
        private final List<Map<String, Object>> ingredients;

        public IngredientsAdapter(List<Map<String, Object>> ingredients) {
            this.ingredients = ingredients;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_ingredient, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Map<String, Object> ingredient = ingredients.get(position);
            holder.bind(ingredient);
        }

        @Override
        public int getItemCount() {
            return ingredients.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            private final TextView nameText;
            private final TextView quantityText;
            private final TextView priceText;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                nameText = itemView.findViewById(R.id.ingredientName);
                quantityText = itemView.findViewById(R.id.ingredientQuantity);
                priceText = itemView.findViewById(R.id.ingredientPrice);
            }

            public void bind(Map<String, Object> ingredient) {
                nameText.setText((String) ingredient.get("name"));
                Double quantity = (Double) ingredient.get("quantity");
                String unit = (String) ingredient.get("unit");
                Double price = (Double) ingredient.get("price");

                quantityText.setText(String.format("%.1f %s", quantity, unit));
                priceText.setText("NRS " + String.format("%.2f", price));
            }
        }
    }

    // Instructions Adapter
    private static class InstructionsAdapter extends RecyclerView.Adapter<InstructionsAdapter.ViewHolder> {
        private final List<String> instructions;

        public InstructionsAdapter(List<String> instructions) {
            this.instructions = instructions;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_instruction, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            holder.bind(position + 1, instructions.get(position));
        }

        @Override
        public int getItemCount() {
            return instructions.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            private final TextView stepNumberText;
            private final TextView instructionText;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                stepNumberText = itemView.findViewById(R.id.stepNumber);
                instructionText = itemView.findViewById(R.id.stepInstruction);
            }

            public void bind(int stepNumber, String instruction) {
                stepNumberText.setText(String.valueOf(stepNumber));
                instructionText.setText(instruction);
            }
        }
    }
}