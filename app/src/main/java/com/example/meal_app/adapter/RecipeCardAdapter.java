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
import com.example.meal_app.databinding.ItemRecipeCardBinding;
import androidx.constraintlayout.widget.ConstraintLayout;
import java.util.List;

/**
 * Adapter class for displaying a list of recipes in a RecyclerView.
 */
public class RecipeCardAdapter extends RecyclerView.Adapter<RecipeCardAdapter.RecipeViewHolder> {

    private final List<Recipe> recipes; // List of recipes to display
    private final OnRecipeClickListener listener; // Listener for handling recipe click events

    /**
     * Interface for handling recipe click events.
     */
    public interface OnRecipeClickListener {

        void onRecipeClick(Recipe recipe); // Called when a recipe is clicked
    }

    /**
     * Constructor for RecipeCardAdapter.
     *
     * @param recipes List of recipes to display
     * @param listener Listener for handling recipe click events
     */
    public RecipeCardAdapter(List<Recipe> recipes, OnRecipeClickListener listener) {
        this.recipes = recipes;
        this.listener = listener;
    }

    @NonNull
    @Override
    public RecipeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate the layout for each recipe card
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_recipe_card, parent, false);
        return new RecipeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecipeViewHolder holder, int position) {
        // Bind the recipe data to the ViewHolder
        Recipe recipe = recipes.get(position);
        holder.bind(recipe);
    }

    @Override
    public int getItemCount() {
        // Return the total number of recipes
        return recipes.size();
    }

    /**
     * ViewHolder class for holding and binding recipe card views.
     */
    public class RecipeViewHolder extends RecyclerView.ViewHolder {

        public final ConstraintLayout foregroundView; // Foreground view for swipe actions
        private final ImageView recipeImage; // ImageView for displaying the recipe image
        private final TextView recipeName; // TextView for displaying the recipe name
        private final TextView recipeCategory; // TextView for displaying the recipe category
        private final TextView recipeTime; // TextView for displaying the cooking time
        private final TextView recipeServings; // TextView for displaying the number of servings

        /**
         * Constructor for RecipeViewHolder.
         *
         * @param itemView The root view of the recipe card
         */
        public RecipeViewHolder(@NonNull View itemView) {
            super(itemView);
            // Initialize views
            foregroundView = itemView.findViewById(R.id.foreground);
            recipeImage = itemView.findViewById(R.id.recipeImage);
            recipeName = itemView.findViewById(R.id.recipeName);
            recipeCategory = itemView.findViewById(R.id.recipeCategory);
            recipeTime = itemView.findViewById(R.id.recipeTime);
            recipeServings = itemView.findViewById(R.id.recipeServings);

            // Set click listener for the recipe card
            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    listener.onRecipeClick(recipes.get(position));
                }
            });
        }

        /**
         * Bind the recipe data to the views.
         *
         * @param recipe The recipe to display
         */
        public void bind(Recipe recipe) {
            // Set recipe details
            recipeName.setText(recipe.getName());
            recipeCategory.setText(recipe.getCategory());
            recipeTime.setText(String.format("%d min", recipe.getCookTime()));
            recipeServings.setText(String.format("%d servings", recipe.getServings()));

            // Load recipe image using Glide
            String imageUrl = recipe.getImageUrl();
            if (imageUrl != null && !imageUrl.isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(imageUrl)
                        .placeholder(R.drawable.placeholder_recipe) // Placeholder image
                        .error(R.drawable.placeholder_recipe) // Error image
                        .centerCrop() // Center crop the image
                        .timeout(60000) // Set timeout for image loading
                        .into(recipeImage);
            } else {
                // Set placeholder image if no image URL is provided
                recipeImage.setImageResource(R.drawable.placeholder_recipe);
            }
        }
    }
}
