package com.example.meal_app.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.meal_app.R;
import com.example.meal_app.model.GroceryItem;
import java.util.List;

/**
 * Adapter class for displaying a list of selected grocery items in a
 * RecyclerView.
 */
public class SelectedGroceryItemAdapter extends RecyclerView.Adapter<SelectedGroceryItemAdapter.ViewHolder> {

    private final List<GroceryItem> items; // List of grocery items to display

    /**
     * Constructor for the adapter.
     *
     * @param items List of grocery items to be displayed.
     */
    public SelectedGroceryItemAdapter(List<GroceryItem> items) {
        this.items = items;
    }

    /**
     * Inflates the layout for each item in the RecyclerView.
     *
     * @param parent The parent ViewGroup.
     * @param viewType The view type of the new View.
     * @return A new ViewHolder instance.
     */
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_selected_grocery, parent, false);
        return new ViewHolder(view);
    }

    /**
     * Binds data to the ViewHolder for a specific position.
     *
     * @param holder The ViewHolder to bind data to.
     * @param position The position of the item in the list.
     */
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        GroceryItem item = items.get(position);
        holder.bind(item);
    }

    /**
     * Returns the total number of items in the list.
     *
     * @return The size of the items list.
     */
    @Override
    public int getItemCount() {
        return items.size();
    }

    /**
     * ViewHolder class for holding and binding views for each grocery item.
     */
    static class ViewHolder extends RecyclerView.ViewHolder {

        private final TextView itemName; // TextView for displaying the item name
        private final TextView itemQuantity; // TextView for displaying the item quantity
        private final TextView itemPrice; // TextView for displaying the item price

        /**
         * Constructor for the ViewHolder.
         *
         * @param itemView The view for the individual item.
         */
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            itemName = itemView.findViewById(R.id.itemName);
            itemQuantity = itemView.findViewById(R.id.itemQuantity);
            itemPrice = itemView.findViewById(R.id.itemPrice);
        }

        /**
         * Binds a GroceryItem's data to the views.
         *
         * @param item The GroceryItem to bind.
         */
        public void bind(GroceryItem item) {
            itemName.setText(item.getName());
            itemQuantity.setText(String.format("%s %s", item.getQuantity(), item.getUnit()));
            itemPrice.setText(String.format("Nrs %.0f", item.getPrice()));
        }
    }
}
