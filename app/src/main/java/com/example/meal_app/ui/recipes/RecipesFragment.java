package com.example.meal_app.ui.recipes;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.meal_app.R;
import com.example.meal_app.adapter.RecipeCardAdapter;
import com.example.meal_app.databinding.FragmentRecipesBinding;
import com.example.meal_app.model.Recipe;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import com.google.firebase.firestore.FieldValue;

public class RecipesFragment extends Fragment {

    private FragmentRecipesBinding binding;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private RecipeCardAdapter adapter;
    private List<Recipe> recipes;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentRecipesBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        setupRecyclerView();
        setupSwipeToDelete();
        setupAddRecipeButton();
        loadRecipes();

        return root;
    }

    private void setupRecyclerView() {
        recipes = new ArrayList<>();
        adapter = new RecipeCardAdapter(recipes, recipe -> {
            Bundle bundle = new Bundle();
            bundle.putString("recipeId", recipe.getId());
            Navigation.findNavController(requireView())
                    .navigate(R.id.action_navigation_recipes_to_recipeDetailsFragment, bundle);
        });

        binding.recipesRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recipesRecyclerView.setAdapter(adapter);
    }

    private void setupSwipeToDelete() {
        ItemTouchHelper.SimpleCallback swipeCallback = new ItemTouchHelper.SimpleCallback(0,
                ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            private float currentSwipeX = 0f;

            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder,
                                @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                // Reset the item position since we're handling the swipe manually
                adapter.notifyItemChanged(viewHolder.getAdapterPosition());
            }

            @Override
            public void onChildDraw(@NonNull android.graphics.Canvas c, @NonNull RecyclerView recyclerView,
                                  @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY,
                                  int actionState, boolean isCurrentlyActive) {
                
                View itemView = viewHolder.itemView;
                View foregroundView = itemView.findViewById(R.id.foreground);
                View editAction = itemView.findViewById(R.id.editAction);
                View deleteAction = itemView.findViewById(R.id.deleteAction);
                
                if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
                    if (isCurrentlyActive) {
                        currentSwipeX = dX;
                        
                        if (dX > 0) { // Right swipe
                            editAction.setVisibility(View.VISIBLE);
                            deleteAction.setVisibility(View.GONE);
                            foregroundView.setTranslationX(dX);
                            
                            // Set click listener for edit button
                            editAction.setOnClickListener(v -> {
                                int position = viewHolder.getAdapterPosition();
                                if (position != RecyclerView.NO_POSITION) {
                                    Recipe recipe = recipes.get(position);
                                    editRecipe(recipe);
                                }
                            });
                        } else if (dX < 0) { // Left swipe
                            deleteAction.setVisibility(View.VISIBLE);
                            editAction.setVisibility(View.GONE);
                            foregroundView.setTranslationX(dX);
                            
                            // Set click listener for delete button
                            deleteAction.setOnClickListener(v -> {
                                int position = viewHolder.getAdapterPosition();
                                if (position != RecyclerView.NO_POSITION) {
                                    Recipe recipe = recipes.get(position);
                                    deleteRecipeAndIngredients(recipe, position);
                                }
                            });
                        }
                    } else {
                        // Keep the swipe position when touch is released
                        foregroundView.setTranslationX(currentSwipeX);
                    }
                }

                if (actionState == ItemTouchHelper.ACTION_STATE_IDLE) {
                    // Reset everything when idle
                    currentSwipeX = 0f;
                    foregroundView.setTranslationX(0f);
                    editAction.setVisibility(View.GONE);
                    deleteAction.setVisibility(View.GONE);
                }
            }

            @Override
            public float getSwipeThreshold(@NonNull RecyclerView.ViewHolder viewHolder) {
                return Float.MAX_VALUE; // Prevent auto-swipe
            }

            @Override
            public float getSwipeEscapeVelocity(float defaultValue) {
                return Float.MAX_VALUE; // Prevent swipe-to-dismiss
            }
        };

        new ItemTouchHelper(swipeCallback).attachToRecyclerView(binding.recipesRecyclerView);
    }

    private void setupAddRecipeButton() {
        binding.addRecipeFab.setOnClickListener(v -> 
            Navigation.findNavController(requireView())
                    .navigate(R.id.action_navigation_recipes_to_addRecipeFragment));
    }

    private void loadRecipes() {
        String userId = auth.getCurrentUser().getUid();
        db.collection("recipes")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    recipes.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Recipe recipe = new Recipe();
                        recipe.setId(document.getId());
                        recipe.setName(document.getString("name"));
                        recipe.setDescription(document.getString("description"));
                        recipe.setImageUrl(document.getString("imageUrl"));
                        recipe.setCategory(document.getString("category"));
                        recipe.setCookTime(document.getLong("cookTime") != null ? 
                                document.getLong("cookTime").intValue() : 0);
                        recipe.setServings(document.getLong("servings") != null ? 
                                document.getLong("servings").intValue() : 0);
                        recipe.setTotalPrice(document.getDouble("totalPrice") != null ? 
                                document.getDouble("totalPrice") : 0.0);
                        recipe.setUserId(document.getString("userId"));
                        recipes.add(recipe);
                    }
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> showError("Error loading recipes: " + e.getMessage()));
    }

    private void deleteRecipeAndIngredients(Recipe recipe, int position) {
        String userId = auth.getCurrentUser().getUid();
        
        // First, get the recipe's ingredients
        db.collection("recipes").document(recipe.getId()).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        List<String> ingredients = (List<String>) documentSnapshot.get("ingredients");
                        String recipeName = documentSnapshot.getString("name");
                        
                        if (ingredients != null && !ingredients.isEmpty() && recipeName != null) {
                            // Create a map of fields to delete
                            Map<String, Object> updates = new HashMap<>();
                            for (String ingredient : ingredients) {
                                String ingredientId = recipeName + "_" + ingredient.toLowerCase().replaceAll("[^a-z0-9]", "_");
                                updates.put(ingredientId, FieldValue.delete());
                            }

                            // Delete ingredients from grocery list
                            db.collection("groceryLists").document(userId)
                                    .update(updates)
                                    .addOnSuccessListener(aVoid -> {
                                        // After ingredients are deleted, delete the recipe
                                        deleteRecipe(recipe, position);
                                    })
                                    .addOnFailureListener(e -> showError("Error removing ingredients: " + e.getMessage()));
                        } else {
                            // If no ingredients, just delete the recipe
                            deleteRecipe(recipe, position);
                        }
                    }
                })
                .addOnFailureListener(e -> showError("Error getting recipe details: " + e.getMessage()));
    }

    private void deleteRecipe(Recipe recipe, int position) {
        db.collection("recipes").document(recipe.getId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    recipes.remove(position);
                    adapter.notifyItemRemoved(position);
                    showError("Recipe deleted");
                })
                .addOnFailureListener(e -> showError("Error deleting recipe: " + e.getMessage()));
    }

    private void editRecipe(Recipe recipe) {
        Bundle bundle = new Bundle();
        bundle.putString("recipeId", recipe.getId());
        Navigation.findNavController(requireView())
                .navigate(R.id.action_navigation_recipes_to_editRecipeFragment, bundle);
    }

    private void showError(String message) {
        if (getView() != null) {
            Snackbar.make(getView(), message, Snackbar.LENGTH_LONG).show();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
} 