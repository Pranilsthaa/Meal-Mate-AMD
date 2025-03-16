package com.example.meal_app.ui.grocery;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.meal_app.R;
import com.example.meal_app.adapter.GroceryAdapter;
import com.example.meal_app.model.GroceryItem;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.SetOptions;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import android.Manifest;
import android.content.pm.PackageManager;
import android.telephony.SmsManager;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import androidx.recyclerview.widget.DividerItemDecoration;
import com.example.meal_app.adapter.SelectedGroceryItemAdapter;
import android.widget.ImageButton;
import android.text.TextUtils;
import android.widget.Button;
import android.view.MotionEvent;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;

public class GroceryFragment extends Fragment implements GroceryAdapter.OnGroceryItemListener {

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private GroceryAdapter adapter;
    private List<GroceryItem> allItems = new ArrayList<>();
    private List<GroceryItem> filteredItems = new ArrayList<>();
    private double totalPrice = 0;
    private double remainingPrice = 0;
    private TextView progressText;
    private ProgressBar progressBar;
    private TextView totalPriceText;
    private TextView remainingPriceText;
    private DocumentReference userGroceryRef;
    private ImageButton shareButton;
    private static final int SMS_PERMISSION_REQUEST_CODE = 123;
    private String userPhone;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_grocery, container, false);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        if (auth.getCurrentUser() != null) {
            userGroceryRef = db.collection("groceryLists").document(auth.getCurrentUser().getUid());
            // Load user's phone number
            db.collection("users").document(auth.getCurrentUser().getUid())
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            userPhone = documentSnapshot.getString("phone");
                        }
                    });
        }

        // Initialize views
        progressText = root.findViewById(R.id.progressText);
        progressBar = root.findViewById(R.id.progressBar);
        totalPriceText = root.findViewById(R.id.totalPrice);
        remainingPriceText = root.findViewById(R.id.remainingPrice);
        EditText searchInput = root.findViewById(R.id.searchInput);
        RecyclerView recyclerView = root.findViewById(R.id.groceryRecyclerView);
        shareButton = root.findViewById(R.id.shareButton);

        // Setup RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new GroceryAdapter(filteredItems, this);
        recyclerView.setAdapter(adapter);

        // Setup share button
        shareButton.setOnClickListener(v -> showShareDialog());

        // Setup search functionality
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterItems(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        // Load grocery items
        loadGroceryItems();

        return root;
    }

    private void loadGroceryItems() {
        if (auth.getCurrentUser() == null) {
            return;
        }

        String userId = auth.getCurrentUser().getUid();

        // First, get user's grocery list state
        userGroceryRef.get().addOnSuccessListener(documentSnapshot -> {
            Map<String, Object> groceryState = documentSnapshot.exists() ? documentSnapshot.getData() : new HashMap<>();

            // Then, get all recipes
            db.collection("recipes")
                    .whereEqualTo("userId", userId)
                    .get()
                    .addOnSuccessListener(recipeSnapshots -> {
                        Map<String, GroceryItem> groceryItems = new HashMap<>();

                        // Process each recipe
                        for (QueryDocumentSnapshot recipeDoc : recipeSnapshots) {
                            List<Map<String, Object>> ingredients = (List<Map<String, Object>>) recipeDoc
                                    .get("ingredients");
                            if (ingredients != null) {
                                for (Map<String, Object> ingredient : ingredients) {
                                    String name = (String) ingredient.get("name");
                                    Double quantity = (Double) ingredient.get("quantity");
                                    String unit = (String) ingredient.get("unit");
                                    String category = (String) ingredient.get("category");
                                    Double price = (Double) ingredient.get("price");

                                    if (name != null && quantity != null && unit != null && price != null) {
                                        String itemId = name.toLowerCase();
                                        if (groceryItems.containsKey(itemId)) {
                                            // Update existing item
                                            GroceryItem existingItem = groceryItems.get(itemId);
                                            existingItem.setQuantity(existingItem.getQuantity() + quantity);
                                            existingItem.setPrice(existingItem.getPrice() + price);
                                        } else {
                                            // Create new item
                                            GroceryItem item = new GroceryItem(itemId, name, quantity, unit, category,
                                                    price);

                                            // Apply saved state if exists
                                            if (groceryState.containsKey(itemId)) {
                                                Map<String, Object> state = (Map<String, Object>) groceryState
                                                        .get(itemId);
                                                if (state != null) {
                                                    item.setPurchased((Boolean) state.get("isPurchased"));
                                                    item.setSelectedForSms((Boolean) state.get("isSelectedForSms"));
                                                }
                                            }

                                            groceryItems.put(itemId, item);
                                        }
                                    }
                                }
                            }
                        }

                        // Update the items list
                        allItems = new ArrayList<>(groceryItems.values());
                        filteredItems.clear();
                        filteredItems.addAll(allItems);
                        adapter.notifyDataSetChanged();

                        // Update totals
                        updateTotals();
                    })
                    .addOnFailureListener(e -> {
                        if (getView() != null) {
                            Snackbar.make(getView(), "Error loading grocery list: " + e.getMessage(),
                                    Snackbar.LENGTH_LONG).show();
                        }
                    });
        });
    }

    private void filterItems(String query) {
        if (query.isEmpty()) {
            filteredItems.clear();
            filteredItems.addAll(allItems);
        } else {
            filteredItems.clear();
            filteredItems.addAll(allItems.stream()
                    .filter(item -> item.getName().toLowerCase().contains(query.toLowerCase()))
                    .collect(Collectors.toList()));
        }
        adapter.notifyDataSetChanged();
        updateTotals();
    }

    private void updateTotals() {
        int totalItems = filteredItems.size();
        int purchasedItems = (int) filteredItems.stream().filter(GroceryItem::isPurchased).count();

        // Update progress
        progressText.setText(String.format("%d/%d items", purchasedItems, totalItems));
        progressBar.setMax(totalItems);
        progressBar.setProgress(purchasedItems);

        // Update prices
        totalPrice = filteredItems.stream().mapToDouble(GroceryItem::getPrice).sum();
        remainingPrice = filteredItems.stream()
                .filter(item -> !item.isPurchased())
                .mapToDouble(GroceryItem::getPrice)
                .sum();

        totalPriceText.setText(String.format("Nrs %.0f", totalPrice));
        remainingPriceText.setText(String.format("Nrs %.0f", remainingPrice));
    }

    private void updateGroceryItemState(GroceryItem item) {
        if (userGroceryRef != null) {
            Map<String, Object> updates = new HashMap<>();
            updates.put(item.getId(), item.toMap());
            userGroceryRef.set(updates, SetOptions.merge())
                    .addOnSuccessListener(aVoid -> adapter.updateItem(item))
                    .addOnFailureListener(e -> {
                        if (getView() != null) {
                            Snackbar.make(getView(), "Failed to update item: " + e.getMessage(),
                                    Snackbar.LENGTH_LONG).show();
                        }
                        // Revert the item's state
                        item.setPurchased(!item.isPurchased());
                        adapter.updateItem(item);
                    });
        }
    }

    @Override
    public void onItemSelectedForSms(GroceryItem item, boolean isSelected) {
        item.setSelectedForSms(isSelected);
        updateGroceryItemState(item);

        // Show/hide share button based on selection
        boolean hasSelectedItems = filteredItems.stream().anyMatch(GroceryItem::isSelectedForSms);
        shareButton.setVisibility(hasSelectedItems ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onBuyClicked(GroceryItem item) {
        item.setPurchased(true);
        updateGroceryItemState(item);
        updateTotals();
    }

    private void showShareDialog() {
        List<GroceryItem> selectedItems = getSelectedItems();
        if (selectedItems.isEmpty()) {
            if (getView() != null) {
                Snackbar.make(getView(), "Please select items to share", Snackbar.LENGTH_SHORT).show();
            }
            return;
        }

        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_share_grocery, null);
        dialog.setContentView(dialogView);

        // Setup touch listener to dismiss keyboard
        dialogView.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                View focusedView = dialogView.findFocus();
                if (focusedView != null) {
                    InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(focusedView.getWindowToken(), 0);
                    focusedView.clearFocus();
                }
            }
            return false;
        });

        // Setup selected items RecyclerView
        RecyclerView selectedItemsRecyclerView = dialogView.findViewById(R.id.selectedItemsRecyclerView);
        selectedItemsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        selectedItemsRecyclerView.addItemDecoration(new DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL));
        SelectedGroceryItemAdapter selectedItemsAdapter = new SelectedGroceryItemAdapter(selectedItems);
        selectedItemsRecyclerView.setAdapter(selectedItemsAdapter);

        EditText phoneNumberInput = dialogView.findViewById(R.id.phoneNumberInput);
        Button shareButton = dialogView.findViewById(R.id.shareButton);

        shareButton.setOnClickListener(v -> {
            String phoneNumber = phoneNumberInput.getText().toString().trim();
            if (validatePhoneNumber(phoneNumber)) {
                requestSmsPermissionAndSend(phoneNumber, selectedItems);
                dialog.dismiss();
            } else {
                phoneNumberInput.setError("Please enter a valid 10-digit phone number");
            }
        });

        dialog.show();
    }

    private boolean validatePhoneNumber(String phoneNumber) {
        return phoneNumber.matches("^[0-9]{10}$");
    }

    private List<GroceryItem> getSelectedItems() {
        return filteredItems.stream()
                .filter(GroceryItem::isSelectedForSms)
                .collect(Collectors.toList());
    }

    private void requestSmsPermissionAndSend(String phoneNumber, List<GroceryItem> items) {
        if (ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(),
                    new String[]{Manifest.permission.SEND_SMS},
                    SMS_PERMISSION_REQUEST_CODE);
        } else {
            sendSms(phoneNumber, items);
        }
    }

    private void sendSms(String phoneNumber, List<GroceryItem> items) {
        try {
            StringBuilder message = new StringBuilder("Grocery List from ");
            message.append(userPhone != null ? userPhone : "a friend").append(":\n\n");

            for (GroceryItem item : items) {
                message.append(item.getName())
                        .append(": ")
                        .append(item.getQuantity())
                        .append(" ")
                        .append(item.getUnit())
                        .append(" - Nrs ")
                        .append(String.format("%.0f", item.getPrice()))
                        .append("\n");
            }

            SmsManager smsManager = SmsManager.getDefault();
            ArrayList<String> parts = smsManager.divideMessage(message.toString());
            smsManager.sendMultipartTextMessage(phoneNumber, null, parts, null, null);

            if (getView() != null) {
                Snackbar.make(getView(), "Grocery list shared successfully", Snackbar.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            if (getView() != null) {
                Snackbar.make(getView(), "Failed to send SMS: " + e.getMessage(), Snackbar.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        if (requestCode == SMS_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, retry sending SMS
                if (getView() != null) {
                    Snackbar.make(getView(), "Permission granted. Please try sharing again.", Snackbar.LENGTH_SHORT)
                            .show();
                }
            } else {
                if (getView() != null) {
                    Snackbar.make(getView(), "SMS permission denied. Cannot share grocery list.", Snackbar.LENGTH_LONG)
                            .show();
                }
            }
        }
    }
}
