package edu.upf.taln.mining.feature.calculator;

import edu.upf.taln.mining.feature.context.DocumentCtx;
import edu.upf.taln.mining.reader.TrainingExample;
import edu.upf.taln.ml.feat.base.FeatCalculator;
import edu.upf.taln.ml.feat.base.MyDouble;
import gate.Annotation;
import gate.AnnotationSet;
import gate.Document;

import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Created by ahmed on 8/9/2017.
 */
public class LexicalHooks implements FeatCalculator<Double, TrainingExample, DocumentCtx> {
   Set<String> lexicalHooks;

    public LexicalHooks(Set<String> lexicalHooks) throws IOException {
        this.lexicalHooks = lexicalHooks;
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
            if(lexicalHooks.contains(token.getFeatures().get("string").toString()))
            {
                value.setValue(1d);
            }
        }
        return value;
    }
}
