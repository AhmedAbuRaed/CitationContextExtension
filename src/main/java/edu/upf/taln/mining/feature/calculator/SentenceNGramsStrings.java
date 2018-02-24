package edu.upf.taln.mining.feature.calculator;

import edu.upf.taln.ml.feat.base.FeatCalculator;
import edu.upf.taln.ml.feat.base.MyString;
import edu.upf.taln.mining.feature.context.DocumentCtx;
import edu.upf.taln.mining.reader.TrainingExample;
import gate.Annotation;
import gate.Document;

/**
 * Created by ahmed on 7/8/16.
 */
public class SentenceNGramsStrings implements FeatCalculator<String, TrainingExample, DocumentCtx> {
    String nGramsASName;
    String nGramsTypeASName;

    public SentenceNGramsStrings(String nGramsASName, String nGramsTypeASName) {
        this.nGramsASName = nGramsASName;
        this.nGramsTypeASName = nGramsTypeASName;
    }

    @Override
    public MyString calculateFeature(TrainingExample obj, DocumentCtx docs, String SentenceLemmasString) {
        MyString value = new MyString("");
        String val = "";
        Document cp = null;

        try {
            cp = docs.getCitationDoc();

            Annotation citSentence = obj.getCitanceSentence();

            for (Annotation annotation : cp.getAnnotations(nGramsASName).get(nGramsTypeASName).get(citSentence.getStartNode().getOffset(), citSentence.getEndNode().getOffset())) {
                val = val + " " + annotation.getFeatures().get("string").toString().replaceAll(" ", "_");
            }

            value.setValue(val);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return value;
    }
}
