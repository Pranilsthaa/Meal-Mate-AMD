package com.example.meal_app.ui.home;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.meal_app.R;
import com.example.meal_app.adapter.RecipeAdapter;
import com.example.meal_app.data.RecipeData;
import com.example.meal_app.databinding.FragmentHomeBinding;
import com.example.meal_app.model.Recipe;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.FieldValue;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.HashSet;
import java.util.Set;
import android.widget.ImageView;

public class HomeFragment extends Fragment implements RecipeAdapter.OnRecipeClickListener {

    private FragmentHomeBinding binding;
    private RecyclerView recipeRecyclerView;
    private RecipeAdapter recipeAdapter;
    private Button showMoreButton;
    private List<Recipe> recipes;
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        try {
            // Initialize Firebase
            db = FirebaseFirestore.getInstance();
            auth = FirebaseAuth.getInstance();

            // Set username in title and setup today's meal plan
            if (auth.getCurrentUser() != null) {
                String userEmail = auth.getCurrentUser().getEmail();
                String username = userEmail != null ? userEmail.split("@")[0] : "User";
                binding.welcomeText.setText("Welcome, " + username);
                setupTodayMealPlan(); // Setup today's meal plan after confirming user is logged in
            } else {
                binding.welcomeText.setText("Welcome, Guest");
            }

            // Initialize views
            recipeRecyclerView = binding.recipeRecyclerView;
            showMoreButton = binding.showMoreButton;

            // Setup RecyclerViews
            setupRecyclerViews();

            // Setup show more button
            if (showMoreButton != null) {
                showMoreButton.setOnClickListener(v -> {
                    if (recipeAdapter != null) {
                        recipeAdapter.showMore();
                        showMoreButton.setVisibility(recipeAdapter.hasMoreItems() ? View.VISIBLE : View.GONE);
                    }
                });

                // Initially hide show more button if there are 5 or fewer items
                showMoreButton.setVisibility(recipeAdapter != null && recipeAdapter.hasMoreItems() ? View.VISIBLE : View.GONE);
            }

            // Setup FAB click listener
            if (binding.addFab != null) {
                binding.addFab.setOnClickListener(view -> {
                    Navigation.findNavController(requireView()).navigate(R.id.action_navigation_home_to_addRecipeFragment);
                });
            }

            // Setup profile button click listener
            if (binding.profileButton != null) {
                binding.profileButton.setOnClickListener(view -> {
                    Navigation.findNavController(requireView()).navigate(R.id.navigation_profile);
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (getView() != null) {
                Snackbar.make(getView(), "Error initializing the app: " + e.getMessage(), Snackbar.LENGTH_LONG).show();
            }
        }

        return root;
    }

    private void setupRecyclerViews() {
        try {
            // Setup featured recipes RecyclerView
            if (recipeRecyclerView != null) {
                recipeRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
                recipes = new ArrayList<>();
                recipeAdapter = new RecipeAdapter(recipes, this);
                recipeRecyclerView.setAdapter(recipeAdapter);
                
                // Load featured recipes from JSON file
                loadFeaturedRecipes();
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (getView() != null) {
                Snackbar.make(getView(), "Error setting up RecyclerViews: " + e.getMessage(), Snackbar.LENGTH_LONG).show();
            }
        }
    }

    private void loadFeaturedRecipes() {
        try {
            // Read the JSON file from assets
            InputStream is = requireContext().getAssets().open("featured_recipes.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            String json = new String(buffer, "UTF-8");

            // Debug log for JSON content
            Log.d("HomeFragment", "Featured Recipes JSON content:");
            Log.d("HomeFragment", json);

            // Parse JSON
            JSONObject jsonObject = new JSONObject(json);
            JSONArray featuredRecipesArray = jsonObject.getJSONArray("featured_recipes");
            
            recipes.clear();
            
            // Convert each JSON object to Recipe
            for (int i = 0; i < featuredRecipesArray.length(); i++) {
                JSONObject recipeJson = featuredRecipesArray.getJSONObject(i);
                Recipe recipe = new Recipe();
                
                recipe.setId(recipeJson.getString("id"));
                recipe.setName(recipeJson.getString("name"));
                recipe.setDescription(recipeJson.getString("description"));
                recipe.setImageUrl(recipeJson.getString("imageUrl"));
                recipe.setCookTime(recipeJson.getInt("cookTime"));
                recipe.setServings(recipeJson.getInt("servings"));
                recipe.setCategory(recipeJson.getString("category"));
                recipe.setCalories(recipeJson.getDouble("calories"));
                recipe.setTotalPrice(recipeJson.optDouble("totalPrice", 0.0));
                recipe.setFeatured(true);

                Log.d("HomeFragment", "Loading featured recipe: " + recipe.getName());

                // Parse ingredients
                JSONArray ingredientsArray = recipeJson.getJSONArray("ingredients");
                List<Map<String, Object>> ingredientsList = new ArrayList<>();
                List<String> ingredientsDisplay = new ArrayList<>();

                Log.d("HomeFragment", "Processing ingredients for " + recipe.getName() + ":");

                for (int j = 0; j < ingredientsArray.length(); j++) {
                    JSONObject ingredientJson = ingredientsArray.getJSONObject(j);
                    Map<String, Object> ingredient = new HashMap<>();
                    
                    // Get ingredient data with detailed logging
                    String name = ingredientJson.getString("name");
                    double quantity = ingredientJson.optDouble("quantity", 1.0);
                    String unit = ingredientJson.optString("unit", "unit");
                    double price = ingredientJson.optDouble("price", 0.0);
                    String type = ingredientJson.optString("type", "Other");

                    Log.d("HomeFragment", String.format("Ingredient from JSON - Name: %s, Quantity: %.2f, Unit: %s, Price: %.2f, Type: %s",
                            name, quantity, unit, price, type));
                    
                    // Create ingredient map with explicit type casting
                    ingredient.put("name", name);
                    ingredient.put("quantity", quantity);
                    ingredient.put("unit", unit);
                    ingredient.put("price", price);  // Store as double
                    ingredient.put("type", type);
                    ingredientsList.add(ingredient);
                    
                    // Create display string
                    String displayString = quantity > 0 ? String.format("%.1f %s %s", quantity, unit, name) : name;
                    ingredientsDisplay.add(displayString);
                    
                    Log.d("HomeFragment", "Created ingredient map: " + ingredient);
                }

                recipe.setIngredientsList(ingredientsList);
                String[] ingredientsDisplayArray = new String[ingredientsDisplay.size()];
                ingredientsDisplay.toArray(ingredientsDisplayArray);
                recipe.setIngredients(ingredientsDisplayArray);

                // Parse instructions
                JSONArray instructionsArray = recipeJson.getJSONArray("instructions");
                List<String> instructionsList = new ArrayList<>();
                for (int j = 0; j < instructionsArray.length(); j++) {
                    instructionsList.add(instructionsArray.getString(j));
                }
                recipe.setInstructions(instructionsList.toArray(new String[0]));

                recipes.add(recipe);
                
                Log.d("HomeFragment", "Added recipe with ingredients: " + ingredientsList);
            }
            
            recipeAdapter.notifyDataSetChanged();

            if (recipes.isEmpty() && getView() != null) {
                Snackbar.make(getView(), "No featured recipes available", Snackbar.LENGTH_SHORT).show();
            }
            
        } catch (IOException | JSONException e) {
            Log.e("HomeFragment", "Error loading featured recipes", e);
            if (getView() != null) {
                Snackbar.make(getView(), "Error loading featured recipes: " + e.getMessage(), Snackbar.LENGTH_LONG).show();
            }
        }
    }

    private void setupTodayMealPlan() {
        // Check if fragment is attached and binding is valid
        if (!isAdded() || binding == null) {
            Log.e("loghere", "Cannot setup meal plan - binding is null or fragment is not attached");
            return;
        }

        // Get today's day name
        Calendar calendar = Calendar.getInstance();
        String[] days = {"Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"};
        String today = days[calendar.get(Calendar.DAY_OF_WEEK) - 1];
        
        Log.d("loghere", "Today's day: " + today);

        // Query meal plan for today
        String userId = auth.getCurrentUser().getUid();
        Log.d("loghere", "Querying meal plans for userId: " + userId + " and day: " + today);
        
        db.collection("meal_plans")
                .whereEqualTo("userId", userId)
                .whereEqualTo("day", today)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    // Check if binding is still valid
                    if (binding == null || !isAdded()) {
                        Log.e("loghere", "Cannot process meal plans - binding is null or fragment is not attached");
                        return;
                    }

                    Log.d("loghere", "Found " + queryDocumentSnapshots.size() + " meal plans for today");
                    if (!queryDocumentSnapshots.isEmpty()) {
                        // Create a set to avoid duplicate recipe IDs
                        Set<String> uniqueRecipeIds = new HashSet<>();
                        
                        // Collect recipe IDs from all meal plans for today
                        for (DocumentSnapshot mealPlan : queryDocumentSnapshots.getDocuments()) {
                            List<String> recipeIds = (List<String>) mealPlan.get("recipeIds");
                            Log.d("loghere", "Meal plan document ID: " + mealPlan.getId() + " contains recipes: " + recipeIds);
                            if (recipeIds != null) {
                                uniqueRecipeIds.addAll(recipeIds);
                            }
                        }
                        
                        Log.d("loghere", "Total unique recipe IDs found: " + uniqueRecipeIds.size());
                        Log.d("loghere", "Unique recipe IDs: " + uniqueRecipeIds);
                        
                        if (!uniqueRecipeIds.isEmpty()) {
                            loadTodayRecipes(new ArrayList<>(uniqueRecipeIds));
                        } else {
                            Log.d("loghere", "No recipe IDs found in meal plans");
                            showEmptyMealPlan();
                        }
                    } else {
                        Log.d("loghere", "No meal plans found for today");
                        showEmptyMealPlan();
                    }
                });
    }

    private void showEmptyMealPlan() {
        // Check if binding is null or fragment is not attached
        if (binding == null || !isAdded()) {
            Log.e("loghere", "Cannot show empty meal plan - binding is null or fragment is not attached");
            return;
        }

        // Hide all meal sections
        binding.breakfastSection.setVisibility(View.GONE);
        binding.lunchSection.setVisibility(View.GONE);
        binding.dinnerSection.setVisibility(View.GONE);
        
        // Show empty state message
        binding.emptyMealPlanText.setVisibility(View.VISIBLE);
        binding.emptyMealPlanText.setText("No meals planned for today");
    }

    private void loadTodayRecipes(List<String> recipeIds) {
        // Check if binding is null or fragment is not attached
        if (binding == null || !isAdded()) {
            Log.e("loghere", "Cannot load recipes - binding is null or fragment is not attached");
            return;
        }

        List<Recipe> breakfastRecipes = new ArrayList<>();
        List<Recipe> lunchRecipes = new ArrayList<>();
        List<Recipe> dinnerRecipes = new ArrayList<>();
        AtomicInteger loadedCount = new AtomicInteger(0);

        Log.d("loghere", "Starting to load " + recipeIds.size() + " recipes");

        for (String recipeId : recipeIds) {
            Log.d("loghere", "Loading recipe with ID: " + recipeId);
            db.collection("recipes").document(recipeId).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            try {
                                Recipe recipe = new Recipe();
                                recipe.setId(documentSnapshot.getId());
                                recipe.setName(documentSnapshot.getString("name"));
                                recipe.setCategory(documentSnapshot.getString("category"));
                                recipe.setImageUrl(documentSnapshot.getString("imageUrl")); // Hardcoded image URL
                                recipe.setCookTime(documentSnapshot.getLong("cookTime") != null ? 
                                    documentSnapshot.getLong("cookTime").intValue() : documentSnapshot.getLong("cookingTime").intValue());
                                recipe.setServings(documentSnapshot.getLong("servings") != null ? 
                                    documentSnapshot.getLong("servings").intValue() : 0);
                                
                                String category = recipe.getCategory().toLowerCase();
                                Log.d("loghere", String.format("Recipe loaded - ID: %s, Name: %s, Category: %s", 
                                    recipe.getId(), recipe.getName(), category));

                                // Categorize recipe
                                if (category.contains("breakfast")) {
                                    breakfastRecipes.add(recipe);
                                    Log.d("loghere", "Added to breakfast category. Current breakfast recipes: " + breakfastRecipes.size());
                                } else if (category.contains("lunch")) {
                                    lunchRecipes.add(recipe);
                                    Log.d("loghere", "Added to lunch category. Current lunch recipes: " + lunchRecipes.size());
                                } else if (category.contains("dinner")) {
                                    dinnerRecipes.add(recipe);
                                    Log.d("loghere", "Added to dinner category. Current dinner recipes: " + dinnerRecipes.size());
                                }

                                int currentCount = loadedCount.incrementAndGet();
                                Log.d("loghere", String.format("Progress: %d/%d recipes loaded", currentCount, recipeIds.size()));
                                
                                if (currentCount == recipeIds.size()) {
                                    // Final null check before updating UI
                                    if (binding != null && isAdded()) {
                                        Log.d("loghere", String.format("All recipes loaded - Breakfast: %d, Lunch: %d, Dinner: %d", 
                                            breakfastRecipes.size(), lunchRecipes.size(), dinnerRecipes.size()));
                                        
                                        // Log details of each category
                                        Log.d("loghere", "Breakfast Recipes: " + getRecipeNames(breakfastRecipes));
                                        Log.d("loghere", "Lunch Recipes: " + getRecipeNames(lunchRecipes));
                                        Log.d("loghere", "Dinner Recipes: " + getRecipeNames(dinnerRecipes));
                                        
                                        // Setup sections
                                        if (!breakfastRecipes.isEmpty()) {
                                            setupMealSection(binding.breakfastSection, "Breakfast", new ArrayList<>(breakfastRecipes));
                                        }
                                        if (!lunchRecipes.isEmpty()) {
                                            setupMealSection(binding.lunchSection, "Lunch", new ArrayList<>(lunchRecipes));
                                        }
                                        if (!dinnerRecipes.isEmpty()) {
                                            setupMealSection(binding.dinnerSection, "Dinner", new ArrayList<>(dinnerRecipes));
                                        }

                                        // Update UI visibility
                                        binding.breakfastSection.setVisibility(breakfastRecipes.isEmpty() ? View.GONE : View.VISIBLE);
                                        binding.lunchSection.setVisibility(lunchRecipes.isEmpty() ? View.GONE : View.VISIBLE);
                                        binding.dinnerSection.setVisibility(dinnerRecipes.isEmpty() ? View.GONE : View.VISIBLE);
                                    }
                                }
                            } catch (Exception e) {
                                Log.e("loghere", "Error processing recipe: " + recipeId + ", Error: " + e.getMessage());
                            }
                        } else {
                            Log.d("loghere", "Recipe document does not exist: " + recipeId);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e("loghere", "Failed to load recipe: " + recipeId + ", Error: " + e.getMessage());
                    });
        }
    }

    // Helper method to get recipe names for logging
    private String getRecipeNames(List<Recipe> recipes) {
        List<String> names = new ArrayList<>();
        for (Recipe recipe : recipes) {
            names.add(recipe.getName());
        }
        return names.toString();
    }

    private void setupMealSection(View section, String title, List<Recipe> recipes) {
        if (recipes.isEmpty()) {
            Log.d("loghere", "Skipping empty " + title + " section");
            return;
        }

        Log.d("loghere", "Setting up " + title + " section with " + recipes.size() + " recipes");
        Log.d("loghere", title + " Recipes: " + getRecipeNames(recipes));

        // Set section title based on the section type
        TextView titleView;
        RecyclerView recyclerView;
        ImageView scrollIndicator;
        
        if (section == binding.breakfastSection) {
            titleView = binding.breakfastTitle;
            recyclerView = binding.breakfastRecyclerView;
            scrollIndicator = binding.breakfastScrollIndicator;
        } else if (section == binding.lunchSection) {
            titleView = binding.lunchTitle;
            recyclerView = binding.lunchRecyclerView;
            scrollIndicator = binding.lunchScrollIndicator;
        } else if (section == binding.dinnerSection) {
            titleView = binding.dinnerTitle;
            recyclerView = binding.dinnerRecyclerView;
            scrollIndicator = binding.dinnerScrollIndicator;
        } else {
            return;
        }

        // Set the title with count
        titleView.setText(String.format("%s (%d)", title, recipes.size()));
        titleView.setVisibility(View.VISIBLE);

        // Configure RecyclerView with fixed width items
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setHasFixedSize(true);
        
        // Add spacing between items
        int spacingInPixels = getResources().getDimensionPixelSize(R.dimen.recipe_card_spacing);
        recyclerView.addItemDecoration(new RecyclerView.ItemDecoration() {
            @Override
            public void getItemOffsets(@NonNull android.graphics.Rect outRect, @NonNull View view, 
                                     @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
                int position = parent.getChildAdapterPosition(view);
                // Add spacing to all items except the last one
                if (position != parent.getAdapter().getItemCount() - 1) {
                    outRect.right = spacingInPixels;
                }
            }
        });
        
        // Create and set adapter with a new instance of the recipe list
        List<Recipe> adapterRecipes = new ArrayList<>(recipes);
        RecipeAdapter adapter = new RecipeAdapter(adapterRecipes, new RecipeAdapter.OnRecipeClickListener() {
            @Override
            public void onAddToPlanClick(Recipe recipe) {
                showAddToMealPlanDialog(recipe);
            }

            @Override
            public void onRecipeClick(Recipe recipe) {
                Bundle bundle = new Bundle();
                bundle.putString("recipeId", recipe.getId());
                bundle.putBoolean("fromMeals", true);
                Navigation.findNavController(requireView())
                        .navigate(R.id.action_navigation_home_to_recipeDetailsFragment, bundle);
            }
        }, true); // Pass true to indicate this is for Today's Meal Plan
        
        recyclerView.setAdapter(adapter);
        adapter.notifyDataSetChanged(); // Ensure the adapter refreshes
        
        // Make sure the section is visible
        section.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.VISIBLE);
        
        // Show scroll indicator only if there are multiple items
        scrollIndicator.setVisibility(recipes.size() > 1 ? View.VISIBLE : View.GONE);
        
        // Add scroll listener to show/hide scroll indicator based on scroll position
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                boolean canScrollHorizontally = recyclerView.canScrollHorizontally(1) || 
                                             recyclerView.canScrollHorizontally(-1);
                scrollIndicator.setVisibility(canScrollHorizontally ? View.VISIBLE : View.GONE);
            }
        });
        
        Log.d("loghere", "Setup complete for " + title + " section. Adapter item count: " + adapter.getItemCount());
    }

    @Override
    public void onAddToPlanClick(Recipe recipe) {
        showAddToMealPlanDialog(recipe);
    }

    @Override
    public void onRecipeClick(Recipe recipe) {
        Bundle bundle = new Bundle();
        bundle.putString("recipeId", recipe.getId());
        bundle.putBoolean("fromMeals", false);
        Navigation.findNavController(requireView())
                .navigate(R.id.action_navigation_home_to_recipeDetailsFragment, bundle);
    }

    private void showAddToMealPlanDialog(Recipe recipe) {
        if (auth.getCurrentUser() == null)
            return;

        // Create and show the dialog
        Dialog dialog = new Dialog(requireContext());
        dialog.setContentView(R.layout.dialog_add_to_meal_plan);
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        // Initialize views
        Spinner daySpinner = dialog.findViewById(R.id.daySpinner);
        Button addButton = dialog.findViewById(R.id.addButton);
        Button cancelButton = dialog.findViewById(R.id.cancelButton);

        // Setup day spinner
        String[] days = { "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday" };
        ArrayAdapter<String> dayAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, days);
        dayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        daySpinner.setAdapter(dayAdapter);

        // Set today as default
        Calendar calendar = Calendar.getInstance();
        int today = calendar.get(Calendar.DAY_OF_WEEK) - 1;
        daySpinner.setSelection(today);

        // Setup buttons
        cancelButton.setOnClickListener(v -> dialog.dismiss());
        addButton.setOnClickListener(v -> {
            String day = (String) daySpinner.getSelectedItem();
            String userId = auth.getCurrentUser().getUid();
            
            // First save the featured recipe to the user's recipes collection
            saveFeaturedRecipeToUserCollection(recipe, userId, () -> {
                // Then add it to the meal plan
                addToMealPlan(recipe.getId(), userId, day);
                dialog.dismiss();
            });
        });

        dialog.show();
    }
    
    private void saveFeaturedRecipeToUserCollection(Recipe recipe, String userId, Runnable onComplete) {
        // Check if this featured recipe is already in the user's collection
        db.collection("recipes")
            .whereEqualTo("userId", userId)
            .whereEqualTo("name", recipe.getName())  // Use name as a simple way to check for duplicates
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                if (queryDocumentSnapshots.isEmpty()) {
                    // Recipe doesn't exist, save it
                    Map<String, Object> recipeData = new HashMap<>();
                    recipeData.put("name", recipe.getName());
                    recipeData.put("description", recipe.getDescription());
                    recipeData.put("imageUrl", recipe.getImageUrl());
                    recipeData.put("cookTime", recipe.getCookTime());
                    recipeData.put("servings", recipe.getServings());
                    recipeData.put("category", recipe.getCategory());
                    recipeData.put("calories", recipe.getCalories());
                    recipeData.put("totalPrice", recipe.getTotalPrice());
                    recipeData.put("userId", userId);
                    
                    // Save ingredients in both formats
                    if (recipe.getIngredientsList() != null && !recipe.getIngredientsList().isEmpty()) {
                        // Save the full ingredient data
                        recipeData.put("ingredientsList", recipe.getIngredientsList());
                        
                        // Create display format ingredients
                        List<String> displayIngredients = new ArrayList<>();
                        for (Map<String, Object> ingredient : recipe.getIngredientsList()) {
                            String name = (String) ingredient.get("name");
                            Object quantityObj = ingredient.get("quantity");
                            String unit = (String) ingredient.get("unit");
                            
                            double quantity = 1.0;
                            if (quantityObj instanceof Number) {
                                quantity = ((Number) quantityObj).doubleValue();
                            }
                            
                            String displayString = quantity > 0 && unit != null ? 
                                String.format("%.1f %s %s", quantity, unit, name) : name;
                            displayIngredients.add(displayString);
                        }
                        recipeData.put("ingredients", displayIngredients);
                    }
                    
                    // Convert instructions if they exist
                    if (recipe.getInstructions() != null && recipe.getInstructions().length > 0) {
                        recipeData.put("instructions", Arrays.asList(recipe.getInstructions()));
                    }
                    
                    db.collection("recipes")
                        .add(recipeData)
                        .addOnSuccessListener(documentReference -> {
                            Log.d("HomeFragment", "Featured recipe saved to user collection with ID: " + documentReference.getId());
                            // Update the recipe ID to the new document ID
                            recipe.setId(documentReference.getId());
                            onComplete.run();
                        })
                        .addOnFailureListener(e -> {
                            Log.e("HomeFragment", "Error saving featured recipe: " + e.getMessage());
                            // Still continue with the original ID if saving fails
                            onComplete.run();
                        });
                } else {
                    // Recipe already exists, use the existing ID
                    String existingId = queryDocumentSnapshots.getDocuments().get(0).getId();
                    Log.d("HomeFragment", "Featured recipe already exists with ID: " + existingId);
                    recipe.setId(existingId);
                    onComplete.run();
                }
            })
            .addOnFailureListener(e -> {
                Log.e("HomeFragment", "Error checking for existing recipe: " + e.getMessage());
                // Continue with the original ID if checking fails
                onComplete.run();
            });
    }

    private void addToMealPlan(String recipeId, String userId, String day) {
        // Check if a meal plan exists for this day
        db.collection("meal_plans")
                .whereEqualTo("userId", userId)
                .whereEqualTo("day", day)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        // Update existing meal plan
                        DocumentSnapshot document = queryDocumentSnapshots.getDocuments().get(0);
                        List<String> existingRecipeIds = (List<String>) document.get("recipeIds");
                        List<String> updatedRecipeIds = new ArrayList<>();

                        if (existingRecipeIds != null) {
                            updatedRecipeIds.addAll(existingRecipeIds);
                        }
                        updatedRecipeIds.add(recipeId);

                        db.collection("meal_plans")
                                .document(document.getId())
                                .update("recipeIds", updatedRecipeIds)
                                .addOnSuccessListener(aVoid -> {
                                    if (getView() != null) {
                                        Snackbar.make(getView(), "Added recipe to " + day, Snackbar.LENGTH_SHORT).show();
                                        setupTodayMealPlan(); // Reload today's meal plan
                                        addIngredientsToGroceryList(recipeId, userId); // Add ingredients to grocery list
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    if (getView() != null) {
                                        Snackbar.make(getView(), "Error updating meal plan: " + e.getMessage(), Snackbar.LENGTH_LONG).show();
                                    }
                                });
                    } else {
                        // Create new meal plan
                        java.util.Map<String, Object> mealPlan = new java.util.HashMap<>();
                        mealPlan.put("userId", userId);
                        mealPlan.put("day", day);
                        mealPlan.put("recipeIds", Arrays.asList(recipeId));

                        db.collection("meal_plans")
                                .add(mealPlan)
                                .addOnSuccessListener(documentReference -> {
                                    if (getView() != null) {
                                        Snackbar.make(getView(), "Added recipe to " + day, Snackbar.LENGTH_SHORT).show();
                                        setupTodayMealPlan(); // Reload today's meal plan
                                        addIngredientsToGroceryList(recipeId, userId); // Add ingredients to grocery list
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    if (getView() != null) {
                                        Snackbar.make(getView(), "Error adding recipe to meal plan: " + e.getMessage(), Snackbar.LENGTH_LONG).show();
                                    }
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    if (getView() != null) {
                        Snackbar.make(getView(), "Error checking meal plan: " + e.getMessage(), Snackbar.LENGTH_LONG).show();
                    }
                });
    }

    private String determineIngredientType(String ingredient) {
        // Common ingredient categories
        ingredient = ingredient.toLowerCase();
        
        if (ingredient.contains("meat") || ingredient.contains("chicken") || 
            ingredient.contains("beef") || ingredient.contains("pork") || 
            ingredient.contains("fish") || ingredient.contains("lamb")) {
            return "Meat & Fish";
        } else if (ingredient.contains("milk") || ingredient.contains("cheese") || 
                   ingredient.contains("yogurt") || ingredient.contains("cream") ||
                   ingredient.contains("butter")) {
            return "Dairy";
        } else if (ingredient.contains("apple") || ingredient.contains("banana") || 
                   ingredient.contains("orange") || ingredient.contains("berry") ||
                   ingredient.contains("fruit")) {
            return "Fruits";
        } else if (ingredient.contains("carrot") || ingredient.contains("potato") || 
                   ingredient.contains("onion") || ingredient.contains("tomato") ||
                   ingredient.contains("vegetable")) {
            return "Vegetables";
        } else if (ingredient.contains("rice") || ingredient.contains("pasta") || 
                   ingredient.contains("flour") || ingredient.contains("bread") ||
                   ingredient.contains("cereal")) {
            return "Grains & Pasta";
        } else if (ingredient.contains("oil") || ingredient.contains("sauce") || 
                   ingredient.contains("vinegar") || ingredient.contains("spice") ||
                   ingredient.contains("herb")) {
            return "Condiments & Spices";
        }
        
        return "Other";
    }

    private void addIngredientsToGroceryList(String recipeId, String userId) {
        if (recipeId == null || userId == null) {
            Log.e("HomeFragment", "Recipe ID or User ID is null");
            return;
        }

        Log.d("HomeFragment", "Adding ingredients to grocery list for recipe: " + recipeId);
        
        // Handle featured recipes
        if (recipeId.startsWith("featured_")) {
            try {
                // Load from featured recipes JSON
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
                        String recipeName = recipeJson.getString("name");
                        JSONArray ingredientsArray = recipeJson.getJSONArray("ingredients");
                        Map<String, Object> updates = new HashMap<>();

                        // Process each ingredient
                        for (int j = 0; j < ingredientsArray.length(); j++) {
                            JSONObject ingredientJson = ingredientsArray.getJSONObject(j);
                            String ingredientName = ingredientJson.getString("name");
                            String ingredientId = recipeName.toLowerCase().replaceAll("[^a-z0-9]", "_")
                                    + "_" + ingredientName.toLowerCase().replaceAll("[^a-z0-9]", "_");
                            
                            // Create ingredient data
                            Map<String, Object> ingredientData = new HashMap<>();
                            ingredientData.put("name", ingredientName);
                            ingredientData.put("quantity", ingredientJson.optDouble("quantity", 1.0));
                            ingredientData.put("unit", ingredientJson.optString("unit", "unit"));
                            ingredientData.put("checked", false);
                            ingredientData.put("recipeId", recipeId);
                            ingredientData.put("recipeName", recipeName);
                            
                            updates.put(ingredientId, ingredientData);
                        }

                        // Update grocery list
                        if (!updates.isEmpty()) {
                            db.collection("groceryLists").document(userId)
                                    .set(updates, SetOptions.merge())
                                    .addOnSuccessListener(aVoid -> {
                                        Log.d("HomeFragment", "Ingredients added to grocery list successfully");
                                        if (getView() != null) {
                                            Snackbar.make(getView(), "Added ingredients to grocery list", Snackbar.LENGTH_SHORT).show();
                                        }
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e("HomeFragment", "Error adding ingredients to grocery list", e);
                                        if (getView() != null) {
                                            Snackbar.make(getView(), "Failed to add ingredients to grocery list", Snackbar.LENGTH_SHORT).show();
                                        }
                                    });
                        }
                        return;
                    }
                }
            } catch (Exception e) {
                Log.e("HomeFragment", "Error processing featured recipe", e);
                if (getView() != null) {
                    Snackbar.make(getView(), "Error adding ingredients to grocery list", Snackbar.LENGTH_SHORT).show();
                }
            }
            return;
        }

        // Handle regular recipes from Firestore
        db.collection("recipes").document(recipeId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String recipeName = documentSnapshot.getString("name");
                        Object ingredientsObj = documentSnapshot.get("ingredients");
                        
                        if (recipeName == null) {
                            Log.e("HomeFragment", "Recipe name is null");
                            return;
                        }

                        Map<String, Object> updates = new HashMap<>();
                        
                        if (ingredientsObj instanceof List<?>) {
                            List<?> ingredientsList = (List<?>) ingredientsObj;
                            for (Object ingredient : ingredientsList) {
                                if (ingredient instanceof String) {
                                    // Handle string format
                                    String ingredientName = (String) ingredient;
                                    String ingredientId = recipeName.toLowerCase().replaceAll("[^a-z0-9]", "_")
                                            + "_" + ingredientName.toLowerCase().replaceAll("[^a-z0-9]", "_");
                                    
                                    Map<String, Object> ingredientData = new HashMap<>();
                                    ingredientData.put("name", ingredientName);
                                    ingredientData.put("quantity", 1.0);
                                    ingredientData.put("unit", "unit");
                                    ingredientData.put("checked", false);
                                    ingredientData.put("recipeId", recipeId);
                                    ingredientData.put("recipeName", recipeName);
                                    
                                    updates.put(ingredientId, ingredientData);
                                } else if (ingredient instanceof Map) {
                                    // Handle map format
                                    @SuppressWarnings("unchecked")
                                    Map<String, Object> ingredientMap = (Map<String, Object>) ingredient;
                                    String ingredientName = (String) ingredientMap.get("name");
                                    
                                    if (ingredientName != null) {
                                        String ingredientId = recipeName.toLowerCase().replaceAll("[^a-z0-9]", "_")
                                                + "_" + ingredientName.toLowerCase().replaceAll("[^a-z0-9]", "_");
                                        
                                        Map<String, Object> ingredientData = new HashMap<>(ingredientMap);
                                        ingredientData.put("checked", false);
                                        ingredientData.put("recipeId", recipeId);
                                        ingredientData.put("recipeName", recipeName);
                                        
                                        updates.put(ingredientId, ingredientData);
                                    }
                                }
                            }
                        }

                        if (!updates.isEmpty()) {
                            db.collection("groceryLists").document(userId)
                                    .set(updates, SetOptions.merge())
                                    .addOnSuccessListener(aVoid -> {
                                        Log.d("HomeFragment", "Ingredients added to grocery list successfully");
                                        if (getView() != null) {
                                            Snackbar.make(getView(), "Added ingredients to grocery list", Snackbar.LENGTH_SHORT).show();
                                        }
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e("HomeFragment", "Error adding ingredients to grocery list", e);
                                        if (getView() != null) {
                                            Snackbar.make(getView(), "Failed to add ingredients to grocery list", Snackbar.LENGTH_SHORT).show();
                                        }
                                    });
                        }
                    } else {
                        Log.e("HomeFragment", "Recipe document does not exist");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("HomeFragment", "Error getting recipe document", e);
                    if (getView() != null) {
                        Snackbar.make(getView(), "Error adding ingredients to grocery list", Snackbar.LENGTH_SHORT).show();
                    }
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
                                        Log.d("HomeFragment", "Successfully removed ingredients from grocery list");
                                        if (getView() != null) {
                                            Snackbar.make(getView(), "Removed ingredients from grocery list", Snackbar.LENGTH_SHORT).show();
                                        }
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e("HomeFragment", "Error removing ingredients from grocery list", e);
                                        if (getView() != null) {
                                            Snackbar.make(getView(), "Error removing ingredients from grocery list", Snackbar.LENGTH_LONG).show();
                                        }
                                    });
                            }
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("HomeFragment", "Error getting recipe ingredients for removal", e);
                    if (getView() != null) {
                        Snackbar.make(getView(), "Error getting recipe ingredients", Snackbar.LENGTH_LONG).show();
                    }
                });
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh today's meal plan when the fragment is focused
        if (auth.getCurrentUser() != null) {
            setupTodayMealPlan();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}