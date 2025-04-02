package com.example.meal_app.model;

import java.util.HashMap;
import java.util.Map;

public class GroceryItem {
    private String id;
    private String name;
    private double quantity;
    private String unit;
    private double price;
    private String category;
    private boolean purchased;
    private boolean selectedForSms;
    private String recipeName;

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
        this.purchased = false;
        this.selectedForSms = false;
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

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public boolean isPurchased() {
        return purchased;
    }

    public void setPurchased(boolean purchased) {
        this.purchased = purchased;
    }

    public boolean isSelectedForSms() {
        return selectedForSms;
    }

    public void setSelectedForSms(boolean selectedForSms) {
        this.selectedForSms = selectedForSms;
    }

    public String getRecipeName() {
        return recipeName;
    }

    public void setRecipeName(String recipeName) {
        this.recipeName = recipeName;
    }

    // Convert to Firestore Map
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("id", id);
        map.put("name", name);
        map.put("quantity", quantity);
        map.put("unit", unit);
        map.put("price", price);
        map.put("category", category);
        map.put("purchased", purchased);
        map.put("selectedForSms", selectedForSms);
        map.put("recipeName", recipeName);
        return map;
    }
}