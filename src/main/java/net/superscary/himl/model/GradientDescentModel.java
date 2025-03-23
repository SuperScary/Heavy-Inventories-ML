package net.superscary.himl.model;

import java.util.HashMap;
import java.util.Map;

public class GradientDescentModel implements MLModel {
    private Map<String, Double> weights;
    private double complexityWeight;
    private final double learningRate;
    private static final double EPSILON = 1e-8;
    private static final double MAX_ERROR = 10.0;

    public GradientDescentModel(double learningRate) {
        this.weights = new HashMap<>();
        this.complexityWeight = 1.0;
        this.learningRate = learningRate;
    }

    @Override
    public void initializeBaseMaterial(String material) {
        weights.putIfAbsent(material, 1.0);
    }

    @Override
    public double predictWeight(Map<String, Integer> baseMaterials, int complexity) {
        double totalWeight = 0.0;
        for (Map.Entry<String, Integer> entry : baseMaterials.entrySet()) {
            String material = entry.getKey();
            int count = entry.getValue();
            double weight = weights.getOrDefault(material, 1.0);
            totalWeight += weight * count;
        }
        return totalWeight * (1.0 + complexityWeight * complexity);
    }

    @Override
    public void updateWeights(Map<String, Integer> baseMaterials, int complexity, double error, double learningRate) {
        // Validate error is within acceptable range
        if (Math.abs(error) > MAX_ERROR) {
            error = Math.signum(error) * MAX_ERROR;
        }
        
        // Skip update if error is too small
        if (Math.abs(error) < EPSILON) {
            return;
        }

        // Update weights for base materials
        for (Map.Entry<String, Integer> entry : baseMaterials.entrySet()) {
            String material = entry.getKey();
            int count = entry.getValue();
            
            double currentWeight = weights.getOrDefault(material, 1.0);
            double gradient = error * count;
            double newWeight = currentWeight + this.learningRate * gradient;
            
            // Ensure weights stay positive and reasonable
            newWeight = Math.max(0.1, Math.min(newWeight, 100.0));
            weights.put(material, newWeight);
        }
        
        // Update complexity weight
        double complexityGradient = error * complexity;
        double newComplexityWeight = complexityWeight + this.learningRate * complexityGradient;
        
        // Ensure complexity weight stays positive and reasonable
        complexityWeight = Math.max(0.1, Math.min(newComplexityWeight, 10.0));
    }

    @Override
    public void printWeights() {
        System.out.println("Material Weights:");
        weights.entrySet().stream()
            .sorted(Map.Entry.comparingByValue((a, b) -> Double.compare(b, a)))  // Sort by weight descending
            .forEach(entry -> System.out.printf("  %s: %.4f%n", entry.getKey(), entry.getValue()));
        System.out.printf("Complexity Weight: %.4f%n", complexityWeight);
    }

    public double getComplexityWeight() {
        return complexityWeight;
    }

    @Override
    public void setComplexityWeight(double weight) {
        this.complexityWeight = Math.max(0.1, Math.min(weight, 10.0));
    }
} 