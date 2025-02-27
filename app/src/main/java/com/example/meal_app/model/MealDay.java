package com.example.meal_app.model;

import java.util.List;
import java.util.ArrayList;

public class MealDay {
    private String day;
    private List<Recipe> recipes;

    public MealDay(String day) {
        this.day = day;
        this.recipes = new ArrayList<>();
    }

    public MealDay(String day, List<Recipe> recipes) {
        this.day = day;
        this.recipes = recipes != null ? recipes : new ArrayList<>();
    }

    public String getDay() {
        return day;
    }

    public void setDay(String day) {
        this.day = day;
    }

    public List<Recipe> getRecipes() {
        return recipes;
    }

    public void setRecipes(List<Recipe> recipes) {
        this.recipes = recipes != null ? recipes : new ArrayList<>();
    }

    public void addRecipe(Recipe recipe) {
        if (recipe != null) {
            recipes.add(recipe);
        }
    }

    public void removeRecipe(Recipe recipe) {
        recipes.remove(recipe);
    }
}