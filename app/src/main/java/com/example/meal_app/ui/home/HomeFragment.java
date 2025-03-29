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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

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
                recipes = RecipeData.getPredefinedRecipes();
                recipeAdapter = new RecipeAdapter(recipes, this);
                recipeRecyclerView.setAdapter(recipeAdapter);
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (getView() != null) {
                Snackbar.make(getView(), "Error setting up RecyclerViews: " + e.getMessage(), Snackbar.LENGTH_LONG).show();
            }
        }
    }

    private void setupTodayMealPlan() {
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
                    Log.d("HomeFragment", "Found " + queryDocumentSnapshots.size() + " meal plans");
                    if (!queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot mealPlan = queryDocumentSnapshots.getDocuments().get(0);
                        List<String> recipeIds = (List<String>) mealPlan.get("recipeIds");
                        Log.d("HomeFragment", "Found " + (recipeIds != null ? recipeIds.size() : 0) + " recipe IDs");
                        
                        if (recipeIds != null && !recipeIds.isEmpty()) {
                            loadTodayRecipes(recipeIds);
                        } else {
                            Log.d("HomeFragment", "No recipe IDs found in meal plan");
                            showEmptyMealPlan();
                        }
                    } else {
                        Log.d("HomeFragment", "No meal plan found for today");
                        showEmptyMealPlan();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("HomeFragment", "Error loading meal plan", e);
                    if (getView() != null) {
                        Snackbar.make(getView(), "Error loading meal plan: " + e.getMessage(), Snackbar.LENGTH_LONG).show();
                    }
                });
    }

    private void showEmptyMealPlan() {
        // Hide all meal sections
        binding.breakfastSection.setVisibility(View.GONE);
        binding.lunchSection.setVisibility(View.GONE);
        binding.dinnerSection.setVisibility(View.GONE);
        
        // Show empty state message
        binding.emptyMealPlanText.setVisibility(View.VISIBLE);
        binding.emptyMealPlanText.setText("No meals planned for today");
    }

    private void loadTodayRecipes(List<String> recipeIds) {
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
                                List<String> ingredientsList = (List<String>) documentSnapshot.get("ingredients");
                                if (ingredientsList != null) {
                                    recipe.setIngredients(ingredientsList.toArray(new String[0]));
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
                            } catch (Exception e) {
                                Log.e("HomeFragment", "Error loading recipe: " + recipeId, e);
                                if (getView() != null) {
                                    Snackbar.make(getView(), "Error loading recipe: " + e.getMessage(), Snackbar.LENGTH_LONG).show();
                                }
                            }
                        } else {
                            Log.d("HomeFragment", "Recipe document does not exist: " + recipeId);
                        }
                        
                        int currentCount = loadedCount.incrementAndGet();
                        Log.d("HomeFragment", "Loaded " + currentCount + " of " + recipeIds.size() + " recipes");
                        
                        if (currentCount == recipeIds.size()) {
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
                        // Convert arrays to Lists for Firestore
                        List<String> ingredientsList = Arrays.asList(recipe.getIngredients());
                        List<String> instructionsList = Arrays.asList(recipe.getInstructions());
                        
                        // Save the recipe to Firestore
                        Map<String, Object> recipeData = new HashMap<>();
                        recipeData.put("id", recipe.getId());
                        recipeData.put("name", recipe.getName());
                        recipeData.put("description", recipe.getDescription());
                        recipeData.put("imageUrl", recipe.getImageUrl());
                        recipeData.put("cookTime", recipe.getCookTime());
                        recipeData.put("servings", recipe.getServings());
                        recipeData.put("ingredients", ingredientsList);
                        recipeData.put("instructions", instructionsList);
                        recipeData.put("category", recipe.getCategory());
                        recipeData.put("calories", recipe.getCalories());
                        recipeData.put("totalPrice", recipe.getTotalPrice());
                        recipeData.put("userId", userId);

                        db.collection("recipes")
                                .document(recipe.getId())
                                .set(recipeData)
                                .addOnSuccessListener(aVoid -> {
                                    navigateToRecipeDetails(recipe.getId());
                                })
                                .addOnFailureListener(e -> {
                                    if (getView() != null) {
                                        Snackbar.make(getView(), "Error saving recipe: " + e.getMessage(), Snackbar.LENGTH_LONG).show();
                                    }
                                });
                    } else {
                        navigateToRecipeDetails(recipe.getId());
                    }
                })
                .addOnFailureListener(e -> {
                    if (getView() != null) {
                        Snackbar.make(getView(), "Error checking recipe: " + e.getMessage(), Snackbar.LENGTH_LONG).show();
                    }
                });
    }

    private void navigateToRecipeDetails(String recipeId) {
        Bundle bundle = new Bundle();
        bundle.putString("recipeId", recipeId);
        bundle.putBoolean("fromMeals", false);
        Navigation.findNavController(requireView())
                .navigate(R.id.action_navigation_home_to_recipeDetailsFragment, bundle);
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
                        // Convert arrays to Lists for Firestore
                        List<String> ingredientsList = Arrays.asList(recipe.getIngredients());
                        List<String> instructionsList = Arrays.asList(recipe.getInstructions());
                        
                        // Save the recipe to Firestore
                        java.util.Map<String, Object> recipeData = new java.util.HashMap<>();
                        recipeData.put("id", recipe.getId());
                        recipeData.put("name", recipe.getName());
                        recipeData.put("description", recipe.getDescription());
                        recipeData.put("imageUrl", recipe.getImageUrl());
                        recipeData.put("cookTime", recipe.getCookTime());
                        recipeData.put("servings", recipe.getServings());
                        recipeData.put("ingredients", ingredientsList);
                        recipeData.put("instructions", instructionsList);
                        recipeData.put("category", recipe.getCategory());
                        recipeData.put("calories", recipe.getCalories());
                        recipeData.put("totalPrice", recipe.getTotalPrice());
                        recipeData.put("userId", userId);

                        db.collection("recipes")
                                .document(recipe.getId())
                                .set(recipeData)
                                .addOnSuccessListener(aVoid -> {
                                    // After saving the recipe, add it to the meal plan
                                    addToMealPlan(recipe.getId(), userId, day);
                                })
                                .addOnFailureListener(e -> {
                                    if (getView() != null) {
                                        Snackbar.make(getView(), "Error saving recipe: " + e.getMessage(), Snackbar.LENGTH_LONG).show();
                                    }
                                });
                    } else {
                        // Recipe already exists, just add it to the meal plan
                        addToMealPlan(recipe.getId(), userId, day);
                    }
                })
                .addOnFailureListener(e -> {
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
        // Get recipe ingredients
        db.collection("recipes").document(recipeId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Get ingredients as List<String>
                        List<String> ingredients = (List<String>) documentSnapshot.get("ingredients");
                        if (ingredients != null && !ingredients.isEmpty()) {
                            // Reference to user's grocery list document
                            DocumentReference groceryListRef = db.collection("groceryLists").document(userId);

                            // Create a map to store all ingredient updates
                            Map<String, Object> updates = new HashMap<>();
                            
                            // Process each ingredient
                            for (String ingredient : ingredients) {
                                // Create a unique ID for the ingredient
                                String ingredientId = ingredient.toLowerCase().replace(" ", "_");
                                
                                // Create ingredient data
                                Map<String, Object> ingredientData = new HashMap<>();
                                ingredientData.put("name", ingredient);
                                ingredientData.put("category", determineIngredientType(ingredient));
                                ingredientData.put("bought", false);
                                ingredientData.put("selectedForSms", false);
                                ingredientData.put("price", 0.0); // Default price
                                ingredientData.put("quantity", 1.0); // Default quantity
                                ingredientData.put("unit", "g"); // Default unit
                                ingredientData.put("userId", userId);
                                
                                // Add to updates map
                                updates.put(ingredientId, ingredientData);
                            }

                            // Update the grocery list document with all ingredients at once
                            groceryListRef.set(updates, SetOptions.merge())
                                .addOnSuccessListener(aVoid -> {
                                    if (getView() != null) {
                                        Snackbar.make(getView(), 
                                            "Added ingredients to grocery list", 
                                            Snackbar.LENGTH_SHORT).show();
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    if (getView() != null) {
                                        Snackbar.make(getView(), 
                                            "Error updating grocery list: " + e.getMessage(), 
                                            Snackbar.LENGTH_LONG).show();
                                    }
                                });
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    if (getView() != null) {
                        Snackbar.make(getView(), 
                            "Error getting recipe ingredients: " + e.getMessage(), 
                            Snackbar.LENGTH_LONG).show();
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