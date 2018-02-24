package edu.upf.taln.mining.feature.calculator;

import edu.upf.taln.mining.feature.context.DocumentCtx;
import edu.upf.taln.mining.reader.ACLMetaData;
import edu.upf.taln.mining.reader.TrainingExample;
import edu.upf.taln.ml.feat.base.FeatCalculator;
import edu.upf.taln.ml.feat.base.MyDouble;
import gate.*;

import java.io.IOException;
import java.util.HashMap;

public class PositionDistance implements FeatCalculator<Double, TrainingExample, DocumentCtx> {
    HashMap<String, ACLMetaData> ACLMetaDataMap = new HashMap<String, ACLMetaData>();

    public PositionDistance(HashMap<String, ACLMetaData> ACLMetaDataMap) throws IOException {
        this.ACLMetaDataMap = ACLMetaDataMap;
    }

    @Override
    public MyDouble calculateFeature(TrainingExample trainingExample, DocumentCtx documentCtx, String s) {
        MyDouble value = new MyDouble(-1d);
        FormalCitation formalCitation = null;

        Document document = documentCtx.getCitationDoc();
        Annotation sentence = trainingExample.getCitanceSentence();

        try {
            formalCitation = new FormalCitation(0, ACLMetaDataMap);
        } catch (IOException e) {
            e.printStackTrace();
        }

        Integer sentenceID = (Integer) sentence.getFeatures().get("SentenceID");

        Integer index = sentenceID - 1;

        while (index > 0) {
            FeatureMap fm = Factory.newFeatureMap();
            fm.put("SentenceID", index);
            AnnotationSet as = document.getAnnotations("Information").get("Sentence", fm);
            if (as.size() > 0) {
                TrainingExample te = new TrainingExample(as.iterator().next(), 0);
                if (formalCitation.calculateFeature(te, documentCtx, "FORMAL").getValue() == 1d) {
                    value.setValue((double) sentenceID - (double) index);
                    break;
                }
            }
            index--;
        }

        Integer index2 = sentenceID + 1;

        if (value.getValue() == -1d) {
            int numberOfSentences = document.getAnnotations("Information").get("Sentence").size();
            while (index2 <= numberOfSentences) {
                FeatureMap fm = Factory.newFeatureMap();
                fm.put("SentenceID", index2);
                AnnotationSet as = document.getAnnotations("Information").get("Sentence", fm);
                if (as.size() > 0) {
                    TrainingExample te = new TrainingExample(as.iterator().next(), 0);
                    if (formalCitation.calculateFeature(te, documentCtx, "FORMAL").getValue() == 1d) {
                        value.setValue((double) index2 - (double) sentenceID);
                        break;
                    }
                }
                index2++;
            }
        }

        return value;
    }
}
