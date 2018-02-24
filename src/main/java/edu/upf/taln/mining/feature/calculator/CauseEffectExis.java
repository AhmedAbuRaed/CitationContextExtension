package edu.upf.taln.mining.feature.calculator;

import edu.upf.taln.mining.feature.context.DocumentCtx;
import edu.upf.taln.mining.reader.TrainingExample;
import edu.upf.taln.ml.feat.base.FeatCalculator;
import edu.upf.taln.ml.feat.base.MyDouble;
import gate.Annotation;
import gate.AnnotationSet;
import gate.Document;

public class CauseEffectExis implements FeatCalculator<Double, TrainingExample, DocumentCtx> {
    @Override
    public MyDouble calculateFeature(TrainingExample trainingExample, DocumentCtx documentCtx, String s) {
        MyDouble value = new MyDouble(0d);
        double count = 0d;
        Document document = documentCtx.getCitationDoc();
        Annotation sentence = trainingExample.getCitanceSentence();

        AnnotationSet cpCause = document.getAnnotations("Causality").get("CAUSE").get(sentence.getStartNode().getOffset(),
                sentence.getEndNode().getOffset());

        AnnotationSet cpEffect = document.getAnnotations("Causality").get("EFFECT").get(sentence.getStartNode().getOffset(),
                sentence.getEndNode().getOffset());

        if (cpCause.size() > 0) {
            count++;
        }
        if (cpEffect.size() > 0) {
            count++;
        }

        value.setValue(count);

        return value;
    }
}
