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
            Log.e("HomeFragment", "Cannot setup meal plan - fragment not attached or binding is null");
            return;
        }

        // Get today's day name
        Calendar calendar = Calendar.getInstance();
        String[] days = {"Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"};
        String today = days[calendar.get(Calendar.DAY_OF_WEEK) - 1];
        
        Log.d("HomeFragment", "Today's day: " + today);

        // Query meal plan for today
        String userId = auth.getCurrentUser().getUid();
        Log.d("HomeFragment", "Querying meal plans for userId: " + userId + " and day: " + today);
        
        db.collection("meal_plans")
                .whereEqualTo("userId", userId)
                .whereEqualTo("day", today)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    // Check if binding is still valid
                    if (binding == null || !isAdded()) {
                        Log.e("HomeFragment", "Cannot process meal plans - binding is null or fragment is not attached");
                        return;
                    }

                    Log.d("HomeFragment", "Found " + queryDocumentSnapshots.size() + " meal plans");
                    if (!queryDocumentSnapshots.isEmpty()) {
                        // Create a set to avoid duplicate recipe IDs
                        Set<String> uniqueRecipeIds = new HashSet<>();
                        
                        // Collect recipe IDs from all meal plans for today
                        for (DocumentSnapshot mealPlan : queryDocumentSnapshots.getDocuments()) {
                            List<String> recipeIds = (List<String>) mealPlan.get("recipeIds");
                            if (recipeIds != null) {
                                uniqueRecipeIds.addAll(recipeIds);
                            }
                        }
                        
                        Log.d("HomeFragment", "Found " + uniqueRecipeIds.size() + " unique recipe IDs");
                        
                        if (!uniqueRecipeIds.isEmpty()) {
                            loadTodayRecipes(new ArrayList<>(uniqueRecipeIds));
                        } else {
                            showEmptyMealPlan();
                        }
                    } else {
                        showEmptyMealPlan();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("HomeFragment", "Error loading meal plan", e);
                    if (isAdded() && getView() != null) {
                        Snackbar.make(getView(), "Error loading meal plan: " + e.getMessage(), Snackbar.LENGTH_LONG).show();
                    }
                });
    }

    private void showEmptyMealPlan() {
        // Check if binding is null or fragment is not attached
        if (binding == null || !isAdded()) {
            Log.e("HomeFragment", "Cannot show empty meal plan - binding is null or fragment is not attached");
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
            Log.e("HomeFragment", "Cannot load recipes - binding is null or fragment is not attached");
            return;
        }

        List<Recipe> breakfastRecipes = new ArrayList<>();
        List<Recipe> lunchRecipes = new ArrayList<>();
        List<Recipe> dinnerRecipes = new ArrayList<>();
        AtomicInteger loadedCount = new AtomicInteger(0);

        // Hide empty state message
        binding.emptyMealPlanText.setVisibility(View.GONE);

        Log.d("HomeFragment", "Loading " + recipeIds.size() + " recipes");

        for (String recipeId : recipeIds) {
            Log.d("HomeFragment", "Loading recipe with ID: " + recipeId);
            db.collection("recipes").document(recipeId).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        // Check if binding is still valid
                        if (binding == null || !isAdded()) {
                            Log.e("HomeFragment", "Cannot process recipe - binding is null or fragment is not attached");
                            return;
                        }

                        if (documentSnapshot.exists()) {
                            try {
                                Recipe recipe = new Recipe();
                                recipe.setId(documentSnapshot.getId());
                                recipe.setName(documentSnapshot.getString("name"));
                                recipe.setDescription(documentSnapshot.getString("description"));
                                recipe.setImageUrl(documentSnapshot.getString("imageUrl"));
                                recipe.setCategory(documentSnapshot.getString("category"));
                                
                                // Handle numeric values
                                Long cookTime = documentSnapshot.getLong("cookTime");
                                recipe.setCookTime(cookTime != null ? cookTime.intValue() : 0);
                                
                                Long servings = documentSnapshot.getLong("servings");
                                recipe.setServings(servings != null ? servings.intValue() : 0);
                                
                                Double calories = documentSnapshot.getDouble("calories");
                                recipe.setCalories(calories != null ? calories : 0.0);
                                
                                Double totalPrice = documentSnapshot.getDouble("totalPrice");
                                recipe.setTotalPrice(totalPrice != null ? totalPrice : 0.0);

                                // Convert Lists to arrays for ingredients and instructions
                                Object ingredientsObj = documentSnapshot.get("ingredients");
                                if (ingredientsObj instanceof List) {
                                    List<?> ingredientsList = (List<?>) ingredientsObj;
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
                                                
                                                String displayString = quantity > 0 && unit != null ? 
                                                    String.format("%.1f %s %s", quantity, unit, name) : name;
                                                displayIngredients.add(displayString);
                                            }
                                        }
                                    }
                                    recipe.setIngredients(displayIngredients.toArray(new String[0]));
                                } else {
                                    recipe.setIngredients(new String[0]);
                                }

                                List<String> instructionsList = (List<String>) documentSnapshot.get("instructions");
                                if (instructionsList != null) {
                                    recipe.setInstructions(instructionsList.toArray(new String[0]));
                                } else {
                                    recipe.setInstructions(new String[0]);
                                }
                                
                                Log.d("HomeFragment", "Loaded recipe: " + recipe.getName() + " with category: " + recipe.getCategory());
                                
                                // Categorize recipe
                                String category = recipe.getCategory().toLowerCase();
                                if (category.contains("breakfast")) {
                                    breakfastRecipes.add(recipe);
                                } else if (category.contains("lunch")) {
                                    lunchRecipes.add(recipe);
                                } else if (category.contains("dinner")) {
                                    dinnerRecipes.add(recipe);
                                }

                                int currentCount = loadedCount.incrementAndGet();
                                Log.d("HomeFragment", "Loaded " + currentCount + " of " + recipeIds.size() + " recipes");
                                
                                if (currentCount == recipeIds.size()) {
                                    // Final null check before updating UI
                                    if (binding != null && isAdded()) {
                                        Log.d("HomeFragment", "All recipes loaded. Breakfast: " + breakfastRecipes.size() + 
                                                ", Lunch: " + lunchRecipes.size() + 
                                                ", Dinner: " + dinnerRecipes.size());
                                        
                                        // All recipes loaded, setup adapters for each section
                                        setupMealSection(binding.breakfastSection, "Breakfast", breakfastRecipes);
                                        setupMealSection(binding.lunchSection, "Lunch", lunchRecipes);
                                        setupMealSection(binding.dinnerSection, "Dinner", dinnerRecipes);

                                        // Show/hide sections based on content
                                        binding.breakfastSection.setVisibility(breakfastRecipes.isEmpty() ? View.GONE : View.VISIBLE);
                                        binding.lunchSection.setVisibility(lunchRecipes.isEmpty() ? View.GONE : View.VISIBLE);
                                        binding.dinnerSection.setVisibility(dinnerRecipes.isEmpty() ? View.GONE : View.VISIBLE);
                                    }
                                }
                            } catch (Exception e) {
                                Log.e("HomeFragment", "Error loading recipe: " + recipeId, e);
                                if (isAdded() && getView() != null) {
                                    Snackbar.make(getView(), "Error loading recipe: " + e.getMessage(), Snackbar.LENGTH_LONG).show();
                                }
                            }
                        } else {
                            Log.d("HomeFragment", "Recipe document does not exist: " + recipeId);
                        }
                    });
        }
    }

    private void setupMealSection(View section, String title, List<Recipe> recipes) {
        if (recipes.isEmpty()) return;

        // Set section title based on the section type
        TextView titleView;
        RecyclerView recyclerView;
        
        if (section == binding.breakfastSection) {
            titleView = binding.breakfastTitle;
            recyclerView = binding.breakfastRecyclerView;
        } else if (section == binding.lunchSection) {
            titleView = binding.lunchTitle;
            recyclerView = binding.lunchRecyclerView;
        } else if (section == binding.dinnerSection) {
            titleView = binding.dinnerTitle;
            recyclerView = binding.dinnerRecyclerView;
        } else {
            return;
        }

        // Set the title
        titleView.setText(title);
        titleView.setVisibility(View.VISIBLE);

        // Configure RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        recyclerView.setHasFixedSize(true);
        
        // Create and set adapter
        RecipeAdapter adapter = new RecipeAdapter(recipes, new RecipeAdapter.OnRecipeClickListener() {
            @Override
            public void onAddToPlanClick(Recipe recipe) {
                // Do nothing as the button will be hidden
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
        
        // Make sure the section is visible
        section.setVisibility(View.VISIBLE);
        
        Log.d("HomeFragment", "Setup section: " + title + " with " + recipes.size() + " recipes");
    }

    @Override
    public void onAddToPlanClick(Recipe recipe) {
        showDaySelectionDialog(recipe);
    }

    @Override
    public void onRecipeClick(Recipe recipe) {
        String userId = auth.getCurrentUser().getUid();
        
        // First save the recipe to Firestore if it doesn't exist
        db.collection("recipes")
                .document(recipe.getId())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        // Get the full ingredient data
                        List<Map<String, Object>> ingredientsList = recipe.getIngredientsList();
                        
                        // Convert ingredients to display format
                        List<String> ingredientsDisplay = new ArrayList<>();
                        for (Map<String, Object> ingredient : ingredientsList) {
                            String name = (String) ingredient.get("name");
                            Object quantityObj = ingredient.get("quantity");
                            String unit = (String) ingredient.get("unit");
                            
                            double quantity = 1.0;
                            if (quantityObj instanceof Number) {
                                quantity = ((Number) quantityObj).doubleValue();
                            }
                            
                            String displayString = quantity > 0 && unit != null ? 
                                String.format("%.1f %s %s", quantity, unit, name) : name;
                            ingredientsDisplay.add(displayString);
                        }
                        
                        // Save the recipe to Firestore
                        Map<String, Object> recipeData = new HashMap<>();
                        recipeData.put("id", recipe.getId());
                        recipeData.put("name", recipe.getName());
                        recipeData.put("description", recipe.getDescription());
                        recipeData.put("imageUrl", recipe.getImageUrl());
                        recipeData.put("cookTime", recipe.getCookTime());
                        recipeData.put("servings", recipe.getServings());
                        recipeData.put("ingredients", ingredientsDisplay);  // Save display format
                        recipeData.put("ingredientsList", ingredientsList); // Save full data
                        recipeData.put("instructions", Arrays.asList(recipe.getInstructions()));
                        recipeData.put("category", recipe.getCategory());
                        recipeData.put("calories", recipe.getCalories());
                        recipeData.put("totalPrice", recipe.getTotalPrice());
                        recipeData.put("userId", userId);

                        db.collection("recipes")
                                .document(recipe.getId())
                                .set(recipeData)
                                .addOnSuccessListener(aVoid -> {
                                    // After saving, navigate to recipe details
                                    Bundle bundle = new Bundle();
                                    bundle.putString("recipeId", recipe.getId());
                                    bundle.putBoolean("fromMeals", false);
                                    Navigation.findNavController(requireView())
                                            .navigate(R.id.action_navigation_home_to_recipeDetailsFragment, bundle);
                                })
                                .addOnFailureListener(e -> {
                                    if (getView() != null) {
                                        Snackbar.make(getView(), "Error saving recipe: " + e.getMessage(), Snackbar.LENGTH_LONG).show();
                                    }
                                });
                    } else {
                        // Recipe exists, just navigate to details
                        Bundle bundle = new Bundle();
                        bundle.putString("recipeId", recipe.getId());
                        bundle.putBoolean("fromMeals", false);
                        Navigation.findNavController(requireView())
                                .navigate(R.id.action_navigation_home_to_recipeDetailsFragment, bundle);
                    }
                })
                .addOnFailureListener(e -> {
                    if (getView() != null) {
                        Snackbar.make(getView(), "Error checking recipe: " + e.getMessage(), Snackbar.LENGTH_LONG).show();
                    }
                });
    }

    private void showDaySelectionDialog(Recipe recipe) {
        Dialog dialog = new Dialog(requireContext());
        dialog.setContentView(R.layout.dialog_select_day);
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        Spinner daySpinner = dialog.findViewById(R.id.daySpinner);
        Button cancelButton = dialog.findViewById(R.id.cancelButton);
        Button confirmButton = dialog.findViewById(R.id.confirmButton);

        // Setup day spinner
        String[] days = {"Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"};
        ArrayAdapter<String> dayAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, days);
        dayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        daySpinner.setAdapter(dayAdapter);

        // Setup button listeners
        cancelButton.setOnClickListener(v -> dialog.dismiss());

        confirmButton.setOnClickListener(v -> {
            String selectedDay = (String) daySpinner.getSelectedItem();
            addRecipeToMealPlan(recipe, selectedDay);
            dialog.dismiss();
        });

        dialog.show();
    }

    private void addRecipeToMealPlan(Recipe recipe, String day) {
        String userId = auth.getCurrentUser().getUid();
        
        // First save the recipe to Firestore if it doesn't exist
        db.collection("recipes")
                .document(recipe.getId())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        Log.d("HomeFragment", "Saving recipe to Firestore: " + recipe.getName());
                        
                        // Get the full ingredient data
                        List<Map<String, Object>> ingredientsList = recipe.getIngredientsList();
                        
                        Log.d("HomeFragment", "Ingredients being saved to Firestore:");
                        for (Map<String, Object> ingredient : ingredientsList) {
                            Log.d("HomeFragment", String.format("Ingredient - Name: %s, Price: %s, Type: %s",
                                    ingredient.get("name"),
                                    ingredient.get("price"),
                                    ingredient.get("type")));
                        }
                        
                        // Save the recipe to Firestore
                        Map<String, Object> recipeData = new HashMap<>();
                        recipeData.put("id", recipe.getId());
                        recipeData.put("name", recipe.getName());
                        recipeData.put("description", recipe.getDescription());
                        recipeData.put("imageUrl", recipe.getImageUrl());
                        recipeData.put("cookTime", recipe.getCookTime());
                        recipeData.put("servings", recipe.getServings());
                        recipeData.put("ingredients", ingredientsList);
                        recipeData.put("instructions", Arrays.asList(recipe.getInstructions()));
                        recipeData.put("category", recipe.getCategory());
                        recipeData.put("calories", recipe.getCalories());
                        recipeData.put("totalPrice", recipe.getTotalPrice());
                        recipeData.put("userId", userId);

                        db.collection("recipes")
                                .document(recipe.getId())
                                .set(recipeData)
                                .addOnSuccessListener(aVoid -> {
                                    Log.d("HomeFragment", "Successfully saved recipe to Firestore");
                                    addToMealPlan(recipe.getId(), userId, day);
                                })
                                .addOnFailureListener(e -> {
                                    Log.e("HomeFragment", "Error saving recipe", e);
                                    if (getView() != null) {
                                        Snackbar.make(getView(), "Error saving recipe: " + e.getMessage(), Snackbar.LENGTH_LONG).show();
                                    }
                                });
                    } else {
                        addToMealPlan(recipe.getId(), userId, day);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("HomeFragment", "Error checking recipe", e);
                    if (getView() != null) {
                        Snackbar.make(getView(), "Error checking recipe: " + e.getMessage(), Snackbar.LENGTH_LONG).show();
                    }
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
        Log.d("HomeFragment", "Adding ingredients to grocery list for recipe: " + recipeId);
        
        // Get recipe ingredients
        db.collection("recipes").document(recipeId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Log.d("HomeFragment", "Recipe document exists");
                        Log.d("HomeFragment", "Recipe name: " + documentSnapshot.getString("name"));
                        Log.d("HomeFragment", "Is featured: " + (documentSnapshot.getString("id").startsWith("featured_")));
                        
                        Object ingredientsObj = documentSnapshot.get("ingredients");
                        Log.d("HomeFragment", "Raw ingredients data structure: " + ingredientsObj.getClass().getName());
                        Log.d("HomeFragment", "Raw ingredients data: " + ingredientsObj);
                        
                        final List<Map<String, Object>> finalIngredients = new ArrayList<>();
                        
                        // Handle both List<String> and List<Map> formats
                        if (ingredientsObj instanceof List<?>) {
                            List<?> ingredientsList = (List<?>) ingredientsObj;
                            Log.d("HomeFragment", "Ingredients list size: " + ingredientsList.size());
                            
                            if (!ingredientsList.isEmpty()) {
                                Object firstItem = ingredientsList.get(0);
                                Log.d("HomeFragment", "First ingredient class type: " + firstItem.getClass().getName());
                                Log.d("HomeFragment", "First ingredient data: " + firstItem);
                                
                                if (firstItem instanceof String) {
                                    Log.d("HomeFragment", "Processing string format ingredients (likely custom recipe)");
                                    // Convert List<String> to List<Map>
                                    for (Object ing : ingredientsList) {
                                        String ingStr = (String) ing;
                                        Log.d("HomeFragment", "Processing ingredient string: " + ingStr);
                                        
                                        // Parse the string format "quantity unit name"
                                        String[] parts = ingStr.split(" ", 3);
                                        Map<String, Object> ingMap = new HashMap<>();
                                        if (parts.length >= 3) {
                                            try {
                                                ingMap.put("quantity", Double.parseDouble(parts[0]));
                                                ingMap.put("unit", parts[1]);
                                                ingMap.put("name", parts[2]);
                                            } catch (NumberFormatException e) {
                                                ingMap.put("name", ingStr);
                                                ingMap.put("quantity", 1.0);
                                                ingMap.put("unit", "unit");
                                            }
                                        } else {
                                            ingMap.put("name", ingStr);
                                            ingMap.put("quantity", 1.0);
                                            ingMap.put("unit", "unit");
                                        }
                                        finalIngredients.add(ingMap);
                                        Log.d("HomeFragment", "Created ingredient map from string: " + ingMap);
                                    }
                                } else if (firstItem instanceof Map) {
                                    Log.d("HomeFragment", "Processing map format ingredients (likely featured recipe)");
                                    // Already in Map format
                                    List<Map<String, Object>> mapList = (List<Map<String, Object>>) ingredientsObj;
                                    for (Map<String, Object> ing : mapList) {
                                        Log.d("HomeFragment", "Processing ingredient map: " + ing);
                                        Log.d("HomeFragment", "Original price value: " + ing.get("price") + 
                                                            " (Type: " + (ing.get("price") != null ? ing.get("price").getClass().getName() : "null") + ")");
                                        
                                        // Create a new map to avoid reference issues
                                        Map<String, Object> newIng = new HashMap<>();
                                        // Copy all fields
                                        for (Map.Entry<String, Object> entry : ing.entrySet()) {
                                            if (entry.getValue() != null) {
                                                if (entry.getKey().equals("price")) {
                                                    // Ensure price is stored as Double
                                                    Object priceValue = entry.getValue();
                                                    if (priceValue instanceof Number) {
                                                        double price = ((Number) priceValue).doubleValue();
                                                        newIng.put("price", price);
                                                        Log.d("HomeFragment", "Converted price to double: " + price);
                                                    } else if (priceValue instanceof String) {
                                                        try {
                                                            double price = Double.parseDouble((String) priceValue);
                                                            newIng.put("price", price);
                                                            Log.d("HomeFragment", "Parsed price from string: " + price);
                                                        } catch (NumberFormatException e) {
                                                            Log.e("HomeFragment", "Error parsing price from string: " + priceValue);
                                                            newIng.put("price", 0.0);
                                                        }
                                                    }
                                                } else {
                                                    newIng.put(entry.getKey(), entry.getValue());
                                                }
                                            }
                                        }
                                        
                                        // Ensure price exists
                                        if (!newIng.containsKey("price")) {
                                            newIng.put("price", 0.0);
                                            Log.d("HomeFragment", "Added default price: 0.0");
                                        }
                                        
                                        Log.d("HomeFragment", "Final ingredient map: " + newIng);
                                        finalIngredients.add(newIng);
                                    }
                                }
                            }
                        }
                        
                        Log.d("HomeFragment", "Final ingredients list size: " + finalIngredients.size());
                        
                        if (!finalIngredients.isEmpty()) {
                            // Reference to user's grocery list document
                            DocumentReference groceryListRef = db.collection("groceryLists").document(userId);

                            // Get the current grocery list first
                            groceryListRef.get().addOnSuccessListener(groceryDoc -> {
                                Map<String, Object> existingList = groceryDoc.exists() ? groceryDoc.getData() : new HashMap<>();
                                Map<String, Object> updates = new HashMap<>();

                                // Process each ingredient
                                for (Map<String, Object> ingredient : finalIngredients) {
                                    String name = (String) ingredient.get("name");
                                    if (name != null) {
                                        // Create a unique ID for the ingredient
                                        String ingredientId = name.toLowerCase().replaceAll("[^a-z0-9]", "_");
                                        Log.d("HomeFragment", "Processing ingredient: " + name + " (ID: " + ingredientId + ")");
                                        Log.d("HomeFragment", "Original ingredient data: " + ingredient);
                                        
                                        // Check if ingredient already exists
                                        if (existingList.containsKey(ingredientId)) {
                                            // Update existing ingredient
                                            Map<String, Object> existingIngredient = (Map<String, Object>) existingList.get(ingredientId);
                                            Log.d("HomeFragment", "Existing ingredient data: " + existingIngredient);
                                            
                                            double existingQuantity = existingIngredient.containsKey("quantity") ? 
                                                ((Number) existingIngredient.get("quantity")).doubleValue() : 0.0;
                                            double newQuantity = ingredient.containsKey("quantity") ? 
                                                ((Number) ingredient.get("quantity")).doubleValue() : 1.0;
                                            
                                            // Create new map to avoid reference issues
                                            Map<String, Object> updatedIngredient = new HashMap<>();
                                            // Copy all existing fields
                                            updatedIngredient.putAll(existingIngredient);
                                            // Update quantity
                                            updatedIngredient.put("quantity", existingQuantity + newQuantity);
                                            
                                            // Handle price: prefer the new price if it exists and is greater than 0
                                            if (ingredient.containsKey("price")) {
                                                Object priceObj = ingredient.get("price");
                                                double newPrice = 0.0;
                                                if (priceObj instanceof Number) {
                                                    newPrice = ((Number) priceObj).doubleValue();
                                                } else if (priceObj instanceof String) {
                                                    try {
                                                        newPrice = Double.parseDouble((String) priceObj);
                                                    } catch (NumberFormatException e) {
                                                        Log.e("HomeFragment", "Error parsing price: " + priceObj);
                                                    }
                                                }
                                                if (newPrice > 0) {
                                                    updatedIngredient.put("price", newPrice);
                                                    Log.d("HomeFragment", "Updated price to: " + newPrice);
                                                }
                                            }
                                            
                                            updates.put(ingredientId, updatedIngredient);
                                            Log.d("HomeFragment", "Updated ingredient data: " + updatedIngredient);
                                        } else {
                                            // Add new ingredient
                                            Map<String, Object> ingredientData = new HashMap<>();
                                            // Copy all fields from original ingredient
                                            ingredientData.putAll(ingredient);
                                            // Add additional fields
                                            ingredientData.put("bought", false);
                                            ingredientData.put("selectedForSms", false);
                                            ingredientData.put("userId", userId);
                                            
                                            // Ensure required fields exist
                                            if (!ingredientData.containsKey("quantity")) {
                                                ingredientData.put("quantity", 1.0);
                                            }
                                            if (!ingredientData.containsKey("unit")) {
                                                ingredientData.put("unit", "unit");
                                            }
                                            if (!ingredientData.containsKey("type")) {
                                                ingredientData.put("type", determineIngredientType(name));
                                            }
                                            
                                            // Handle price: ensure it's stored as Double
                                            if (ingredient.containsKey("price")) {
                                                Object priceObj = ingredient.get("price");
                                                double price = 0.0;
                                                if (priceObj instanceof Number) {
                                                    price = ((Number) priceObj).doubleValue();
                                                } else if (priceObj instanceof String) {
                                                    try {
                                                        price = Double.parseDouble((String) priceObj);
                                                    } catch (NumberFormatException e) {
                                                        Log.e("HomeFragment", "Error parsing price: " + priceObj);
                                                    }
                                                }
                                                ingredientData.put("price", price);
                                                Log.d("HomeFragment", "Set price to: " + price);
                                            } else {
                                                ingredientData.put("price", 0.0);
                                                Log.d("HomeFragment", "Set default price: 0.0");
                                            }
                                            
                                            updates.put(ingredientId, ingredientData);
                                            Log.d("HomeFragment", "New ingredient data: " + ingredientData);
                                        }
                                    }
                                }

                                // Update the grocery list with merged ingredients
                                if (!updates.isEmpty()) {
                                    Log.d("HomeFragment", "Final updates to be applied: " + updates);
                                    groceryListRef.set(updates, SetOptions.merge())
                                        .addOnSuccessListener(aVoid -> {
                                            Log.d("HomeFragment", "Successfully added ingredients to grocery list");
                                            if (getView() != null) {
                                                Snackbar.make(getView(), "Added ingredients to grocery list", Snackbar.LENGTH_SHORT).show();
                                            }
                                        })
                                        .addOnFailureListener(e -> {
                                            Log.e("HomeFragment", "Error updating grocery list", e);
                                            if (getView() != null) {
                                                Snackbar.make(getView(), "Error updating grocery list: " + e.getMessage(), Snackbar.LENGTH_LONG).show();
                                            }
                                        });
                                }
                            });
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("HomeFragment", "Error getting recipe ingredients", e);
                    if (getView() != null) {
                        Snackbar.make(getView(), "Error getting recipe ingredients: " + e.getMessage(), Snackbar.LENGTH_LONG).show();
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