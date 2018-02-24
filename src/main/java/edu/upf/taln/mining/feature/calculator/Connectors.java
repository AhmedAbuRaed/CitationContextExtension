package edu.upf.taln.mining.feature.calculator;

import java.util.*;

import edu.upf.taln.mining.feature.context.DocumentCtx;
import edu.upf.taln.mining.reader.TrainingExample;
import edu.upf.taln.ml.feat.base.FeatCalculator;
import edu.upf.taln.ml.feat.base.MyDouble;
import gate.*;
import gate.util.InvalidOffsetException;

/**
 * Created by Ahmed on 11/13/15.
 */
public class Connectors implements FeatCalculator<Double, TrainingExample, DocumentCtx> {
    Set<String> connectorsSet = new HashSet<String>(Arrays.asList("also", "although", "besides", "but", "despite", "even though",
            "furthermore", "however", "in addition", "inspite of", "instead", "instead of", "moreover", "nonetheless",
            "on the contrary", "on the other hand", "regardless of", "still", "then", "though", "whereas", "while", "yet"));

    @Override
    public MyDouble calculateFeature(TrainingExample trainingExample, DocumentCtx documentCtx, String s) {
        MyDouble value = new MyDouble(0d);
        Document document = documentCtx.getCitationDoc();
        Annotation sentence = trainingExample.getCitanceSentence();

        for(String connector: connectorsSet)
        {
            try {
                if(document.getContent().getContent(sentence.getStartNode().getOffset(),
                        sentence.getEndNode().getOffset()).toString().toLowerCase().trim().startsWith(connector))
                {
                    value.setValue(1d);
                }
            } catch (InvalidOffsetException e) {
                e.printStackTrace();
            }
        }
        return value;
    }
}