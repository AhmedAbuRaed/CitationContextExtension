package edu.upf.taln.mining.feature.calculator;

import edu.upf.taln.mining.feature.context.DocumentCtx;
import edu.upf.taln.mining.reader.TrainingExample;
import edu.upf.taln.ml.feat.base.FeatCalculator;
import edu.upf.taln.ml.feat.base.MyDouble;
import gate.Annotation;
import gate.AnnotationSet;
import gate.Document;

public class HighestProbFacet implements FeatCalculator<Double, TrainingExample, DocumentCtx> {

    @Override
    public MyDouble calculateFeature(TrainingExample trainingExample, DocumentCtx documentCtx, String s) {
        MyDouble value = new MyDouble(0d);

        Document cp = documentCtx.getCitationDoc();

        Annotation citSentence = trainingExample.getCitanceSentence();

        AnnotationSet cpSentenceProbabilities = cp.getAnnotations("Analysis").get("Sentence_LOA").get(citSentence.getStartNode().getOffset(),
                citSentence.getEndNode().getOffset());

        if (cpSentenceProbabilities.size() > 0) {
            Double max = 0d;
            String facet = "Unspecified";
            Annotation cpSentence = cpSentenceProbabilities.iterator().next();
            for (Object key : cpSentence.getFeatures().keySet()) {
                if (key.toString().startsWith("PROB_DRI")) {
                    if (cpSentence.getFeatures().get(key) != null && max < (Double) cpSentence.getFeatures().get(key)) {
                        max = (Double) cpSentence.getFeatures().get(key);
                        facet = key.toString().substring(key.toString().lastIndexOf("_") + 1);
                    }
                }
            }
            switch (facet) {
                case "Approach":
                    value.setValue(1d);
                    break;
                case "Background":
                    value.setValue(2d);
                    break;
                case "Challenge":
                    value.setValue(3d);
                    break;
                case "FutureWork":
                    value.setValue(4d);
                    break;
                case "Outcome":
                    value.setValue(5d);
                    break;
                case "Unspecified":
                    value.setValue(0d);
                    break;
            }
        }

        return value;
    }
}
