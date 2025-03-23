# Minecraft Recipe ML System

A Java-based machine learning system that analyzes Minecraft crafting recipes to learn weights for base materials and predict item weights.

## Features

- **Recipe Analysis**: Parses Minecraft crafting recipe JSONs to build a dependency graph of items
- **Base Material Resolution**: Recursively resolves complex recipes into their base materials
- **Weight Learning**: Uses gradient descent to learn weights for base materials
- **Weight Prediction**: Predicts item weights based on learned base material weights

## Project Structure

```
src/main/java/com/minecraft/ml/
├── Main.java                 # Main entry point
├── model/
│   └── ItemWeightModel.java  # Handles weight learning and prediction
├── parser/
│   └── RecipeParser.java     # Parses and analyzes Minecraft recipes
└── training/
    └── Trainer.java         # Handles the training process
```

## Requirements

- Java 11 or higher
- Gradle
- Minecraft recipe JSON files (from `data/minecraft/recipes/`)

## Building

```bash
gradle build
```

## Running

```bash
gradle run
```

## How It Works

1. The system loads Minecraft recipe JSONs from the specified directory
2. It builds a dependency graph of all items and their crafting requirements
3. For each item, it can resolve its recipe into base materials (materials that aren't crafted)
4. Using training data (items with known weights), it learns weights for base materials
5. Once trained, it can predict weights for any item based on its base materials

## Training Data

The system requires training data in the form of item weights. These can be added using the `Trainer.addTrainingExample()` method:

```java
trainer.addTrainingExample("minecraft:iron_sword", 5.0);
trainer.addTrainingExample("minecraft:iron_pickaxe", 7.0);
```

## Output

The system provides:
- Base materials for each item
- Learned weights for base materials
- Predicted vs actual weights for training items
- Error metrics during training

## Example Output

```
Starting training...
Epoch 1: Average Error = 2.3456
Epoch 2: Average Error = 1.8765
...

Final Evaluation:
-----------------
Item: minecraft:iron_sword
  Base Materials: {minecraft:iron_ingot=2, minecraft:stick=1}
  Actual Weight: 5.00
  Predicted Weight: 4.85
  Error: 0.15

Learned Base Material Weights:
-----------------------------
minecraft:iron_ingot: 2.3456
minecraft:stick: 0.8765
```

## Contributing

Feel free to submit issues and enhancement requests! 