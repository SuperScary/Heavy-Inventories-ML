package net.superscary.himl.model;

import java.util.HashMap;
import java.util.Map;

public class ItemWeightModel {
    private final Map<String, Double> baseMaterialWeights;
    private final double learningRate;

    public ItemWeightModel(double learningRate) {
        this.baseMaterialWeights = new HashMap<>();
        this.learningRate = learningRate;
    }

    public void initializeBaseMaterial(String material) {
        if (!baseMaterialWeights.containsKey(material)) {
            baseMaterialWeights.put(material, 1.0); // Initialize with default weight
        }
    }

    public double getBaseMaterialWeight(String material) {
        return baseMaterialWeights.getOrDefault(material, 1.0);
    }

    public void updateWeights(Map<String, Integer> baseMaterials, double actualWeight, double predictedWeight) {
        double error = actualWeight - predictedWeight;
        
        for (Map.Entry<String, Integer> entry : baseMaterials.entrySet()) {
            String material = entry.getKey();
            int count = entry.getValue();
            
            double currentWeight = getBaseMaterialWeight(material);
            double gradient = error * count;
            double newWeight = currentWeight + learningRate * gradient;
            
            baseMaterialWeights.put(material, newWeight);
        }
    }

    public double predictWeight(Map<String, Integer> baseMaterials) {
        double totalWeight = 0.0;
        for (Map.Entry<String, Integer> entry : baseMaterials.entrySet()) {
            String material = entry.getKey();
            int count = entry.getValue();
            totalWeight += getBaseMaterialWeight(material) * count;
        }
        return totalWeight;
    }

    public Map<String, Double> getBaseMaterialWeights() {
        return new HashMap<>(baseMaterialWeights);
    }
} 