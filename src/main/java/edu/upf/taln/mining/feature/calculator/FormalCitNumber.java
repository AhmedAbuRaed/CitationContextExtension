package edu.upf.taln.mining.feature.calculator;

import edu.upf.taln.mining.feature.context.DocumentCtx;
import edu.upf.taln.mining.reader.TrainingExample;
import edu.upf.taln.ml.feat.base.FeatCalculator;
import edu.upf.taln.ml.feat.base.MyDouble;
import gate.Annotation;
import gate.AnnotationSet;
import gate.Document;

public class FormalCitNumber implements FeatCalculator<Double, TrainingExample, DocumentCtx> {
    @Override
    public MyDouble calculateFeature(TrainingExample trainingExample, DocumentCtx documentCtx, String s) {
        MyDouble value = new MyDouble(0d);
        Document document = documentCtx.getCitationDoc();
        Annotation sentence = trainingExample.getCitanceSentence();

        AnnotationSet annotationSet = document.getAnnotations("Analysis").get("CitMarker").get(sentence.getStartNode().getOffset(),
                sentence.getEndNode().getOffset());

        value.setValue(Double.parseDouble(String.valueOf(annotationSet.size())));

        return value;
    }
}
