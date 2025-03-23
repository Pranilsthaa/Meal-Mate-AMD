package com.example.meal_app.ui.home;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

public class HomeFragment extends Fragment implements RecipeAdapter.OnRecipeClickListener {

    private FragmentHomeBinding binding;
    private RecyclerView recipeRecyclerView;
    private RecipeAdapter recipeAdapter;
    private Button showMoreButton;
    private List<Recipe> recipes;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private RecyclerView breakfastRecyclerView;
    private RecyclerView lunchRecyclerView;
    private RecyclerView dinnerRecyclerView;
    private RecipeAdapter breakfastAdapter;
    private RecipeAdapter lunchAdapter;
    private RecipeAdapter dinnerAdapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        try {
            // Initialize Firebase
            db = FirebaseFirestore.getInstance();
            auth = FirebaseAuth.getInstance();

            // Set username in title
            if (auth.getCurrentUser() != null) {
                String userEmail = auth.getCurrentUser().getEmail();
                String username = userEmail != null ? userEmail.split("@")[0] : "User";
                binding.welcomeText.setText("Welcome, " + username);
            } else {
                binding.welcomeText.setText("Welcome, Guest");
            }

            // Initialize views
            recipeRecyclerView = binding.recipeRecyclerView;
            showMoreButton = binding.showMoreButton;
            breakfastRecyclerView = binding.breakfastRecyclerView;
            lunchRecyclerView = binding.lunchRecyclerView;
            dinnerRecyclerView = binding.dinnerRecyclerView;

            // Setup RecyclerViews
            setupRecyclerViews();
            loadTodaysMealPlan();

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
                    // TODO: Implement profile functionality
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

            // Setup meal plan RecyclerViews
            if (breakfastRecyclerView != null) {
                breakfastRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
                breakfastAdapter = new RecipeAdapter(new ArrayList<>(), this);
                breakfastRecyclerView.setAdapter(breakfastAdapter);
            }

            if (lunchRecyclerView != null) {
                lunchRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
                lunchAdapter = new RecipeAdapter(new ArrayList<>(), this);
                lunchRecyclerView.setAdapter(lunchAdapter);
            }

            if (dinnerRecyclerView != null) {
                dinnerRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
                dinnerAdapter = new RecipeAdapter(new ArrayList<>(), this);
                dinnerRecyclerView.setAdapter(dinnerAdapter);
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (getView() != null) {
                Snackbar.make(getView(), "Error setting up RecyclerViews: " + e.getMessage(), Snackbar.LENGTH_LONG).show();
            }
        }
    }

    private void loadTodaysMealPlan() {
        String userId = auth.getCurrentUser().getUid();
        String today = getCurrentDay();

        db.collection("meal_plans")
                .whereEqualTo("userId", userId)
                .whereEqualTo("day", today)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot document = queryDocumentSnapshots.getDocuments().get(0);
                        List<String> recipeIds = (List<String>) document.get("recipeIds");

                        if (recipeIds != null && !recipeIds.isEmpty()) {
                            loadRecipesForMealPlan(recipeIds);
                        } else {
                            // No recipes for today
                            clearMealPlanViews();
                        }
                    } else {
                        // No meal plan for today
                        clearMealPlanViews();
                    }
                })
                .addOnFailureListener(e -> {
                    if (getView() != null) {
                        Snackbar.make(getView(), "Error loading today's meal plan: " + e.getMessage(), Snackbar.LENGTH_LONG).show();
                    }
                });
    }

    private void loadRecipesForMealPlan(List<String> recipeIds) {
        if (recipeIds == null || recipeIds.isEmpty()) {
            clearMealPlanViews();
            return;
        }

        db.collection("recipes")
                .whereIn("id", recipeIds)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Recipe> breakfastRecipes = new ArrayList<>();
                    List<Recipe> lunchRecipes = new ArrayList<>();
                    List<Recipe> dinnerRecipes = new ArrayList<>();

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Recipe recipe = new Recipe();
                        recipe.setId(document.getId());
                        recipe.setName(document.getString("name") != null ? document.getString("name") : "");
                        recipe.setDescription(document.getString("description") != null ? document.getString("description") : "");
                        recipe.setImageUrl(document.getString("imageUrl") != null ? document.getString("imageUrl") : "");
                        recipe.setCategory(document.getString("category") != null ? document.getString("category") : "");
                        recipe.setCookTime(document.getLong("cookTime") != null ? document.getLong("cookTime").intValue() : 0);
                        recipe.setServings(document.getLong("servings") != null ? document.getLong("servings").intValue() : 0);
                        recipe.setCalories(document.getDouble("calories") != null ? document.getDouble("calories") : 0.0);
                        recipe.setTotalPrice(document.getDouble("totalPrice") != null ? document.getDouble("totalPrice") : 0.0);
                        recipe.setUserId(document.getString("userId") != null ? document.getString("userId") : "");

                        // Handle arrays
                        List<String> ingredientsList = (List<String>) document.get("ingredients");
                        List<String> instructionsList = (List<String>) document.get("instructions");
                        
                        recipe.setIngredients(ingredientsList != null ? ingredientsList.toArray(new String[0]) : new String[0]);
                        recipe.setInstructions(instructionsList != null ? instructionsList.toArray(new String[0]) : new String[0]);

                        String category = recipe.getCategory().toLowerCase();
                        if (category.contains("breakfast")) {
                            breakfastRecipes.add(recipe);
                        } else if (category.contains("lunch")) {
                            lunchRecipes.add(recipe);
                        } else if (category.contains("dinner")) {
                            dinnerRecipes.add(recipe);
                        }
                    }

                    updateMealPlanViews(breakfastRecipes, lunchRecipes, dinnerRecipes);
                })
                .addOnFailureListener(e -> {
                    if (getView() != null) {
                        Snackbar.make(getView(), "Error loading recipes: " + e.getMessage(), Snackbar.LENGTH_LONG).show();
                    }
                });
    }

    private void updateMealPlanViews(List<Recipe> breakfastRecipes, List<Recipe> lunchRecipes, List<Recipe> dinnerRecipes) {
        if (breakfastAdapter != null) {
            breakfastAdapter.updateData(breakfastRecipes);
        }
        if (lunchAdapter != null) {
            lunchAdapter.updateData(lunchRecipes);
        }
        if (dinnerAdapter != null) {
            dinnerAdapter.updateData(dinnerRecipes);
        }

        // Show/hide sections based on content
        if (binding.breakfastSection != null) {
            binding.breakfastSection.setVisibility(breakfastRecipes.isEmpty() ? View.GONE : View.VISIBLE);
        }
        if (binding.lunchSection != null) {
            binding.lunchSection.setVisibility(lunchRecipes.isEmpty() ? View.GONE : View.VISIBLE);
        }
        if (binding.dinnerSection != null) {
            binding.dinnerSection.setVisibility(dinnerRecipes.isEmpty() ? View.GONE : View.VISIBLE);
        }
    }

    private void clearMealPlanViews() {
        if (breakfastAdapter != null) {
            breakfastAdapter.updateData(new ArrayList<>());
        }
        if (lunchAdapter != null) {
            lunchAdapter.updateData(new ArrayList<>());
        }
        if (dinnerAdapter != null) {
            dinnerAdapter.updateData(new ArrayList<>());
        }

        // Hide all sections
        if (binding.breakfastSection != null) {
            binding.breakfastSection.setVisibility(View.GONE);
        }
        if (binding.lunchSection != null) {
            binding.lunchSection.setVisibility(View.GONE);
        }
        if (binding.dinnerSection != null) {
            binding.dinnerSection.setVisibility(View.GONE);
        }
    }

    private String getCurrentDay() {
        Calendar calendar = Calendar.getInstance();
        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
        String[] days = {"Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"};
        return days[dayOfWeek - 1];
    }

    @Override
    public void onAddToPlanClick(Recipe recipe) {
        showDaySelectionDialog(recipe);
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
                                        loadTodaysMealPlan(); // Reload today's meal plan
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
                                        loadTodaysMealPlan(); // Reload today's meal plan
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}