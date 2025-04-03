package com.example.meal_app.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.meal_app.R;
import com.example.meal_app.model.Recipe;
import java.util.List;

/**
 * Adapter class for displaying a list of assigned meals in a RecyclerView.
 */
public class AssignedMealsAdapter extends RecyclerView.Adapter<AssignedMealsAdapter.ViewHolder> {

    private final List<Recipe> recipes; // List of recipes to display
    private final OnMealRemoveListener listener; // Listener for handling meal removal actions

    /**
     * Interface for handling meal removal events.
     */
    public interface OnMealRemoveListener {

        void onMealRemove(Recipe recipe); // Called when a meal is removed
    }

    /**
     * Constructor for the adapter.
     *
     * @param recipes List of recipes to display.
     * @param listener Listener for handling meal removal actions.
     */
    public AssignedMealsAdapter(List<Recipe> recipes, OnMealRemoveListener listener) {
        this.recipes = recipes;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate the layout for each item in the RecyclerView
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_assigned_meal, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        // Bind the data for the current recipe to the ViewHolder
        Recipe recipe = recipes.get(position);
        holder.bind(recipe);
    }

    @Override
    public int getItemCount() {
        // Return the total number of recipes
        return recipes.size();
    }

    /**
     * ViewHolder class for managing individual item views in the RecyclerView.
     */
    class ViewHolder extends RecyclerView.ViewHolder {

        private final ImageView recipeImage; // ImageView for displaying the recipe image
        private final TextView recipeName; // TextView for displaying the recipe name
        private final TextView cookingTime; // TextView for displaying the cooking time
        private final TextView servings; // TextView for displaying the number of servings
        private final ImageButton removeButton; // Button for removing a recipe

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            // Initialize views
            recipeImage = itemView.findViewById(R.id.recipeImage);
            recipeName = itemView.findViewById(R.id.recipeName);
            cookingTime = itemView.findViewById(R.id.cookingTime);
            servings = itemView.findViewById(R.id.servings);
            removeButton = itemView.findViewById(R.id.removeButton);

            // Set up the remove button click listener
            removeButton.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    // Notify the listener when a meal is removed
                    listener.onMealRemove(recipes.get(position));
                }
            });
        }

        /**
         * Bind the recipe data to the views.
         *
         * @param recipe The recipe to display.
         */
        public void bind(Recipe recipe) {
            // Set the recipe name, cooking time, and servings
            recipeName.setText(recipe.getName());
            cookingTime.setText(recipe.getCookTime() + " min");
            servings.setText(recipe.getServings() + " servings");

            // Load the recipe image using Glide, or hide the ImageView if no image is available
            if (recipe.getImageUrl() != null && !recipe.getImageUrl().isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(recipe.getImageUrl())
                        .centerCrop()
                        .into(recipeImage);
                recipeImage.setVisibility(View.VISIBLE);
            } else {
                recipeImage.setVisibility(View.GONE);
            }
        }
    }
}
