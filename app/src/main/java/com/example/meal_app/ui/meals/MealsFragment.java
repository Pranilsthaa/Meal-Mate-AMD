package com.example.meal_app.ui.meals;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.meal_app.R;
import com.example.meal_app.adapter.MealDayAdapter;
import com.example.meal_app.databinding.FragmentMealsBinding;
import com.example.meal_app.model.MealDay;
import com.example.meal_app.model.Recipe;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.DocumentSnapshot;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MealsFragment extends Fragment {

    private FragmentMealsBinding binding;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private RecyclerView mealsRecyclerView;
    private MealDayAdapter adapter;
    private List<MealDay> mealDays;
    private List<Recipe> allRecipes;
    private Calendar currentWeek;
    private Map<String, List<Map<String, Object>>> weeklyMeals;
    private SimpleDateFormat weekFormat;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        currentWeek = Calendar.getInstance();
        weeklyMeals = new HashMap<>();
        weekFormat = new SimpleDateFormat("MM/dd/yyyy", Locale.getDefault());
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentMealsBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        setupWeekNavigation();
        setupAssignMealsButton();
        setupRecyclerView();
        loadRecipes();
        loadMealPlan();

        return root;
    }

    private void setupWeekNavigation() {
        updateWeekText();

        binding.previousWeekButton.setOnClickListener(v -> {
            currentWeek.add(Calendar.WEEK_OF_YEAR, -1);
            updateWeekText();
            loadWeeklyMeals();
        });

        binding.nextWeekButton.setOnClickListener(v -> {
            currentWeek.add(Calendar.WEEK_OF_YEAR, 1);
            updateWeekText();
            loadWeeklyMeals();
        });
    }

    private void updateWeekText() {
        String weekText = "Week of " + weekFormat.format(currentWeek.getTime());
        binding.weekText.setText(weekText);
    }

    private void setupAssignMealsButton() {
        binding.assignMealsButton.setOnClickListener(v -> showAssignMealsDialog());
    }

    private void setupRecyclerView() {
        mealsRecyclerView = binding.mealsRecyclerView;
        mealDays = new ArrayList<>();

        // Initialize meal days
        String[] days = { "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday" };
        for (String day : days) {
            mealDays.add(new MealDay(day, new ArrayList<>()));
        }

        adapter = new MealDayAdapter(mealDays, new MealDayAdapter.OnMealDayListener() {
            @Override
            public void onMealDayClick(String day) {
                // Show assign meal dialog for the selected day
                for (MealDay mealDay : mealDays) {
                    if (mealDay.getDay().equals(day)) {
                        showAssignMealDialog(mealDay);
                        break;
                    }
                }
            }

            @Override
            public void onRemoveMealClick(String day, Recipe recipe) {
                // Remove meal from the selected day
                for (MealDay mealDay : mealDays) {
                    if (mealDay.getDay().equals(day)) {
                        removeMealFromDay(mealDay, recipe);
                        break;
                    }
                }
            }
        });
        mealsRecyclerView.setAdapter(adapter);
        mealsRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
    }

    private void loadRecipes() {
        allRecipes = new ArrayList<>();
        db.collection("recipes")
                .whereEqualTo("userId", auth.getCurrentUser().getUid())
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Recipe recipe = document.toObject(Recipe.class);
                        if (recipe != null) {
                            recipe.setId(document.getId());
                            allRecipes.add(recipe);
                        }
                    }
                })
                .addOnFailureListener(e -> Snackbar
                        .make(requireView(), "Failed to load recipes: " + e.getMessage(), Snackbar.LENGTH_LONG).show());
    }

    private void loadMealPlan() {
        db.collection("mealPlans")
                .document(auth.getCurrentUser().getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Map<String, Object> data = documentSnapshot.getData();
                        if (data != null) {
                            for (MealDay mealDay : mealDays) {
                                Object dayData = data.get(mealDay.getDay());
                                if (dayData instanceof List<?>) {
                                    List<?> dayMeals = (List<?>) dayData;
                                    List<Recipe> recipes = new ArrayList<>();

                                    for (Object mealObj : dayMeals) {
                                        if (mealObj instanceof Map) {
                                            @SuppressWarnings("unchecked")
                                            Map<String, Object> meal = (Map<String, Object>) mealObj;
                                            String recipeId = (String) meal.get("recipeId");
                                            Recipe recipe = findRecipeById(recipeId);
                                            if (recipe != null) {
                                                recipes.add(recipe);
                                            }
                                        }
                                    }
                                    mealDay.setRecipes(recipes);
                                }
                            }
                            adapter.notifyDataSetChanged();
                        }
                    }
                })
                .addOnFailureListener(e -> Snackbar
                        .make(requireView(), "Failed to load meal plan: " + e.getMessage(), Snackbar.LENGTH_LONG)
                        .show());
    }

    private Recipe findRecipeById(String recipeId) {
        for (Recipe recipe : allRecipes) {
            if (recipe.getId().equals(recipeId)) {
                return recipe;
            }
        }
        return null;
    }

    private void showAssignMealsDialog() {
        Dialog dialog = new Dialog(requireContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_assign_meals);

        RecyclerView recipesRecyclerView = dialog.findViewById(R.id.recipesRecyclerView);
        recipesRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        dialog.findViewById(R.id.closeButton).setOnClickListener(v -> dialog.dismiss());

        loadRecipes(recipesRecyclerView, dialog);

        dialog.show();
    }

    private void loadRecipes(RecyclerView recyclerView, Dialog dialog) {
        String userId = auth.getCurrentUser().getUid();
        Snackbar.make(requireView(), "Loading recipes for user: " + userId, Snackbar.LENGTH_SHORT).show();

        // First, verify if the collection exists and has documents
        db.collection("recipes").get()
                .addOnSuccessListener(allDocs -> {
                    Snackbar.make(requireView(), "Total recipes in collection: " + allDocs.size(),
                            Snackbar.LENGTH_SHORT).show();

                    // Now query for user's recipes
                    db.collection("recipes")
                            .whereEqualTo("userId", userId)
                            .get()
                            .addOnSuccessListener(queryDocumentSnapshots -> {
                                int totalDocs = queryDocumentSnapshots.size();
                                Snackbar.make(requireView(), "Found " + totalDocs + " recipes for current user",
                                        Snackbar.LENGTH_SHORT).show();

                                if (totalDocs == 0) {
                                    // Let's check the first few documents to see if userId field exists
                                    if (allDocs.size() > 0) {
                                        DocumentSnapshot firstDoc = allDocs.getDocuments().get(0);
                                        String fields = firstDoc.getData() != null ? firstDoc.getData().toString()
                                                : "no data";
                                        requireActivity().runOnUiThread(() -> {
                                            Snackbar.make(recyclerView, "First document fields: " + fields,
                                                    Snackbar.LENGTH_LONG).show();
                                        });
                                    }
                                }

                                List<Map<String, Object>> recipes = new ArrayList<>();
                                for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                                    Map<String, Object> recipe = document.getData();
                                    recipe.put("id", document.getId());
                                    recipes.add(recipe);
                                }

                                if (recipes.isEmpty()) {
                                    Snackbar.make(requireView(), "No recipes found. Add some recipes first!",
                                            Snackbar.LENGTH_LONG)
                                            .show();
                                    dialog.dismiss();
                                } else {
                                    setupRecipesAdapter(recyclerView, recipes, dialog);
                                }
                            })
                            .addOnFailureListener(e -> {
                                String errorMsg = "Failed to load recipes: " + e.getMessage();
                                Snackbar.make(requireView(), errorMsg, Snackbar.LENGTH_LONG).show();
                                dialog.dismiss();
                            });
                })
                .addOnFailureListener(e -> {
                    String errorMsg = "Failed to check recipes collection: " + e.getMessage();
                    Snackbar.make(requireView(), errorMsg, Snackbar.LENGTH_LONG).show();
                    dialog.dismiss();
                });
    }

    private void setupRecipesAdapter(RecyclerView recyclerView, List<Map<String, Object>> recipes, Dialog dialog) {
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        RecipeAssignAdapter adapter = new RecipeAssignAdapter(recipes, (recipe, day) -> {
            assignMealToDay(recipe, day);
            dialog.dismiss();
        });
        recyclerView.setAdapter(adapter);
    }

    private void loadWeeklyMeals() {
        // Load meals from Firestore for the current week
        weeklyMeals.clear();
        loadMealPlan();
    }

    private void assignMealToDay(Map<String, Object> recipe, String day) {
        // Find the corresponding MealDay
        for (MealDay mealDay : mealDays) {
            if (mealDay.getDay().equals(day)) {
                // Create a Recipe object from the map with all required data
                Recipe newRecipe = new Recipe();
                newRecipe.setId((String) recipe.get("id"));
                newRecipe.setName((String) recipe.get("name"));
                newRecipe.setDescription((String) recipe.get("description"));
                newRecipe.setImageUrl((String) recipe.get("imageUrl"));
                newRecipe.setCategory((String) recipe.get("category"));
                newRecipe.setCookingTime(((Long) recipe.get("cookingTime")).intValue());
                newRecipe.setServings(((Long) recipe.get("servings")).intValue());
                newRecipe.setTotalPrice(((Number) recipe.get("totalPrice")).doubleValue());
                newRecipe.setUserId((String) recipe.get("userId"));

                // Add the recipe to the day
                mealDay.getRecipes().add(newRecipe);

                // Update Firestore with complete recipe data
                Map<String, Object> update = new HashMap<>();
                List<Map<String, Object>> meals = new ArrayList<>();
                for (Recipe r : mealDay.getRecipes()) {
                    Map<String, Object> meal = new HashMap<>();
                    meal.put("recipeId", r.getId());
                    meal.put("name", r.getName());
                    meal.put("description", r.getDescription());
                    meal.put("imageUrl", r.getImageUrl());
                    meal.put("category", r.getCategory());
                    meal.put("cookingTime", r.getCookingTime());
                    meal.put("servings", r.getServings());
                    meal.put("totalPrice", r.getTotalPrice());
                    meal.put("userId", r.getUserId());
                    meals.add(meal);
                }
                update.put(day, meals);

                db.collection("mealPlans")
                        .document(auth.getCurrentUser().getUid())
                        .set(update, SetOptions.merge())
                        .addOnSuccessListener(aVoid -> {
                            adapter.notifyDataSetChanged();
                            Snackbar.make(requireView(), "Meal assigned successfully", Snackbar.LENGTH_SHORT).show();
                        })
                        .addOnFailureListener(e -> Snackbar
                                .make(requireView(), "Failed to assign meal: " + e.getMessage(), Snackbar.LENGTH_LONG)
                                .show());

                break;
            }
        }
    }

    private void showAssignMealDialog(MealDay mealDay) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_assign_meals, null);
        RecyclerView recipesRecyclerView = dialogView.findViewById(R.id.recipesRecyclerView);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle("Assign Meal for " + mealDay.getDay())
                .setView(dialogView)
                .setNegativeButton("Cancel", null)
                .create();

        RecipeAdapter recipeAdapter = new RecipeAdapter(allRecipes, recipe -> {
            assignRecipeToDay(mealDay, recipe);
            dialog.dismiss();
        });

        recipesRecyclerView.setAdapter(recipeAdapter);
        recipesRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        dialog.show();
    }

    private void assignRecipeToDay(MealDay mealDay, Recipe recipe) {
        List<Recipe> dayRecipes = mealDay.getRecipes();
        dayRecipes.add(recipe);

        // Update Firestore
        Map<String, Object> update = new HashMap<>();
        List<Map<String, Object>> meals = new ArrayList<>();

        for (Recipe r : dayRecipes) {
            Map<String, Object> meal = new HashMap<>();
            meal.put("recipeId", r.getId());
            meals.add(meal);
        }

        update.put(mealDay.getDay(), meals);

        db.collection("mealPlans")
                .document(auth.getCurrentUser().getUid())
                .set(update, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    adapter.notifyDataSetChanged();
                    Snackbar.make(requireView(), "Meal assigned successfully", Snackbar.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> Snackbar
                        .make(requireView(), "Failed to assign meal: " + e.getMessage(), Snackbar.LENGTH_LONG).show());
    }

    private void removeMealFromDay(MealDay mealDay, Recipe recipe) {
        List<Recipe> dayRecipes = mealDay.getRecipes();
        dayRecipes.remove(recipe);

        // Update Firestore
        Map<String, Object> update = new HashMap<>();
        List<Map<String, Object>> meals = new ArrayList<>();

        for (Recipe r : dayRecipes) {
            Map<String, Object> meal = new HashMap<>();
            meal.put("recipeId", r.getId());
            meals.add(meal);
        }

        update.put(mealDay.getDay(), meals);

        db.collection("mealPlans")
                .document(auth.getCurrentUser().getUid())
                .set(update, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    adapter.notifyDataSetChanged();
                    Snackbar.make(requireView(), "Meal removed successfully", Snackbar.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> Snackbar
                        .make(requireView(), "Failed to remove meal: " + e.getMessage(), Snackbar.LENGTH_LONG)
                        .show());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

class RecipeAdapter extends RecyclerView.Adapter<RecipeAdapter.ViewHolder> {
    private final List<Recipe> recipes;
    private final OnRecipeSelectedListener listener;

    public interface OnRecipeSelectedListener {
        void onRecipeSelected(Recipe recipe);
    }

    public RecipeAdapter(List<Recipe> recipes, OnRecipeSelectedListener listener) {
        this.recipes = recipes;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_recipe_assign, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Recipe recipe = recipes.get(position);
        holder.bind(recipe);
    }

    @Override
    public int getItemCount() {
        return recipes.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView recipeName;
        private final TextView cookingTime;
        private final TextView servings;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            recipeName = itemView.findViewById(R.id.recipeName);
            cookingTime = itemView.findViewById(R.id.cookingTime);
            servings = itemView.findViewById(R.id.servings);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    listener.onRecipeSelected(recipes.get(position));
                }
            });
        }

        public void bind(Recipe recipe) {
            recipeName.setText(recipe.getName());
            cookingTime.setText(recipe.getCookingTime() + " min");
            servings.setText(recipe.getServings() + " servings");
        }
    }
}

class RecipeAssignAdapter extends RecyclerView.Adapter<RecipeAssignAdapter.ViewHolder> {
    private final List<Map<String, Object>> recipes;
    private final OnRecipeAssignListener listener;

    public interface OnRecipeAssignListener {
        void onRecipeAssigned(Map<String, Object> recipe, String day);
    }

    public RecipeAssignAdapter(List<Map<String, Object>> recipes, OnRecipeAssignListener listener) {
        this.recipes = recipes;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_recipe_assign, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Map<String, Object> recipe = recipes.get(position);
        holder.bind(recipe);
    }

    @Override
    public int getItemCount() {
        return recipes.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView recipeName;
        private final TextView cookingTime;
        private final TextView servings;
        private final Spinner daySpinner;
        private final ImageView recipeImage;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            recipeName = itemView.findViewById(R.id.recipeName);
            cookingTime = itemView.findViewById(R.id.cookingTime);
            servings = itemView.findViewById(R.id.servings);
            daySpinner = itemView.findViewById(R.id.daySpinner);
            recipeImage = itemView.findViewById(R.id.recipeImage);

            setupDaySpinner();
        }

        private void setupDaySpinner() {
            String[] days = { "Select day", "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday",
                    "Saturday" };
            ArrayAdapter<String> adapter = new ArrayAdapter<>(itemView.getContext(),
                    android.R.layout.simple_spinner_item, days);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            daySpinner.setAdapter(adapter);

            daySpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                    if (position > 0) {
                        listener.onRecipeAssigned(recipes.get(getAdapterPosition()), days[position]);
                        daySpinner.setSelection(0);
                    }
                }

                @Override
                public void onNothingSelected(android.widget.AdapterView<?> parent) {
                }
            });
        }

        public void bind(Map<String, Object> recipe) {
            try {
                String name = (String) recipe.get("name");
                Long cookingTimeValue = (Long) recipe.get("cookingTime");
                Long servingsValue = (Long) recipe.get("servings");
                String imageUrl = (String) recipe.get("imageUrl");

                recipeName.setText(name != null ? name : "Unnamed Recipe");
                cookingTime.setText(cookingTimeValue != null ? cookingTimeValue + " min" : "0 min");
                servings.setText(servingsValue != null ? servingsValue + " servings" : "0 servings");

                // Load recipe image if available
                if (imageUrl != null && !imageUrl.isEmpty()) {
                    Glide.with(itemView.getContext())
                            .load(imageUrl)
                            .centerCrop()
                            .into(recipeImage);
                    recipeImage.setVisibility(View.VISIBLE);
                } else {
                    recipeImage.setVisibility(View.GONE);
                }
            } catch (Exception e) {
                // Handle any potential casting errors
                recipeName.setText("Error loading recipe");
                cookingTime.setText("--");
                servings.setText("--");
                recipeImage.setVisibility(View.GONE);
            }
        }
    }
}