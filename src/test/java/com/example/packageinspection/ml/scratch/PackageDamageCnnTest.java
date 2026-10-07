package com.example.packageinspection.ml.scratch;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PackageDamageCnnTest {

    @Test
    void predictionReturnsProbabilitiesForExactlyTheFiveOrderedClasses() {
        PackageDamageCnn model = new PackageDamageCnn(12L);
        PackageDamageCnn.Prediction prediction = model.predict(sampleImage());

        assertEquals(List.of("DENT", "TEAR", "PUNCTURE", "CRUSHED", "NO_DAMAGE"),
                PackageDamageCnn.classLabels());
        assertEquals(5, prediction.probabilities().length);
        assertEquals(1.0f, sum(prediction.probabilities()), 1.0e-5f);
        assertEquals(prediction.probabilities()[prediction.classIndex()], prediction.confidence());
        assertTrue(prediction.classIndex() >= 0 && prediction.classIndex() < 5);
    }

    @Test
    void miniBatchUpdateReducesLossOnTheSameDeterministicExample() {
        PackageDamageCnn model = new PackageDamageCnn(27L);
        PackageDamageCnn.TrainingExample example = new PackageDamageCnn.TrainingExample(sampleImage(), 2);

        float firstLoss = model.trainBatch(List.of(example), 0.02f).meanCrossEntropy();
        float lastLoss = firstLoss;
        for (int i = 0; i < 30; i++) {
            lastLoss = model.trainBatch(List.of(example), 0.02f).meanCrossEntropy();
        }

        assertTrue(lastLoss < firstLoss);
    }

    @Test
    void duplicatedExamplesProduceTheSameMeanGradientUpdateAsOneExample() {
        PackageDamageCnn singleModel = new PackageDamageCnn(101L);
        PackageDamageCnn duplicatedModel = new PackageDamageCnn(101L);
        PackageDamageCnn.TrainingExample example = new PackageDamageCnn.TrainingExample(sampleImage(), 3);

        singleModel.trainBatch(List.of(example), 0.01f);
        duplicatedModel.trainBatch(List.of(example, example), 0.01f);

        float[] singleProbabilities = singleModel.predict(sampleImage()).probabilities();
        float[] duplicatedProbabilities = duplicatedModel.predict(sampleImage()).probabilities();
        for (int i = 0; i < singleProbabilities.length; i++) {
            assertEquals(singleProbabilities[i], duplicatedProbabilities[i], 1.0e-6f);
        }
    }

    @Test
    void rejectsWrongInputShapeAndInvalidTrainingArguments() {
        PackageDamageCnn model = new PackageDamageCnn(5L);

        assertThrows(IllegalArgumentException.class, () -> model.predict(new float[1][31][32]));
        assertThrows(IllegalArgumentException.class, () -> model.trainBatch(List.of(), 0.01f));
        assertThrows(IllegalArgumentException.class,
                () -> model.trainBatch(List.of(new PackageDamageCnn.TrainingExample(sampleImage(), 5)), 0.01f));
        assertThrows(IllegalArgumentException.class,
                () -> model.trainBatch(List.of(new PackageDamageCnn.TrainingExample(sampleImage(), 0)), 0.0f));
    }

    private static float[][][] sampleImage() {
        float[][][] image = new float[1][32][32];
        for (int y = 0; y < 32; y++) {
            for (int x = 0; x < 32; x++) {
                image[0][y][x] = ((y * 7 + x * 11) % 256) / 255.0f;
            }
        }
        return image;
    }

    private static float sum(float[] values) {
        float result = 0.0f;
        for (float value : values) {
            result += value;
        }
        return result;
    }
}
