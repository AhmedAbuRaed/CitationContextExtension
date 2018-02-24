package edu.upf.taln.mining.feature.calculator;

import java.io.IOException;

import edu.upf.taln.mining.feature.context.DocumentCtx;
import edu.upf.taln.mining.reader.TrainingExample;
import edu.upf.taln.ml.feat.base.FeatCalculator;
import edu.upf.taln.ml.feat.base.MyDouble;
import gate.*;
import gate.util.InvalidOffsetException;

/**
 * Created by Ahmed on 11/13/15.
 */
public class SectionMarkers implements FeatCalculator<Double, TrainingExample, DocumentCtx> {
    private Integer relativePosition = -1;

    public SectionMarkers(Integer relativePosition) throws IOException {
        this.relativePosition = relativePosition;
    }

    @Override
    public MyDouble calculateFeature(TrainingExample trainingExample, DocumentCtx documentCtx, String s) {
        MyDouble value = new MyDouble(0d);
        Document document = documentCtx.getCitationDoc();
        Annotation sentence = trainingExample.getCitanceSentence();
        Integer sentenceID = (Integer) sentence.getFeatures().get("SentenceID");

        if (relativePosition == 0) {
            try {
                if (document.getContent().getContent(sentence.getStartNode().getOffset(),
                        sentence.getEndNode().getOffset()).toString().toLowerCase().trim().matches("^\\d+(?:\\.\\d+)*[ \\t]+[A-Z]\\S.*$")) {
                    value.setValue(1d);
                }
            } catch (InvalidOffsetException e) {
                e.printStackTrace();
            }
        } else if (relativePosition == -1) {
            if (sentenceID > 1) {
                FeatureMap fm = Factory.newFeatureMap();
                fm.put("SentenceID", sentenceID - 1);
                AnnotationSet as = document.getAnnotations("Information").get("Sentence", fm);
                if (as.size() > 0) {
                    sentence = as.iterator().next();
                    try {
                        if (document.getContent().getContent(sentence.getStartNode().getOffset(),
                                sentence.getEndNode().getOffset()).toString().toLowerCase().trim().matches("^\\d+(?:\\.\\d+)*[ \\t]+[A-Z]\\S.*$")) {
                            value.setValue(1d);
                        }
                    } catch (InvalidOffsetException e) {
                        e.printStackTrace();
                    }
                }
            }
        } else if (relativePosition == 1) {
            if (sentenceID > 1) {
                FeatureMap fm = Factory.newFeatureMap();
                fm.put("SentenceID", sentenceID + 1);
                AnnotationSet as = document.getAnnotations("Information").get("Sentence", fm);
                if (as.size() > 0) {
                    sentence = as.iterator().next();
                    try {
                        if (document.getContent().getContent(sentence.getStartNode().getOffset(),
                                sentence.getEndNode().getOffset()).toString().toLowerCase().trim().matches("^\\d+(?:\\.\\d+)*[ \\t]+[A-Z]\\S.*$")) {
                            value.setValue(1d);
                        }
                    } catch (InvalidOffsetException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return value;
    }
}