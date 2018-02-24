package edu.upf.taln.mining;

import edu.upf.taln.dri.lib.Factory;
import edu.upf.taln.dri.lib.exception.DRIexception;
import edu.upf.taln.mining.feature.calculator.*;
import edu.upf.taln.mining.feature.context.DocumentCtx;
import edu.upf.taln.mining.process.ProcessAsTraining;
import edu.upf.taln.mining.reader.ACLMetaData;
import edu.upf.taln.mining.reader.TrainingExample;
import gate.Annotation;
import gate.AnnotationSet;
import gate.Document;
import gate.FeatureMap;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.deeplearning4j.models.embeddings.loader.WordVectorSerializer;
import org.deeplearning4j.models.word2vec.Word2Vec;

import java.io.*;
import java.util.*;

/**
 * Created by ahmed on 5/30/2016.
 */
public class Main {
    public static void main(String args[]) {

        if (args.length > 0) {
            String workingDir = args[2].trim();
            String[] targetOptions = args[3].split("\\_");
            String[] targetClusters = Arrays.copyOfRange(targetOptions, 0, targetOptions.length - 2);
            String target = targetOptions[0];
            String datasetType = targetOptions[targetOptions.length - 2];
            boolean isTrain;
            if (targetOptions[targetOptions.length - 1].equals("train")) {
                isTrain = true;
            } else {
                isTrain = false;
            }
            int max = 0;
            double predPercentage = 0.0;

            if (targetOptions.length == 5) {
                max = Integer.parseInt(targetOptions[3]);
                predPercentage = Double.parseDouble(targetOptions[4]);
            }

            List<Logger> loggers = Collections.<Logger>list(LogManager.getCurrentLoggers());
            loggers.add(LogManager.getRootLogger());
            for (Logger logger : loggers) {
                logger.setLevel(Level.OFF);
            }

            System.out.println("Initializing Dr. Inventor Framework ...");
            try {
                // A) IMPORTANT: Initialize Dr. Inventor Framework
                // A.1) set the local path to the config_file previously downloaded
                Factory.setDRIPropertyFilePath(workingDir + File.separator + "DRIconfig.properties");
                // A.2) Initialize the Dr. Inventor Text Mining Framework
                Factory.initFramework();
            } catch (DRIexception drIexception) {
                drIexception.printStackTrace();
            }
            System.out.println("Dr. Inventor Initialized ...");

            switch (args[1]) {
                case "ProcessPipeline":
                    if (isTrain) {
                        ProcessAsTraining.ProcessAsTraining(workingDir, datasetType, targetClusters);
                    } else {
                        //ProcessAsTestingPipeline.ProcessAsTesting(workingDirectory, datasetType, target);
                        //ProcessAsTestingPipeline.ProcessAsSplitTesting(workingDirectory, datasetType, target);
                        //ProcessAsTestingPipeline.ProcessAsSplitOneTesting(workingDirectory, datasetType, target);
                        //ProcessAsTestingPipeline.ProcessAsSystemTesting(workingDir, datasetType, target, max, predPercentage);
                    }
                    break;
            }
/*
            HashMap<String, ACLMetaData> aclMetaDataHashMap = Utilities.loadACLMetaData(workingDir);
            HashMap<String, gate.Document> documents = Utilities.getDocuments("A92-1018", "D:/Research/UPF/Projects/CitationContextExtension/datasets", "PreProcessed");

            ReferenceAbsBWESim referenceAbsBCVSim = null;
            referenceAbsBCVSim = new ReferenceAbsBWESim(bnvec);

            //Document document = gate.Factory.newDocument(new URL("file:///" + workingDir + "/datasets/testing/J90-1003/P05-1014_RICH.xml"), "UTF-8");
            Document document = documents.get("W96-0113");
            FeatureMap fm = gate.Factory.newFeatureMap();
            fm.put("SentenceID", 12);
            AnnotationSet sentences = (AnnotationSet) document.getAnnotations("Information").get("Sentence", fm);
            Annotation sentence = sentences.iterator().next();
            TrainingExample te = new TrainingExample(sentence, 0);
            DocumentCtx documentCtx = new DocumentCtx(document, documents.get("A92-1018"));
            System.out.println(referenceAbsBCVSim.calculateFeature(te, documentCtx, "FORMAL").getValue());*/

        } else {
            System.out.println("Please input arguments");
        }
    }
}

