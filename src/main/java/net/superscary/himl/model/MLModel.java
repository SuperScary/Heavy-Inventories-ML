package net.superscary.himl.model;

import java.util.Map;

public interface MLModel {
    /**
     * Initialize or update the model's parameters
     * @param material The base material to initialize
     */
    void initializeBaseMaterial(String material);

    /**
     * Predict the weight of an item based on its base materials and recipe complexity
     * @param baseMaterials Map of base materials to their counts
     * @param recipeComplexity The complexity of the crafting recipe (number of steps)
     * @return The predicted weight
     */
    double predictWeight(Map<String, Integer> baseMaterials, int recipeComplexity);

    /**
     * Updates the model weights based on the error between predicted and target values.
     * @param baseMaterials Map of base materials and their counts
     * @param complexity Recipe complexity
     * @param error Error between predicted and target values
     * @param learningRate Learning rate for weight updates
     */
    void updateWeights(Map<String, Integer> baseMaterials, int complexity, double error, double learningRate);

    /**
     * Print the current weights of the model in a human-readable format
     */
    void printWeights();

    /**
     * Get the current complexity weight of the model
     * @return The complexity weight
     */
    double getComplexityWeight();

    /**
     * Set the complexity weight of the model
     * @param weight The new complexity weight
     */
    void setComplexityWeight(double weight);
} 