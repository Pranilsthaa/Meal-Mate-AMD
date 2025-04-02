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
import androidx.core.content.ContextCompat;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.InputStream;

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

            private final ColorDrawable backgroundDelete = new ColorDrawable(ContextCompat.getColor(getContext(), R.color.delete_red));
            private final ColorDrawable backgroundEdit = new ColorDrawable(ContextCompat.getColor(getContext(), R.color.edit_blue));
            private final Drawable iconDelete = ContextCompat.getDrawable(getContext(), R.drawable.ic_delete);
            private final Drawable iconEdit = ContextCompat.getDrawable(getContext(), R.drawable.ic_edit);

            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder,
                                @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                if (position == RecyclerView.NO_POSITION) {
                    return;
                }
                Recipe recipe = recipes.get(position);

                if (direction == ItemTouchHelper.LEFT) {
                    recipes.remove(position);
                    adapter.notifyItemRemoved(position);
                    showUndoSnackbar(recipe, position);
                } else {
                    editRecipe(recipe);
                    adapter.notifyItemChanged(position);
                }
            }

            @Override
            public void onChildDraw(@NonNull android.graphics.Canvas c, @NonNull RecyclerView recyclerView,
                                  @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY,
                                  int actionState, boolean isCurrentlyActive) {

                View itemView = viewHolder.itemView;
                int iconMargin = (itemView.getHeight() - iconDelete.getIntrinsicHeight()) / 2;
                int iconTop = itemView.getTop() + (itemView.getHeight() - iconDelete.getIntrinsicHeight()) / 2;
                int iconBottom = iconTop + iconDelete.getIntrinsicHeight();

                if (dX > 0) {
                    int iconLeft = itemView.getLeft() + iconMargin;
                    int iconRight = itemView.getLeft() + iconMargin + iconEdit.getIntrinsicWidth();
                    iconEdit.setBounds(iconLeft, iconTop, iconRight, iconBottom);
                    backgroundEdit.setBounds(itemView.getLeft(), itemView.getTop(),
                            itemView.getLeft() + ((int) dX), itemView.getBottom());
                    backgroundEdit.draw(c);
                    iconEdit.draw(c);
                } else if (dX < 0) {
                    int iconLeft = itemView.getRight() - iconMargin - iconDelete.getIntrinsicWidth();
                    int iconRight = itemView.getRight() - iconMargin;
                    iconDelete.setBounds(iconLeft, iconTop, iconRight, iconBottom);
                    backgroundDelete.setBounds(itemView.getRight() + ((int) dX), itemView.getTop(),
                            itemView.getRight(), itemView.getBottom());
                    backgroundDelete.draw(c);
                    iconDelete.draw(c);
                } else {
                    backgroundEdit.setBounds(0, 0, 0, 0);
                    backgroundDelete.setBounds(0, 0, 0, 0);
                    backgroundEdit.draw(c);
                    backgroundDelete.draw(c);
                }

                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
            }

            @Override
            public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                final View foregroundView = ((RecipeCardAdapter.RecipeViewHolder) viewHolder).foregroundView;
                getDefaultUIUtil().clearView(foregroundView);
            }

            @Override
            public void onSelectedChanged(RecyclerView.ViewHolder viewHolder, int actionState) {
                if (viewHolder != null) {
                    final View foregroundView = ((RecipeCardAdapter.RecipeViewHolder) viewHolder).foregroundView;
                    getDefaultUIUtil().onSelected(foregroundView);
                }
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
        String userName = auth.getCurrentUser().getDisplayName();
        
        // Update the title with user's name
        if (binding.recipesTitle != null) {
            String title = "Recipes";
            if (userName != null && !userName.isEmpty()) {
                title += " • " + userName;
            }
            binding.recipesTitle.setText(title);
            binding.recipesTitle.setBackgroundResource(0); // Remove background
            binding.recipesTitle.setTextColor(getResources().getColor(android.R.color.black, null));
        }

        // Only load user recipes
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
                        recipe.setFeatured(false);
                        recipes.add(recipe);
                    }
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> showError("Error loading recipes: " + e.getMessage()));
    }

    private void showUndoSnackbar(final Recipe recipe, final int position) {
        Snackbar snackbar = Snackbar.make(binding.getRoot(), recipe.getName() + " deleted", Snackbar.LENGTH_LONG);
        snackbar.setAction("UNDO", view -> {
            recipes.add(position, recipe);
            adapter.notifyItemInserted(position);
            binding.recipesRecyclerView.scrollToPosition(position);
        });
        snackbar.addCallback(new Snackbar.Callback() {
            @Override
            public void onDismissed(Snackbar transientBottomBar, int event) {
                if (event != Snackbar.Callback.DISMISS_EVENT_ACTION) {
                    deleteRecipeAndIngredientsFromFirestore(recipe);
                }
            }
        });
        snackbar.show();
    }

    private void deleteRecipeAndIngredientsFromFirestore(Recipe recipe) {
        String userId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
        if (userId == null) {
            showError("Error: User not logged in.");
            return;
        }

        // Don't try to delete featured recipes from Firestore
        if (recipe.isFeatured()) {
            recipes.remove(recipe);
            adapter.notifyDataSetChanged();
            return;
        }

        db.collection("recipes").document(recipe.getId()).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Object ingredientsObj = documentSnapshot.get("ingredients");
                        String recipeName = documentSnapshot.getString("name");
                        List<String> ingredientNames = new ArrayList<>();

                        if (ingredientsObj instanceof List<?>) {
                            List<?> ingredientsList = (List<?>) ingredientsObj;
                            for (Object ingredient : ingredientsList) {
                                if (ingredient instanceof String) {
                                    // If it's a string, add it directly
                                    ingredientNames.add((String) ingredient);
                                } else if (ingredient instanceof Map) {
                                    // If it's a map, get the name field
                                    @SuppressWarnings("unchecked")
                                    Map<String, Object> ingredientMap = (Map<String, Object>) ingredient;
                                    String name = (String) ingredientMap.get("name");
                                    if (name != null) {
                                        ingredientNames.add(name);
                                    }
                                }
                            }
                        }

                        if (!ingredientNames.isEmpty() && recipeName != null) {
                            Map<String, Object> updates = new HashMap<>();
                            for (String ingredient : ingredientNames) {
                                String ingredientId = recipeName.toLowerCase().replaceAll("[^a-z0-9]", "_")
                                        + "_" + ingredient.toLowerCase().replaceAll("[^a-z0-9]", "_");
                                updates.put(ingredientId, FieldValue.delete());
                            }

                            db.collection("groceryLists").document(userId)
                                    .update(updates)
                                    .addOnSuccessListener(aVoid -> deleteRecipeDocument(recipe))
                                    .addOnFailureListener(e -> {
                                        System.err.println("Error removing ingredients from grocery list: " + e.getMessage());
                                        deleteRecipeDocument(recipe);
                                    });
                        } else {
                            deleteRecipeDocument(recipe);
                        }
                    } else {
                        showError("Recipe document not found for deletion.");
                    }
                })
                .addOnFailureListener(e -> showError("Error getting recipe details for deletion: " + e.getMessage()));
    }

    private void deleteRecipeDocument(Recipe recipe) {
        db.collection("recipes").document(recipe.getId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    System.out.println("Recipe " + recipe.getId() + " deleted from Firestore.");
                })
                .addOnFailureListener(e -> showError("Error deleting recipe from Firestore: " + e.getMessage()));
    }

    private void deleteRecipeAndIngredients(Recipe recipe, int position) {
        deleteRecipeAndIngredientsFromFirestore(recipe);
        if (position >= 0 && position < recipes.size() && recipes.get(position).getId().equals(recipe.getId())) {
            recipes.remove(position);
            adapter.notifyItemRemoved(position);
        }
    }

    private void editRecipe(Recipe recipe) {
        Bundle bundle = new Bundle();
        bundle.putString("recipeId", recipe.getId());
        try {
            Navigation.findNavController(requireView())
                    .navigate(R.id.action_navigation_recipes_to_editRecipeFragment, bundle);
        } catch (IllegalStateException | IllegalArgumentException e) {
            showError("Could not navigate to edit recipe.");
            System.err.println("Navigation error: " + e.getMessage());
        }
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