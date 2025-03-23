package com.example.meal_app.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.meal_app.R;
import com.example.meal_app.model.Recipe;
import com.google.android.material.button.MaterialButton;
import java.util.ArrayList;
import java.util.List;

public class RecipeAdapter extends RecyclerView.Adapter<RecipeAdapter.ViewHolder> {
    private List<Recipe> recipes;
    private OnRecipeClickListener listener;
    private static final int ITEMS_PER_PAGE = 5;
    private int currentPage = 1;

    public interface OnRecipeClickListener {
        void onAddToPlanClick(Recipe recipe);
    }

    public RecipeAdapter(List<Recipe> recipes, OnRecipeClickListener listener) {
        this.recipes = recipes;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_recipe, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Recipe recipe = recipes.get(position);
        
        holder.recipeName.setText(recipe.getName());
        holder.recipeDescription.setText(recipe.getDescription());
        holder.cookingTime.setText(String.format("%d min", recipe.getCookTime()));
        holder.servings.setText(String.format("%d servings", recipe.getServings()));
        holder.calories.setText(String.format("%.0f kcal", recipe.getCalories()));
        
        // Load recipe image
        Glide.with(holder.itemView.getContext())
            .load(recipe.getImageUrl())
            .placeholder(R.drawable.placeholder_recipe)
            .error(R.drawable.placeholder_recipe)
            .into(holder.recipeImage);
            
        holder.addToPlanButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onAddToPlanClick(recipe);
            }
        });
    }

    @Override
    public int getItemCount() {
        return Math.min(recipes.size(), currentPage * ITEMS_PER_PAGE);
    }

    public void showMore() {
        currentPage++;
        notifyDataSetChanged();
    }

    public boolean hasMoreItems() {
        return currentPage * ITEMS_PER_PAGE < recipes.size();
    }

    public void updateData(List<Recipe> newRecipes) {
        this.recipes = newRecipes;
        this.currentPage = 1;
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView recipeImage;
        TextView recipeName;
        TextView recipeDescription;
        TextView cookingTime;
        TextView servings;
        TextView calories;
        MaterialButton addToPlanButton;

        ViewHolder(View itemView) {
            super(itemView);
            recipeImage = itemView.findViewById(R.id.recipeImage);
            recipeName = itemView.findViewById(R.id.recipeName);
            recipeDescription = itemView.findViewById(R.id.recipeDescription);
            cookingTime = itemView.findViewById(R.id.cookingTime);
            servings = itemView.findViewById(R.id.servings);
            calories = itemView.findViewById(R.id.calories);
            addToPlanButton = itemView.findViewById(R.id.addToPlanButton);
        }
    }
} 