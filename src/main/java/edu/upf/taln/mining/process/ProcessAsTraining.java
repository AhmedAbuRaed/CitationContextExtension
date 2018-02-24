package edu.upf.taln.mining.process;

import edu.upf.taln.mining.Utilities;
import edu.upf.taln.mining.feature.context.DocumentCtx;
import edu.upf.taln.mining.reader.ACLMetaData;
import edu.upf.taln.mining.reader.TrainingExample;
import edu.upf.taln.ml.feat.FeatureSet;
import gate.Annotation;
import gate.AnnotationSet;
import gate.Document;
import gate.Factory;
import org.deeplearning4j.models.embeddings.loader.WordVectorSerializer;
import org.deeplearning4j.models.word2vec.Word2Vec;

import java.io.File;
import java.util.HashMap;
import java.util.Set;

public class ProcessAsTraining {
    public static void ProcessAsTraining(String workingDirectory, String datasetType, String[] targetClusters) {
        System.out.println("Started Training Pipeline ...");
        String outputInstancesType = "Training";

        Word2Vec gvec = null;
        Word2Vec aclvec = null;
        Word2Vec bnvec = null;

        //Load Word2Vec models in case it is in the pipeline
        System.out.println("Loading Word2vec Models ...");
        //Get file from resources folder
        File gModel = new File(workingDirectory + File.separator + "GoogleNews-vectors-negative300.bin.gz");
        gvec = WordVectorSerializer.readWord2VecModel(gModel);

        File aclModel = new File(workingDirectory + File.separator + "ACL300.txt");
        aclvec = WordVectorSerializer.readWord2VecModel(aclModel);

        File bnModel = new File(workingDirectory + File.separator + "sw2v_synsets_cbow_wikipedia_vectors.bin");
        bnvec = WordVectorSerializer.readWord2VecModel(bnModel);
        System.out.println("Word2vec Models Loaded ...");

        HashMap<String, ACLMetaData> metaDataACLMap = Utilities.loadACLMetaData(workingDirectory);

        Set<String> stopWords = Utilities.getStopWordsSet(workingDirectory);
        Set<String> lexicalHooks = Utilities.detectLexicalHooks(workingDirectory, metaDataACLMap);

        for (String targetCluster : targetClusters) {
            int parsedInstances = 0;
            File clusterFolder = new File(workingDirectory + "/datasets/" + datasetType + File.separator + targetCluster);

            System.out.println("Processing Cluster:" + clusterFolder.getName());
            File inputFolder = new File(clusterFolder.getPath());
            System.out.println("Extracting documents ...");
            HashMap<String, Document> documents = Utilities.getDocuments(clusterFolder.getName(), inputFolder.getPath(), "PreProcessed");
            System.out.println("Documents Extracted ...");

            Set<String> accronyms = Utilities.detectAcronyms(documents, metaDataACLMap);

            System.out.println("Genetaring FeatureSet ...");
            FeatureSet<TrainingExample, DocumentCtx> featureSet = Utilities.generateFeatureSet(documents, metaDataACLMap, gvec, aclvec, bnvec, stopWords, accronyms, lexicalHooks);
            System.out.println("Genetaring Matches FeatureSet Done ...");

            System.out.println("Generating Instances ...");

            for (String key : documents.keySet()) {
                if (!key.equals(clusterFolder.getName())) {
                    Document doc = documents.get(key);

                    AnnotationSet docInformation = doc.getAnnotations("Information");
                    AnnotationSet docExcluded = doc.getAnnotations("Excluded");
                    AnnotationSet docFormalCitations = doc.getAnnotations("Formal_Citations");
                    AnnotationSet docInFormalCitations = doc.getAnnotations("InFormal_Citations");

                    for (Annotation infoAnnotation : docInformation) {
                        AnnotationSet IFAnnotationSet = docInFormalCitations.get(infoAnnotation.getStartNode().getOffset(),
                                infoAnnotation.getEndNode().getOffset());
                        if (IFAnnotationSet.size() > 0) {
                            try {
                                parsedInstances++;
                                System.out.println("Parsing " + outputInstancesType + " instance " + parsedInstances
                                        + ": (document: " + doc.getName() + " id: " + infoAnnotation.getFeatures().get("SentenceID")
                                        + "):");
                                // Set training context
                                DocumentCtx trCtx = new DocumentCtx(doc, documents.get(clusterFolder.getName()));
                                TrainingExample te = new TrainingExample(infoAnnotation, 1);
                                featureSet.addElement(te, trCtx);
                            } catch (Exception e) {
                                System.out.println("Error generating " + outputInstancesType + " instance " + parsedInstances
                                        + ": (document: " + doc.getName() + " id: " + infoAnnotation.getFeatures().get("SentenceID")
                                        + "):");
                                e.printStackTrace();
                            }
                        } else {
                            try {
                                parsedInstances++;
                                System.out.println("Parsing " + outputInstancesType + " instance " + parsedInstances
                                        + ": (document: " + doc.getName() + " id: " + infoAnnotation.getFeatures().get("SentenceID")
                                        + "):");
                                // Set training context
                                DocumentCtx trCtx = new DocumentCtx(doc, documents.get(clusterFolder.getName()));
                                TrainingExample te = new TrainingExample(infoAnnotation, 0);
                                featureSet.addElement(te, trCtx);
                            } catch (Exception e) {
                                System.out.println("Error generating " + outputInstancesType + " instance " + parsedInstances
                                        + ": (document: " + doc.getName() + " id: " + infoAnnotation.getFeatures().get("SentenceID")
                                        + "):");
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }
            Utilities.FeatureSetToARFF(featureSet, workingDirectory, clusterFolder.getName() + "_LASTUSTraining", "1");

            for (String k : documents.keySet()) {
                Factory.deleteResource(documents.get(k));
            }
            System.gc();
        }
        System.out.println("Training Pipeline Done ...");
    }
}
