package com.example.meal_app.ui.recipe;

import android.app.Dialog;
import android.os.Bundle;
import android.util.Log;
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
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;

public class RecipeDetailsFragment extends Fragment {

    private FirebaseFirestore db; // Firestore database instance
    private String recipeId; // ID of the recipe to display
    private View root; // Root view of the fragment

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        root = inflater.inflate(R.layout.fragment_recipe_details, container, false);
        db = FirebaseFirestore.getInstance(); // Initialize Firestore instance

        // Get recipe ID and navigation arguments
        if (getArguments() != null) {
            recipeId = getArguments().getString("recipeId");
            boolean isFromMeals = getArguments().getBoolean("fromMeals", true);

            // Hide "Add to Meal Plan" button if navigating from meals screen
            Button addToMealPlanButton = root.findViewById(R.id.addToMealPlanButton);
            if (isFromMeals) {
                addToMealPlanButton.setVisibility(View.GONE);
            }
        }

        // Setup back button functionality
        ImageButton backButton = root.findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().onBackPressed();
            }
        });

        // Load recipe details if recipe ID is available
        if (recipeId != null) {
            loadRecipeDetails();
        }

        return root;
    }

    private void loadRecipeDetails() {
        // Check if the recipe is a featured recipe (IDs starting with "featured_")
        if (recipeId != null && recipeId.startsWith("featured_")) {
            try {
                // Load featured recipes from assets
                InputStream is = requireContext().getAssets().open("featured_recipes.json");
                int size = is.available();
                byte[] buffer = new byte[size];
                is.read(buffer);
                is.close();
                String json = new String(buffer, "UTF-8");

                JSONObject jsonObject = new JSONObject(json);
                JSONArray featuredRecipesArray = jsonObject.getJSONArray("featured_recipes");

                // Find the matching featured recipe
                for (int i = 0; i < featuredRecipesArray.length(); i++) {
                    JSONObject recipeJson = featuredRecipesArray.getJSONObject(i);
                    if (recipeId.equals(recipeJson.getString("id"))) {
                        updateUIForFeaturedRecipe(recipeJson); // Update UI for featured recipe
                        return;
                    }
                }
                // Handle case where recipe is not found
                handleError("Featured recipe not found");
            } catch (Exception e) {
                handleError("Error loading featured recipe: " + e.getMessage());
            }
        } else {
            // Load recipe details from Firestore
            db.collection("recipes").document(recipeId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            updateUI(documentSnapshot); // Update UI with Firestore data
                        }
                    })
                    .addOnFailureListener(e -> {
                        Snackbar.make(root, "Error loading recipe: " + e.getMessage(), Snackbar.LENGTH_LONG).show();
                    });
        }
    }

    private void updateUIForFeaturedRecipe(JSONObject recipeJson) throws Exception {
        try {
            // Update recipe image
            ImageView recipeImage = root.findViewById(R.id.recipeImage);
            String imageUrl = recipeJson.getString("imageUrl");
            if (imageUrl != null && !imageUrl.isEmpty()) {
                Glide.with(this)
                        .load(imageUrl)
                        .placeholder(R.drawable.placeholder_recipe)
                        .error(R.drawable.placeholder_recipe)
                        .centerCrop()
                        .into(recipeImage);
            } else {
                recipeImage.setImageResource(R.drawable.placeholder_recipe);
            }

            // Update basic recipe information
            TextView nameText = root.findViewById(R.id.recipeName);
            TextView descriptionText = root.findViewById(R.id.recipeDescription);
            TextView categoryText = root.findViewById(R.id.recipeCategory);
            TextView cookingTimeText = root.findViewById(R.id.cookingTime);
            TextView servingsText = root.findViewById(R.id.servings);
            TextView totalPriceText = root.findViewById(R.id.totalPrice);

            nameText.setText(recipeJson.getString("name"));
            descriptionText.setText(recipeJson.getString("description"));
            categoryText.setText(recipeJson.getString("category"));
            cookingTimeText.setText(recipeJson.getInt("cookTime") + " min");
            servingsText.setText(recipeJson.getInt("servings") + " servings");

            // Calculate total price from ingredients
            double totalPrice = 0.0;
            JSONArray ingredientsArray = recipeJson.getJSONArray("ingredients");
            List<String> displayIngredients = new ArrayList<>();

            for (int i = 0; i < ingredientsArray.length(); i++) {
                JSONObject ingredientJson = ingredientsArray.getJSONObject(i);
                String name = ingredientJson.getString("name");
                double quantity = ingredientJson.optDouble("quantity", 1.0);
                String unit = ingredientJson.optString("unit", "unit");
                double price = ingredientJson.optDouble("price", 0.0);

                totalPrice += price;

                String displayString = quantity > 0
                        ? String.format("%.1f %s %s", quantity, unit, name) : name;
                displayIngredients.add(displayString);
            }

            totalPriceText.setText("NRS " + String.format("%.2f", totalPrice));

            // Setup ingredients list
            RecyclerView ingredientsRecyclerView = root.findViewById(R.id.ingredientsRecyclerView);
            ingredientsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
            ingredientsRecyclerView.setAdapter(new IngredientsAdapter(displayIngredients));

            // Setup instructions list
            RecyclerView instructionsRecyclerView = root.findViewById(R.id.instructionsRecyclerView);
            JSONArray instructionsArray = recipeJson.getJSONArray("instructions");
            List<String> instructions = new ArrayList<>();
            for (int i = 0; i < instructionsArray.length(); i++) {
                instructions.add(instructionsArray.getString(i));
            }
            instructionsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
            instructionsRecyclerView.setAdapter(new InstructionsAdapter(instructions));

        } catch (Exception e) {
            handleError("Error updating UI for featured recipe: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void updateUI(DocumentSnapshot document) {
        if (document == null || !document.exists()) {
            handleError("Recipe not found");
            return;
        }

        try {
            // Update recipe image
            ImageView recipeImage = root.findViewById(R.id.recipeImage);
            String imageUrl = document.getString("imageUrl");
            if (imageUrl != null && !imageUrl.isEmpty()) {
                Glide.with(this)
                        .load(imageUrl)
                        .placeholder(R.drawable.placeholder_recipe)
                        .error(R.drawable.placeholder_recipe)
                        .centerCrop()
                        .into(recipeImage);
            } else {
                recipeImage.setImageResource(R.drawable.placeholder_recipe);
            }

            // Update basic recipe information with null checks
            TextView nameText = root.findViewById(R.id.recipeName);
            TextView descriptionText = root.findViewById(R.id.recipeDescription);
            TextView categoryText = root.findViewById(R.id.recipeCategory);
            TextView cookingTimeText = root.findViewById(R.id.cookingTime);
            TextView servingsText = root.findViewById(R.id.servings);
            TextView totalPriceText = root.findViewById(R.id.totalPrice);

            nameText.setText(document.getString("name") != null ? document.getString("name") : "N/A");
            descriptionText.setText(document.getString("description") != null ? document.getString("description") : "No description available");
            categoryText.setText(document.getString("category") != null ? document.getString("category") : "Uncategorized");

            // Handle numeric values safely
            Long cookingTime = document.getLong("cookTime") != null ? document.getLong("cookTime") : document.getLong("cookingTime");
            if (cookingTime != null) {
                cookingTimeText.setText(cookingTime + " min");
            } else {
                cookingTimeText.setText("N/A");
                handleError("cookTime is null or invalid");
            }

            Long servings = document.getLong("servings");
            if (servings != null) {
                servingsText.setText(servings + " servings");
            } else {
                servingsText.setText("N/A");
                handleError("servings is null or invalid");
            }

            // Calculate total price from ingredients
            double totalPrice = 0.0;
            Object ingredientsObj = document.get("ingredients");
            if (ingredientsObj instanceof List) {
                List<?> ingredientsList = (List<?>) ingredientsObj;
                for (Object ingredient : ingredientsList) {
                    if (ingredient instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> ingredientMap = (Map<String, Object>) ingredient;
                        Object priceObj = ingredientMap.get("price");

                        if (priceObj instanceof Number) {
                            totalPrice += ((Number) priceObj).doubleValue();
                        }
                    }
                }
            }

            totalPriceText.setText("NRS " + String.format("%.2f", totalPrice));

            // Setup ingredients list with type checking
            RecyclerView ingredientsRecyclerView = root.findViewById(R.id.ingredientsRecyclerView);
            if (ingredientsObj instanceof List) {
                List<?> ingredientsList = (List<?>) ingredientsObj;
                if (ingredientsList != null && !ingredientsList.isEmpty()) {
                    List<String> displayIngredients = new ArrayList<>();
                    for (Object ingredient : ingredientsList) {
                        if (ingredient instanceof String) {
                            // If it's already a string, add it directly
                            displayIngredients.add((String) ingredient);
                        } else if (ingredient instanceof Map) {
                            // If it's a map, format it as a string
                            @SuppressWarnings("unchecked")
                            Map<String, Object> ingredientMap = (Map<String, Object>) ingredient;
                            String name = (String) ingredientMap.get("name");
                            Object quantityObj = ingredientMap.get("quantity");
                            String unit = (String) ingredientMap.get("unit");

                            if (name != null) {
                                double quantity = 1.0;
                                if (quantityObj instanceof Number) {
                                    quantity = ((Number) quantityObj).doubleValue();
                                }

                                String displayString = quantity > 0 && unit != null
                                        ? String.format("%.1f %s %s", quantity, unit, name) : name;
                                displayIngredients.add(displayString);
                            }
                        }
                    }
                    ingredientsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
                    ingredientsRecyclerView.setAdapter(new IngredientsAdapter(displayIngredients));
                } else {
                    handleError("ingredients list is empty or null");
                }
            } else {
                handleError("ingredients is not in the expected format");
            }

            // Setup instructions list with type checking
            RecyclerView instructionsRecyclerView = root.findViewById(R.id.instructionsRecyclerView);
            Object instructionsObj = document.get("instructions");
            if (instructionsObj instanceof List) {
                List<String> instructions = (List<String>) instructionsObj;
                if (instructions != null && !instructions.isEmpty()) {
                    instructionsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
                    instructionsRecyclerView.setAdapter(new InstructionsAdapter(instructions));
                } else {
                    handleError("instructions list is empty or null");
                }
            } else {
                handleError("instructions is not in the expected format");
            }

        } catch (Exception e) {
            handleError("Error updating UI: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void showDaySelectionDialog() {
        // Show a dialog to select a day for meal planning
        String[] days = {"Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"};
        new android.app.AlertDialog.Builder(requireContext())
                .setTitle("Select Day")
                .setItems(days, (dialog, which) -> {
                    assignRecipeToDay(days[which]); // Assign recipe to the selected day
                })
                .show();
    }

    private void assignRecipeToDay(String day) {
        if (recipeId == null) {
            return;
        }

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
        // Display error message using Snackbar and log it
        if (getActivity() != null && root != null) {
            Snackbar.make(root, message, Snackbar.LENGTH_LONG).show();
            Log.e("RecipeDetailsFragment", message);
        }
    }

    // Adapter for displaying ingredients in a RecyclerView
    private static class IngredientsAdapter extends RecyclerView.Adapter<IngredientsAdapter.ViewHolder> {

        private final List<String> ingredients;

        public IngredientsAdapter(List<String> ingredients) {
            this.ingredients = ingredients;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(android.R.layout.simple_list_item_1, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            holder.bind(ingredients.get(position)); // Bind ingredient data to the view
        }

        @Override
        public int getItemCount() {
            return ingredients.size(); // Return the number of ingredients
        }

        static class ViewHolder extends RecyclerView.ViewHolder {

            private final TextView textView;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                textView = (TextView) itemView;
            }

            public void bind(String ingredient) {
                textView.setText(ingredient); // Set ingredient text
            }
        }
    }

    // Adapter for displaying instructions in a RecyclerView
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
            holder.bind(position + 1, instructions.get(position)); // Bind instruction data to the view
        }

        @Override
        public int getItemCount() {
            return instructions.size(); // Return the number of instructions
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
                stepNumberText.setText(String.valueOf(stepNumber)); // Set step number
                instructionText.setText(instruction); // Set instruction text
            }
        }
    }
}
