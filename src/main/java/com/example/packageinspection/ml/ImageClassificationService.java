package com.example.packageinspection.ml;

import org.springframework.web.multipart.MultipartFile;
import java.nio.file.Path;

public interface ImageClassificationService {

    ClassificationResult classify(MultipartFile file);

    ClassificationResult classify(Path imagePath);
}
