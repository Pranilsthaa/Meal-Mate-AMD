package com.example.meal_app.adapter;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.meal_app.R;
import com.example.meal_app.model.MealDay;
import com.example.meal_app.model.Recipe;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Collections;

public class MealDayAdapter extends RecyclerView.Adapter<MealDayAdapter.ViewHolder> {
    private final List<MealDay> mealDays;
    private final OnMealDayListener listener;
    private final Map<String, Boolean> loadingStates = new HashMap<>();

    public interface OnMealDayListener {
        void onMealDayClick(String day);

        void onRemoveMealClick(String day, Recipe recipe);

        void onRecipeClick(Recipe recipe);
    }

    public MealDayAdapter(List<MealDay> mealDays, OnMealDayListener listener) {
        this.mealDays = mealDays;
        this.listener = listener;
    }

    public void setDayLoading(String day, boolean isLoading) {
        loadingStates.put(day, isLoading);
        notifyDataSetChanged(); // You might want to use a more specific notify method
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_meal_day, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MealDay mealDay = mealDays.get(position);
        holder.bind(mealDay);
    }

    @Override
    public int getItemCount() {
        return mealDays.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView dayText;
        private final ViewGroup mealsContainer;
        private final View loadingOverlay;
        private final ProgressBar progressBar;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            dayText = itemView.findViewById(R.id.dayText);
            mealsContainer = itemView.findViewById(R.id.mealsContainer);
            loadingOverlay = itemView.findViewById(R.id.loadingOverlay);
            progressBar = itemView.findViewById(R.id.progressBar);
        }

        public void bind(MealDay mealDay) {
            dayText.setText(mealDay.getDay());
            mealsContainer.removeAllViews();

            // Show/hide loading state
            boolean isLoading = loadingStates.containsKey(mealDay.getDay()) && loadingStates.get(mealDay.getDay());

            List<Recipe> recipes = mealDay.getRecipes();
            if (recipes.isEmpty() && !isLoading) {
                // Show empty state
                View emptyView = LayoutInflater.from(itemView.getContext())
                        .inflate(R.layout.item_empty_meal_day, mealsContainer, false);
                emptyView.setOnClickListener(v -> listener.onMealDayClick(mealDay.getDay()));
                mealsContainer.addView(emptyView);
            } else {
                // Group recipes by category
                Map<String, List<Recipe>> categorizedRecipes = new HashMap<>();
                for (Recipe recipe : recipes) {
                    String category = recipe.getCategory() != null ? recipe.getCategory() : "Other";
                    if (!categorizedRecipes.containsKey(category)) {
                        categorizedRecipes.put(category, new ArrayList<>());
                    }
                    categorizedRecipes.get(category).add(recipe);
                }

                // Sort categories alphabetically
                List<String> categories = new ArrayList<>(categorizedRecipes.keySet());
                Collections.sort(categories);

                // Add recipes by category
                for (String category : categories) {
                    // Add category header
                    View categoryHeader = LayoutInflater.from(itemView.getContext())
                            .inflate(R.layout.item_category_header, mealsContainer, false);
                    TextView categoryTitle = categoryHeader.findViewById(R.id.categoryTitle);
                    categoryTitle.setText(category);
                    mealsContainer.addView(categoryHeader);

                    // Add recipes for this category
                    for (Recipe recipe : categorizedRecipes.get(category)) {
                        View recipeView = LayoutInflater.from(itemView.getContext())
                                .inflate(R.layout.item_assigned_meal, mealsContainer, false);

                        ImageView recipeImage = recipeView.findViewById(R.id.recipeImage);
                        TextView recipeName = recipeView.findViewById(R.id.recipeName);
                        TextView cookingTime = recipeView.findViewById(R.id.cookingTime);
                        TextView servings = recipeView.findViewById(R.id.servings);
                        ImageButton removeButton = recipeView.findViewById(R.id.removeButton);
                        View loadingOverlay = recipeView.findViewById(R.id.loadingOverlay);

                        recipeName.setText(recipe.getName());
                        cookingTime.setText(recipe.getCookTime() + " min");
                        servings.setText(recipe.getServings() + " servings");

                        if (recipe.getImageUrl() != null && !recipe.getImageUrl().isEmpty()) {
                            Glide.with(itemView.getContext())
                                    .load(recipe.getImageUrl())
                                    .placeholder(R.drawable.placeholder_recipe)
                                    .error(R.drawable.placeholder_recipe)
                                    .centerCrop()
                                    .into(recipeImage);
                        } else {
                            recipeImage.setImageResource(R.drawable.placeholder_recipe);
                        }

                        // Handle loading state for individual cards
                        if (isLoading) {
                            recipeView.setAlpha(0.5f);
                            loadingOverlay.setVisibility(View.VISIBLE);
                            removeButton.setEnabled(false);
                            recipeView.setEnabled(false);
                        } else {
                            recipeView.setAlpha(1.0f);
                            loadingOverlay.setVisibility(View.GONE);
                            removeButton.setEnabled(true);
                            recipeView.setEnabled(true);
                            
                            removeButton.setOnClickListener(v -> {
                                // Show loading state for this card only
                                loadingOverlay.setVisibility(View.VISIBLE);
                                recipeView.setAlpha(0.5f);
                                removeButton.setEnabled(false);
                                recipeView.setEnabled(false);
                                
                                listener.onRemoveMealClick(mealDay.getDay(), recipe);
                            });
                            recipeView.setOnClickListener(v -> listener.onRecipeClick(recipe));
                        }

                        mealsContainer.addView(recipeView);
                    }
                }

                // Add "Add Another Meal" button if not in loading state
                if (!isLoading) {
                    View addButton = LayoutInflater.from(itemView.getContext())
                            .inflate(R.layout.item_add_meal_button, mealsContainer, false);
                    addButton.setOnClickListener(v -> listener.onMealDayClick(mealDay.getDay()));
                    mealsContainer.addView(addButton);
                }
            }
        }
    }
}