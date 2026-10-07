package com.example.packageinspection.ml.scratch;

/**
 * Numerically stable softmax and multiclass cross-entropy. Subtracting the
 * largest logit avoids overflow without changing the normalized probabilities.
 */
final class SoftmaxCrossEntropy {

    private SoftmaxCrossEntropy() {
    }

    static float[] softmax(float[] logits) {
        return distribution(logits).probabilities;
    }

    static Result evaluate(float[] logits, int targetClass) {
        if (logits == null || logits.length == 0 || targetClass < 0 || targetClass >= logits.length) {
            throw new IllegalArgumentException("Logits must be non-empty and target class must be in range.");
        }

        Distribution distribution = distribution(logits);
        float loss = (float) (distribution.logSumExp - logits[targetClass]);
        // The softmax-cross-entropy derivative simplifies to p - oneHot(target).
        float[] logitsGradient = distribution.probabilities.clone();
        logitsGradient[targetClass] -= 1.0f;
        return new Result(loss, distribution.probabilities, logitsGradient);
    }

    private static Distribution distribution(float[] logits) {
        if (logits == null || logits.length == 0) {
            throw new IllegalArgumentException("Logits must be non-empty.");
        }
        float maxLogit = Float.NEGATIVE_INFINITY;
        for (float logit : logits) {
            if (!Float.isFinite(logit)) {
                throw new IllegalArgumentException("Logits must all be finite.");
            }
            maxLogit = Math.max(maxLogit, logit);
        }

        double exponentialSum = 0.0;
        double[] exponentials = new double[logits.length];
        for (int i = 0; i < logits.length; i++) {
            exponentials[i] = Math.exp(logits[i] - maxLogit);
            exponentialSum += exponentials[i];
        }

        float[] probabilities = new float[logits.length];
        for (int i = 0; i < logits.length; i++) {
            probabilities[i] = (float) (exponentials[i] / exponentialSum);
        }

        double logSumExp = maxLogit + Math.log(exponentialSum);
        return new Distribution(probabilities, logSumExp);
    }

    private record Distribution(float[] probabilities, double logSumExp) {
    }

    record Result(float loss, float[] probabilities, float[] logitsGradient) {
    }
}
