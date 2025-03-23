package com.example.meal_app.model;

public class Recipe {
    private String id;
    private String name;
    private String description;
    private String imageUrl;
    private int cookTime;
    private int servings;
    private String[] ingredients;
    private String[] instructions;
    private String category;
    private double calories;
    private double totalPrice;
    private String userId;

    // Default constructor for Firestore
    public Recipe() {
        this.id = "";
        this.name = "";
        this.description = "";
        this.imageUrl = "";
        this.cookTime = 0;
        this.servings = 0;
        this.ingredients = new String[0];
        this.instructions = new String[0];
        this.category = "";
        this.calories = 0.0;
        this.totalPrice = 0.0;
        this.userId = "";
    }

    public Recipe(String id, String name, String description, String imageUrl, int cookTime, 
                 int servings, String[] ingredients, String[] instructions, 
                 String category, double calories) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.imageUrl = imageUrl;
        this.cookTime = cookTime;
        this.servings = servings;
        this.ingredients = ingredients;
        this.instructions = instructions;
        this.category = category;
        this.calories = calories;
    }

    // Getters
    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getImageUrl() { return imageUrl; }
    public int getCookTime() { return cookTime; }
    public int getCookingTime() { return getCookTime(); }
    public int getServings() { return servings; }
    public String[] getIngredients() { return ingredients; }
    public String[] getInstructions() { return instructions; }
    public String getCategory() { return category; }
    public double getCalories() { return calories; }
    public double getTotalPrice() { return totalPrice; }
    public String getUserId() { return userId; }

    // Setters
    public void setId(String id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setDescription(String description) { this.description = description; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public void setCookTime(int cookTime) { this.cookTime = cookTime; }
    public void setServings(int servings) { this.servings = servings; }
    public void setIngredients(String[] ingredients) { this.ingredients = ingredients; }
    public void setInstructions(String[] instructions) { this.instructions = instructions; }
    public void setCategory(String category) { this.category = category; }
    public void setCalories(double calories) { this.calories = calories; }
    public void setTotalPrice(double totalPrice) { this.totalPrice = totalPrice; }
    public void setUserId(String userId) { this.userId = userId; }
}