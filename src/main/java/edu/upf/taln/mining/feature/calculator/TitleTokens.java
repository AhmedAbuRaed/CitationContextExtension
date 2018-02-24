package edu.upf.taln.mining.feature.calculator;

import edu.upf.taln.mining.feature.context.DocumentCtx;
import edu.upf.taln.mining.reader.TrainingExample;
import edu.upf.taln.ml.feat.base.FeatCalculator;
import edu.upf.taln.ml.feat.base.MyDouble;
import gate.Annotation;
import gate.Document;

import java.util.Set;

public class TitleTokens implements FeatCalculator<Double, TrainingExample, DocumentCtx> {

    // Stop-words to filter out.
    private final Set<String> stopWords;

    public TitleTokens(Set<String> stopWords) {
        this.stopWords = stopWords;
    }

    @Override
    public MyDouble calculateFeature(TrainingExample trainingExample, DocumentCtx documentCtx, String s) {
        MyDouble value = new MyDouble(0d);
        Document document = documentCtx.getCitationDoc();
        Annotation sentence = trainingExample.getCitanceSentence();
        String[] referenceTitleTokens = new String[0];
        Double count = 0d;

        if (document.getFeatures().get("referenceTitle") != null) {
            referenceTitleTokens = document.getFeatures().get("referenceTitle").toString().split(" ");
        }

        for (String referenceToken : referenceTitleTokens) {
            referenceToken = referenceToken.replaceAll("[:;]", "");
            if (!stopWords.contains(referenceToken)) {
                for (Annotation sentenceToken : document.getAnnotations("Analysis").get("Token").get(sentence.getStartNode().getOffset(), sentence.getEndNode().getOffset())) {
                    if ((!stopWords.contains(sentenceToken.getFeatures().get("string").toString().toLowerCase())) && (referenceToken.trim().toLowerCase().equals(sentenceToken.getFeatures().get("string").toString().toLowerCase()))) {
                        count++;
                    }
                }
            }
        }
        value.setValue(count);

        return value;
    }
}
