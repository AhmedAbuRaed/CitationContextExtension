package edu.upf.taln.mining.feature.calculator;

import edu.upf.taln.mining.Utilities;
import edu.upf.taln.mining.feature.context.DocumentCtx;
import edu.upf.taln.mining.reader.TrainingExample;
import edu.upf.taln.ml.feat.base.FeatCalculator;
import edu.upf.taln.ml.feat.base.MyDouble;
import gate.*;
import gate.util.InvalidOffsetException;

import java.util.List;

public class ReferenceAbsWCVSim implements FeatCalculator<Double, TrainingExample, DocumentCtx> {

    @Override
    public MyDouble calculateFeature(TrainingExample trainingExample, DocumentCtx documentCtx, String s) {
        MyDouble value = new MyDouble(0d);
        Document rp = documentCtx.getReferenceDoc();
        Document cp = documentCtx.getCitationDoc();

        Annotation citSentence = trainingExample.getCitanceSentence();
        AnnotationSet citSentenceNormalizedContextVectors = cp.getAnnotations("Analysis").get("Vector_Norm").get(citSentence.getStartNode().getOffset(), citSentence.getEndNode().getOffset());

        List<Annotation> referenceAbstract;
        if ((rp.getAnnotations("Original markups").get("abstract_text") != null) && ((referenceAbstract = rp.getAnnotations("Original markups").get("abstract_text").inDocumentOrder()) != null) &&
                (referenceAbstract.size() > 0)) {
            List<Annotation> referenceAbstractSentences = rp.getAnnotations("Original markups").get("Sentence").get(referenceAbstract.get(0).getStartNode().getOffset(),
                    referenceAbstract.get(referenceAbstract.size() - 1).getEndNode().getOffset()).inDocumentOrder();
            FeatureMap abstractFM = Factory.newFeatureMap();
            for (int i = 0; i < referenceAbstractSentences.size(); i++) {
                AnnotationSet normalizedContextVectors = rp.getAnnotations("Analysis").get("Vector_Norm").get(referenceAbstractSentences.get(i).getStartNode().getOffset(), referenceAbstractSentences.get(i).getEndNode().getOffset());
                if (normalizedContextVectors.size() > 0) {
                    if (normalizedContextVectors.iterator().hasNext()) {
                        if (i == 0) {
                            abstractFM = normalizedContextVectors.iterator().next().getFeatures();
                        } else {
                            abstractFM = Utilities.combineNormalizedVectors(abstractFM, normalizedContextVectors.iterator().next().getFeatures());
                        }
                    }
                }
            }
            if (citSentenceNormalizedContextVectors.size() > 0) {
                if (citSentenceNormalizedContextVectors.iterator().hasNext()) {
                    value.setValue(summa.scorer.Cosine.cosine1(abstractFM, citSentenceNormalizedContextVectors.iterator().next().getFeatures()));
                }
            }
        } else {
            List<Annotation> referenceSentences = rp.getAnnotations("Original markups").get("Sentence").inDocumentOrder();
            FeatureMap abstractFM = Factory.newFeatureMap();
            for (int i = 0; i < 12 && i < referenceSentences.size(); i++) {
                AnnotationSet normalizedContextVectors = rp.getAnnotations("Analysis").get("Vector_Norm").get(referenceSentences.get(i).getStartNode().getOffset(), referenceSentences.get(i).getEndNode().getOffset());
                if (normalizedContextVectors.size() > 0) {
                    if (normalizedContextVectors.iterator().hasNext()) {
                        if (i == 0) {
                            abstractFM = normalizedContextVectors.iterator().next().getFeatures();
                        } else {
                            try {
                                if (rp.getContent().getContent(referenceSentences.get(i).getStartNode().getOffset(), referenceSentences.get(i).getEndNode().getOffset()).toString().startsWith("Introduction")) {
                                    break;
                                }
                            } catch (InvalidOffsetException e) {
                                e.printStackTrace();
                            }
                            abstractFM = Utilities.combineNormalizedVectors(abstractFM, normalizedContextVectors.iterator().next().getFeatures());
                        }
                    }
                }
            }
            if (citSentenceNormalizedContextVectors.size() > 0) {
                if (citSentenceNormalizedContextVectors.iterator().hasNext()) {
                    value.setValue(summa.scorer.Cosine.cosine1(abstractFM,
                            citSentenceNormalizedContextVectors.iterator().next().getFeatures()));
                }
            }
        }

        return value;
    }
}