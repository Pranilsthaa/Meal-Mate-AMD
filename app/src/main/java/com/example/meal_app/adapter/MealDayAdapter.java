package com.example.meal_app.adapter;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.meal_app.R;
import com.example.meal_app.model.MealDay;
import com.example.meal_app.model.Recipe;
import java.util.List;

public class MealDayAdapter extends RecyclerView.Adapter<MealDayAdapter.ViewHolder> {
    private final List<MealDay> mealDays;
    private final OnMealDayListener listener;

    public interface OnMealDayListener {
        void onMealDayClick(String day);

        void onRemoveMealClick(String day, Recipe recipe);
    }

    public MealDayAdapter(List<MealDay> mealDays, OnMealDayListener listener) {
        this.mealDays = mealDays;
        this.listener = listener;
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

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            dayText = itemView.findViewById(R.id.dayText);
            mealsContainer = itemView.findViewById(R.id.mealsContainer);
        }

        public void bind(MealDay mealDay) {
            dayText.setText(mealDay.getDay());
            mealsContainer.removeAllViews();

            if (mealDay.getRecipes().isEmpty()) {
                // Add placeholder text for empty days
                View emptyView = LayoutInflater.from(itemView.getContext())
                        .inflate(R.layout.item_empty_meal_day, mealsContainer, false);

                TextView emptyText = emptyView.findViewById(R.id.emptyText);
                emptyText.setText("No meals assigned yet");

                emptyView.setOnClickListener(v -> listener.onMealDayClick(mealDay.getDay()));
                mealsContainer.addView(emptyView);
                return;
            }

            for (Recipe recipe : mealDay.getRecipes()) {
                View mealView = LayoutInflater.from(itemView.getContext())
                        .inflate(R.layout.item_meal_card, mealsContainer, false);

                // Bind recipe data to the card
                ImageView recipeImage = mealView.findViewById(R.id.recipeImage);
                TextView recipeName = mealView.findViewById(R.id.recipeName);
                TextView recipeCategory = mealView.findViewById(R.id.recipeCategory);
                TextView cookingTime = mealView.findViewById(R.id.cookingTime);
                TextView servings = mealView.findViewById(R.id.servings);
                ImageButton removeButton = mealView.findViewById(R.id.removeButton);

                recipeName.setText(recipe.getName());
                recipeCategory.setText(recipe.getCategory());
                cookingTime.setText(recipe.getCookingTime() + " min");
                servings.setText(recipe.getServings() + " servings");

                // Load recipe image
                if (recipe.getImageUrl() != null && !recipe.getImageUrl().isEmpty()) {
                    Glide.with(itemView.getContext())
                            .load(recipe.getImageUrl())
                            .centerCrop()
                            .placeholder(R.drawable.placeholder_recipe)
                            .into(recipeImage);
                }

                // Handle remove button click
                removeButton.setOnClickListener(v -> listener.onRemoveMealClick(mealDay.getDay(), recipe));

                // Handle card click to show recipe details
                mealView.setOnClickListener(v -> {
                    Bundle bundle = new Bundle();
                    bundle.putString("recipeId", recipe.getId());
                    bundle.putBoolean("fromMeals", true);
                    Navigation.findNavController(v)
                            .navigate(R.id.action_navigation_meals_to_recipeDetailsFragment, bundle);
                });

                mealsContainer.addView(mealView);
            }

            // Add meal button at the bottom if there are already meals
            View addButton = LayoutInflater.from(itemView.getContext())
                    .inflate(R.layout.item_add_meal_button, mealsContainer, false);
            addButton.setOnClickListener(v -> listener.onMealDayClick(mealDay.getDay()));
            mealsContainer.addView(addButton);
        }
    }
}