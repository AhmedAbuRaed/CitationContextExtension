package edu.upf.taln.mining.feature.calculator;

import java.util.Iterator;

import edu.upf.taln.mining.feature.context.DocumentCtx;
import edu.upf.taln.mining.reader.TrainingExample;
import edu.upf.taln.ml.feat.base.FeatCalculator;
import edu.upf.taln.ml.feat.base.MyDouble;
import gate.*;

/**
 * Created by Ahmed on 11/13/15.
 */
public class AuthorName implements FeatCalculator<Double, TrainingExample, DocumentCtx> {
    @Override
    public MyDouble calculateFeature(TrainingExample trainingExample, DocumentCtx documentCtx, String s) {
        MyDouble value = new MyDouble(0d);
        Document document = documentCtx.getCitationDoc();
        Annotation sentence = trainingExample.getCitanceSentence();

        if (document.getFeatures().get("referenceAuthors") != null && document.getFeatures().get("referenceAuthors").toString().split(",").length > 0) {
            String fAuthor = document.getFeatures().get("referenceAuthors").toString().split(",")[0];

            AnnotationSet tokens = document.getAnnotations("Analysis").get("Token").get(sentence.getStartNode().getOffset(), sentence.getEndNode().getOffset());
            Iterator tokensIterator = tokens.iterator();
            while (tokensIterator.hasNext()) {
                Annotation token = (Annotation) tokensIterator.next();
                if (token.getFeatures().get("string").toString().contains(fAuthor)) {
                    value.setValue(1d);
                }
            }
        }

        return value;
    }
}
