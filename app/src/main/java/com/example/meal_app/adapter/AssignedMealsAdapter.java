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

public class AssignedMealsAdapter extends RecyclerView.Adapter<AssignedMealsAdapter.ViewHolder> {
    private final List<Recipe> recipes;
    private final OnMealRemoveListener listener;

    public interface OnMealRemoveListener {
        void onMealRemove(Recipe recipe);
    }

    public AssignedMealsAdapter(List<Recipe> recipes, OnMealRemoveListener listener) {
        this.recipes = recipes;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_assigned_meal, parent, false);
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
        private final ImageView recipeImage;
        private final TextView recipeName;
        private final TextView cookingTime;
        private final TextView servings;
        private final ImageButton removeButton;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            recipeImage = itemView.findViewById(R.id.recipeImage);
            recipeName = itemView.findViewById(R.id.recipeName);
            cookingTime = itemView.findViewById(R.id.cookingTime);
            servings = itemView.findViewById(R.id.servings);
            removeButton = itemView.findViewById(R.id.removeButton);

            removeButton.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    listener.onMealRemove(recipes.get(position));
                }
            });
        }

        public void bind(Recipe recipe) {
            recipeName.setText(recipe.getName());
            cookingTime.setText(recipe.getCookingTime() + " min");
            servings.setText(recipe.getServings() + " servings");

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