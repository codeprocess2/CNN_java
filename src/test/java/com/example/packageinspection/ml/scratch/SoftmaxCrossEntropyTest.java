package com.example.packageinspection.ml.scratch;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SoftmaxCrossEntropyTest {

    @Test
    void softmaxAndCrossEntropyRemainFiniteForLargeLogits() {
        SoftmaxCrossEntropy.Result result =
                SoftmaxCrossEntropy.evaluate(new float[]{10000.0f, 9999.0f, -10000.0f}, 1);

        assertTrue(Float.isFinite(result.loss()));
        assertTrue(result.loss() > 0.0f);
        assertEquals(1.0f, sum(result.probabilities()), 1.0e-6f);
        assertEquals(result.probabilities()[0], result.logitsGradient()[0], 1.0e-7f);
        assertEquals(result.probabilities()[1] - 1.0f, result.logitsGradient()[1], 1.0e-7f);
    }

    @Test
    void crossEntropyLogitGradientMatchesFiniteDifferences() {
        float[] logits = {0.4f, -0.2f, 0.9f};
        int target = 2;
        float analytic = SoftmaxCrossEntropy.evaluate(logits, target).logitsGradient()[0];
        float epsilon = 0.001f;

        logits[0] += epsilon;
        float positive = SoftmaxCrossEntropy.evaluate(logits, target).loss();
        logits[0] -= 2.0f * epsilon;
        float negative = SoftmaxCrossEntropy.evaluate(logits, target).loss();

        float numerical = (positive - negative) / (2.0f * epsilon);
        assertEquals(numerical, analytic, 0.001f);
    }

    private static float sum(float[] values) {
        float result = 0.0f;
        for (float value : values) {
            result += value;
        }
        return result;
    }
}
