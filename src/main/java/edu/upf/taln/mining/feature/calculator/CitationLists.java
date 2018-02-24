package edu.upf.taln.mining.feature.calculator;

import java.io.IOException;
import java.util.HashMap;

import edu.upf.taln.mining.reader.ACLMetaData;
import edu.upf.taln.mining.feature.context.DocumentCtx;
import edu.upf.taln.mining.reader.TrainingExample;
import edu.upf.taln.ml.feat.base.FeatCalculator;
import edu.upf.taln.ml.feat.base.MyDouble;
import gate.*;

/**
 * Created by Ahmed on 11/13/15.
 */
public class CitationLists implements FeatCalculator<Double, TrainingExample, DocumentCtx> {
    HashMap<String, ACLMetaData> ACLMetaDataMap = new HashMap<String, ACLMetaData>();

    public CitationLists(HashMap<String, ACLMetaData> ACLMetaDataMap) throws IOException {
        this.ACLMetaDataMap = ACLMetaDataMap;
    }

    @Override
    public MyDouble calculateFeature(TrainingExample trainingExample, DocumentCtx documentCtx, String s) {
        MyDouble value = new MyDouble(0d);
        Document document = documentCtx.getCitationDoc();
        Annotation sentence = trainingExample.getCitanceSentence();
        try {
            FormalCitation formalCitation = new FormalCitation(0, this.ACLMetaDataMap);
            MyDouble myDouble = formalCitation.calculateFeature(trainingExample, documentCtx, "FormalCitation");
            if (myDouble.getValue() == 1) {
                AnnotationSet citationsAnnotations = document.getAnnotations("Analysis").get("CitMarker");
                if(citationsAnnotations.size() > 1)
                {
                    value.setValue(1d);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return value;
    }
}
