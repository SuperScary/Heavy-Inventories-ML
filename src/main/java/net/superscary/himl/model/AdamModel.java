package net.superscary.himl.model;

import java.util.HashMap;
import java.util.Map;

public class AdamModel implements MLModel {
    private Map<String, Double> weights;
    private Map<String, Double> momentumMap;
    private Map<String, Double> velocityMap;
    private double learningRate;
    private double complexityWeight;
    private double complexityMomentum;
    private double complexityVelocity;
    private final double beta1;
    private final double beta2;
    private final double epsilon;
    private int timestep;
    private static final double MAX_ERROR = 10.0;
    private static final double EPSILON = 1e-8;

    public AdamModel(double learningRate) {
        this.weights = new HashMap<>();
        this.momentumMap = new HashMap<>();
        this.velocityMap = new HashMap<>();
        this.learningRate = learningRate;
        this.complexityWeight = 1.0;
        this.complexityMomentum = 0.0;
        this.complexityVelocity = 0.0;
        this.beta1 = 0.9;
        this.beta2 = 0.999;
        this.epsilon = 1e-8;
        this.timestep = 0;
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

        timestep++;
        
        // Update weights for base materials
        for (Map.Entry<String, Integer> entry : baseMaterials.entrySet()) {
            String material = entry.getKey();
            int count = entry.getValue();
            
            // Initialize momentum and velocity if not present
            if (!momentumMap.containsKey(material)) {
                momentumMap.put(material, 0.0);
                velocityMap.put(material, 0.0);
            }
            
            double currentWeight = weights.getOrDefault(material, 1.0);
            double gradient = error * count;
            
            // Update momentum and velocity
            double momentum = beta1 * momentumMap.get(material) + (1 - beta1) * gradient;
            double velocity = beta2 * velocityMap.get(material) + (1 - beta2) * gradient * gradient;
            
            momentumMap.put(material, momentum);
            velocityMap.put(material, velocity);
            
            // Bias correction
            double momentumCorrected = momentum / (1 - Math.pow(beta1, timestep));
            double velocityCorrected = velocity / (1 - Math.pow(beta2, timestep));
            
            // Update weight using instance learningRate
            double update = this.learningRate * momentumCorrected / (Math.sqrt(velocityCorrected) + this.epsilon);
            double newWeight = currentWeight + update;
            
            // Ensure weights stay positive and reasonable
            newWeight = Math.max(0.1, Math.min(newWeight, 100.0));
            weights.put(material, newWeight);
        }
        
        // Update complexity weight
        double complexityGradient = error * complexity;
        
        // Update momentum and velocity for complexity
        complexityMomentum = beta1 * complexityMomentum + (1 - beta1) * complexityGradient;
        complexityVelocity = beta2 * complexityVelocity + (1 - beta2) * complexityGradient * complexityGradient;
        
        // Bias correction
        double momentumCorrected = complexityMomentum / (1 - Math.pow(beta1, timestep));
        double velocityCorrected = complexityVelocity / (1 - Math.pow(beta2, timestep));
        
        // Update complexity weight using instance learningRate
        double update = this.learningRate * momentumCorrected / (Math.sqrt(velocityCorrected) + this.epsilon);
        double newComplexityWeight = complexityWeight + update;
        
        // Ensure complexity weight stays positive and reasonable
        complexityWeight = Math.max(0.1, Math.min(newComplexityWeight, 10.0));
    }

    @Override
    public void printWeights() {
        System.out.println("Material Weights:");
        weights.entrySet().stream()
            .sorted(Map.Entry.comparingByValue((a, b) -> Double.compare(b, a)))  // Sort by weight descending
            .forEach(entry -> {
                String material = entry.getKey();
                double weight = entry.getValue();
                double momentum = momentumMap.getOrDefault(material, 0.0);
                double velocity = velocityMap.getOrDefault(material, 0.0);
                System.out.printf("  %s: weight=%.4f, momentum=%.4f, velocity=%.4f%n",
                    material, weight, momentum, velocity);
            });
        System.out.printf("Complexity Weight: %.4f (momentum=%.4f, velocity=%.4f)%n",
            complexityWeight, complexityMomentum, complexityVelocity);
    }

    public double getComplexityWeight() {
        return complexityWeight;
    }

    @Override
    public void setComplexityWeight(double weight) {
        this.complexityWeight = Math.max(0.1, Math.min(weight, 10.0));
    }
} 