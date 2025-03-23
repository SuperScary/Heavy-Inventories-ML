package net.superscary.himl.parser;

import net.superscary.himl.model.MLModel;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class RecipeParser {
    private final MLModel model;
    private final Map<String, Recipe> recipes;
    private final Set<String> baseMaterials;
    private final Map<String, Integer> recipeComplexity;

    public RecipeParser(MLModel model) {
        this.model = model;
        this.recipes = new HashMap<>();
        this.baseMaterials = new HashSet<>();
        this.recipeComplexity = new HashMap<>();
    }

    public void loadRecipes(String recipesFile) throws IOException {
        //System.out.println("Loading recipes from: " + recipesFile);
        String content = Files.readString(Paths.get(recipesFile));
        //System.out.println("File content length: " + content.length());
        
        Gson gson = new Gson();
        JsonArray recipesArray = gson.fromJson(content, JsonArray.class);
        //System.out.println("Found " + recipesArray.size() + " recipes");
        
        for (JsonElement element : recipesArray) {
            try {
                JsonObject recipeObj = element.getAsJsonObject();
                parseRecipe(recipeObj);
            } catch (Exception e) {
                System.err.println("Error parsing recipe: " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        //System.out.println("Total recipes in map: " + recipes.size());
        //System.out.println("Base materials found: " + baseMaterials);
    }

    private void calculateRecipeComplexity() {
        for (String itemId : recipes.keySet()) {
            calculateComplexity(itemId, new HashSet<>());
        }
    }

    private int calculateComplexity(String itemId, Set<String> visited) {
        if (visited.contains(itemId)) {
            return 0;
        }
        visited.add(itemId);

        Recipe recipe = recipes.get(itemId);
        if (recipe == null) {
            return 0; // Base material
        }

        int maxComplexity = 0;
        for (String ingredient : recipe.getIngredients().keySet()) {
            int ingredientComplexity = calculateComplexity(ingredient, visited);
            maxComplexity = Math.max(maxComplexity, ingredientComplexity);
        }

        int complexity = maxComplexity + 1; // Add 1 for this crafting step
        recipeComplexity.put(itemId, complexity);
        return complexity;
    }

    public int getRecipeComplexity(String itemId) {
        return recipeComplexity.getOrDefault(itemId, 0);
    }

    private void parseRecipe(JsonObject recipeObj) {
        try {
            // Extract item name
            String itemName = recipeObj.get("itemName").getAsString();
            if (itemName == null) {
                System.out.println("Warning: No itemName found in recipe JSON");
                return;
            }
            System.out.println("\nParsing recipe for item: " + itemName);

            // Extract ingredients based on recipe type
            Map<String, Integer> ingredients = new HashMap<>();
            String type = recipeObj.get("type").getAsString();
            System.out.println("Recipe type: " + type);
            
            if ("minecraft:crafting_shaped".equals(type)) {
                // Parse shaped recipe
                JsonArray pattern = recipeObj.getAsJsonArray("pattern");
                JsonObject key = recipeObj.getAsJsonObject("key");
                if (pattern != null && key != null) {
                    System.out.println("Pattern: " + pattern);
                    System.out.println("Key: " + key);
                    parseShapedRecipe(pattern, key, ingredients);
                } else {
                    System.out.println("Warning: Missing pattern or key for shaped recipe");
                }
            } else if ("minecraft:crafting_shapeless".equals(type)) {
                // Parse shapeless recipe
                JsonArray ingredientsList = recipeObj.getAsJsonArray("ingredients");
                if (ingredientsList != null) {
                    System.out.println("Ingredients list: " + ingredientsList);
                    parseShapelessRecipe(ingredientsList, ingredients);
                } else {
                    System.out.println("Warning: Missing ingredients for shapeless recipe");
                }
            }

            if (!ingredients.isEmpty()) {
                Recipe recipe = new Recipe(itemName, ingredients);
                recipes.put(itemName, recipe);
                System.out.println("Added recipe for " + itemName + " with " + ingredients.size() + " ingredients: " + ingredients);
                
                // Initialize base materials in the model
                ingredients.keySet().forEach(ingredient -> {
                    baseMaterials.add(ingredient);
                    model.initializeBaseMaterial(ingredient);
                });
            } else {
                System.out.println("Warning: No ingredients found for " + itemName);
            }
            
        } catch (Exception e) {
            System.err.println("Error parsing recipe: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void parseShapedRecipe(JsonArray pattern, JsonObject key, Map<String, Integer> ingredients) {
        // Parse pattern
        for (JsonElement rowElement : pattern) {
            String row = rowElement.getAsString().replaceAll("\\s+", "");
            System.out.println("Processing row: " + row);
            
            for (char c : row.toCharArray()) {
                if (c != ' ') {
                    JsonObject keyItem = key.getAsJsonObject(String.valueOf(c));
                    if (keyItem != null) {
                        String item = keyItem.get("item").getAsString();
                        ingredients.merge(item, 1, Integer::sum);
                        System.out.println("Added ingredient: " + item);
                    } else {
                        System.out.println("Warning: No mapping found for character: " + c);
                    }
                }
            }
        }
    }

    private void parseShapelessRecipe(JsonArray ingredientsList, Map<String, Integer> ingredients) {
        for (JsonElement ingredientElement : ingredientsList) {
            JsonObject ingredient = ingredientElement.getAsJsonObject();
            String item = ingredient.get("item").getAsString();
            ingredients.merge(item, 1, Integer::sum);
            System.out.println("Added shapeless ingredient: " + item);
        }
    }

    public Map<String, Integer> resolveBaseMaterials(String itemId) {
        //System.out.println("Resolving base materials for: " + itemId);
        Map<String, Integer> result = resolveBaseMaterials(itemId, new HashSet<>());
        //System.out.println("Base materials for " + itemId + ": " + result);
        return result;
    }

    private Map<String, Integer> resolveBaseMaterials(String itemId, Set<String> visited) {
        if (visited.contains(itemId)) {
            System.out.println("Found visited item: " + itemId);
            Map<String, Integer> result = new HashMap<>();
            result.put(itemId, 1);
            baseMaterials.add(itemId);
            return result;
        }
        visited.add(itemId);

        Recipe recipe = recipes.get(itemId);
        if (recipe == null) {
            System.out.println("Found base material: " + itemId);
            Map<String, Integer> result = new HashMap<>();
            result.put(itemId, 1);
            baseMaterials.add(itemId);
            return result;
        }

        //System.out.println("Processing recipe for: " + itemId);
        Map<String, Integer> baseMaterials = new HashMap<>();
        for (Map.Entry<String, Integer> entry : recipe.getIngredients().entrySet()) {
            String ingredient = entry.getKey();
            int count = entry.getValue();
            System.out.println("Processing ingredient: " + ingredient + " (count: " + count + ")");
            
            Map<String, Integer> resolved = resolveBaseMaterials(ingredient, visited);
            for (Map.Entry<String, Integer> baseEntry : resolved.entrySet()) {
                baseMaterials.merge(baseEntry.getKey(), baseEntry.getValue() * count, Integer::sum);
            }
        }
        return baseMaterials;
    }

    public Set<String> getBaseMaterials() {
        System.out.println("Current base materials: " + baseMaterials);
        return new HashSet<>(baseMaterials);
    }

    /**
     * Returns a set of all recipe IDs that have been loaded.
     * @return Set of recipe IDs
     */
    public Set<String> getRecipes() {
        return new HashSet<>(recipes.keySet());
    }

    private static class Recipe {
        private final String itemId;
        private final Map<String, Integer> ingredients;

        public Recipe(String itemId, Map<String, Integer> ingredients) {
            this.itemId = itemId;
            this.ingredients = new HashMap<>(ingredients);
        }

        public String getItemId() {
            return itemId;
        }

        public Map<String, Integer> getIngredients() {
            return new HashMap<>(ingredients);
        }
    }
} 