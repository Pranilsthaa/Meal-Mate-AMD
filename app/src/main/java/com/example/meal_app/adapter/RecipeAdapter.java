package com.example.meal_app.adapter;

import android.util.Log;
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
    private boolean isMealPlanAdapter;

    public interface OnRecipeClickListener {
        void onAddToPlanClick(Recipe recipe);
        void onRecipeClick(Recipe recipe);
    }

    public RecipeAdapter(List<Recipe> recipes, OnRecipeClickListener listener) {
        this(recipes, listener, false);
    }

    public RecipeAdapter(List<Recipe> recipes, OnRecipeClickListener listener, boolean isMealPlanAdapter) {
        this.recipes = recipes;
        this.listener = listener;
        this.isMealPlanAdapter = isMealPlanAdapter;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        if (isMealPlanAdapter) {
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.meal_plan_recipe_card, parent, false);
        } else {
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.featured_recipe_card, parent, false);
        }
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Recipe recipe = recipes.get(position);
        
        // Set recipe name
        holder.recipeName.setText(recipe.getName());

        // Load recipe image using Glide with better error handling
        String imageUrl = recipe.getImageUrl();
        if (imageUrl != null && !imageUrl.isEmpty()) {
            Log.d("loghere", "Loading image for " + recipe.getName() + ": " + imageUrl);
            try {
                Glide.with(holder.itemView.getContext())
                    .load(imageUrl)
                    .placeholder(R.drawable.placeholder_image)
                    .error(R.drawable.error_image)
                    .centerCrop()
                    .listener(new com.bumptech.glide.request.RequestListener<android.graphics.drawable.Drawable>() {
                        @Override
                        public boolean onLoadFailed(@androidx.annotation.Nullable com.bumptech.glide.load.engine.GlideException e, Object model, com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target, boolean isFirstResource) {
                            Log.e("loghere", "Failed to load image for " + recipe.getName() + ": " + e.getMessage());
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(android.graphics.drawable.Drawable resource, Object model, com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target, com.bumptech.glide.load.DataSource dataSource, boolean isFirstResource) {
                            Log.d("loghere", "Successfully loaded image for " + recipe.getName());
                            return false;
                        }
                    })
                    .into(holder.recipeImage);
            } catch (Exception e) {
                Log.e("loghere", "Exception loading image for " + recipe.getName() + ": " + e.getMessage());
                holder.recipeImage.setImageResource(R.drawable.placeholder_image);
            }
        } else {
            Log.d("loghere", "No image URL for " + recipe.getName() + ", using placeholder");
            holder.recipeImage.setImageResource(R.drawable.placeholder_image);
        }

        // Set cook time
        if (holder.recipeCookTime != null) {
            holder.recipeCookTime.setText(recipe.getCookTime() + " min");
        }

        // Set servings
        if (holder.recipeServings != null) {
            holder.recipeServings.setText(recipe.getServings() + " ppl");
        }

        // Only set these for featured recipe cards
        if (!isMealPlanAdapter) {
            if (holder.recipeDescription != null) {
                holder.recipeDescription.setText(recipe.getDescription());
            }
            if (holder.recipeCategory != null) {
                holder.recipeCategory.setText(recipe.getCategory());
            }
            if (holder.recipeCalories != null) {
                holder.recipeCalories.setText(String.format("%.0f cal", recipe.getCalories()));
            }
            if (holder.addToPlanButton != null) {
                holder.addToPlanButton.setVisibility(View.VISIBLE);
                holder.addToPlanButton.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onAddToPlanClick(recipe);
                    }
                });
            }
        }

        // Set click listener for the entire item
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onRecipeClick(recipe);
            }
        });
    }

    @Override
    public int getItemCount() {
        // Show all items for meal plan, use pagination otherwise
        int count = isMealPlanAdapter ? recipes.size() : Math.min(recipes.size(), currentPage * ITEMS_PER_PAGE);
        Log.d("loghere", "RecipeAdapter getItemCount - isMealPlan: " + isMealPlanAdapter + 
              ", total recipes: " + recipes.size() + ", returning count: " + count);
        return count;
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

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public final TextView recipeName;
        public final ImageView recipeImage;
        public final TextView recipeCookTime;
        public final TextView recipeServings;
        // Optional views that might not be present in meal plan cards
        public final TextView recipeDescription;
        public final TextView recipeCategory;
        public final TextView recipeCalories;
        public final View addToPlanButton;

        public ViewHolder(View view) {
            super(view);
            recipeName = view.findViewById(R.id.recipeName);
            recipeImage = view.findViewById(R.id.recipeImage);
            recipeCookTime = view.findViewById(R.id.recipeCookTime);
            recipeServings = view.findViewById(R.id.recipeServings);
            // These might be null for meal plan cards
            recipeDescription = view.findViewById(R.id.recipeDescription);
            recipeCategory = view.findViewById(R.id.recipeCategory);
            recipeCalories = view.findViewById(R.id.recipeCalories);
            addToPlanButton = view.findViewById(R.id.addToPlanButton);
        }
    }
} 