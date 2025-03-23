package net.superscary.himl.trainer;

import net.superscary.himl.model.MLModel;
import net.superscary.himl.parser.RecipeParser;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.*;
import java.nio.file.*;
import java.util.*;

public class Trainer {
    private final MLModel model1;
    private final MLModel model2;
    private final RecipeParser recipeParser;
    private final Random random;
    private static final double BASE_WEIGHT = 0.5;
    private static final double COMPLEXITY_FACTOR = 0.1;
    private static final double MAX_TARGET_WEIGHT = 10.0;
    private static final int MAX_TRAINING_SESSIONS = 5;
    private static final double MIN_ERROR_THRESHOLD = 0.1;

    public Trainer(MLModel model1, MLModel model2, RecipeParser recipeParser) {
        this.model1 = model1;
        this.model2 = model2;
        this.recipeParser = recipeParser;
        this.random = new Random();
    }

    private double calculateTargetWeight(Map<String, Integer> baseMaterials, int complexity) {
        // Base weight from number of materials, normalized
        double totalMaterials = baseMaterials.values().stream()
            .mapToDouble(Integer::doubleValue)
            .sum();
        double normalizedMaterials = totalMaterials / (1.0 + totalMaterials); // Normalize to [0,1]
        
        // Calculate base weight with normalization
        double baseWeight = 0.0;
        for (Map.Entry<String, Integer> entry : baseMaterials.entrySet()) {
            String material = entry.getKey();
            int count = entry.getValue();
            
            // Estimate material weight based on its characteristics
            double materialWeight = estimateMaterialWeight(material);
            baseWeight += materialWeight * count;
        }
        
        // Normalize base weight and apply BASE_WEIGHT scaling
        baseWeight = (baseWeight / (1.0 + baseWeight)) * BASE_WEIGHT;
        
        // Add normalized complexity contribution
        double normalizedComplexity = complexity / (1.0 + complexity); // Normalize to [0,1]
        double complexityWeight = normalizedComplexity * COMPLEXITY_FACTOR;
        
        // Combine and clip final target weight
        double targetWeight = baseWeight + complexityWeight;
        return Math.min(targetWeight, MAX_TARGET_WEIGHT);
    }
    
    private double estimateMaterialWeight(String material) {
        // Extract material type from the name
        String[] parts = material.split(":");
        if (parts.length != 2) return 1.0;
        
        String name = parts[1].toLowerCase();
        
        // Precious materials
        if (name.contains("diamond") || name.contains("netherite")) {
            return 8.0;
        }
        if (name.contains("gold") || name.contains("emerald")) {
            return 6.0;
        }
        if (name.contains("iron") || name.contains("copper")) {
            return 4.0;
        }
        
        // Building blocks
        if (name.contains("stone") || name.contains("cobblestone") || name.contains("brick")) {
            return 2.0;
        }
        if (name.contains("wood") || name.contains("planks") || name.contains("log")) {
            return 1.0;
        }
        
        // Light materials
        if (name.contains("string") || name.contains("paper") || name.contains("feather")) {
            return 0.2;
        }
        
        // Crafting materials
        if (name.contains("stick")) {
            return 0.5;
        }
        if (name.contains("ingot")) {
            return 4.0;
        }
        if (name.contains("nugget")) {
            return 0.5;
        }
        
        // Resources
        if (name.contains("coal") || name.contains("redstone")) {
            return 1.0;
        }
        if (name.contains("dust") || name.contains("powder")) {
            return 0.5;
        }
        
        // Default weight for unknown materials
        return 1.0;
    }

    public void train(int epochs, double learningRate) {
        Set<String> items = new HashSet<>(recipeParser.getRecipes());
        if (items.isEmpty()) {
            System.out.println("Warning: No recipes found for training");
            return;
        }
        
        System.out.println("Starting training with " + items.size() + " recipes");
        List<String> itemList = new ArrayList<>(items);
        
        for (int epoch = 0; epoch < epochs; epoch++) {
            Collections.shuffle(itemList, random);
            int validItems = 0;
            double totalError1 = 0.0;
            double totalError2 = 0.0;
            
            for (String item : itemList) {
                try {
                    Map<String, Integer> baseMaterials = recipeParser.resolveBaseMaterials(item);
                    if (baseMaterials.isEmpty()) {
                        continue;
                    }
                    
                    int complexity = recipeParser.getRecipeComplexity(item);
                    double targetWeight = calculateTargetWeight(baseMaterials, complexity);
                    
                    // Train model1
                    double prediction1 = model1.predictWeight(baseMaterials, complexity);
                    if (!Double.isNaN(prediction1)) {
                        double error1 = targetWeight - prediction1;
                        model1.updateWeights(baseMaterials, complexity, error1, learningRate);
                        totalError1 += Math.abs(error1);
                        validItems++;
                    }
                    
                    // Train model2
                    double prediction2 = model2.predictWeight(baseMaterials, complexity);
                    if (!Double.isNaN(prediction2)) {
                        double error2 = targetWeight - prediction2;
                        model2.updateWeights(baseMaterials, complexity, error2, learningRate);
                        totalError2 += Math.abs(error2);
                    }
                    
                } catch (Exception e) {
                    System.err.println("Error training on item " + item + ": " + e.getMessage());
                }
            }
            
            if (validItems == 0) {
                System.out.println("Warning: No valid items in epoch " + epoch);
                continue;
            }
            
            if (epoch % 10 == 0) {
                double avgError1 = totalError1 / validItems;
                double avgError2 = totalError2 / validItems;
                System.out.printf("Epoch %d - Average errors: Gradient Descent=%.4f, Adam=%.4f%n",
                    epoch, avgError1, avgError2);
            }
        }

        // Output final trained weights
        System.out.println("\nFinal Trained Weights:");
        System.out.println("Gradient Descent Model Weights:");
        model1.printWeights();
        System.out.println("\nAdam Model Weights:");
        model2.printWeights();
    }

    public double evaluate() {
        Set<String> items = new HashSet<>(recipeParser.getRecipes());
        if (items.isEmpty()) {
            System.out.println("Warning: No recipes found for evaluation!");
            return Double.NaN;
        }
        
        System.out.println("Starting evaluation with " + items.size() + " recipes");
        double totalError = 0.0;
        int validItems = 0;
        
        for (String item : items) {
            try {
                Map<String, Integer> baseMaterials = recipeParser.resolveBaseMaterials(item);
                if (baseMaterials.isEmpty()) {
                    continue;
                }
                
                int complexity = recipeParser.getRecipeComplexity(item);
                double targetWeight = calculateTargetWeight(baseMaterials, complexity);
                
                double prediction1 = model1.predictWeight(baseMaterials, complexity);
                double prediction2 = model2.predictWeight(baseMaterials, complexity);
                
                if (!Double.isNaN(prediction1) && !Double.isNaN(prediction2)) {
                    double error = Math.abs(targetWeight - prediction1) + Math.abs(targetWeight - prediction2);
                    totalError += error;
                    validItems++;
                }
            } catch (Exception e) {
                System.err.println("Error evaluating item " + item + ": " + e.getMessage());
            }
        }
        
        if (validItems == 0) {
            System.out.println("Warning: No valid items found for evaluation!");
            return Double.NaN;
        }
        
        return totalError / (2.0 * validItems); // Average error across both models
    }

    public void saveTrainedData() throws IOException {
        Set<String> recipeItems = recipeParser.getRecipes();
        if (recipeItems.isEmpty()) {
            System.out.println("Warning: No recipes found to save");
            return;
        }

        // Get mod ID from first item
        String firstItem = recipeItems.iterator().next();
        String modId = firstItem.split(":")[0];
        String outputFile = modId + "_weights.json";

        // Create JSON structure
        JsonObject output = new JsonObject();
        JsonObject baseMaterials = new JsonObject();
        JsonObject items = new JsonObject();

        // Add base material weights
        for (String material : recipeParser.getBaseMaterials()) {
            JsonObject materialData = new JsonObject();
            materialData.addProperty("weight", model1.predictWeight(Map.of(material, 1), 0));
            baseMaterials.add(material, materialData);
        }
        output.add("base_materials", baseMaterials);

        // Add item weights
        for (String item : recipeItems) {
            Map<String, Integer> itemBaseMaterials = recipeParser.resolveBaseMaterials(item);
            if (!itemBaseMaterials.isEmpty()) {
                int complexity = recipeParser.getRecipeComplexity(item);
                double weight = model1.predictWeight(itemBaseMaterials, complexity);
                
                JsonObject itemData = new JsonObject();
                itemData.addProperty("weight", weight);
                items.add(item, itemData);
            }
        }
        output.add("items", items);

        // Write to file with pretty printing
        Gson gson = new Gson();
        String json = gson.toJson(output);
        // Add indentation
        json = json.replace("{\"", "{\n  \"")
                   .replace(",\"", ",\n  \"")
                   .replace("{\"base_materials\":{\"", "{\n  \"base_materials\": {\n    \"")
                   .replace("{\"items\":{\"", "{\n  \"items\": {\n    \"")
                   .replace("},\"", "},\n  \"")
                   .replace("}}", "\n  }\n}");
        Files.writeString(Paths.get(outputFile), json);
        System.out.println("Saved trained data to " + outputFile);
    }

    public void trainMultipleSessions(int epochsPerSession, double learningRate) throws IOException {
        Set<String> items = new HashSet<>(recipeParser.getRecipes());
        if (items.isEmpty()) {
            System.out.println("Warning: No recipes found for training");
            return;
        }

        // Get mod ID for file naming
        String firstItem = items.iterator().next();
        String modId = firstItem.split(":")[0];
        String stateFile = modId + "_model_state.json";

        double bestError = Double.MAX_VALUE;
        int session = 0;
        boolean shouldContinue = true;

        while (shouldContinue && session < MAX_TRAINING_SESSIONS) {
            System.out.printf("\nStarting training session %d/%d%n", session + 1, MAX_TRAINING_SESSIONS);
            
            // Load previous state if it exists
            if (session > 0) {
                loadModelState(stateFile);
            }

            // Train for this session
            train(epochsPerSession, learningRate);
            
            // Evaluate current state
            double currentError = evaluate();
            System.out.printf("Session %d - Average error: %.4f%n", session + 1, currentError);

            // Save current state
            saveModelState(stateFile);

            // Check if we've improved enough
            if (currentError < bestError) {
                bestError = currentError;
                System.out.printf("New best error: %.4f%n", bestError);
                
                // Save best state
                String bestStateFile = modId + "_best_model_state.json";
                saveModelState(bestStateFile);
                
                // Stop if we've reached a good enough error
                if (bestError < MIN_ERROR_THRESHOLD) {
                    System.out.println("Reached target error threshold, stopping training");
                    shouldContinue = false;
                }
            } else {
                System.out.println("No improvement in this session");
            }

            session++;
        }

        // Load best state for final output
        String bestStateFile = modId + "_best_model_state.json";
        if (Files.exists(Paths.get(bestStateFile))) {
            loadModelState(bestStateFile);
        }

        // Save final trained data
        saveTrainedData();
    }

    private void saveModelState(String stateFile) throws IOException {
        JsonObject state = new JsonObject();
        
        // Save model1 state (Gradient Descent)
        JsonObject model1State = new JsonObject();
        model1State.addProperty("complexity_weight", model1.getComplexityWeight());
        state.add("model1", model1State);
        
        // Save model2 state (Adam)
        JsonObject model2State = new JsonObject();
        model2State.addProperty("complexity_weight", model2.getComplexityWeight());
        state.add("model2", model2State);

        // Write to file with pretty printing
        Gson gson = new Gson();
        String json = gson.toJson(state);
        // Add indentation
        json = json.replace("{\"", "{\n  \"")
                   .replace(",\"", ",\n  \"")
                   .replace("}}", "\n  }\n}");
        Files.writeString(Paths.get(stateFile), json);
    }

    private void loadModelState(String stateFile) throws IOException {
        if (!Files.exists(Paths.get(stateFile))) {
            return;
        }

        String content = Files.readString(Paths.get(stateFile));
        Gson gson = new Gson();
        JsonObject state = gson.fromJson(content, JsonObject.class);

        // Load model1 state
        if (state.has("model1")) {
            JsonObject model1State = state.getAsJsonObject("model1");
            if (model1State.has("complexity_weight")) {
                model1.setComplexityWeight(model1State.get("complexity_weight").getAsDouble());
            }
        }

        // Load model2 state
        if (state.has("model2")) {
            JsonObject model2State = state.getAsJsonObject("model2");
            if (model2State.has("complexity_weight")) {
                model2.setComplexityWeight(model2State.get("complexity_weight").getAsDouble());
            }
        }
    }
} 