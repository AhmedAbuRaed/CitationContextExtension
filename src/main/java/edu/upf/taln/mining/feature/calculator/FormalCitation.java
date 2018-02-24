package edu.upf.taln.mining.feature.calculator;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;

import edu.upf.taln.mining.reader.ACLMetaData;
import edu.upf.taln.mining.feature.context.DocumentCtx;
import edu.upf.taln.mining.reader.TrainingExample;
import edu.upf.taln.ml.feat.base.FeatCalculator;
import edu.upf.taln.ml.feat.base.MyDouble;
import gate.*;
import gate.util.InvalidOffsetException;

/**
 * Created by Ahmed on 11/13/15.
 */
public class FormalCitation implements FeatCalculator<Double, TrainingExample, DocumentCtx> {
    HashMap<String, ACLMetaData> ACLMetaDataMap = new HashMap<String, ACLMetaData>();
    private Integer relativePosition = -1;

    public FormalCitation(Integer relativePosition, HashMap<String, ACLMetaData> ACLMetaDataMap) throws IOException {
        relativePosition = (relativePosition != null) ? relativePosition : 0;
        this.relativePosition = relativePosition;
        this.ACLMetaDataMap = ACLMetaDataMap;
    }

    @Override
    public MyDouble calculateFeature(TrainingExample trainingExample, DocumentCtx documentCtx, String s) {
        MyDouble value = new MyDouble(0d);
        Document document = documentCtx.getCitationDoc();
        Annotation sentence = trainingExample.getCitanceSentence();
        ACLMetaData referenceMetaData = ACLMetaDataMap.get(document.getFeatures().get("referenceACLNumber"));
        String year = null;
        String citationMarkerText;
        AnnotationSet citationMarkers = null;
        AnnotationSet analysis = document.getAnnotations("Analysis");

        if (relativePosition == 0) {
            citationMarkers = analysis.get("CitMarker").get(sentence.getStartNode().getOffset(), sentence.getEndNode().getOffset());
        } else if (relativePosition == -1) {
            Integer sentenceID = (Integer) sentence.getFeatures().get("SentenceID");
            if (sentenceID > 1) {
                FeatureMap fm = Factory.newFeatureMap();
                fm.put("SentenceID", sentenceID - 1);
                AnnotationSet as = document.getAnnotations("Information").get("Sentence", fm);
                if (as.size() > 0) {
                    sentence = as.iterator().next();
                    citationMarkers = analysis.get("CitMarker").get(sentence.getStartNode().getOffset(), sentence.getEndNode().getOffset());
                }
            }
        }

        if (citationMarkers != null) {
            Iterator citationMarkersIterator = citationMarkers.iterator();
            while (citationMarkersIterator.hasNext()) {
                Annotation citationMarker = (Annotation) citationMarkersIterator.next();
                AnnotationSet years = analysis.get("Year").get(citationMarker.getStartNode().getOffset(), citationMarker.getEndNode().getOffset());

                if (years.size() > 0) {
                    try {
                        year = document.getContent().getContent(years.iterator().next().getStartNode().getOffset(), years.iterator().next().getEndNode().getOffset()).toString();
                    } catch (InvalidOffsetException e) {
                        e.printStackTrace();
                    }
                } else {
                    AnnotationSet tokensInCitationMarker = document.getAnnotations("Analysis").get("Token").get(citationMarker.getStartNode().getOffset(), citationMarker.getEndNode().getOffset());
                    for (Annotation token : tokensInCitationMarker) {
                        if ((token.getFeatures().get("kind").equals("number")) && (token.getFeatures().get("string").toString().trim().length() == 4)) {
                            year = token.getFeatures().get("string").toString();
                        }
                    }
                }

                if (year != null) {
                    citationMarkerText = citationMarker.getFeatures().get("bibReference").toString();
                    if (citationMarkerText.contains(" and ")) {
                        String fAuthor = citationMarkerText.split(" ")[0];
                        String sAuthor = citationMarkerText.split(" ")[2].replaceAll(",", "");

                        if (referenceMetaData != null && referenceMetaData.getAuthors().size() > 1)
                            if (referenceMetaData.getAuthors().get(0).contains(",") && referenceMetaData.getAuthors().get(1).contains(",")) {
                                if (referenceMetaData.getYear().equals(year) && referenceMetaData.getAuthors().get(0).split(",")[0].trim().equals(fAuthor)
                                        && referenceMetaData.getAuthors().get(1).split(",")[0].trim().equals(sAuthor)) {
                                    value.setValue(1d);
                                }
                            }
                    } else {
                        String fAuthor = citationMarkerText.split(" ")[0];
                        if (referenceMetaData != null && referenceMetaData.getAuthors().size() > 0)
                            if (referenceMetaData.getAuthors().get(0).contains(",")) {
                                if (referenceMetaData.getYear().equals(year) && referenceMetaData.getAuthors().get(0).split(",")[0].trim().equals(fAuthor)) {
                                    value.setValue(1d);
                                }
                                else if(fAuthor.endsWith("s") || fAuthor.endsWith("'s"))
                                {
                                    fAuthor = fAuthor.replaceAll("['s]", "");
                                    if (referenceMetaData.getYear().equals(year) && referenceMetaData.getAuthors().get(0).split(",")[0].trim().equals(fAuthor)) {
                                        value.setValue(1d);
                                    }
                                }
                            }
                    }
                }
            }
        }
        return value;
    }
}
