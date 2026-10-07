package com.example.packageinspection.ml.scratch;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;

class Conv2DLayerTest {

    @Test
    void convolutionHasValidOutputShapeAndItsKernelGradientMatchesFiniteDifferences() {
        Conv2DLayer layer = new Conv2DLayer(1, 1, 2, new Random(4));
        float[][][] input = {{
                {0.2f, -0.3f, 0.7f},
                {0.5f, 0.8f, -0.4f},
                {-0.6f, 0.1f, 0.9f}
        }};
        float[][][] upstream = {{
                {0.4f, -0.2f},
                {0.3f, 0.6f}
        }};

        float[][][] output = layer.forward(input);
        assertEquals(1, output.length);
        assertEquals(2, output[0].length);
        assertEquals(2, output[0][0].length);

        Conv2DLayer.BackwardResult backward = layer.backward(input, upstream);
        float analytic = backward.gradients().weights[0][0][0][1];
        float original = layer.weight(0, 0, 0, 1);
        float epsilon = 0.001f;
        layer.setWeight(0, 0, 0, 1, original + epsilon);
        float positive = objective(layer.forward(input), upstream);
        layer.setWeight(0, 0, 0, 1, original - epsilon);
        float negative = objective(layer.forward(input), upstream);
        layer.setWeight(0, 0, 0, 1, original);

        float numerical = (positive - negative) / (2.0f * epsilon);
        assertEquals(numerical, analytic, 0.001f);
    }

    private static float objective(float[][][] output, float[][][] upstream) {
        float result = 0.0f;
        for (int y = 0; y < output[0].length; y++) {
            for (int x = 0; x < output[0][y].length; x++) {
                result += output[0][y][x] * upstream[0][y][x];
            }
        }
        return result;
    }
}
