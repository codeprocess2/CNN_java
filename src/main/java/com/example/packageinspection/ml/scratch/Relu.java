package com.example.packageinspection.ml.scratch;

/**
 * Elementwise max(0, x); its derivative is 1 for positive inputs and 0 otherwise.
 */
final class Relu {

    private Relu() {
    }

    static float[][][] forward(float[][][] input) {
        float[][][] output = new float[input.length][input[0].length][input[0][0].length];
        for (int channel = 0; channel < input.length; channel++) {
            for (int y = 0; y < input[channel].length; y++) {
                for (int x = 0; x < input[channel][y].length; x++) {
                    output[channel][y][x] = Math.max(0.0f, input[channel][y][x]);
                }
            }
        }
        return output;
    }

    static float[][][] backward(float[][][] preActivation, float[][][] outputGradient) {
        if (preActivation.length != outputGradient.length
                || preActivation[0].length != outputGradient[0].length
                || preActivation[0][0].length != outputGradient[0][0].length) {
            throw new IllegalArgumentException("ReLU gradient shape must match its input shape.");
        }
        float[][][] inputGradient = new float[preActivation.length]
                [preActivation[0].length][preActivation[0][0].length];
        for (int channel = 0; channel < preActivation.length; channel++) {
            for (int y = 0; y < preActivation[channel].length; y++) {
                for (int x = 0; x < preActivation[channel][y].length; x++) {
                    inputGradient[channel][y][x] = preActivation[channel][y][x] > 0.0f
                            ? outputGradient[channel][y][x] : 0.0f;
                }
            }
        }
        return inputGradient;
    }
}
