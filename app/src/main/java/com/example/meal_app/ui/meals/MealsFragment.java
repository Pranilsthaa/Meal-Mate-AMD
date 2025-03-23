package com.example.meal_app.ui.meals;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
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
        String[] days = { "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday" };
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
        if (auth.getCurrentUser() == null)
            return;

        String userId = auth.getCurrentUser().getUid();
        binding.progressBar.setVisibility(View.VISIBLE);
        mealsRecyclerView.setVisibility(View.GONE);

        db.collection("meal_plans")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
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
        String[] days = { "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday" };
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
                                    loadMealPlan();
                                    adapter.setDayLoading(day, false);
                                })
                                .addOnFailureListener(e -> {
                                    handleError("Error creating meal plan: " + e.getMessage());
                                    adapter.setDayLoading(day, false);
                                });
                    } else {
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
                                    loadMealPlan();
                                    adapter.setDayLoading(day, false);
                                })
                                .addOnFailureListener(e -> {
                                    handleError("Error updating meal plan: " + e.getMessage());
                                    adapter.setDayLoading(day, false);
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    handleError("Error checking meal plan: " + e.getMessage());
                    adapter.setDayLoading(day, false);
                });
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
        if (mealDay == null || recipe == null || auth.getCurrentUser() == null)
            return;

        String userId = auth.getCurrentUser().getUid();
        String day = mealDay.getDay();

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
                                        .addOnSuccessListener(aVoid -> loadMealPlan())
                                        .addOnFailureListener(
                                                e -> handleError("Error deleting meal plan: " + e.getMessage()));
                            } else {
                                // Update with remaining recipes
                                db.collection("meal_plans")
                                        .document(document.getId())
                                        .update("recipeIds", recipeIds)
                                        .addOnSuccessListener(aVoid -> loadMealPlan())
                                        .addOnFailureListener(
                                                e -> handleError("Error updating meal plan: " + e.getMessage()));
                            }
                        }
                    }
                })
                .addOnFailureListener(e -> handleError("Error removing meal: " + e.getMessage()));
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
        private final TextView recipeCategory;
        private final ImageView recipeImage;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            recipeName = itemView.findViewById(R.id.recipeName);
            cookingTime = itemView.findViewById(R.id.cookingTime);
            servings = itemView.findViewById(R.id.servings);
            recipeCategory = itemView.findViewById(R.id.recipeCategory);
            recipeImage = itemView.findViewById(R.id.recipeImage);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    listener.onRecipeSelected(recipes.get(position));
                }
            });
        }

        public void bind(Recipe recipe) {
            recipeName.setText(recipe.getName());
            cookingTime.setText(recipe.getCookTime() + " min");
            servings.setText(recipe.getServings() + " servings");
            recipeCategory.setText(recipe.getCategory());

            String imageUrl = recipe.getImageUrl();
            if (imageUrl != null && !imageUrl.isEmpty()) {
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

class RecipeAssignAdapter extends RecyclerView.Adapter<RecipeAssignAdapter.ViewHolder> {
    private final List<Map<String, Object>> recipes;
    private final OnRecipeClickListener listener;

    public interface OnRecipeClickListener {
        void onRecipeClick(Map<String, Object> recipe);
    }

    public RecipeAssignAdapter(List<Map<String, Object>> recipes, OnRecipeClickListener listener) {
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
        private final TextView recipeCategory;
        private final ImageView recipeImage;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            recipeName = itemView.findViewById(R.id.recipeName);
            cookingTime = itemView.findViewById(R.id.cookingTime);
            servings = itemView.findViewById(R.id.servings);
            recipeCategory = itemView.findViewById(R.id.recipeCategory);
            recipeImage = itemView.findViewById(R.id.recipeImage);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    listener.onRecipeClick(recipes.get(position));
                }
            });
        }

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

interface OnRecipesLoadedListener {
    void onRecipesLoaded(List<Recipe> recipes);
}