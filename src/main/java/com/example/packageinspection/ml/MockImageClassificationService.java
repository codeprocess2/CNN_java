package com.example.packageinspection.ml;

import com.example.packageinspection.entity.DamageType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Random;

@Service("mockImageClassificationService")
public class MockImageClassificationService implements ImageClassificationService {

    private static final Logger logger = LoggerFactory.getLogger(MockImageClassificationService.class);
    private final Random random = new Random();

    @Override
    public ClassificationResult classify(MultipartFile file) {
        String filename = file.getOriginalFilename() != null ? file.getOriginalFilename().toLowerCase(Locale.ROOT) : "";
        return analyzeFilenameOrGenerate(filename);
    }

    @Override
    public ClassificationResult classify(Path imagePath) {
        String filename = imagePath.getFileName().toString().toLowerCase(Locale.ROOT);
        return analyzeFilenameOrGenerate(filename);
    }

    private ClassificationResult analyzeFilenameOrGenerate(String filename) {
        logger.info("Executing Mock CNN Inference engine for filename: {}", filename);
        
        DamageType predictedClass;
        double confidence;

        if (filename.contains("dent")) {
            predictedClass = DamageType.DENT;
            confidence = 0.91 + (random.nextDouble() * 0.07);
        } else if (filename.contains("tear")) {
            predictedClass = DamageType.TEAR;
            confidence = 0.88 + (random.nextDouble() * 0.10);
        } else if (filename.contains("puncture")) {
            predictedClass = DamageType.PUNCTURE;
            confidence = 0.93 + (random.nextDouble() * 0.05);
        } else if (filename.contains("crushed") || filename.contains("crush")) {
            predictedClass = DamageType.CRUSHED;
            confidence = 0.89 + (random.nextDouble() * 0.09);
        } else if (filename.contains("clean") || filename.contains("nodamage") || filename.contains("ok") || filename.contains("good")) {
            predictedClass = DamageType.NO_DAMAGE;
            confidence = 0.95 + (random.nextDouble() * 0.04);
        } else {
            // Heuristic random selection
            DamageType[] types = DamageType.values();
            predictedClass = types[random.nextInt(types.length - 1)]; // exclude UNKNOWN
            confidence = 0.75 + (random.nextDouble() * 0.22);
        }

        confidence = Math.round(confidence * 100.0) / 100.0;

        return ClassificationResult.builder()
                .predictedClass(predictedClass)
                .confidence(confidence)
                .isMockModel(true)
                .details("Inference completed via Mock/Demo CNN engine (Model file not loaded). Replace with trained DJL PyTorch TorchScript model.")
                .build();
    }
}
