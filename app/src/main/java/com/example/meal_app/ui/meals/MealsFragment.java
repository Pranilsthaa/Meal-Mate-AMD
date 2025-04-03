package com.example.meal_app.ui.meals;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.ImageButton;
import android.widget.Button;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.meal_app.R;
import com.example.meal_app.adapter.MealDayAdapter;
import com.example.meal_app.adapter.AssignedMealsAdapter;
import com.example.meal_app.databinding.FragmentMealsBinding;
import com.example.meal_app.model.MealDay;
import com.example.meal_app.model.Recipe;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MealsFragment extends Fragment {

    private FragmentMealsBinding binding;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private RecyclerView mealsRecyclerView;
    private MealDayAdapter adapter;
    private List<MealDay> mealDays;
    private List<Recipe> allRecipes;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentMealsBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // Set username in title
        TextView titleText = root.findViewById(R.id.titleText);
        String userEmail = auth.getCurrentUser().getEmail();
        String username = userEmail != null ? userEmail.split("@")[0] : "User";
        titleText.setText(username + "'s Meal Plan");

        setupAssignMealsButton();
        setupRecyclerView();

        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadRecipes(recipes -> {
            allRecipes = recipes;
            loadMealPlan();
        });
    }

    private void setupAssignMealsButton() {
        binding.assignMealsButton.setOnClickListener(v -> showAssignMealsDialog(null));
    }

    private void setupRecyclerView() {
        mealsRecyclerView = binding.mealsRecyclerView;
        mealsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        mealDays = new ArrayList<>();

        // Initialize days of the week
        String[] days = {"Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"};
        for (String day : days) {
            mealDays.add(new MealDay(day));
        }

        adapter = new MealDayAdapter(mealDays, new MealDayAdapter.OnMealDayListener() {
            @Override
            public void onMealDayClick(String day) {
                showAssignMealsDialog(day);
            }

            @Override
            public void onRemoveMealClick(String day, Recipe recipe) {
                adapter.setDayLoading(day, true);
                removeMealFromDay(findMealDayByName(day), recipe);
            }

            @Override
            public void onRecipeClick(Recipe recipe) {
                Bundle bundle = new Bundle();
                bundle.putString("recipeId", recipe.getId());
                Navigation.findNavController(requireView())
                        .navigate(R.id.action_navigation_meals_to_recipeDetailsFragment, bundle);
            }
        });
        mealsRecyclerView.setAdapter(adapter);
    }

    private void loadRecipes(OnRecipesLoadedListener listener) {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        db.collection("recipes")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Recipe> recipes = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Recipe recipe = new Recipe();
                        recipe.setId(document.getId());
                        recipe.setName(document.getString("name") != null ? document.getString("name") : "");
                        recipe.setDescription(document.getString("description") != null ? document.getString("description") : "");
                        recipe.setImageUrl(document.getString("imageUrl") != null ? document.getString("imageUrl") : "");
                        recipe.setCategory(document.getString("category") != null ? document.getString("category") : "");
                        recipe.setCookTime(document.getLong("cookTime") != null ? document.getLong("cookTime").intValue() : 0);
                        recipe.setServings(document.getLong("servings") != null ? document.getLong("servings").intValue() : 0);
                        recipe.setTotalPrice(document.getDouble("totalPrice") != null ? document.getDouble("totalPrice") : 0.0);
                        recipe.setUserId(document.getString("userId") != null ? document.getString("userId") : "");
                        recipes.add(recipe);
                    }
                    listener.onRecipesLoaded(recipes);
                })
                .addOnFailureListener(e -> handleError("Error loading recipes: " + e.getMessage()));
    }

    private void loadMealPlan() {
        if (auth.getCurrentUser() == null || binding == null || !isAdded()) {
            return;
        }

        String userId = auth.getCurrentUser().getUid();

        // Only show full-screen loader if the RecyclerView is not visible (initial load)
        if (mealsRecyclerView.getVisibility() != View.VISIBLE && binding != null) {
            binding.progressBar.setVisibility(View.VISIBLE);
            mealsRecyclerView.setVisibility(View.GONE);
        }

        db.collection("meal_plans")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    // Check if binding is still valid and fragment is attached
                    if (binding == null || !isAdded()) {
                        return;
                    }

                    // Reset all days
                    for (MealDay mealDay : mealDays) {
                        mealDay.setRecipes(new ArrayList<>());
                    }

                    // Populate meals for each day
                    for (DocumentSnapshot document : queryDocumentSnapshots) {
                        String day = document.getString("day");
                        List<String> recipeIds = (List<String>) document.get("recipeIds");

                        if (day != null && recipeIds != null) {
                            MealDay mealDay = findMealDayByName(day);
                            if (mealDay != null) {
                                for (String recipeId : recipeIds) {
                                    Recipe recipe = findRecipeById(recipeId);
                                    if (recipe != null) {
                                        mealDay.addRecipe(recipe);
                                    }
                                }
                            }
                        }
                    }

                    binding.progressBar.setVisibility(View.GONE);
                    mealsRecyclerView.setVisibility(View.VISIBLE);
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    // Check if binding is still valid and fragment is attached
                    if (binding == null || !isAdded()) {
                        return;
                    }

                    binding.progressBar.setVisibility(View.GONE);
                    mealsRecyclerView.setVisibility(View.VISIBLE);
                    handleError("Error loading meal plan: " + e.getMessage());
                });
    }

    private Recipe findRecipeById(String recipeId) {
        for (Recipe recipe : allRecipes) {
            if (recipe.getId().equals(recipeId)) {
                return recipe;
            }
        }
        return null;
    }

    private void showAssignMealsDialog(String preSelectedDay) {
        // Create and show the dialog
        Dialog dialog = new Dialog(requireContext());
        dialog.setContentView(R.layout.dialog_assign_meals);
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        // Initialize views
        Spinner daySpinner = dialog.findViewById(R.id.daySpinner);
        RecyclerView recipesRecyclerView = dialog.findViewById(R.id.recipesRecyclerView);
        ImageButton closeButton = dialog.findViewById(R.id.closeButton);

        // Setup day spinner
        String[] days = {"Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"};
        ArrayAdapter<String> dayAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, days);
        dayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        daySpinner.setAdapter(dayAdapter);

        // If a day is preselected, set it
        if (preSelectedDay != null) {
            for (int i = 0; i < days.length; i++) {
                if (days[i].equals(preSelectedDay)) {
                    daySpinner.setSelection(i);
                    break;
                }
            }
        }

        // Setup recipes RecyclerView
        recipesRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        loadRecipes(recipes -> {
            RecipeAdapter adapter = new RecipeAdapter(recipes, recipe -> {
                String selectedDay = (String) daySpinner.getSelectedItem();
                assignMealToDay(selectedDay, recipe.getId());
                dialog.dismiss();
            });
            recipesRecyclerView.setAdapter(adapter);
        });

        // Setup close button
        closeButton.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void assignMealToDay(String day, String recipeId) {
        String userId = auth.getCurrentUser().getUid();
        MealDay mealDay = findMealDayByName(day);

        // Show loading state only for the affected day
        adapter.setDayLoading(day, true);

        db.collection("meal_plans")
                .whereEqualTo("userId", userId)
                .whereEqualTo("day", day)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        // Create new meal plan for the day
                        Map<String, Object> mealPlan = new HashMap<>();
                        mealPlan.put("userId", userId);
                        mealPlan.put("day", day);
                        mealPlan.put("recipeIds", Arrays.asList(recipeId));

                        db.collection("meal_plans")
                                .add(mealPlan)
                                .addOnSuccessListener(documentReference -> {
                                    // Find the recipe and add it to the local data
                                    Recipe recipe = findRecipeById(recipeId);
                                    if (recipe != null && mealDay != null) {
                                        mealDay.addRecipe(recipe);
                                        adapter.notifyDataSetChanged();
                                    }
                                    adapter.setDayLoading(day, false);
                                    // Add ingredients to grocery list
                                    addIngredientsToGroceryList(recipeId, userId);
                                })
                                .addOnFailureListener(e -> {
                                    handleError("Error creating meal plan: " + e.getMessage());
                                    adapter.setDayLoading(day, false);
                                });
                    } else {
                        // Update existing meal plan
                        DocumentSnapshot document = queryDocumentSnapshots.getDocuments().get(0);
                        List<String> recipeIds = (List<String>) document.get("recipeIds");
                        if (recipeIds == null) {
                            recipeIds = new ArrayList<>();
                        }

                        if (!recipeIds.contains(recipeId)) {
                            recipeIds.add(recipeId);
                            db.collection("meal_plans")
                                    .document(document.getId())
                                    .update("recipeIds", recipeIds)
                                    .addOnSuccessListener(aVoid -> {
                                        // Find the recipe and add it to the local data
                                        Recipe recipe = findRecipeById(recipeId);
                                        if (recipe != null && mealDay != null) {
                                            mealDay.addRecipe(recipe);
                                            adapter.notifyDataSetChanged();
                                        }
                                        adapter.setDayLoading(day, false);
                                        // Add ingredients to grocery list
                                        addIngredientsToGroceryList(recipeId, userId);
                                    })
                                    .addOnFailureListener(e -> {
                                        handleError("Error updating meal plan: " + e.getMessage());
                                        adapter.setDayLoading(day, false);
                                    });
                        } else {
                            adapter.setDayLoading(day, false);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    handleError("Error checking meal plan: " + e.getMessage());
                    adapter.setDayLoading(day, false);
                });
    }

    private void addIngredientsToGroceryList(String recipeId, String userId) {
        // Get recipe ingredients
        db.collection("recipes").document(recipeId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Object ingredientsObj = documentSnapshot.get("ingredients");
                        final List<Map<String, Object>> finalIngredients = new ArrayList<>();

                        // Handle both List<String> and List<Map> formats
                        if (ingredientsObj instanceof List<?>) {
                            List<?> ingredientsList = (List<?>) ingredientsObj;
                            if (!ingredientsList.isEmpty()) {
                                if (ingredientsList.get(0) instanceof String) {
                                    // Convert List<String> to List<Map>
                                    for (Object ing : ingredientsList) {
                                        String ingStr = (String) ing;
                                        Map<String, Object> ingMap = new HashMap<>();
                                        ingMap.put("name", ingStr);
                                        ingMap.put("quantity", 1.0);
                                        ingMap.put("unit", "unit");
                                        finalIngredients.add(ingMap);
                                    }
                                } else if (ingredientsList.get(0) instanceof Map) {
                                    // Already in Map format
                                    finalIngredients.addAll((List<Map<String, Object>>) ingredientsObj);
                                }
                            }
                        }

                        if (!finalIngredients.isEmpty()) {
                            // Reference to user's grocery list document
                            DocumentReference groceryListRef = db.collection("groceryLists").document(userId);

                            // Get the current grocery list first
                            groceryListRef.get().addOnSuccessListener(groceryDoc -> {
                                Map<String, Object> existingList = new HashMap<>();
                                if (groceryDoc.exists() && groceryDoc.getData() != null) {
                                    existingList.putAll(groceryDoc.getData());
                                }
                                Map<String, Object> updates = new HashMap<>();

                                // Process each ingredient
                                for (Map<String, Object> ingredient : finalIngredients) {
                                    String name = (String) ingredient.get("name");
                                    if (name != null) {
                                        // Create a unique ID for the ingredient
                                        String ingredientId = name.toLowerCase().replaceAll("[^a-z0-9]", "_");

                                        // Check if ingredient already exists
                                        Object existingObj = existingList.get(ingredientId);
                                        Map<String, Object> existingIngredient = (existingObj instanceof Map)
                                                ? (Map<String, Object>) existingObj : null;

                                        if (existingIngredient != null) {
                                            // Update existing ingredient
                                            Object existingQuantityObj = existingIngredient.get("quantity");
                                            double existingQuantity = (existingQuantityObj instanceof Number)
                                                    ? ((Number) existingQuantityObj).doubleValue() : 0.0;

                                            Object newQuantityObj = ingredient.get("quantity");
                                            double newQuantity = (newQuantityObj instanceof Number)
                                                    ? ((Number) newQuantityObj).doubleValue() : 1.0;

                                            // Combine quantities
                                            Map<String, Object> updatedIngredient = new HashMap<>(existingIngredient);
                                            updatedIngredient.put("quantity", existingQuantity + newQuantity);
                                            updates.put(ingredientId, updatedIngredient);
                                        } else {
                                            // Add new ingredient
                                            Map<String, Object> ingredientData = new HashMap<>(ingredient);
                                            ingredientData.put("bought", false);
                                            ingredientData.put("selectedForSms", false);
                                            ingredientData.put("userId", userId);
                                            if (!ingredientData.containsKey("quantity")) {
                                                ingredientData.put("quantity", 1.0);
                                            }
                                            if (!ingredientData.containsKey("unit")) {
                                                ingredientData.put("unit", "unit");
                                            }
                                            updates.put(ingredientId, ingredientData);
                                        }
                                    }
                                }

                                // Update the grocery list with merged ingredients
                                if (!updates.isEmpty()) {
                                    groceryListRef.set(updates, SetOptions.merge())
                                            .addOnSuccessListener(aVoid -> {
                                                Log.d("MealsFragment", "Successfully added ingredients to grocery list");
                                                Snackbar.make(requireView(), "Added ingredients to grocery list", Snackbar.LENGTH_SHORT).show();
                                            })
                                            .addOnFailureListener(e -> {
                                                Log.e("MealsFragment", "Error updating grocery list", e);
                                                Snackbar.make(requireView(), "Error updating grocery list: " + e.getMessage(), Snackbar.LENGTH_LONG).show();
                                            });
                                }
                            });
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("MealsFragment", "Error getting recipe ingredients", e);
                    Snackbar.make(requireView(), "Error getting recipe ingredients: " + e.getMessage(), Snackbar.LENGTH_LONG).show();
                });
    }

    private String determineIngredientType(String ingredient) {
        // Common ingredient categories
        ingredient = ingredient.toLowerCase();

        if (ingredient.contains("meat") || ingredient.contains("chicken")
                || ingredient.contains("beef") || ingredient.contains("pork")
                || ingredient.contains("fish") || ingredient.contains("lamb")) {
            return "Meat & Fish";
        } else if (ingredient.contains("milk") || ingredient.contains("cheese")
                || ingredient.contains("yogurt") || ingredient.contains("cream")
                || ingredient.contains("butter")) {
            return "Dairy";
        } else if (ingredient.contains("apple") || ingredient.contains("banana")
                || ingredient.contains("orange") || ingredient.contains("berry")
                || ingredient.contains("fruit")) {
            return "Fruits";
        } else if (ingredient.contains("carrot") || ingredient.contains("potato")
                || ingredient.contains("onion") || ingredient.contains("tomato")
                || ingredient.contains("vegetable")) {
            return "Vegetables";
        } else if (ingredient.contains("rice") || ingredient.contains("pasta")
                || ingredient.contains("flour") || ingredient.contains("bread")
                || ingredient.contains("cereal")) {
            return "Grains & Pasta";
        } else if (ingredient.contains("oil") || ingredient.contains("sauce")
                || ingredient.contains("vinegar") || ingredient.contains("spice")
                || ingredient.contains("herb")) {
            return "Condiments & Spices";
        }

        return "Other";
    }

    private MealDay findMealDayByName(String dayName) {
        for (MealDay mealDay : mealDays) {
            if (mealDay.getDay().equals(dayName)) {
                return mealDay;
            }
        }
        return null;
    }

    private void removeMealFromDay(MealDay mealDay, Recipe recipe) {
        if (mealDay == null || recipe == null || auth.getCurrentUser() == null) {
            return;
        }

        String userId = auth.getCurrentUser().getUid();
        String day = mealDay.getDay();

        // Remove the recipe locally first for immediate feedback
        mealDay.removeRecipe(recipe);
        adapter.notifyDataSetChanged();

        db.collection("meal_plans")
                .whereEqualTo("userId", userId)
                .whereEqualTo("day", day)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot document = queryDocumentSnapshots.getDocuments().get(0);
                        List<String> recipeIds = (List<String>) document.get("recipeIds");

                        if (recipeIds != null) {
                            recipeIds.remove(recipe.getId());

                            if (recipeIds.isEmpty()) {
                                // If no recipes left, delete the document
                                db.collection("meal_plans")
                                        .document(document.getId())
                                        .delete()
                                        .addOnSuccessListener(aVoid -> {
                                            adapter.setDayLoading(day, false);
                                            if (getView() != null) {
                                                Snackbar.make(getView(), "Recipe removed from " + day, Snackbar.LENGTH_SHORT).show();
                                            }
                                        })
                                        .addOnFailureListener(e -> {
                                            handleError("Error removing meal plan: " + e.getMessage());
                                            adapter.setDayLoading(day, false);
                                            // Revert local change if update failed
                                            mealDay.addRecipe(recipe);
                                            adapter.notifyDataSetChanged();
                                        });
                            } else {
                                // Update with remaining recipes
                                db.collection("meal_plans")
                                        .document(document.getId())
                                        .update("recipeIds", recipeIds)
                                        .addOnSuccessListener(aVoid -> {
                                            adapter.setDayLoading(day, false);
                                            if (getView() != null) {
                                                Snackbar.make(getView(), "Recipe removed from " + day, Snackbar.LENGTH_SHORT).show();
                                            }
                                        })
                                        .addOnFailureListener(e -> {
                                            handleError("Error updating meal plan: " + e.getMessage());
                                            adapter.setDayLoading(day, false);
                                            // Revert local change if update failed
                                            mealDay.addRecipe(recipe);
                                            adapter.notifyDataSetChanged();
                                        });
                            }
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    handleError("Error checking meal plan: " + e.getMessage());
                    adapter.setDayLoading(day, false);
                    // Revert local change if update failed
                    mealDay.addRecipe(recipe);
                    adapter.notifyDataSetChanged();
                });
    }

    private void removeIngredientsFromGroceryList(String recipeId, String userId) {
        // Get recipe ingredients to remove
        db.collection("recipes").document(recipeId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Object ingredientsObj = documentSnapshot.get("ingredients");
                        List<String> ingredientNames = new ArrayList<>();

                        // Handle both List<String> and List<Map> formats
                        if (ingredientsObj instanceof List<?>) {
                            List<?> ingredientsList = (List<?>) ingredientsObj;
                            if (!ingredientsList.isEmpty()) {
                                if (ingredientsList.get(0) instanceof String) {
                                    // If ingredients are stored as strings
                                    for (Object ing : ingredientsList) {
                                        ingredientNames.add((String) ing);
                                    }
                                } else if (ingredientsList.get(0) instanceof Map) {
                                    // If ingredients are stored as maps
                                    List<Map<String, Object>> ingredients = (List<Map<String, Object>>) ingredientsObj;
                                    for (Map<String, Object> ingredient : ingredients) {
                                        String name = (String) ingredient.get("name");
                                        if (name != null) {
                                            ingredientNames.add(name);
                                        }
                                    }
                                }
                            }
                        }

                        if (!ingredientNames.isEmpty()) {
                            DocumentReference groceryListRef = db.collection("groceryLists").document(userId);

                            // Create a map of fields to delete
                            Map<String, Object> updates = new HashMap<>();
                            for (String name : ingredientNames) {
                                String ingredientId = name.toLowerCase().replaceAll("[^a-z0-9]", "_");
                                updates.put(ingredientId, FieldValue.delete());
                            }

                            // Remove the ingredients from the grocery list
                            if (!updates.isEmpty()) {
                                groceryListRef.update(updates)
                                        .addOnSuccessListener(aVoid -> {
                                            Log.d("MealsFragment", "Successfully removed ingredients from grocery list");
                                            Snackbar.make(requireView(), "Removed ingredients from grocery list", Snackbar.LENGTH_SHORT).show();
                                        })
                                        .addOnFailureListener(e -> {
                                            Log.e("MealsFragment", "Error removing ingredients from grocery list", e);
                                            Snackbar.make(requireView(), "Error removing ingredients from grocery list", Snackbar.LENGTH_LONG).show();
                                        });
                            }
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("MealsFragment", "Error getting recipe ingredients for removal", e);
                    Snackbar.make(requireView(), "Error getting recipe ingredients", Snackbar.LENGTH_LONG).show();
                });
    }

    private void handleError(String errorMessage) {
        if (getActivity() != null && getView() != null) {
            Snackbar.make(getView(), errorMessage, Snackbar.LENGTH_LONG).show();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
// Adapter for displaying a list of recipes in a RecyclerView

class RecipeAdapter extends RecyclerView.Adapter<RecipeAdapter.ViewHolder> {

    private final List<Recipe> recipes; // List of recipes to display
    private final OnRecipeSelectedListener listener; // Listener for recipe selection events

    // Interface for handling recipe selection
    public interface OnRecipeSelectedListener {

        void onRecipeSelected(Recipe recipe); // Called when a recipe is selected
    }

    // Constructor to initialize the adapter with a list of recipes and a listener
    public RecipeAdapter(List<Recipe> recipes, OnRecipeSelectedListener listener) {
        this.recipes = recipes;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate the layout for each recipe item
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_recipe_assign, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        // Bind the recipe data to the ViewHolder
        Recipe recipe = recipes.get(position);
        holder.bind(recipe);
    }

    @Override
    public int getItemCount() {
        // Return the total number of recipes
        return recipes.size();
    }

    // ViewHolder class to hold and bind views for each recipe item
    class ViewHolder extends RecyclerView.ViewHolder {

        private final TextView recipeName;
        private final TextView cookingTime;
        private final TextView servings;
        private final TextView recipeCategory;
        private final ImageView recipeImage;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            // Initialize views
            recipeName = itemView.findViewById(R.id.recipeName);
            cookingTime = itemView.findViewById(R.id.cookingTime);
            servings = itemView.findViewById(R.id.servings);
            recipeCategory = itemView.findViewById(R.id.recipeCategory);
            recipeImage = itemView.findViewById(R.id.recipeImage);

            // Set click listener for the item view
            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    listener.onRecipeSelected(recipes.get(position));
                }
            });
        }

        // Bind recipe data to the views
        public void bind(Recipe recipe) {
            recipeName.setText(recipe.getName());
            cookingTime.setText(recipe.getCookTime() + " min");
            servings.setText(recipe.getServings() + " servings");
            recipeCategory.setText(recipe.getCategory());

            String imageUrl = recipe.getImageUrl();
            if (imageUrl != null && !imageUrl.isEmpty()) {
                // Load recipe image using Glide
                Glide.with(itemView.getContext())
                        .load(imageUrl)
                        .placeholder(R.drawable.placeholder_recipe)
                        .error(R.drawable.placeholder_recipe)
                        .centerCrop()
                        .into(recipeImage);
            } else {
                recipeImage.setImageResource(R.drawable.placeholder_recipe);
            }
        }
    }
}

// Adapter for displaying a list of recipes in a different format (Map-based)
class RecipeAssignAdapter extends RecyclerView.Adapter<RecipeAssignAdapter.ViewHolder> {

    private final List<Map<String, Object>> recipes; // List of recipes as maps
    private final OnRecipeClickListener listener; // Listener for recipe click events

    // Interface for handling recipe click events
    public interface OnRecipeClickListener {

        void onRecipeClick(Map<String, Object> recipe); // Called when a recipe is clicked
    }

    // Constructor to initialize the adapter with a list of recipes and a listener
    public RecipeAssignAdapter(List<Map<String, Object>> recipes, OnRecipeClickListener listener) {
        this.recipes = recipes;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate the layout for each recipe item
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_recipe_assign, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        // Bind the recipe data to the ViewHolder
        Map<String, Object> recipe = recipes.get(position);
        holder.bind(recipe);
    }

    @Override
    public int getItemCount() {
        // Return the total number of recipes
        return recipes.size();
    }

    // ViewHolder class to hold and bind views for each recipe item
    class ViewHolder extends RecyclerView.ViewHolder {

        private final TextView recipeName;
        private final TextView cookingTime;
        private final TextView servings;
        private final TextView recipeCategory;
        private final ImageView recipeImage;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            // Initialize views
            recipeName = itemView.findViewById(R.id.recipeName);
            cookingTime = itemView.findViewById(R.id.cookingTime);
            servings = itemView.findViewById(R.id.servings);
            recipeCategory = itemView.findViewById(R.id.recipeCategory);
            recipeImage = itemView.findViewById(R.id.recipeImage);

            // Set click listener for the item view
            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    listener.onRecipeClick(recipes.get(position));
                }
            });
        }

        // Bind recipe data (as a map) to the views
        public void bind(Map<String, Object> recipe) {
            try {
                String name = (String) recipe.get("name");
                Long cookingTimeValue = (Long) recipe.get("cookTime");
                Long servingsValue = (Long) recipe.get("servings");
                String category = (String) recipe.get("category");
                String imageUrl = (String) recipe.get("imageUrl");

                recipeName.setText(name != null ? name : "Unnamed Recipe");
                cookingTime.setText(cookingTimeValue != null ? cookingTimeValue + " min" : "0 min");
                servings.setText(servingsValue != null ? servingsValue + " servings" : "0 servings");
                recipeCategory.setText(category != null ? category : "Uncategorized");

                if (imageUrl != null && !imageUrl.isEmpty()) {
                    // Load recipe image using Glide
                    Glide.with(itemView.getContext())
                            .load(imageUrl)
                            .placeholder(R.drawable.placeholder_recipe)
                            .error(R.drawable.placeholder_recipe)
                            .centerCrop()
                            .into(recipeImage);
                } else {
                    recipeImage.setImageResource(R.drawable.placeholder_recipe);
                }
            } catch (Exception e) {
                // Handle any potential casting errors
                recipeName.setText("Error loading recipe");
                cookingTime.setText("--");
                servings.setText("--");
                recipeCategory.setText("--");
                recipeImage.setImageResource(R.drawable.placeholder_recipe);
            }
        }
    }
}

// Listener interface for loading recipes asynchronously
interface OnRecipesLoadedListener {

    void onRecipesLoaded(List<Recipe> recipes); // Called when recipes are loaded
}
