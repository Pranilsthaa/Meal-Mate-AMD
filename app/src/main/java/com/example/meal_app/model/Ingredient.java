package com.example.meal_app.model;

import java.io.Serializable;

public class Ingredient implements Serializable {
    private String name;
    private String category;
    private double quantity;
    private String unit;
    private double price;
    private boolean bought;
    private boolean selectedForSms;

    // Default constructor for Firestore
    public Ingredient() {
    }

    public Ingredient(String name, String category, double quantity, String unit, double price) {
        this.name = name;
        this.category = category;
        this.quantity = quantity;
        this.unit = unit;
        this.price = price;
        this.bought = false;
        this.selectedForSms = false;
    }

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
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

    public boolean isBought() {
        return bought;
    }

    public void setBought(boolean bought) {
        this.bought = bought;
    }

    public boolean isSelectedForSms() {
        return selectedForSms;
    }

    public void setSelectedForSms(boolean selectedForSms) {
        this.selectedForSms = selectedForSms;
    }

    @Override
    public String toString() {
        return String.format("%.1f %s %s", quantity, unit, name);
    }
} 