# AI Warehouse Package Quality Inspection System

A Spring Boot application for package inspection, damage classification, and incident tracking. The repository includes both the existing DJL-based application classifier and a separate educational CNN implemented from scratch with Java arrays and standard Java operations.

## Current status of the from-scratch CNN

The CNN model and its training mathematics are implemented in:

```text
src/main/java/com/example/packageinspection/ml/scratch/
```

The model supports a forward pass, five-class prediction, cross-entropy loss, backpropagation, and averaged mini-batch gradient-descent updates.

**There is not yet a complete image-training application.** In particular, this repository does not yet include:

- A dataset/image loader or image-to-`[1][32][32]` preprocessing.
- A ready-to-run training `main` class, epoch/validation/test runner, or CLI.
- Saving/loading learned CNN weights.
- Spring Boot integration for this CNN.

Consequently, there is not yet a command that trains this model from a folder of package images. The existing Spring Boot upload path still uses the existing DJL classifier (or its mock fallback); it does not use the from-scratch model.

## From-scratch CNN architecture

Input arrays use channel-first layout: `[channels][height][width]`. Images supplied directly to the model must already be grayscale, 32×32, and represented as finite `float` values.

| Stage | Operation | Output shape |
|---|---|---:|
| Input | Grayscale image | `[1][32][32]` |
| Conv 1 | 8 filters, 3×3, stride 1, valid | `[8][30][30]` |
| ReLU 1 | Element-wise `max(0, x)` | `[8][30][30]` |
| Max pool 1 | 2×2, stride 2 | `[8][15][15]` |
| Conv 2 | 16 filters, 3×3, stride 1, valid | `[16][13][13]` |
| ReLU 2 | Element-wise `max(0, x)` | `[16][13][13]` |
| Max pool 2 | 2×2, stride 2 | `[16][6][6]` |
| Flatten | Channel-major order | `576` values |
| Dense | 576 inputs, 5 logits | `[5]` |
| Softmax | Stable normalized probabilities | `[5]` |

The class indices are fixed:

| Index | Label |
|---:|---|
| 0 | `DENT` |
| 1 | `TEAR` |
| 2 | `PUNCTURE` |
| 3 | `CRUSHED` |
| 4 | `NO_DAMAGE` |

Weights use He initialization and biases start at zero. Softmax subtracts the maximum logit for numerical stability. Backpropagation includes the convolution, ReLU, max-pooling argmax routes, flatten, and dense operations.

## Using the model API today

`PackageDamageCnn` is a model class, not a standalone program. Construct it and call `predict` or `trainBatch` from Java code. It does not read image files: the caller must supply an image tensor in the required format.

```java
import com.example.packageinspection.ml.scratch.PackageDamageCnn;

import java.util.List;

class ScratchCnnExample {
    void useModel(float[][][] image32x32) {
        // A seed makes initial weights reproducible for experiments and tests.
        PackageDamageCnn model = new PackageDamageCnn(42L);

        // Prediction includes the label, winning probability, and all five probabilities.
        PackageDamageCnn.Prediction prediction = model.predict(image32x32);
        System.out.println(prediction.predictedClass());
        System.out.println(prediction.confidence());
        System.out.println(java.util.Arrays.toString(prediction.probabilities()));

        // Labels use the fixed class indices listed above.
        int correctLabel = 0; // DENT
        var example = new PackageDamageCnn.TrainingExample(image32x32, correctLabel);

        // This performs forward propagation, cross-entropy/backpropagation, and one SGD update.
        PackageDamageCnn.BatchMetrics metrics =
                model.trainBatch(List.of(example), 0.001f);
        System.out.println("Batch loss: " + metrics.meanCrossEntropy());
        System.out.println("Batch accuracy: " + metrics.accuracy());
    }
}
```

`image32x32` must have shape `[1][32][32]`, with finite pixel values. A typical future preprocessing pipeline would decode an image with `ImageIO`, resize it to 32×32, convert RGB to grayscale, and scale intensities to `[0,1]`. That preprocessing is **not currently included**, so the example expects the tensor to be provided by its caller.

## How model training will work once a dataset is available

The training runner should be a separate Java class in the scratch-CNN package, for example:

```text
src/main/java/com/example/packageinspection/ml/scratch/PackageDamageCnnTrainer.java
```

That trainer does not exist yet. Once implemented alongside an image preprocessor and dataset loader, it can:

1. Read labeled training and validation images from separate dataset splits.
2. Convert each image to a `[1][32][32]` float tensor and each label to its class index.
3. Shuffle training examples and divide them into batches.
4. Call `model.trainBatch(batch, learningRate)` for each training batch in each epoch.
5. Aggregate and report training loss/accuracy, then measure validation accuracy without updating weights.
6. Save the resulting model parameters for later inference (model persistence is also not implemented yet).

The planned dataset layout is:

```text
dataset/
├── train/
│   ├── DENT/
│   ├── TEAR/
│   ├── PUNCTURE/
│   ├── CRUSHED/
│   └── NO_DAMAGE/
├── validation/
│   ├── DENT/
│   ├── TEAR/
│   ├── PUNCTURE/
│   ├── CRUSHED/
│   └── NO_DAMAGE/
└── test/
    ├── DENT/
    ├── TEAR/
    ├── PUNCTURE/
    ├── CRUSHED/
    └── NO_DAMAGE/
```

Keep the test split separate from training and validation. **No real dataset is currently present, and no real-data training or accuracy results are claimed.**

## Build and test

Requirements: JDK 17 or later and Maven 3.8 or later.

```powershell
# Compile the project
mvn compile

# Run all project tests
mvn test

# Run only the from-scratch CNN mathematics tests
mvn "-Dtest=PackageDamageCnnTest,Conv2DLayerTest,DenseLayerTest,ActivationAndPoolingTest,SoftmaxCrossEntropyTest" test
```

The CNN's focused tests cover operation shapes, finite-difference gradient checks, stable softmax/cross-entropy, prediction outputs, and mini-batch updates. They use deterministic numeric tensors, not package-photo data.

## Existing Spring Boot application

The Spring Boot application is separate from the educational scratch CNN:

- `POST /api/inspections` stores an uploaded image, classifies it using the existing `ImageClassificationService`, and records the inspection.
- `DJLImageClassificationService` currently handles that interface and attempts to load its configured DJL/PyTorch model. If it cannot load the model, it falls back to `MockImageClassificationService`.
- The scratch CNN is not selected by the application and does not yet load image files or saved weights.

Configure the existing database and application settings as needed, then run:

```powershell
mvn spring-boot:run
```

The dashboard is served at `http://localhost:8080`.

## Technology

- Java 17+
- Spring Boot 3.2.3, Spring Web, Spring Data JPA, and Bean Validation
- PostgreSQL for the configured application profile; H2 for tests
- Flyway database migrations
- DJL/PyTorch for the existing application inference path only
- JUnit 5 and Mockito

