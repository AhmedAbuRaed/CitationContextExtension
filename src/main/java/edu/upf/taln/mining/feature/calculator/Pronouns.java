package edu.upf.taln.mining.feature.calculator;

import java.util.*;

import edu.upf.taln.mining.feature.context.DocumentCtx;
import edu.upf.taln.mining.reader.TrainingExample;
import edu.upf.taln.ml.feat.base.FeatCalculator;
import edu.upf.taln.ml.feat.base.MyDouble;
import gate.*;

/**
 * Created by Ahmed on 11/13/15.
 */
public class Pronouns implements FeatCalculator<Double, TrainingExample, DocumentCtx> {
    Set<String> pronounsSet = new HashSet<String>(Arrays.asList("he", "she", "his", "her", "they", "their", "this",
            "these", "such", "that", "those", "previous"));

    @Override
    public MyDouble calculateFeature(TrainingExample trainingExample, DocumentCtx documentCtx, String s) {
        MyDouble value = new MyDouble(0d);
        Document document = documentCtx.getCitationDoc();
        Annotation sentence = trainingExample.getCitanceSentence();
        List<Annotation> tokens = document.getAnnotations("Analysis").get("Token").get(sentence.getStartNode().getOffset(), sentence.getEndNode().getOffset()).inDocumentOrder();

        if(tokens.size() > 0) {
            if (pronounsSet.contains(tokens.get(0).getFeatures().get("string").toString().toLowerCase())) {
                value.setValue(1d);
            }
        }
        return value;
    }
}
