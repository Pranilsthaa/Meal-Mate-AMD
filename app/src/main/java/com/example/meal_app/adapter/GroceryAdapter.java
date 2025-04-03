package com.example.meal_app.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.meal_app.R;
import com.example.meal_app.model.GroceryItem;
import java.util.List;

/**
 * Adapter class for managing and displaying grocery items in a RecyclerView.
 * Supports both header and item views.
 */
public class GroceryAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_HEADER = 0; // View type for header
    private static final int TYPE_ITEM = 1;   // View type for item

    private List<Object> items; // List of items (can be headers or grocery items)
    private OnGroceryItemListener listener; // Listener for item interactions

    /**
     * Interface for handling grocery item interactions.
     */
    public interface OnGroceryItemListener {

        void onBuyClicked(GroceryItem item); // Called when the buy button is clicked

        void onItemSelectedForSms(GroceryItem item, boolean isSelected); // Called when the SMS checkbox is toggled
    }

    /**
     * Constructor for the adapter.
     *
     * @param items List of items to display.
     * @param listener Listener for item interactions.
     */
    public GroceryAdapter(List<Object> items, OnGroceryItemListener listener) {
        this.items = items;
        this.listener = listener;
    }

    /**
     * Updates the data in the adapter and refreshes the view.
     *
     * @param newItems New list of items to display.
     */
    public void updateData(List<Object> newItems) {
        this.items = newItems;
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        // Determine the view type based on the item type
        return items.get(position) instanceof String ? TYPE_HEADER : TYPE_ITEM;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_HEADER) {
            // Inflate header layout
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_grocery_header, parent, false);
            return new HeaderViewHolder(view);
        } else {
            // Inflate item layout
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_grocery, parent, false);
            return new ItemViewHolder(view, listener);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof HeaderViewHolder) {
            // Bind header data
            ((HeaderViewHolder) holder).bind((String) items.get(position));
        } else if (holder instanceof ItemViewHolder) {
            // Bind item data
            ((ItemViewHolder) holder).bind((GroceryItem) items.get(position));
        }
    }

    @Override
    public int getItemCount() {
        return items.size(); // Return the total number of items
    }

    /**
     * Updates a specific grocery item in the list and refreshes its view.
     *
     * @param updatedItem The updated grocery item.
     */
    public void updateItem(GroceryItem updatedItem) {
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i) instanceof GroceryItem) {
                GroceryItem item = (GroceryItem) items.get(i);
                if (item.getId().equals(updatedItem.getId())) {
                    items.set(i, updatedItem);
                    notifyItemChanged(i); // Notify that the item has changed
                    break;
                }
            }
        }
    }

    /**
     * ViewHolder for header items.
     */
    static class HeaderViewHolder extends RecyclerView.ViewHolder {

        private TextView headerText; // TextView for displaying the header

        HeaderViewHolder(View itemView) {
            super(itemView);
            headerText = (TextView) itemView;
        }

        /**
         * Binds the header text to the view.
         *
         * @param header The header text.
         */
        void bind(String header) {
            headerText.setText(header);
        }
    }

    /**
     * ViewHolder for grocery items.
     */
    static class ItemViewHolder extends RecyclerView.ViewHolder {

        private TextView nameText; // TextView for item name
        private TextView quantityText; // TextView for item quantity
        private TextView priceText; // TextView for item price
        private CheckBox smsCheckbox; // Checkbox for SMS selection
        private Button buyButton; // Button for marking item as purchased
        private OnGroceryItemListener itemListener; // Listener for item interactions

        ItemViewHolder(View itemView, OnGroceryItemListener listener) {
            super(itemView);
            this.itemListener = listener;
            nameText = itemView.findViewById(R.id.ingredientName);
            quantityText = itemView.findViewById(R.id.quantity);
            priceText = itemView.findViewById(R.id.price);
            smsCheckbox = itemView.findViewById(R.id.checkbox);
            buyButton = itemView.findViewById(R.id.buyButton);
        }

        /**
         * Binds the grocery item data to the view.
         *
         * @param item The grocery item.
         */
        void bind(GroceryItem item) {
            nameText.setText(item.getName());
            quantityText.setText(String.format("%.1f %s", item.getQuantity(), item.getUnit()));
            priceText.setText(String.format("Nrs %.0f", item.getPrice()));

            // Update buy button state based on purchase status
            updateBuyButtonState(item.isPurchased());

            // Update checkbox state based on SMS selection and purchase status
            smsCheckbox.setChecked(item.isSelectedForSms());
            smsCheckbox.setEnabled(!item.isPurchased());
            smsCheckbox.setAlpha(item.isPurchased() ? 0.5f : 1.0f);

            // Set click listeners for buy button and checkbox
            buyButton.setOnClickListener(v -> {
                if (itemListener != null) {
                    itemListener.onBuyClicked(item);
                }
            });

            smsCheckbox.setOnClickListener(v -> {
                if (itemListener != null && !item.isPurchased()) {
                    itemListener.onItemSelectedForSms(item, smsCheckbox.isChecked());
                }
            });
        }

        /**
         * Updates the appearance of the buy button based on purchase status.
         *
         * @param isPurchased Whether the item is purchased.
         */
        private void updateBuyButtonState(boolean isPurchased) {
            if (isPurchased) {
                buyButton.setText("Purchased");
                buyButton.setBackgroundResource(R.drawable.button_purchased);
            } else {
                buyButton.setText("Buy");
                buyButton.setBackgroundResource(R.drawable.button_buy);
            }
        }
    }
}
