package com.example.meal_app.data;

import com.example.meal_app.model.Recipe;
import java.util.ArrayList;
import java.util.List;

public class RecipeData {
    public static List<Recipe> getPredefinedRecipes() {
        List<Recipe> recipes = new ArrayList<>();

        // Breakfast Recipes
        Recipe pancakes = new Recipe();
        pancakes.setId("1");
        pancakes.setName("Classic Pancakes");
        pancakes.setDescription("Fluffy, golden pancakes perfect for breakfast");
        pancakes.setImageUrl("https://example.com/pancakes.jpg");
        pancakes.setCookTime(15);
        pancakes.setServings(4);
        pancakes.setIngredients(new String[]{"2 cups flour", "2 eggs", "1 cup milk", "2 tbsp sugar", "1 tsp baking powder"});
        pancakes.setInstructions(new String[]{"Mix dry ingredients", "Add wet ingredients", "Cook on griddle"});
        pancakes.setCategory("Breakfast");
        pancakes.setCalories(250);
        pancakes.setTotalPrice(5.0);
        recipes.add(pancakes);

        Recipe overnightOats = new Recipe();
        overnightOats.setId("2");
        overnightOats.setName("Overnight Oats");
        overnightOats.setDescription("Healthy and easy overnight oats with fruits");
        overnightOats.setImageUrl("https://example.com/overnight-oats.jpg");
        overnightOats.setCookTime(0);
        overnightOats.setServings(2);
        overnightOats.setIngredients(new String[]{"1 cup oats", "1 cup milk", "1 banana", "1 tbsp honey", "1 tbsp chia seeds"});
        overnightOats.setInstructions(new String[]{"Mix ingredients", "Refrigerate overnight", "Top with fruits"});
        overnightOats.setCategory("Breakfast");
        overnightOats.setCalories(300);
        overnightOats.setTotalPrice(3.0);
        recipes.add(overnightOats);

        // Main Course Recipes
        Recipe chickenStirFry = new Recipe();
        chickenStirFry.setId("3");
        chickenStirFry.setName("Chicken Stir Fry");
        chickenStirFry.setDescription("Quick and healthy chicken stir fry with vegetables");
        chickenStirFry.setImageUrl("https://example.com/stir-fry.jpg");
        chickenStirFry.setCookTime(20);
        chickenStirFry.setServings(4);
        chickenStirFry.setIngredients(new String[]{"500g chicken", "2 cups mixed vegetables", "3 tbsp soy sauce", "2 cloves garlic"});
        chickenStirFry.setInstructions(new String[]{"Cut chicken", "Stir fry vegetables", "Add sauce"});
        chickenStirFry.setCategory("Lunch");
        chickenStirFry.setCalories(400);
        chickenStirFry.setTotalPrice(12.0);
        recipes.add(chickenStirFry);

        Recipe carbonara = new Recipe();
        carbonara.setId("4");
        carbonara.setName("Spaghetti Carbonara");
        carbonara.setDescription("Classic Italian pasta dish with eggs and cheese");
        carbonara.setImageUrl("https://example.com/carbonara.jpg");
        carbonara.setCookTime(15);
        carbonara.setServings(4);
        carbonara.setIngredients(new String[]{"400g spaghetti", "200g pancetta", "4 eggs", "100g pecorino", "Black pepper"});
        carbonara.setInstructions(new String[]{"Cook pasta", "Fry pancetta", "Mix with eggs"});
        carbonara.setCategory("Dinner");
        carbonara.setCalories(600);
        carbonara.setTotalPrice(15.0);
        recipes.add(carbonara);

        // Vegetarian Recipes
        Recipe vegetableCurry = new Recipe();
        vegetableCurry.setId("5");
        vegetableCurry.setName("Vegetable Curry");
        vegetableCurry.setDescription("Spicy and flavorful vegetable curry");
        vegetableCurry.setImageUrl("https://example.com/curry.jpg");
        vegetableCurry.setCookTime(30);
        vegetableCurry.setServings(4);
        vegetableCurry.setIngredients(new String[]{"Mixed vegetables", "Coconut milk", "Curry powder", "Onion", "Garlic"});
        vegetableCurry.setInstructions(new String[]{"Sauté vegetables", "Add spices", "Simmer in coconut milk"});
        vegetableCurry.setCategory("Dinner");
        vegetableCurry.setCalories(350);
        vegetableCurry.setTotalPrice(8.0);
        recipes.add(vegetableCurry);

        // Dessert Recipes
        recipes.add(new Recipe(
            "7",
            "Chocolate Cake",
            "Rich and moist chocolate cake",
            "https://example.com/chocolate-cake.jpg",
            35,
            8,
            new String[]{"2 cups flour", "2 cups sugar", "3/4 cup cocoa", "2 eggs", "1 cup milk"},
            new String[]{"Mix dry ingredients", "Add wet ingredients", "Bake"},
            "Dessert",
            350
        ));

        recipes.add(new Recipe(
            "8",
            "Apple Pie",
            "Classic American apple pie",
            "https://example.com/apple-pie.jpg",
            45,
            8,
            new String[]{"6 apples", "Pie crust", "Cinnamon", "Sugar", "Butter"},
            new String[]{"Prepare apples", "Fill pie crust", "Bake"},
            "Dessert",
            300
        ));

        // Quick Meals
        recipes.add(new Recipe(
            "9",
            "Quesadillas",
            "Quick and easy Mexican quesadillas",
            "https://example.com/quesadillas.jpg",
            15,
            4,
            new String[]{"Tortillas", "Cheese", "Chicken", "Bell peppers", "Onion"},
            new String[]{"Fill tortillas", "Cook until golden", "Cut and serve"},
            "Quick Meal",
            400
        ));

        recipes.add(new Recipe(
            "10",
            "Pizza",
            "Homemade pizza with fresh toppings",
            "https://example.com/pizza.jpg",
            15,
            4,
            new String[]{"Pizza dough", "Tomato sauce", "Mozzarella", "Toppings"},
            new String[]{"Roll dough", "Add toppings", "Bake"},
            "Quick Meal",
            500
        ));

        // Healthy Options
        recipes.add(new Recipe(
            "11",
            "Quinoa Bowl",
            "Healthy quinoa bowl with vegetables",
            "https://example.com/quinoa-bowl.jpg",
            20,
            2,
            new String[]{"Quinoa", "Mixed vegetables", "Avocado", "Lemon", "Olive oil"},
            new String[]{"Cook quinoa", "Prepare vegetables", "Assemble bowl"},
            "Healthy",
            350
        ));

        recipes.add(new Recipe(
            "12",
            "Salmon Salad",
            "Fresh and healthy salmon salad",
            "https://example.com/salmon-salad.jpg",
            10,
            2,
            new String[]{"Salmon", "Mixed greens", "Cherry tomatoes", "Cucumber", "Olive oil"},
            new String[]{"Cook salmon", "Prepare salad", "Combine"},
            "Healthy",
            400
        ));

        // International Cuisine
        recipes.add(new Recipe(
            "13",
            "Pad Thai",
            "Classic Thai noodle dish",
            "https://example.com/pad-thai.jpg",
            15,
            4,
            new String[]{"Rice noodles", "Tofu", "Bean sprouts", "Peanuts", "Pad Thai sauce"},
            new String[]{"Cook noodles", "Prepare sauce", "Stir fry"},
            "International",
            450
        ));

        recipes.add(new Recipe(
            "14",
            "Sushi Rolls",
            "Fresh and healthy sushi rolls",
            "https://example.com/sushi.jpg",
            20,
            4,
            new String[]{"Sushi rice", "Nori", "Fish", "Vegetables", "Soy sauce"},
            new String[]{"Prepare rice", "Roll sushi", "Cut and serve"},
            "International",
            300
        ));

        recipes.add(new Recipe(
            "15",
            "Greek Salad",
            "Fresh Mediterranean salad",
            "https://example.com/greek-salad.jpg",
            0,
            4,
            new String[]{"Cucumber", "Tomatoes", "Olives", "Feta", "Olive oil"},
            new String[]{"Chop vegetables", "Combine ingredients", "Dress with oil"},
            "International",
            200
        ));

        return recipes;
    }
} 