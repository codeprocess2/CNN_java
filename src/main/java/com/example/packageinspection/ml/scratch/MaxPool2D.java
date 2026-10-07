package com.example.packageinspection.ml.scratch;

/**
 * Non-overlapping/strided max pooling. Forward stores each winning coordinate
 * so backward routes that pooled gradient only to the selected input value.
 */
final class MaxPool2D {

    private final int poolSize;
    private final int stride;

    MaxPool2D(int poolSize, int stride) {
        if (poolSize <= 0 || stride <= 0) {
            throw new IllegalArgumentException("Pool size and stride must be positive.");
        }
        this.poolSize = poolSize;
        this.stride = stride;
    }

    Result forward(float[][][] input) {
        int channels = input.length;
        int inputHeight = input[0].length;
        int inputWidth = input[0][0].length;
        int outputHeight = (inputHeight - poolSize) / stride + 1;
        int outputWidth = (inputWidth - poolSize) / stride + 1;
        if (outputHeight <= 0 || outputWidth <= 0) {
            throw new IllegalArgumentException("Input dimensions must be at least the pooling window size.");
        }

        float[][][] output = new float[channels][outputHeight][outputWidth];
        int[][][] maxIndices = new int[channels][outputHeight][outputWidth];
        for (int channel = 0; channel < channels; channel++) {
            for (int outY = 0; outY < outputHeight; outY++) {
                for (int outX = 0; outX < outputWidth; outX++) {
                    int startY = outY * stride;
                    int startX = outX * stride;
                    float maxValue = Float.NEGATIVE_INFINITY;
                    int maxY = startY;
                    int maxX = startX;
                    for (int ky = 0; ky < poolSize; ky++) {
                        for (int kx = 0; kx < poolSize; kx++) {
                            float value = input[channel][startY + ky][startX + kx];
                            if (value > maxValue) {
                                maxValue = value;
                                maxY = startY + ky;
                                maxX = startX + kx;
                            }
                        }
                    }
                    output[channel][outY][outX] = maxValue;
                    maxIndices[channel][outY][outX] = maxY * inputWidth + maxX;
                }
            }
        }
        return new Result(output, maxIndices, inputHeight, inputWidth);
    }

    float[][][] backward(Result result, float[][][] outputGradient) {
        int channels = result.maxIndices.length;
        int outputHeight = result.maxIndices[0].length;
        int outputWidth = result.maxIndices[0][0].length;
        if (outputGradient.length != channels
                || outputGradient[0].length != outputHeight
                || outputGradient[0][0].length != outputWidth) {
            throw new IllegalArgumentException("Pooling gradient shape must match the pooled output.");
        }

        float[][][] inputGradient = new float[channels][result.inputHeight][result.inputWidth];
        for (int channel = 0; channel < channels; channel++) {
            for (int y = 0; y < outputHeight; y++) {
                for (int x = 0; x < outputWidth; x++) {
                    int flatIndex = result.maxIndices[channel][y][x];
                    int inputY = flatIndex / result.inputWidth;
                    int inputX = flatIndex % result.inputWidth;
                    inputGradient[channel][inputY][inputX] += outputGradient[channel][y][x];
                }
            }
        }
        return inputGradient;
    }

    static final class Result {
        private final float[][][] values;
        private final int[][][] maxIndices;
        private final int inputHeight;
        private final int inputWidth;

        private Result(float[][][] values, int[][][] maxIndices, int inputHeight, int inputWidth) {
            this.values = values;
            this.maxIndices = maxIndices;
            this.inputHeight = inputHeight;
            this.inputWidth = inputWidth;
        }

        float[][][] values() {
            return values;
        }
    }
}
