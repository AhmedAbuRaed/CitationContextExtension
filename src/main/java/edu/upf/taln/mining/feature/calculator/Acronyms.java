package edu.upf.taln.mining.feature.calculator;

import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import edu.upf.taln.mining.feature.context.DocumentCtx;
import edu.upf.taln.mining.reader.TrainingExample;
import edu.upf.taln.ml.feat.base.FeatCalculator;
import edu.upf.taln.ml.feat.base.MyDouble;
import gate.*;

/**
 * Created by Ahmed on 11/13/15.
 */
public class Acronyms implements FeatCalculator<Double, TrainingExample, DocumentCtx> {
    Set<String> Acronyms = new HashSet<String>();

    public Acronyms(Set<String> Acronyms) throws IOException {
        this.Acronyms = Acronyms;
    }

    @Override
    public MyDouble calculateFeature(TrainingExample trainingExample, DocumentCtx documentCtx, String s) {
        MyDouble value = new MyDouble(0d);
        Document document = documentCtx.getCitationDoc();
        Annotation sentence = trainingExample.getCitanceSentence();
        AnnotationSet tokens = document.getAnnotations("Analysis").get("Token").get(sentence.getStartNode().getOffset(), sentence.getEndNode().getOffset());
        Iterator tokenIterator = tokens.iterator();
        while (tokenIterator.hasNext())
        {
            Annotation token = (Annotation) tokenIterator.next();
            if(Acronyms.contains(token.getFeatures().get("string").toString()))
            {
                value.setValue(1d);
            }
        }
        return value;
    }
}
