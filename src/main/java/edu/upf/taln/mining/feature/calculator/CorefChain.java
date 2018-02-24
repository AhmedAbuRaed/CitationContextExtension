package edu.upf.taln.mining.feature.calculator;

import edu.upf.taln.mining.feature.context.DocumentCtx;
import edu.upf.taln.mining.reader.ACLMetaData;
import edu.upf.taln.mining.reader.TrainingExample;
import edu.upf.taln.ml.feat.base.FeatCalculator;
import edu.upf.taln.ml.feat.base.MyDouble;
import gate.Annotation;
import gate.AnnotationSet;
import gate.Document;

import java.io.IOException;
import java.util.HashMap;

public class CorefChain implements FeatCalculator<Double, TrainingExample, DocumentCtx> {
    HashMap<String, ACLMetaData> ACLMetaDataMap = new HashMap<String, ACLMetaData>();

    public CorefChain(HashMap<String, ACLMetaData> ACLMetaDataMap) throws IOException {
        this.ACLMetaDataMap = ACLMetaDataMap;
    }

    @Override
    public MyDouble calculateFeature(TrainingExample trainingExample, DocumentCtx documentCtx, String s) {
        MyDouble value = new MyDouble(0d);
        Document document = documentCtx.getCitationDoc();
        Annotation sentence = trainingExample.getCitanceSentence();

        AnnotationSet corefChains = document.getAnnotations("CorefChains");
        AnnotationSet sentenceCoRefChains = corefChains.get(sentence.getStartNode().getOffset(), sentence.getEndNode().getOffset());

        FormalCitation formalCitation = null;
        try {
            formalCitation = new FormalCitation(0, ACLMetaDataMap);
        } catch (IOException e) {
            e.printStackTrace();
        }

        for(Annotation sentenceCoRefChain: sentenceCoRefChains)
        {
            if(sentenceCoRefChain.getFeatures().get("type").equals("PRONOMINAL"))
            {
                AnnotationSet corefChainTypeAS = corefChains.get(sentenceCoRefChain.getType());
                for(Annotation typeAnnotation: corefChainTypeAS)
                {
                    if(typeAnnotation.getFeatures().get("type").equals("NOMINAL")){
                        AnnotationSet sentences = document.getAnnotations("Information").get("Sentence").get(typeAnnotation.getStartNode().getOffset(), typeAnnotation.getEndNode().getOffset());
                        if(sentences.size() > 0)
                        {
                            TrainingExample te = new TrainingExample(sentences.iterator().next(), 0);
                            if(formalCitation.calculateFeature(te, documentCtx, s).getValue() == 1d)
                            {
                                value.setValue(1d);
                            }
                        }
                    }
                }
            }
        }

        return value;
    }
}
