package com.smartdoor.service;

import com.smartdoor.api.dto.ApiDtos.ModelInfoResponse;
import jakarta.annotation.PostConstruct;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import weka.classifiers.Evaluation;
import weka.classifiers.trees.J48;
import weka.core.DenseInstance;
import weka.core.Instances;
import weka.core.converters.CSVLoader;

import java.io.InputStream;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Random;

@Service
public class DecisionTreeService {
    private J48 classifier;
    private Instances structure;
    private ModelInfoResponse info;

    @PostConstruct
    void train() throws Exception {
        ClassPathResource resource = new ClassPathResource("ml/access-training.csv");
        byte[] sourceBytes;
        try (InputStream input = resource.getInputStream()) { sourceBytes = input.readAllBytes(); }

        CSVLoader loader = new CSVLoader();
        loader.setNominalAttributes("first-last");
        loader.setSource(new java.io.ByteArrayInputStream(sourceBytes));
        Instances data = loader.getDataSet();
        data.setClassIndex(data.numAttributes() - 1);

        classifier = new J48();
        classifier.setConfidenceFactor(0.25f);
        classifier.setMinNumObj(2);
        classifier.buildClassifier(data);
        structure = new Instances(data, 0);

        Evaluation evaluation = new Evaluation(data);
        evaluation.crossValidateModel(classifier, data, Math.min(5, data.numInstances()), new Random(42));
        int authorizedIndex = data.classAttribute().indexOfValue("AUTHORIZED");
        String version = "j48-" + HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(sourceBytes)).substring(0, 12);
        info = new ModelInfoResponse(version, evaluation.pctCorrect() / 100.0,
                evaluation.precision(authorizedIndex), evaluation.recall(authorizedIndex),
                evaluation.toMatrixString(), classifier.toString(), data.numInstances());
    }

    public Prediction predict(boolean doorAllowed, boolean scheduleAllowed, boolean deviceRegistered,
                              String userRole, String recentFailures) {
        try {
            DenseInstance instance = new DenseInstance(structure.numAttributes());
            instance.setDataset(structure);
            instance.setValue(0, yesNo(doorAllowed));
            instance.setValue(1, yesNo(scheduleAllowed));
            instance.setValue(2, yesNo(deviceRegistered));
            instance.setValue(3, userRole);
            instance.setValue(4, recentFailures);
            instance.setMissing(5);
            String result = structure.classAttribute().value((int) classifier.classifyInstance(instance));
            List<String> path = List.of(
                    "MODEL doorAllowed=" + yesNo(doorAllowed),
                    "MODEL scheduleAllowed=" + yesNo(scheduleAllowed),
                    "MODEL deviceRegistered=" + yesNo(deviceRegistered),
                    "MODEL userRole=" + userRole,
                    "MODEL recentFailures=" + recentFailures,
                    "MODEL result=" + result);
            return new Prediction(result, path, info.version());
        } catch (Exception exception) {
            throw new IllegalStateException("Decision Tree evaluation failed", exception);
        }
    }

    public ModelInfoResponse info() { return info; }
    private static String yesNo(boolean value) { return value ? "yes" : "no"; }
    public record Prediction(String result, List<String> path, String version) {}
}

