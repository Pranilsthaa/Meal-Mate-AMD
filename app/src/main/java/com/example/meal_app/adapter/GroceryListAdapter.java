package com.example.meal_app.adapter;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.meal_app.R;
import com.example.meal_app.model.Ingredient;
import java.util.List;
import java.util.Map;

public class GroceryListAdapter extends RecyclerView.Adapter<GroceryListAdapter.ViewHolder> {

    private static final String TAG = "GroceryListAdapter";
    private final Map<String, List<Ingredient>> categorizedIngredients;
    private final OnIngredientClickListener listener;

    public GroceryListAdapter(Map<String, List<Ingredient>> categorizedIngredients, OnIngredientClickListener listener) {
        this.categorizedIngredients = categorizedIngredients;
        this.listener = listener;
        Log.d(TAG, "Adapter created with " + categorizedIngredients.size() + " categories");
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Log.d(TAG, "Creating view holder for viewType: " + viewType);
        View view = LayoutInflater.from(parent.getContext())
                .inflate(viewType == 0 ? R.layout.item_grocery_category : R.layout.item_grocery_ingredient,
                        parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        int categoryCount = 0;
        int currentPosition = position;

        // Find the category and ingredient for this position
        String currentCategory = null;
        Ingredient currentIngredient = null;

        for (Map.Entry<String, List<Ingredient>> entry : categorizedIngredients.entrySet()) {
            String category = entry.getKey();
            List<Ingredient> ingredients = entry.getValue();

            // Check if position is for category header
            if (currentPosition == categoryCount) {
                currentCategory = category;
                break;
            }
            currentPosition--;

            // Check if position is within this category's ingredients
            if (currentPosition < ingredients.size()) {
                currentCategory = category;
                currentIngredient = ingredients.get(currentPosition);
                break;
            }
            currentPosition -= ingredients.size();
            categoryCount++;
        }

        if (currentCategory != null) {
            if (currentIngredient == null) {
                // This is a category header
                Log.d(TAG, "Binding category header: " + currentCategory);
                holder.bindCategory(currentCategory);
            } else {
                // This is an ingredient
                Log.d(TAG, "Binding ingredient: " + currentIngredient.getName() + " in category: " + currentCategory);
                holder.bindIngredient(currentIngredient, listener);
            }
        }
    }

    @Override
    public int getItemCount() {
        int count = 0;
        for (Map.Entry<String, List<Ingredient>> entry : categorizedIngredients.entrySet()) {
            count++; // Category header
            count += entry.getValue().size(); // Ingredients
        }
        Log.d(TAG, "Total item count: " + count);
        return count;
    }

    @Override
    public int getItemViewType(int position) {
        int categoryCount = 0;
        int currentPosition = position;

        for (Map.Entry<String, List<Ingredient>> entry : categorizedIngredients.entrySet()) {
            if (currentPosition == categoryCount) {
                Log.d(TAG, "View type for position " + position + ": Category header");
                return 0; // Category header
            }
            currentPosition--;

            if (currentPosition < entry.getValue().size()) {
                Log.d(TAG, "View type for position " + position + ": Ingredient");
                return 1; // Ingredient
            }
            currentPosition -= entry.getValue().size();
            categoryCount++;
        }

        return 1; // Default to ingredient view type
    }

    public interface OnIngredientClickListener {

        void onIngredientClick(Ingredient ingredient);

        void onCheckboxClick(Ingredient ingredient, boolean isChecked);

        void onSmsCheckboxClick(Ingredient ingredient, boolean isChecked);
    }

    /**
     * ViewHolder class for the GroceryListAdapter, responsible for managing and
     * binding views for both category titles and individual ingredients in a
     * RecyclerView.
     */
    static class ViewHolder extends RecyclerView.ViewHolder {

        /**
         * TextView for displaying the category title.
         */
        private final TextView categoryTitle;

        /**
         * TextView for displaying the name of an ingredient.
         */
        private final TextView ingredientName;

        /**
         * TextView for displaying the details of an ingredient, such as
         * quantity and unit.
         */
        private final TextView ingredientDetails;

        /**
         * CheckBox for marking an ingredient as bought.
         */
        private final CheckBox boughtCheckbox;

        /**
         * CheckBox for selecting an ingredient for SMS sharing.
         */
        private final CheckBox smsCheckbox;

        /**
         * Layout container for the ingredient views.
         */
        private final View ingredientLayout;

        /**
         * Constructor for initializing the ViewHolder with the provided
         * itemView.
         *
         * @param itemView The view representing a single item in the
         * RecyclerView.
         */
        ViewHolder(View itemView) {
            super(itemView);
            categoryTitle = itemView.findViewById(R.id.categoryTitle);
            ingredientName = itemView.findViewById(R.id.ingredientName);
            ingredientDetails = itemView.findViewById(R.id.ingredientDetails);
            boughtCheckbox = itemView.findViewById(R.id.boughtCheckbox);
            smsCheckbox = itemView.findViewById(R.id.smsCheckbox);
            ingredientLayout = itemView.findViewById(R.id.ingredientLayout);
        }

        /**
         * Binds a category title to the ViewHolder. Displays the category title
         * and hides the ingredient layout.
         *
         * @param category The category title to be displayed.
         */
        void bindCategory(String category) {
            Log.d(TAG, "Binding category: " + category);
            categoryTitle.setVisibility(View.VISIBLE);
            ingredientLayout.setVisibility(View.GONE);
            categoryTitle.setText(category);
        }

        /**
         * Binds an ingredient to the ViewHolder. Displays the ingredient
         * details and sets up listeners for user interactions.
         *
         * @param ingredient The ingredient to be displayed.
         * @param listener The listener for handling ingredient-related
         * interactions.
         */
        void bindIngredient(Ingredient ingredient, OnIngredientClickListener listener) {
            Log.d(TAG, "Binding ingredient: " + ingredient.getName());
            categoryTitle.setVisibility(View.GONE);
            ingredientLayout.setVisibility(View.VISIBLE);

            ingredientName.setText(ingredient.getName());
            ingredientDetails.setText(String.format("%.1f %s", ingredient.getQuantity(), ingredient.getUnit()));
            boughtCheckbox.setChecked(ingredient.isBought());
            smsCheckbox.setChecked(ingredient.isSelectedForSms());

            ingredientLayout.setOnClickListener(v -> listener.onIngredientClick(ingredient));
            boughtCheckbox.setOnCheckedChangeListener((buttonView, isChecked)
                    -> listener.onCheckboxClick(ingredient, isChecked));
            smsCheckbox.setOnCheckedChangeListener((buttonView, isChecked)
                    -> listener.onSmsCheckboxClick(ingredient, isChecked));
        }
    }
}
