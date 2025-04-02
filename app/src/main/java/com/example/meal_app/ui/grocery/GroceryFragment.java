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
import android.util.Log;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.widget.SearchView;
import android.widget.Toast;

public class GroceryFragment extends Fragment implements GroceryAdapter.OnGroceryItemListener, SensorEventListener {

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private String userId;
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

    // Shake detection
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private static final float SHAKE_THRESHOLD = 12.0f;
    private static final int MIN_TIME_BETWEEN_SHAKES = 1000;
    private long lastShakeTime;
    private float lastX = 0;
    private float lastY = 0;
    private float lastZ = 0;

    private SearchView searchView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() != null) {
            userId = auth.getCurrentUser().getUid();
        }
        
        // Initialize sensor manager
        sensorManager = (SensorManager) requireActivity().getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_grocery, container, false);

        try {
            // Initialize views first
            progressText = root.findViewById(R.id.progressText);
            progressBar = root.findViewById(R.id.progressBar);
            totalPriceText = root.findViewById(R.id.totalPrice);
            remainingPriceText = root.findViewById(R.id.remainingPrice);
            searchView = root.findViewById(R.id.searchView);
            RecyclerView recyclerView = root.findViewById(R.id.groceryRecyclerView);
            shareButton = root.findViewById(R.id.shareButton);

            // Setup RecyclerView
            recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
            adapter = new GroceryAdapter(new ArrayList<>(), this);
            recyclerView.setAdapter(adapter);

            // Setup share button
            shareButton.setOnClickListener(v -> showShareDialog());

            // Setup search functionality
            searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String query) {
                    filterItems(query);
                    return true;
                }

                @Override
                public boolean onQueryTextChange(String newText) {
                    filterItems(newText);
                    return true;
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
                                showError("Error loading user data: " + e.getMessage());
                            }
                        });
            } else {
                // User is not logged in, show message
                if (getView() != null) {
                    showError("Please log in to view your grocery list");
                }
                return root;
            }

            // Load grocery items
            loadGroceryItems();

        } catch (Exception e) {
            if (getView() != null) {
                showError("Error initializing grocery list: " + e.getMessage());
            }
            e.printStackTrace();
        }

        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (sensorManager != null && accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            long currentTime = System.currentTimeMillis();
            if ((currentTime - lastShakeTime) > MIN_TIME_BETWEEN_SHAKES) {
                float x = event.values[0];
                float y = event.values[1];
                float z = event.values[2];

                float deltaX = Math.abs(lastX - x);
                float deltaY = Math.abs(lastY - y);
                float deltaZ = Math.abs(lastZ - z);

                if ((deltaX > SHAKE_THRESHOLD && deltaY > SHAKE_THRESHOLD) ||
                    (deltaX > SHAKE_THRESHOLD && deltaZ > SHAKE_THRESHOLD) ||
                    (deltaY > SHAKE_THRESHOLD && deltaZ > SHAKE_THRESHOLD)) {
                    lastShakeTime = currentTime;
                    deleteCheckedItems();
                }

                lastX = x;
                lastY = y;
                lastZ = z;
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Not needed
    }

    private void loadGroceryItems() {
        Log.d("GroceryFragment", "Loading grocery items for userId: " + userId);

        db.collection("groceryLists").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Map<String, Object> groceryList = documentSnapshot.getData();
                        Log.d("GroceryFragment", "Grocery list data: " + groceryList);
                        
                        if (groceryList != null && !groceryList.isEmpty()) {
                            List<GroceryItem> ingredients = new ArrayList<>();
                            for (Map.Entry<String, Object> entry : groceryList.entrySet()) {
                                try {
                                    Object value = entry.getValue();
                                    if (value == null) {
                                        Log.d("GroceryFragment", "Skipping null value for key: " + entry.getKey());
                                        continue;
                                    }
                                    
                                    Map<String, Object> ingredientData = (Map<String, Object>) value;
                                    Log.d("GroceryFragment", "Processing item: " + entry.getKey());
                                    Log.d("GroceryFragment", "Item data: " + ingredientData);
                                    
                                    // Skip if required fields are missing
                                    if (!ingredientData.containsKey("name")) {
                                        Log.e("GroceryFragment", "Missing name for item: " + entry.getKey());
                                continue;
                            }

                                    GroceryItem ingredient = new GroceryItem();
                                    ingredient.setId(entry.getKey());
                                    ingredient.setName((String) ingredientData.get("name"));
                                    
                                    // Handle numeric values with null checks
                                    Object quantityObj = ingredientData.get("quantity");
                                    if (quantityObj instanceof Number) {
                                        ingredient.setQuantity(((Number) quantityObj).doubleValue());
                                    } else {
                                        ingredient.setQuantity(1.0); // Default value
                                    }
                                    
                                    Object priceObj = ingredientData.get("price");
                                    if (priceObj instanceof Number) {
                                        ingredient.setPrice(((Number) priceObj).doubleValue());
                                    } else {
                                        ingredient.setPrice(0.0); // Default value
                                    }
                                    
                                    // Handle unit with null check
                                    Object unitObj = ingredientData.get("unit");
                                    ingredient.setUnit(unitObj != null ? (String) unitObj : "");
                                    
                                    // Handle category with null check
                                    Object categoryObj = ingredientData.get("category");
                                    ingredient.setCategory(categoryObj != null ? (String) categoryObj : "Other");
                                    
                                    // Handle boolean values with null checks
                                    Object boughtObj = ingredientData.get("bought");
                                    ingredient.setPurchased(boughtObj instanceof Boolean ? (Boolean) boughtObj : false);
                                    
                                    Object selectedForSmsObj = ingredientData.get("selectedForSms");
                                    ingredient.setSelectedForSms(selectedForSmsObj instanceof Boolean ? (Boolean) selectedForSmsObj : false);
                                    
                                    Log.d("GroceryFragment", "Created ingredient object: " + ingredient);
                                    ingredients.add(ingredient);
                                } catch (Exception e) {
                                    Log.e("GroceryFragment", "Error processing ingredient: " + entry.getKey(), e);
                                }
                            }
                            
                            Log.d("GroceryFragment", "Total ingredients loaded: " + ingredients.size());
                            if (!ingredients.isEmpty()) {
                                updateGroceryList(ingredients);
                            } else {
                                showEmptyState();
                            }
                        } else {
                            Log.d("GroceryFragment", "Grocery list is empty");
                            showEmptyState();
                        }
                    } else {
                        Log.d("GroceryFragment", "No grocery list document found");
                        showEmptyState();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("GroceryFragment", "Error loading grocery list", e);
                    if (getView() != null) {
                        Snackbar.make(getView(), "Error loading grocery list: " + e.getMessage(), Snackbar.LENGTH_LONG).show();
                    }
                });
    }

    private void updateGroceryList(List<GroceryItem> ingredients) {
        Log.d("GroceryFragment", "Updating grocery list with " + ingredients.size() + " ingredients");
        
        // Group ingredients by category
        Map<String, List<GroceryItem>> categorizedIngredients = new HashMap<>();
        for (GroceryItem ingredient : ingredients) {
            // Determine category based on ingredient name and type
            String category = determineIngredientCategory(ingredient.getName());
            if (!categorizedIngredients.containsKey(category)) {
                categorizedIngredients.put(category, new ArrayList<>());
            }
            ingredient.setCategory(category); // Set the category in the item
            categorizedIngredients.get(category).add(ingredient);
        }
        
        Log.d("GroceryFragment", "Categories found: " + categorizedIngredients.keySet());

        // Sort categories and create final list
        List<Object> items = new ArrayList<>();
        String[] categoryOrder = {
            "Meat & Fish",
            "Dairy",
            "Fruits",
            "Vegetables",
            "Grains & Pasta",
            "Condiments & Spices",
            "Other"
        };

        // Add categories in order
        for (String category : categoryOrder) {
            List<GroceryItem> categoryItems = categorizedIngredients.get(category);
            if (categoryItems != null && !categoryItems.isEmpty()) {
                // Add category header
                items.add(category);
                // Sort items within category by name
                categoryItems.sort((a, b) -> a.getName().compareToIgnoreCase(b.getName()));
                // Add sorted items
                items.addAll(categoryItems);
            }
        }

        // Update adapter with the new items
        if (adapter != null) {
            Log.d("GroceryFragment", "Setting up RecyclerView");
            allItems = items;
            filteredItems = new ArrayList<>(items);
            adapter.updateData(filteredItems);

            // Update totals and progress
            updateTotals();
            updateProgress();
            
            // Show/hide views based on whether we have items
            if (isAdded() && getView() != null) {
                View recyclerView = getView().findViewById(R.id.groceryRecyclerView);
                View emptyStateLayout = getView().findViewById(R.id.emptyStateLayout);
                
                if (recyclerView != null && emptyStateLayout != null) {
                    if (items.isEmpty()) {
                        recyclerView.setVisibility(View.GONE);
                        emptyStateLayout.setVisibility(View.VISIBLE);
                    } else {
                        recyclerView.setVisibility(View.VISIBLE);
                        emptyStateLayout.setVisibility(View.GONE);
                    }
                }
            }
        }
    }

    private String determineIngredientCategory(String ingredient) {
        ingredient = ingredient.toLowerCase();
        
        // Meat & Fish
        if (ingredient.contains("meat") || ingredient.contains("chicken") || 
            ingredient.contains("beef") || ingredient.contains("pork") || 
            ingredient.contains("fish") || ingredient.contains("lamb") ||
            ingredient.contains("shrimp") || ingredient.contains("seafood")) {
            return "Meat & Fish";
        }
        
        // Dairy
        if (ingredient.contains("milk") || ingredient.contains("cheese") || 
            ingredient.contains("yogurt") || ingredient.contains("cream") ||
            ingredient.contains("butter") || ingredient.contains("curd") ||
            ingredient.contains("paneer")) {
            return "Dairy";
        }
        
        // Fruits
        if (ingredient.contains("apple") || ingredient.contains("banana") || 
            ingredient.contains("orange") || ingredient.contains("berry") ||
            ingredient.contains("fruit") || ingredient.contains("mango") ||
            ingredient.contains("grape") || ingredient.contains("melon")) {
            return "Fruits";
        }
        
        // Vegetables
        if (ingredient.contains("carrot") || ingredient.contains("potato") || 
            ingredient.contains("onion") || ingredient.contains("tomato") ||
            ingredient.contains("vegetable") || ingredient.contains("garlic") ||
            ingredient.contains("ginger") || ingredient.contains("pepper") ||
            ingredient.contains("lettuce") || ingredient.contains("spinach") ||
            ingredient.contains("cucumber")) {
            return "Vegetables";
        }
        
        // Grains & Pasta
        if (ingredient.contains("rice") || ingredient.contains("pasta") || 
            ingredient.contains("flour") || ingredient.contains("bread") ||
            ingredient.contains("cereal") || ingredient.contains("noodle") ||
            ingredient.contains("wheat") || ingredient.contains("grain")) {
            return "Grains & Pasta";
        }
        
        // Condiments & Spices
        if (ingredient.contains("oil") || ingredient.contains("sauce") || 
            ingredient.contains("vinegar") || ingredient.contains("spice") ||
            ingredient.contains("herb") || ingredient.contains("salt") ||
            ingredient.contains("sugar") || ingredient.contains("masala") ||
            ingredient.contains("powder") || ingredient.contains("seasoning")) {
            return "Condiments & Spices";
        }
        
        return "Other";
    }

    private void showEmptyState() {
        Log.d("GroceryFragment", "Showing empty state");
        if (isAdded() && getView() != null) {
            View recyclerView = getView().findViewById(R.id.groceryRecyclerView);
            View emptyStateLayout = getView().findViewById(R.id.emptyStateLayout);
            
            if (recyclerView != null && emptyStateLayout != null) {
                recyclerView.setVisibility(View.GONE);
                emptyStateLayout.setVisibility(View.VISIBLE);
            } else {
                Log.e("GroceryFragment", "RecyclerView or emptyStateLayout is null");
            }
        } else {
            Log.e("GroceryFragment", "Fragment is not attached or view is null");
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

    private void updateTotalPrice() {
        if (getView() == null) return;

        double total = 0;
        double remaining = 0;

        for (Object item : allItems) {
            if (item instanceof GroceryItem) {
                GroceryItem groceryItem = (GroceryItem) item;
                total += groceryItem.getPrice();
                if (!groceryItem.isPurchased()) {
                    remaining += groceryItem.getPrice();
                }
            }
        }

        totalPrice = total;
        remainingPrice = remaining;

        totalPriceText.setText(String.format("Total: Nrs %.0f", totalPrice));
        remainingPriceText.setText(String.format("Remaining: Nrs %.0f", remainingPrice));
    }

    private void updateProgress() {
        if (getView() == null) return;

        int purchasedCount = 0;
        int totalCount = 0;

        for (Object item : allItems) {
            if (item instanceof GroceryItem) {
                GroceryItem groceryItem = (GroceryItem) item;
                if (groceryItem.isPurchased()) {
                    purchasedCount++;
                }
                totalCount++;
            }
        }

        // Calculate progress as percentage of purchased items
        double progress = totalCount > 0 ? (purchasedCount * 100.0 / totalCount) : 0;
        progressBar.setProgress((int) progress);
        progressText.setText(String.format("%d/%d items", purchasedCount, totalCount));
    }

    private void deleteCheckedItems() {
        List<GroceryItem> checkedItems = new ArrayList<>();
        for (Object item : allItems) {
            if (item instanceof GroceryItem && ((GroceryItem) item).isPurchased()) {
                checkedItems.add((GroceryItem) item);
            }
        }

        if (checkedItems.isEmpty()) {
            // Show toast at the top of the screen
            Toast.makeText(requireContext(), "No purchased items to delete", Toast.LENGTH_SHORT).show();
            return;
        }

        // Delete checked items from Firestore
        for (GroceryItem item : checkedItems) {
            String ingredientId = item.getId();
            
            db.collection("groceryLists").document(userId)
                .update(ingredientId, null)
                .addOnSuccessListener(aVoid -> {
                    // Remove item from lists
                    allItems.remove(item);
                    filteredItems.remove(item);
                    adapter.notifyDataSetChanged();
                    updateTotals();
                    updateProgress();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(requireContext(), "Error deleting item: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
        }

        Toast.makeText(requireContext(), "Deleting purchased items...", Toast.LENGTH_SHORT).show();
    }

    private void showError(String message) {
        if (getContext() != null) {
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        }
    }
}
