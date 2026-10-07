package com.example.packageinspection.ml.scratch;

import java.util.Random;

/**
 * Fully connected affine transform: output = weights * input + biases.
 */
final class DenseLayer {

    private final int inputSize;
    private final int outputSize;
    private final float[][] weights;
    private final float[] biases;

    DenseLayer(int inputSize, int outputSize, Random random) {
        this.inputSize = inputSize;
        this.outputSize = outputSize;
        this.weights = new float[outputSize][inputSize];
        this.biases = new float[outputSize];

        // He initialization uses variance 2/fan-in; biases start at zero.
        float standardDeviation = (float) Math.sqrt(2.0 / inputSize);
        for (int out = 0; out < outputSize; out++) {
            for (int in = 0; in < inputSize; in++) {
                weights[out][in] = (float) random.nextGaussian() * standardDeviation;
            }
        }
    }

    float[] forward(float[] input) {
        if (input == null || input.length != inputSize) {
            throw new IllegalArgumentException("Dense input must contain " + inputSize + " values.");
        }
        float[] output = new float[outputSize];
        for (int out = 0; out < outputSize; out++) {
            float sum = biases[out];
            for (int in = 0; in < inputSize; in++) {
                sum += weights[out][in] * input[in];
            }
            output[out] = sum;
        }
        return output;
    }

    BackwardResult backward(float[] input, float[] outputGradient) {
        if (input == null || input.length != inputSize || outputGradient == null
                || outputGradient.length != outputSize) {
            throw new IllegalArgumentException("Dense backward tensors have an unexpected length.");
        }

        float[] inputGradient = new float[inputSize];
        Gradients gradients = new Gradients(outputSize, inputSize);
        for (int out = 0; out < outputSize; out++) {
            float delta = outputGradient[out];
            gradients.biases[out] = delta;
            for (int in = 0; in < inputSize; in++) {
                gradients.weights[out][in] = delta * input[in];
                inputGradient[in] += weights[out][in] * delta;
            }
        }
        return new BackwardResult(inputGradient, gradients);
    }

    void applyGradients(Gradients gradients, float scale, float learningRate) {
        for (int out = 0; out < outputSize; out++) {
            biases[out] -= learningRate * gradients.biases[out] * scale;
            for (int in = 0; in < inputSize; in++) {
                weights[out][in] -= learningRate * gradients.weights[out][in] * scale;
            }
        }
    }

    Gradients newGradients() {
        return new Gradients(outputSize, inputSize);
    }

    static void add(Gradients destination, Gradients source) {
        for (int out = 0; out < destination.weights.length; out++) {
            destination.biases[out] += source.biases[out];
            for (int in = 0; in < destination.weights[out].length; in++) {
                destination.weights[out][in] += source.weights[out][in];
            }
        }
    }

    float weight(int output, int input) {
        return weights[output][input];
    }

    void setWeight(int output, int input, float value) {
        weights[output][input] = value;
    }

    static final class Gradients {
        final float[][] weights;
        final float[] biases;

        private Gradients(int outputSize, int inputSize) {
            weights = new float[outputSize][inputSize];
            biases = new float[outputSize];
        }
    }

    record BackwardResult(float[] inputGradient, Gradients gradients) {
    }
}
