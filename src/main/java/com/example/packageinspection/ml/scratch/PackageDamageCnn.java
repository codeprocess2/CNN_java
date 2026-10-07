package com.example.packageinspection.ml.scratch;

import java.util.List;
import java.util.Random;

/**
 * A small, trainable five-class CNN implemented only with Java arrays and scalar operations.
 * Tensor values use the channel-first layout {@code [channels][height][width]}.
 */
public final class PackageDamageCnn {

    public static final int INPUT_CHANNELS = 1;
    public static final int INPUT_HEIGHT = 32;
    public static final int INPUT_WIDTH = 32;
    public static final int CLASS_COUNT = 5;
    public static final int FLATTENED_SIZE = 16 * 6 * 6;

    private static final List<String> CLASS_LABELS = List.of(
            "DENT", "TEAR", "PUNCTURE", "CRUSHED", "NO_DAMAGE");

    private final Conv2DLayer conv1;
    private final MaxPool2D pool1;
    private final Conv2DLayer conv2;
    private final MaxPool2D pool2;
    private final DenseLayer dense;

    public PackageDamageCnn() {
        this(new Random());
    }

    public PackageDamageCnn(long seed) {
        this(new Random(seed));
    }

    private PackageDamageCnn(Random random) {
        conv1 = new Conv2DLayer(1, 8, 3, random);
        pool1 = new MaxPool2D(2, 2);
        conv2 = new Conv2DLayer(8, 16, 3, random);
        pool2 = new MaxPool2D(2, 2);
        dense = new DenseLayer(FLATTENED_SIZE, CLASS_COUNT, random);
    }

    public static List<String> classLabels() {
        return CLASS_LABELS;
    }

    public Prediction predict(float[][][] input) {
        ForwardPass pass = forward(input);
        float[] probabilities = SoftmaxCrossEntropy.softmax(pass.logits);
        int predictedClass = argmax(probabilities);
        return new Prediction(
                predictedClass,
                CLASS_LABELS.get(predictedClass),
                probabilities[predictedClass],
                probabilities);
    }

    /**
     * Applies one averaged mini-batch SGD update. Loss and accuracy describe the batch
     * immediately before that update.
     */
    public BatchMetrics trainBatch(List<TrainingExample> batch, float learningRate) {
        if (batch == null || batch.isEmpty()) {
            throw new IllegalArgumentException("Training batch must contain at least one example.");
        }
        if (!Float.isFinite(learningRate) || learningRate <= 0.0f) {
            throw new IllegalArgumentException("Learning rate must be finite and greater than zero.");
        }

        DenseLayer.Gradients denseGradients = dense.newGradients();
        Conv2DLayer.Gradients conv1Gradients = conv1.newGradients();
        Conv2DLayer.Gradients conv2Gradients = conv2.newGradients();
        double totalLoss = 0.0;
        int correct = 0;

        for (TrainingExample example : batch) {
            if (example == null) {
                throw new IllegalArgumentException("Training batch cannot contain null examples.");
            }
            validateInput(example.image());
            if (example.label() < 0 || example.label() >= CLASS_COUNT) {
                throw new IllegalArgumentException("Class index must be between 0 and " + (CLASS_COUNT - 1) + ".");
            }

            ForwardPass pass = forward(example.image());
            SoftmaxCrossEntropy.Result lossAndGradient =
                    SoftmaxCrossEntropy.evaluate(pass.logits, example.label());
            totalLoss += lossAndGradient.loss();
            if (argmax(lossAndGradient.probabilities()) == example.label()) {
                correct++;
            }

            DenseLayer.BackwardResult denseBackward = dense.backward(
                    pass.flattened, lossAndGradient.logitsGradient());
            DenseLayer.add(denseGradients, denseBackward.gradients());

            float[][][] pool2Gradient = unflatten(
                    denseBackward.inputGradient(), 16, 6, 6);
            float[][][] relu2Gradient = pool2.backward(pass.pool2Result, pool2Gradient);
            float[][][] conv2Gradient = Relu.backward(pass.conv2PreActivation, relu2Gradient);
            Conv2DLayer.BackwardResult conv2Backward =
                    conv2.backward(pass.pool1Result.values(), conv2Gradient);
            Conv2DLayer.add(conv2Gradients, conv2Backward.gradients());

            float[][][] pool1Gradient = pool1.backward(pass.pool1Result, conv2Backward.inputGradient());
            float[][][] relu1Gradient = Relu.backward(pass.conv1PreActivation, pool1Gradient);
            Conv2DLayer.BackwardResult conv1Backward = conv1.backward(example.image(), relu1Gradient);
            Conv2DLayer.add(conv1Gradients, conv1Backward.gradients());
        }

        // Average the accumulated mini-batch gradients before applying SGD.
        float gradientScale = 1.0f / batch.size();
        dense.applyGradients(denseGradients, gradientScale, learningRate);
        conv2.applyGradients(conv2Gradients, gradientScale, learningRate);
        conv1.applyGradients(conv1Gradients, gradientScale, learningRate);

        return new BatchMetrics(
                (float) (totalLoss / batch.size()),
                (float) correct / batch.size(),
                batch.size());
    }

    private ForwardPass forward(float[][][] input) {
        validateInput(input);

        // Valid 3x3 convolution: [1,32,32] -> [8,30,30].
        float[][][] conv1PreActivation = conv1.forward(input);
        float[][][] conv1Activated = Relu.forward(conv1PreActivation);
        MaxPool2D.Result pool1Result = pool1.forward(conv1Activated);

        // Second valid convolution and pooling: [8,15,15] -> [16,13,13] -> [16,6,6].
        float[][][] conv2PreActivation = conv2.forward(pool1Result.values());
        float[][][] conv2Activated = Relu.forward(conv2PreActivation);
        MaxPool2D.Result pool2Result = pool2.forward(conv2Activated);
        float[] flattened = flatten(pool2Result.values());

        // Dense outputs are logits; softmax converts them into class probabilities.
        float[] logits = dense.forward(flattened);
        return new ForwardPass(
                conv1PreActivation, pool1Result, conv2PreActivation, pool2Result, flattened, logits);
    }

    private static float[] flatten(float[][][] tensor) {
        int channels = tensor.length;
        int height = tensor[0].length;
        int width = tensor[0][0].length;
        float[] result = new float[channels * height * width];
        int index = 0;
        for (int channel = 0; channel < channels; channel++) {
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    result[index++] = tensor[channel][y][x];
                }
            }
        }
        return result;
    }

    private static float[][][] unflatten(float[] values, int channels, int height, int width) {
        if (values.length != channels * height * width) {
            throw new IllegalArgumentException("Flattened gradient has an unexpected length.");
        }
        float[][][] tensor = new float[channels][height][width];
        int index = 0;
        for (int channel = 0; channel < channels; channel++) {
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    tensor[channel][y][x] = values[index++];
                }
            }
        }
        return tensor;
    }

    private static int argmax(float[] values) {
        int bestIndex = 0;
        for (int i = 1; i < values.length; i++) {
            if (values[i] > values[bestIndex]) {
                bestIndex = i;
            }
        }
        return bestIndex;
    }

    private static void validateInput(float[][][] input) {
        if (input == null || input.length != INPUT_CHANNELS
                || input[0] == null || input[0].length != INPUT_HEIGHT) {
            throw new IllegalArgumentException("Input must have shape [1][32][32].");
        }
        for (float[] row : input[0]) {
            if (row == null || row.length != INPUT_WIDTH) {
                throw new IllegalArgumentException("Input must have shape [1][32][32].");
            }
            for (float value : row) {
                if (!Float.isFinite(value)) {
                    throw new IllegalArgumentException("Input values must all be finite.");
                }
            }
        }
    }

    public record TrainingExample(float[][][] image, int label) {
    }

    public record BatchMetrics(float meanCrossEntropy, float accuracy, int sampleCount) {
    }

    public static final class Prediction {
        private final int classIndex;
        private final String predictedClass;
        private final float confidence;
        private final float[] probabilities;

        private Prediction(int classIndex, String predictedClass, float confidence, float[] probabilities) {
            this.classIndex = classIndex;
            this.predictedClass = predictedClass;
            this.confidence = confidence;
            this.probabilities = probabilities.clone();
        }

        public int classIndex() {
            return classIndex;
        }

        public String predictedClass() {
            return predictedClass;
        }

        public float confidence() {
            return confidence;
        }

        public float[] probabilities() {
            return probabilities.clone();
        }
    }

    private record ForwardPass(
            float[][][] conv1PreActivation,
            MaxPool2D.Result pool1Result,
            float[][][] conv2PreActivation,
            MaxPool2D.Result pool2Result,
            float[] flattened,
            float[] logits) {
    }
}
