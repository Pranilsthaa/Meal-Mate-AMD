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

public class GroceryAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int TYPE_HEADER = 0;
    private static final int TYPE_ITEM = 1;

    private List<Object> items;
    private OnGroceryItemListener listener;

    public interface OnGroceryItemListener {
        void onBuyClicked(GroceryItem item);
        void onItemSelectedForSms(GroceryItem item, boolean isSelected);
    }

    public GroceryAdapter(List<Object> items, OnGroceryItemListener listener) {
        this.items = items;
        this.listener = listener;
    }

    public void updateData(List<Object> newItems) {
        this.items = newItems;
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return items.get(position) instanceof String ? TYPE_HEADER : TYPE_ITEM;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_HEADER) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_grocery_header, parent, false);
            return new HeaderViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_grocery, parent, false);
            return new ItemViewHolder(view, listener);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof HeaderViewHolder) {
            ((HeaderViewHolder) holder).bind((String) items.get(position));
        } else if (holder instanceof ItemViewHolder) {
            ((ItemViewHolder) holder).bind((GroceryItem) items.get(position));
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void updateItem(GroceryItem updatedItem) {
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i) instanceof GroceryItem) {
                GroceryItem item = (GroceryItem) items.get(i);
                if (item.getId().equals(updatedItem.getId())) {
                    items.set(i, updatedItem);
                    notifyItemChanged(i);
                    break;
                }
            }
        }
    }

    static class HeaderViewHolder extends RecyclerView.ViewHolder {
        private TextView headerText;

        HeaderViewHolder(View itemView) {
            super(itemView);
            headerText = (TextView) itemView;
        }

        void bind(String header) {
            headerText.setText(header);
        }
    }

    static class ItemViewHolder extends RecyclerView.ViewHolder {
        private TextView nameText;
        private TextView quantityText;
        private TextView priceText;
        private CheckBox smsCheckbox;
        private Button buyButton;
        private OnGroceryItemListener itemListener;

        ItemViewHolder(View itemView, OnGroceryItemListener listener) {
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
                buyButton.setBackgroundResource(R.drawable.button_purchased);
            } else {
                buyButton.setText("Buy");
                buyButton.setBackgroundResource(R.drawable.button_buy);
            }
        }
    }
}