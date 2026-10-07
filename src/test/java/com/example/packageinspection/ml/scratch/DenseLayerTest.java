package com.example.packageinspection.ml.scratch;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DenseLayerTest {

    @Test
    void denseWeightGradientMatchesFiniteDifferences() {
        DenseLayer layer = new DenseLayer(3, 2, new Random(9));
        float[] input = {0.25f, -0.8f, 0.6f};
        float[] upstream = {0.7f, -0.35f};

        DenseLayer.BackwardResult backward = layer.backward(input, upstream);
        float analytic = backward.gradients().weights[1][2];
        float original = layer.weight(1, 2);
        float epsilon = 0.001f;
        layer.setWeight(1, 2, original + epsilon);
        float positive = dot(layer.forward(input), upstream);
        layer.setWeight(1, 2, original - epsilon);
        float negative = dot(layer.forward(input), upstream);
        layer.setWeight(1, 2, original);

        float numerical = (positive - negative) / (2.0f * epsilon);
        assertEquals(numerical, analytic, 0.001f);
    }

    private static float dot(float[] left, float[] right) {
        float result = 0.0f;
        for (int i = 0; i < left.length; i++) {
            result += left[i] * right[i];
        }
        return result;
    }
}
