package edu.upf.taln.mining.feature.calculator;

import edu.upf.taln.mining.feature.context.DocumentCtx;
import edu.upf.taln.mining.reader.TrainingExample;
import edu.upf.taln.ml.feat.base.FeatCalculator;
import edu.upf.taln.ml.feat.base.MyDouble;
import gate.Annotation;
import gate.Document;

public class GAZConceptCount implements FeatCalculator<Double, TrainingExample, DocumentCtx> {
    @Override
    public MyDouble calculateFeature(TrainingExample trainingExample, DocumentCtx documentCtx, String s) {
        MyDouble value = new MyDouble(0d);
        Document document = documentCtx.getCitationDoc();
        Annotation sentence = trainingExample.getCitanceSentence();
        Double count = 0d;

        for (Annotation annotation : document.getAnnotations("Analysis").get("Lookup").get(sentence.getStartNode().getOffset(), sentence.getEndNode().getOffset())) {
            if (annotation.getFeatures().get("majorType").toString().equals("concept_lexicon")) {
                count++;
            }
        }
        value.setValue(count);
        return value;
    }
}
