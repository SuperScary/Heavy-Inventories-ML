package net.superscary.himl.model;

public class MLModelFactory {
    public static MLModel createModel(String type, double learningRate) {
        switch (type.toLowerCase()) {
            case "gradient":
                return new GradientDescentModel(learningRate);
            case "adam":
                return new AdamModel(learningRate);
            default:
                throw new IllegalArgumentException("Unknown model type: " + type);
        }
    }
} 