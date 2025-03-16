package com.example.meal_app.adapter;

import android.graphics.Paint;
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
import java.util.concurrent.ConcurrentHashMap;

public class GroceryAdapter extends RecyclerView.Adapter<GroceryAdapter.ViewHolder> {
    private final List<GroceryItem> items;
    private final OnGroceryItemListener listener;
    private final ConcurrentHashMap<String, Boolean> processingItems = new ConcurrentHashMap<>();

    public interface OnGroceryItemListener {
        void onItemSelectedForSms(GroceryItem item, boolean isSelected);

        void onBuyClicked(GroceryItem item);
    }

    public GroceryAdapter(List<GroceryItem> items, OnGroceryItemListener listener) {
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_grocery, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        GroceryItem item = items.get(position);
        holder.bind(item);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void updateItem(GroceryItem item) {
        int position = -1;
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).getId().equals(item.getId())) {
                position = i;
                break;
            }
        }
        if (position != -1) {
            items.set(position, item);
            notifyItemChanged(position);
        }
        // Remove from processing map after update
        processingItems.remove(item.getId());
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final CheckBox checkbox;
        private final TextView ingredientName;
        private final TextView quantity;
        private final TextView category;
        private final TextView price;
        private final Button buyButton;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            checkbox = itemView.findViewById(R.id.checkbox);
            ingredientName = itemView.findViewById(R.id.ingredientName);
            quantity = itemView.findViewById(R.id.quantity);
            category = itemView.findViewById(R.id.category);
            price = itemView.findViewById(R.id.price);
            buyButton = itemView.findViewById(R.id.buyButton);
        }

        public void bind(GroceryItem item) {
            // Set basic information
            ingredientName.setText(item.getName());
            quantity.setText(String.format("%.1f %s", item.getQuantity(), item.getUnit()));
            category.setText(item.getCategory());
            price.setText(String.format("Nrs %.0f", item.getPrice()));

            // Handle SMS selection checkbox
            checkbox.setOnCheckedChangeListener(null); // Remove previous listener
            checkbox.setChecked(item.isSelectedForSms());
            checkbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                item.setSelectedForSms(isChecked);
                listener.onItemSelectedForSms(item, isChecked);
            });

            // Handle purchased state
            if (item.isPurchased()) {
                buyButton.setText("Purchased");
                buyButton.setEnabled(false);
                buyButton.setAlpha(0.5f);
                ingredientName.setPaintFlags(ingredientName.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            } else {
                buyButton.setText("Buy");
                buyButton.setEnabled(!processingItems.containsKey(item.getId()));
                buyButton.setAlpha(1.0f);
                ingredientName.setPaintFlags(ingredientName.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
            }

            // Handle buy button click
            buyButton.setOnClickListener(v -> {
                if (!processingItems.containsKey(item.getId())) {
                    processingItems.put(item.getId(), true);
                    buyButton.setEnabled(false);
                    listener.onBuyClicked(item);
                }
            });
        }
    }
}