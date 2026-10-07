package com.example.packageinspection.ml.scratch;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ActivationAndPoolingTest {

    @Test
    void reluZerosNegativeValuesAndBackpropagatesOnlyThroughPositiveValues() {
        float[][][] input = {{{-2.0f, 0.0f, 3.0f}}};
        float[][][] upstream = {{{4.0f, 5.0f, 6.0f}}};

        float[][][] output = Relu.forward(input);
        float[][][] inputGradient = Relu.backward(input, upstream);

        assertEquals(0.0f, output[0][0][0]);
        assertEquals(0.0f, output[0][0][1]);
        assertEquals(3.0f, output[0][0][2]);
        assertEquals(0.0f, inputGradient[0][0][0]);
        assertEquals(0.0f, inputGradient[0][0][1]);
        assertEquals(6.0f, inputGradient[0][0][2]);
    }

    @Test
    void maxPoolRoutesGradientToTheSelectedMaximum() {
        MaxPool2D pool = new MaxPool2D(2, 2);
        float[][][] input = {{
                {1.0f, 2.0f, 0.0f, 1.0f},
                {3.0f, 4.0f, 2.0f, 0.0f},
                {0.0f, 1.0f, 5.0f, 2.0f},
                {2.0f, 0.0f, 1.0f, 3.0f}
        }};

        MaxPool2D.Result result = pool.forward(input);
        float[][][] inputGradient = pool.backward(result, new float[][][]{{{7.0f, 8.0f}, {9.0f, 10.0f}}});

        assertEquals(2, result.values()[0].length);
        assertEquals(2, result.values()[0][0].length);
        assertEquals(4.0f, result.values()[0][0][0]);
        assertEquals(5.0f, result.values()[0][1][1]);
        assertEquals(7.0f, inputGradient[0][1][1]);
        assertEquals(8.0f, inputGradient[0][1][2]);
        assertEquals(9.0f, inputGradient[0][3][0]);
        assertEquals(10.0f, inputGradient[0][2][2]);
        assertEquals(0.0f, inputGradient[0][0][0]);
    }
}
