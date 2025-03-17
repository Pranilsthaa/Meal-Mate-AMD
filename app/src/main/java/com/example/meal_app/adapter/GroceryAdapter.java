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

public class GroceryAdapter extends RecyclerView.Adapter<GroceryAdapter.ViewHolder> {
    private List<GroceryItem> items;
    private OnGroceryItemListener listener;

    public interface OnGroceryItemListener {
        void onBuyClicked(GroceryItem item);
        void onItemSelectedForSms(GroceryItem item, boolean isSelected);
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
        return new ViewHolder(view, listener);
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

    public void updateItem(GroceryItem updatedItem) {
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).getId().equals(updatedItem.getId())) {
                items.set(i, updatedItem);
                notifyItemChanged(i);
                break;
            }
        }
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private TextView nameText;
        private TextView quantityText;
        private TextView priceText;
        private CheckBox smsCheckbox;
        private Button buyButton;
        private OnGroceryItemListener itemListener;

        ViewHolder(View itemView, OnGroceryItemListener listener) {
            super(itemView);
            this.itemListener = listener;
            nameText = itemView.findViewById(R.id.ingredientName);
            quantityText = itemView.findViewById(R.id.quantity);
            priceText = itemView.findViewById(R.id.price);
            smsCheckbox = itemView.findViewById(R.id.checkbox);
            buyButton = itemView.findViewById(R.id.buyButton);
        }

        void bind(GroceryItem item) {
            nameText.setText(item.getName());
            quantityText.setText(String.format("%.1f %s", item.getQuantity(), item.getUnit()));
            priceText.setText(String.format("Nrs %.0f", item.getPrice()));

            // Update buy button state
            updateBuyButtonState(item.isPurchased());

            // Update checkbox state
            smsCheckbox.setChecked(item.isSelectedForSms());
            smsCheckbox.setEnabled(!item.isPurchased());
            smsCheckbox.setAlpha(item.isPurchased() ? 0.5f : 1.0f);

            // Set click listeners
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

        private void updateBuyButtonState(boolean isPurchased) {
            if (isPurchased) {
                buyButton.setText("Purchased");
                buyButton.setBackgroundResource(R.drawable.button_purchased_background);
            } else {
                buyButton.setText("Buy");
                buyButton.setBackgroundResource(R.drawable.button_buy_background);
            }
        }
    }
}