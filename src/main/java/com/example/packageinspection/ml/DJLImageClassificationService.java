package com.example.packageinspection.ml;

import ai.djl.inference.Predictor;
import ai.djl.modality.Classifications;
import ai.djl.modality.cv.Image;
import ai.djl.modality.cv.ImageFactory;
import ai.djl.modality.cv.transform.Normalize;
import ai.djl.modality.cv.transform.Resize;
import ai.djl.modality.cv.transform.ToTensor;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ZooModel;
import ai.djl.translate.Pipeline;
import ai.djl.translate.Translator;
import com.example.packageinspection.entity.DamageType;
import com.example.packageinspection.exception.ModelInferenceException;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

@Service
@Primary
public class DJLImageClassificationService implements ImageClassificationService {

    private static final Logger logger = LoggerFactory.getLogger(DJLImageClassificationService.class);

    @Value("${cnn.model.path:models/package-quality-model.pt}")
    private String modelPathString = "models/package-quality-model.pt";

    private final MockImageClassificationService mockFallbackService;
    private ZooModel<Image, Classifications> zooModel;
    private boolean isModelLoaded = false;

    public DJLImageClassificationService(MockImageClassificationService mockFallbackService) {
        this.mockFallbackService = mockFallbackService;
    }

    @PostConstruct
    public void init() {
        Path modelPath = Paths.get(modelPathString);
        if (!Files.exists(modelPath)) {
            logger.warn("DJL Model file not found at: {}. Using Mock CNN fallback engine.", modelPath.toAbsolutePath());
            isModelLoaded = false;
            return;
        }

        try {
            logger.info("Initializing Deep Java Library (DJL) model from: {}", modelPath.toAbsolutePath());
            List<String> classes = Arrays.asList("DENT", "TEAR", "PUNCTURE", "CRUSHED", "NO_DAMAGE");
            
            Pipeline pipeline = new Pipeline();
            pipeline.add(new Resize(224, 224));
            pipeline.add(new ToTensor());
            pipeline.add(new Normalize(
                    new float[]{0.485f, 0.456f, 0.406f},
                    new float[]{0.229f, 0.224f, 0.225f}
            ));

            Translator<Image, Classifications> translator = ai.djl.modality.cv.translator.ImageClassificationTranslator.builder()
                    .setPipeline(pipeline)
                    .optSynset(classes)
                    .build();

            Criteria<Image, Classifications> criteria = Criteria.builder()
                    .setTypes(Image.class, Classifications.class)
                    .optModelPath(modelPath)
                    .optTranslator(translator)
                    .optEngine("PyTorch")
                    .build();

            this.zooModel = criteria.loadModel();
            this.isModelLoaded = true;
            logger.info("DJL PyTorch Model successfully loaded!");
        } catch (Exception e) {
            logger.error("Failed to load DJL PyTorch Model: {}. Switching to Mock fallback.", e.getMessage());
            isModelLoaded = false;
        }
    }

    @Override
    public ClassificationResult classify(MultipartFile file) {
        if (!isModelLoaded) {
            return mockFallbackService.classify(file);
        }

        try (InputStream is = file.getInputStream()) {
            Image image = ImageFactory.getInstance().fromInputStream(is);
            return runDJLInference(image);
        } catch (Exception e) {
            throw new ModelInferenceException("Failed to run DJL CNN inference: " + e.getMessage(), e);
        }
    }

    @Override
    public ClassificationResult classify(Path imagePath) {
        if (!isModelLoaded) {
            return mockFallbackService.classify(imagePath);
        }

        try {
            Image image = ImageFactory.getInstance().fromFile(imagePath);
            return runDJLInference(image);
        } catch (Exception e) {
            throw new ModelInferenceException("Failed to load image for DJL inference: " + e.getMessage(), e);
        }
    }

    private ClassificationResult runDJLInference(Image image) {
        try (Predictor<Image, Classifications> predictor = zooModel.newPredictor()) {
            Classifications result = predictor.predict(image);
            Classifications.Classification topItem = result.best();

            String className = topItem.getClassName().toUpperCase(Locale.ROOT);
            double confidence = Math.round(topItem.getProbability() * 100.0) / 100.0;

            DamageType damageType;
            try {
                damageType = DamageType.valueOf(className);
            } catch (IllegalArgumentException e) {
                damageType = DamageType.UNKNOWN;
            }

            return ClassificationResult.builder()
                    .predictedClass(damageType)
                    .confidence(confidence)
                    .isMockModel(false)
                    .details("Inference completed via DJL PyTorch Engine in Java.")
                    .build();
        } catch (Exception e) {
            throw new ModelInferenceException("DJL Prediction execution failed", e);
        }
    }

    @PreDestroy
    public void close() {
        if (zooModel != null) {
            zooModel.close();
        }
    }
}
