package com.example.meal_app.model;

import java.util.HashMap;
import java.util.Map;

public class GroceryItem {
    private String id;
    private String name;
    private double quantity;
    private String unit;
    private String category;
    private double price;
    private boolean isPurchased;
    private boolean isSelectedForSms; // Remove transient to persist the field

    public GroceryItem() {
        // Required empty constructor for Firestore
    }

    public GroceryItem(String id, String name, double quantity, String unit, String category, double price) {
        this.id = id;
        this.name = name;
        this.quantity = quantity;
        this.unit = unit;
        this.category = category;
        this.price = price;
        this.isPurchased = false;
        this.isSelectedForSms = false;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getQuantity() {
        return quantity;
    }

    public void setQuantity(double quantity) {
        this.quantity = quantity;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public boolean isPurchased() {
        return isPurchased;
    }

    public void setPurchased(boolean purchased) {
        isPurchased = purchased;
    }

    public boolean isSelectedForSms() {
        return isSelectedForSms;
    }

    public void setSelectedForSms(boolean selectedForSms) {
        isSelectedForSms = selectedForSms;
    }

    // Convert to Firestore Map
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("name", name);
        map.put("quantity", quantity);
        map.put("unit", unit);
        map.put("category", category);
        map.put("price", price);
        map.put("isPurchased", isPurchased);
        map.put("isSelectedForSms", isSelectedForSms);
        return map;
    }
}