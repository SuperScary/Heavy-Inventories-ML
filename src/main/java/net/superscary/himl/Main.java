package net.superscary.himl;

import net.superscary.himl.model.MLModel;
import net.superscary.himl.model.MLModelFactory;
import net.superscary.himl.parser.RecipeParser;
import net.superscary.himl.trainer.Trainer;

import java.io.IOException;
import java.util.Map;

public class Main {
    public static void main(String[] args) {
        try {
            // Create models with different learning rates
            MLModel model1 = MLModelFactory.createModel("gradient", 0.01);
            MLModel model2 = MLModelFactory.createModel("adam", 0.01);
            
            // Create recipe parser and load recipes
            RecipeParser recipeParser = new RecipeParser(model1);
            System.out.println("Loading recipes...");
            recipeParser.loadRecipes("recipes.json");
            
            // Initialize base materials for both models
            System.out.println("Initializing base materials...");
            for (String material : recipeParser.getBaseMaterials()) {
                model1.initializeBaseMaterial(material);
                model2.initializeBaseMaterial(material);
            }
            
            // Create trainer and train models
            Trainer trainer = new Trainer(model1, model2, recipeParser);
            trainer.trainMultipleSessions(1000, 0.5);
            
            // Evaluate models
            double avgError = trainer.evaluate();
            System.out.println("Average error after training: " + avgError);
            
            // Save trained data
            trainer.saveTrainedData();
            
            // Test predictions
            System.out.println("\nTesting predictions for some recipes:");
            for (String item : recipeParser.getRecipes()) {
                Map<String, Integer> baseMaterials = recipeParser.resolveBaseMaterials(item);
                if (!baseMaterials.isEmpty()) {
                    int complexity = recipeParser.getRecipeComplexity(item);
                    double prediction1 = model1.predictWeight(baseMaterials, complexity);
                    double prediction2 = model2.predictWeight(baseMaterials, complexity);
                    System.out.printf("Item: %s%n", item);
                    //System.out.printf("  Base materials: %s%n", baseMaterials);
                    //System.out.printf("  Complexity: %d%n", complexity);
                    //System.out.printf("  Gradient Descent prediction: %.2f%n", prediction1);
                    //System.out.printf("  Adam prediction: %.2f%n", prediction2);
                    System.out.println();
                }
            }
            
        } catch (IOException e) {
            System.err.println("Error loading recipes: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("Unexpected error: " + e.getMessage());
            e.printStackTrace();
        }
    }
} 