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
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
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
import com.example.meal_app.utils.ToastUtil;

public class GroceryFragment extends Fragment implements GroceryAdapter.OnGroceryItemListener {

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private GroceryAdapter adapter;
    private List<Object> allItems = new ArrayList<>();
    private List<Object> filteredItems = new ArrayList<>();
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
    private String pendingPhoneNumber;
    private List<GroceryItem> pendingItems;
    private long lastBuyClickTime = 0;
    private long lastShareClickTime = 0;
    private long lastSelectionClickTime = 0;
    private static final long CLICK_DELAY = 500; // 500ms delay between clicks

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_grocery, container, false);

        try {
            db = FirebaseFirestore.getInstance();
            auth = FirebaseAuth.getInstance();

            // Initialize views first
            progressText = root.findViewById(R.id.progressText);
            progressBar = root.findViewById(R.id.progressBar);
            totalPriceText = root.findViewById(R.id.totalPrice);
            remainingPriceText = root.findViewById(R.id.remainingPrice);
            EditText searchInput = root.findViewById(R.id.searchInput);
            RecyclerView recyclerView = root.findViewById(R.id.groceryRecyclerView);
            shareButton = root.findViewById(R.id.shareButton);

            // Setup RecyclerView
            recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
            adapter = new GroceryAdapter(new ArrayList<>(), this);
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

            // Check if user is logged in
            if (auth.getCurrentUser() != null) {
                userGroceryRef = db.collection("groceryLists").document(auth.getCurrentUser().getUid());
                // Load user's phone number
                db.collection("users").document(auth.getCurrentUser().getUid())
                        .get()
                        .addOnSuccessListener(documentSnapshot -> {
                            if (documentSnapshot.exists()) {
                                userPhone = documentSnapshot.getString("phone");
                            }
                        })
                        .addOnFailureListener(e -> {
                            if (getView() != null) {
                                ToastUtil.showErrorToast(requireContext(), "Error loading user data: " + e.getMessage());
                            }
                        });
            } else {
                // User is not logged in, show message
                if (getView() != null) {
                    ToastUtil.showErrorToast(requireContext(), "Please log in to view your grocery list");
                }
                return root;
            }

            // Load grocery items
            loadGroceryItems();

        } catch (Exception e) {
            if (getView() != null) {
                ToastUtil.showErrorToast(requireContext(), "Error initializing grocery list: " + e.getMessage());
            }
            e.printStackTrace();
        }

        return root;
    }

    private void loadGroceryItems() {
        if (auth.getCurrentUser() == null || userGroceryRef == null) {
            if (getView() != null) {
                ToastUtil.showErrorToast(requireContext(), "Please log in to view your grocery list");
            }
            return;
        }

        try {
            String userId = auth.getCurrentUser().getUid();
            System.out.println("Loading grocery items for user: " + userId);

            userGroceryRef.get().addOnSuccessListener(documentSnapshot -> {
                try {
                    System.out.println("Document exists: " + documentSnapshot.exists());
                    if (!documentSnapshot.exists()) {
                        if (getView() != null) {
                            ToastUtil.showErrorToast(requireContext(), "No grocery list found for this user");
                        }
                        return;
                    }

                    Map<String, Object> data = documentSnapshot.getData();
                    System.out.println("Document data: " + data);

                    // Create a map to store items by type
                    Map<String, List<GroceryItem>> itemsByType = new HashMap<>();
                    List<GroceryItem> uncategorizedItems = new ArrayList<>();

                    // Process each field in the document
                    for (Map.Entry<String, Object> entry : data.entrySet()) {
                        try {
                            String fieldName = entry.getKey();
                            Object value = entry.getValue();
                            
                            // Skip non-item fields
                            if (fieldName.equals("groceryState")) {
                                continue;
                            }

                            // Each item is stored as a map
                            if (value instanceof Map) {
                                Map<String, Object> itemData = (Map<String, Object>) value;
                                System.out.println("Processing item: " + fieldName);
                                System.out.println("Item data: " + itemData);

                                String name = (String) itemData.get("name");
                                Double quantity = ((Number) itemData.get("quantity")).doubleValue();
                                String unit = (String) itemData.get("unit");
                                String type = (String) itemData.get("type");
                                Double price = ((Number) itemData.get("price")).doubleValue();
                                Boolean isPurchased = (Boolean) itemData.get("isPurchased");
                                Boolean isSelectedForSms = (Boolean) itemData.get("isSelectedForSms");

                                if (name != null && quantity != null && unit != null && price != null) {
                                    GroceryItem item = new GroceryItem(fieldName, name, quantity, unit, type, price);
                                    if (isPurchased != null) {
                                        item.setPurchased(isPurchased);
                                    }
                                    if (isSelectedForSms != null) {
                                        item.setSelectedForSms(isSelectedForSms);
                                    }

                                    // Add item to appropriate list based on type
                                    if (type != null && !type.isEmpty()) {
                                        itemsByType.computeIfAbsent(type, k -> new ArrayList<>()).add(item);
                                    } else {
                                        uncategorizedItems.add(item);
                                    }
                                }
                            }
                        } catch (Exception e) {
                            System.out.println("Error processing item: " + e.getMessage());
                            e.printStackTrace();
                        }
                    }

                    // Create a sorted list with headers and items
                    List<Object> sortedList = new ArrayList<>();
                    String[] types = getResources().getStringArray(R.array.ingredient_types);
                    
                    // Add categorized items
                    for (String type : types) {
                        List<GroceryItem> typeItems = itemsByType.get(type);
                        if (typeItems != null && !typeItems.isEmpty()) {
                            sortedList.add(type);
                            sortedList.addAll(typeItems);
                        }
                    }

                    // Add uncategorized items at the end
                    if (!uncategorizedItems.isEmpty()) {
                        sortedList.add("Others");
                        sortedList.addAll(uncategorizedItems);
                    }

                    // Update the adapter with the new sorted list
                    allItems = sortedList;
                    filteredItems.clear();
                    filteredItems.addAll(allItems);
                    adapter.updateData(filteredItems);

                    // Update totals
                    updateTotals();

                    // Update share button visibility
                    updateShareButtonVisibility();

                } catch (Exception e) {
                    System.out.println("Error processing grocery list: " + e.getMessage());
                    e.printStackTrace();
                    if (getView() != null) {
                        ToastUtil.showErrorToast(requireContext(), "Error processing grocery list: " + e.getMessage());
                    }
                }
            })
            .addOnFailureListener(e -> {
                System.out.println("Error loading grocery list: " + e.getMessage());
                e.printStackTrace();
                if (getView() != null) {
                    ToastUtil.showErrorToast(requireContext(), "Error loading grocery list: " + e.getMessage());
                }
            });
        } catch (Exception e) {
            System.out.println("Error initializing grocery list: " + e.getMessage());
            e.printStackTrace();
            if (getView() != null) {
                ToastUtil.showErrorToast(requireContext(), "Error initializing grocery list: " + e.getMessage());
            }
        }
    }

    private void filterItems(String query) {
        if (query.isEmpty()) {
            filteredItems.clear();
            filteredItems.addAll(allItems);
        } else {
            filteredItems.clear();
            String lowerQuery = query.toLowerCase();
            
            // Keep track of the current section
            String currentSection = null;
            
            // Iterate through all items
            for (Object item : allItems) {
                if (item instanceof String) {
                    // This is a section header
                    currentSection = (String) item;
                } else if (item instanceof GroceryItem) {
                    GroceryItem groceryItem = (GroceryItem) item;
                    // If item matches the query, add its section header (if not already added) and the item
                    if (groceryItem.getName().toLowerCase().contains(lowerQuery)) {
                        if (currentSection != null && !filteredItems.contains(currentSection)) {
                            filteredItems.add(currentSection);
                        }
                        filteredItems.add(groceryItem);
                    }
                }
            }
        }
        adapter.updateData(filteredItems);
        updateTotals();
    }

    private void updateTotals() {
        if (getView() == null) return;

        double total = 0;
        double remaining = 0;
        int purchasedCount = 0;
        int totalCount = 0;

        for (Object item : allItems) {
            if (item instanceof GroceryItem) {
                GroceryItem groceryItem = (GroceryItem) item;
                total += groceryItem.getPrice();
                if (!groceryItem.isPurchased()) {
                    remaining += groceryItem.getPrice();
                } else {
                    purchasedCount++;
                }
                totalCount++;
            }
        }

        totalPrice = total;
        remainingPrice = remaining;

        // Calculate progress as percentage of purchased items
        double progress = totalCount > 0 ? (purchasedCount * 100.0 / totalCount) : 0;
        progressBar.setProgress((int) progress);
        progressText.setText(String.format("%d/%d items", purchasedCount, totalCount));
        totalPriceText.setText(String.format("Total: Nrs %.0f", totalPrice));
        remainingPriceText.setText(String.format("Remaining: Nrs %.0f", remainingPrice));
    }

    private void updateShareButtonVisibility() {
        boolean hasSelectedItems = filteredItems.stream()
                .filter(item -> item instanceof GroceryItem)
                .map(item -> (GroceryItem) item)
                .anyMatch(GroceryItem::isSelectedForSms);
        shareButton.setVisibility(hasSelectedItems ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onItemSelectedForSms(GroceryItem item, boolean isSelected) {
        // Prevent rapid clicking
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastSelectionClickTime < CLICK_DELAY) {
            return;
        }
        lastSelectionClickTime = currentTime;

        if (auth.getCurrentUser() == null || userGroceryRef == null) {
            ToastUtil.showErrorToast(requireContext(), "Please log in to update items");
            return;
        }

        item.setSelectedForSms(isSelected);

        // Create a map with all the item's data
        Map<String, Object> itemData = item.toMap();
        
        // Update in Firestore using update with the item's ID
        userGroceryRef.update(item.getId(), itemData)
            .addOnSuccessListener(aVoid -> {
                adapter.updateItem(item);
                // Show/hide share button based on selection
                updateShareButtonVisibility();
                ToastUtil.showSuccessToast(requireContext(), 
                    isSelected ? "Item selected for sharing" : "Item unselected for sharing");
            })
            .addOnFailureListener(e -> {
                // Revert the change if update fails
                item.setSelectedForSms(!isSelected);
                adapter.updateItem(item);
                if (getView() != null) {
                    ToastUtil.showErrorToast(requireContext(), "Error updating selection: " + e.getMessage());
                }
            });
    }

    @Override
    public void onBuyClicked(GroceryItem item) {
        // Prevent rapid clicking
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastBuyClickTime < CLICK_DELAY) {
            return;
        }
        lastBuyClickTime = currentTime;

        if (auth.getCurrentUser() == null || userGroceryRef == null) {
            ToastUtil.showErrorToast(requireContext(), "Please log in to update items");
            return;
        }

        // Toggle the purchased state
        boolean newPurchasedState = !item.isPurchased();
        item.setPurchased(newPurchasedState);

        // Create a map with all the item's data
        Map<String, Object> itemData = item.toMap();
        
        // Update in Firestore using update with the item's ID
        userGroceryRef.update(item.getId(), itemData)
            .addOnSuccessListener(aVoid -> {
                adapter.updateItem(item);
                updateTotals();
                ToastUtil.showSuccessToast(requireContext(), 
                    newPurchasedState ? "Item marked as purchased" : "Item marked as unpurchased");
            })
            .addOnFailureListener(e -> {
                // Revert the change if update fails
                item.setPurchased(!newPurchasedState);
                adapter.updateItem(item);
                ToastUtil.showErrorToast(requireContext(), "Error updating item: " + e.getMessage());
            });
    }

    private void showShareDialog() {
        // Prevent rapid clicking
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastShareClickTime < CLICK_DELAY) {
            return;
        }
        lastShareClickTime = currentTime;

        List<GroceryItem> selectedItems = getSelectedItems();
        if (selectedItems.isEmpty()) {
            ToastUtil.showErrorToast(requireContext(), "Please select items to share");
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
        // Allow phone numbers with or without country code
        // Nepal phone numbers are 10 digits without country code
        // With country code (+977), they would be 13 or 14 digits
        return phoneNumber.matches("^(\\+977)?[0-9]{10}$");
    }

    private List<GroceryItem> getSelectedItems() {
        return filteredItems.stream()
                .filter(item -> item instanceof GroceryItem)
                .map(item -> (GroceryItem) item)
                .filter(GroceryItem::isSelectedForSms)
                .collect(Collectors.toList());
    }

    private void requestSmsPermissionAndSend(String phoneNumber, List<GroceryItem> items) {
        // Store the data for later use after permission is granted
        this.pendingPhoneNumber = phoneNumber;
        this.pendingItems = new ArrayList<>(items);
        
        if (ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            // Show explanation if needed
            if (shouldShowRequestPermissionRationale(Manifest.permission.SEND_SMS)) {
                ToastUtil.showErrorToast(requireContext(), 
                    "SMS permission is needed to share your grocery list");
                
                // Request permission after showing explanation
                ActivityCompat.requestPermissions(requireActivity(),
                        new String[]{Manifest.permission.SEND_SMS},
                        SMS_PERMISSION_REQUEST_CODE);
            } else {
                ActivityCompat.requestPermissions(requireActivity(),
                        new String[]{Manifest.permission.SEND_SMS},
                        SMS_PERMISSION_REQUEST_CODE);
            }
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

            // Format phone number
            String formattedNumber = phoneNumber;
            if (!phoneNumber.startsWith("+")) {
                // Remove any spaces or special characters
                phoneNumber = phoneNumber.replaceAll("[^0-9]", "");
                // Add Nepal country code if not present
                if (phoneNumber.length() == 10) {
                    formattedNumber = "+977" + phoneNumber;
                } else if (phoneNumber.length() == 13 && phoneNumber.startsWith("977")) {
                    formattedNumber = "+" + phoneNumber;
                }
            }

            // Validate the final phone number
            if (!formattedNumber.matches("^\\+977[0-9]{10}$")) {
                throw new IllegalArgumentException("Invalid phone number format");
            }

            SmsManager smsManager = SmsManager.getDefault();
            ArrayList<String> parts = smsManager.divideMessage(message.toString());
            
            // Send SMS parts
            for (String part : parts) {
                smsManager.sendTextMessage(formattedNumber, null, part, null, null);
            }

            if (getView() != null) {
                ToastUtil.showSuccessToast(requireContext(), "Grocery list shared successfully");
            }
        } catch (IllegalArgumentException e) {
            ToastUtil.showErrorToast(requireContext(), 
                "Invalid phone number format. Please enter a valid 10-digit number.");
        } catch (Exception e) {
            ToastUtil.showErrorToast(requireContext(), "Failed to send SMS: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        if (requestCode == SMS_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (pendingPhoneNumber != null && pendingItems != null) {
                    sendSms(pendingPhoneNumber, pendingItems);
                    pendingPhoneNumber = null;
                    pendingItems = null;
                } else {
                    ToastUtil.showErrorToast(requireContext(), 
                        "Permission granted. Please try sharing again.");
                }
            } else {
                ToastUtil.showErrorToast(requireContext(), 
                    "SMS permission denied. Cannot share grocery list.");
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }
}
