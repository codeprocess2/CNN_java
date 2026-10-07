package com.example.packageinspection.ml.scratch;

import java.util.Random;

/**
 * Valid, stride-one cross-correlation layer. Each output position sums the
 * matching input window against a filter; backward computes derivatives for
 * both the filters and input using the chain rule.
 */
final class Conv2DLayer {

    private final int inputChannels;
    private final int outputChannels;
    private final int kernelSize;
    private final float[][][][] weights;
    private final float[] biases;

    Conv2DLayer(int inputChannels, int outputChannels, int kernelSize, Random random) {
        this.inputChannels = inputChannels;
        this.outputChannels = outputChannels;
        this.kernelSize = kernelSize;
        this.weights = new float[outputChannels][inputChannels][kernelSize][kernelSize];
        this.biases = new float[outputChannels];

        // He initialization uses variance 2/fan-in to keep ReLU activations scaled.
        float standardDeviation = (float) Math.sqrt(2.0 / (inputChannels * kernelSize * kernelSize));
        for (int out = 0; out < outputChannels; out++) {
            for (int in = 0; in < inputChannels; in++) {
                for (int y = 0; y < kernelSize; y++) {
                    for (int x = 0; x < kernelSize; x++) {
                        weights[out][in][y][x] = (float) random.nextGaussian() * standardDeviation;
                    }
                }
            }
        }
    }

    float[][][] forward(float[][][] input) {
        validateTensor(input, inputChannels, "input");
        int outputHeight = input[0].length - kernelSize + 1;
        int outputWidth = input[0][0].length - kernelSize + 1;
        if (outputHeight <= 0 || outputWidth <= 0) {
            throw new IllegalArgumentException("Input dimensions must be at least the convolution kernel size.");
        }

        float[][][] output = new float[outputChannels][outputHeight][outputWidth];
        for (int out = 0; out < outputChannels; out++) {
            for (int y = 0; y < outputHeight; y++) {
                for (int x = 0; x < outputWidth; x++) {
                    float sum = biases[out];
                    for (int in = 0; in < inputChannels; in++) {
                        for (int ky = 0; ky < kernelSize; ky++) {
                            for (int kx = 0; kx < kernelSize; kx++) {
                                sum += input[in][y + ky][x + kx] * weights[out][in][ky][kx];
                            }
                        }
                    }
                    output[out][y][x] = sum;
                }
            }
        }
        return output;
    }

    BackwardResult backward(float[][][] input, float[][][] outputGradient) {
        validateTensor(input, inputChannels, "input");
        int outputHeight = input[0].length - kernelSize + 1;
        int outputWidth = input[0][0].length - kernelSize + 1;
        validateShape(outputGradient, outputChannels, outputHeight, outputWidth, "outputGradient");

        float[][][] inputGradient = new float[inputChannels][input[0].length][input[0][0].length];
        Gradients gradients = new Gradients(outputChannels, inputChannels, kernelSize);

        for (int out = 0; out < outputChannels; out++) {
            for (int y = 0; y < outputHeight; y++) {
                for (int x = 0; x < outputWidth; x++) {
                    float delta = outputGradient[out][y][x];
                    gradients.biases[out] += delta;
                    for (int in = 0; in < inputChannels; in++) {
                        for (int ky = 0; ky < kernelSize; ky++) {
                            for (int kx = 0; kx < kernelSize; kx++) {
                                gradients.weights[out][in][ky][kx] += input[in][y + ky][x + kx] * delta;
                                inputGradient[in][y + ky][x + kx] += weights[out][in][ky][kx] * delta;
                            }
                        }
                    }
                }
            }
        }
        return new BackwardResult(inputGradient, gradients);
    }

    void applyGradients(Gradients gradients, float scale, float learningRate) {
        for (int out = 0; out < outputChannels; out++) {
            biases[out] -= learningRate * gradients.biases[out] * scale;
            for (int in = 0; in < inputChannels; in++) {
                for (int y = 0; y < kernelSize; y++) {
                    for (int x = 0; x < kernelSize; x++) {
                        weights[out][in][y][x] -= learningRate * gradients.weights[out][in][y][x] * scale;
                    }
                }
            }
        }
    }

    Gradients newGradients() {
        return new Gradients(outputChannels, inputChannels, kernelSize);
    }

    static void add(Gradients destination, Gradients source) {
        for (int out = 0; out < destination.weights.length; out++) {
            destination.biases[out] += source.biases[out];
            for (int in = 0; in < destination.weights[out].length; in++) {
                for (int y = 0; y < destination.weights[out][in].length; y++) {
                    for (int x = 0; x < destination.weights[out][in][y].length; x++) {
                        destination.weights[out][in][y][x] += source.weights[out][in][y][x];
                    }
                }
            }
        }
    }

    float weight(int outputChannel, int inputChannel, int y, int x) {
        return weights[outputChannel][inputChannel][y][x];
    }

    void setWeight(int outputChannel, int inputChannel, int y, int x, float value) {
        weights[outputChannel][inputChannel][y][x] = value;
    }

    private static void validateTensor(float[][][] tensor, int expectedChannels, String name) {
        if (tensor == null || tensor.length != expectedChannels || tensor.length == 0
                || tensor[0] == null || tensor[0].length == 0 || tensor[0][0] == null
                || tensor[0][0].length == 0) {
            throw new IllegalArgumentException(name + " must be a non-empty [channels][height][width] tensor.");
        }
        int height = tensor[0].length;
        int width = tensor[0][0].length;
        for (float[][] channel : tensor) {
            if (channel == null || channel.length != height) {
                throw new IllegalArgumentException(name + " has inconsistent channel heights.");
            }
            for (float[] row : channel) {
                if (row == null || row.length != width) {
                    throw new IllegalArgumentException(name + " has inconsistent row widths.");
                }
            }
        }
    }

    private static void validateShape(
            float[][][] tensor, int channels, int height, int width, String name) {
        validateTensor(tensor, channels, name);
        if (tensor[0].length != height || tensor[0][0].length != width) {
            throw new IllegalArgumentException(name + " has an unexpected spatial shape.");
        }
    }

    static final class Gradients {
        final float[][][][] weights;
        final float[] biases;

        private Gradients(int outputChannels, int inputChannels, int kernelSize) {
            weights = new float[outputChannels][inputChannels][kernelSize][kernelSize];
            biases = new float[outputChannels];
        }
    }

    record BackwardResult(float[][][] inputGradient, Gradients gradients) {
    }
}
