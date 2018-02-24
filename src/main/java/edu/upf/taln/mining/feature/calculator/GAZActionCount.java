package edu.upf.taln.mining.feature.calculator;

import edu.upf.taln.mining.feature.context.DocumentCtx;
import edu.upf.taln.mining.reader.TrainingExample;
import edu.upf.taln.ml.feat.base.FeatCalculator;
import edu.upf.taln.ml.feat.base.MyBase;
import edu.upf.taln.ml.feat.base.MyDouble;
import gate.Annotation;
import gate.AnnotationSet;
import gate.Document;

public class GAZActionCount implements FeatCalculator<Double, TrainingExample, DocumentCtx> {
    @Override
    public MyBase<Double> calculateFeature(TrainingExample trainingExample, DocumentCtx documentCtx, String s) {
        MyDouble value = new MyDouble(0d);
        Document document = documentCtx.getCitationDoc();
        Annotation sentence = trainingExample.getCitanceSentence();
        Double count = 0d;

        for (Annotation annotation : document.getAnnotations("Analysis").get("Lookup").get(sentence.getStartNode().getOffset(), sentence.getEndNode().getOffset())) {
            if (annotation.getFeatures().get("majorType").toString().equals("action_lexicon")) {
                count++;
            }
        }
        value.setValue(count);
        return value;
    }
}
