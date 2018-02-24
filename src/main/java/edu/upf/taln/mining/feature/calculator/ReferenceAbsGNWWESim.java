package edu.upf.taln.mining.feature.calculator;

import edu.upf.taln.mining.feature.context.DocumentCtx;
import edu.upf.taln.mining.reader.TrainingExample;
import edu.upf.taln.ml.feat.base.FeatCalculator;
import edu.upf.taln.ml.feat.base.MyDouble;
import gate.Annotation;
import gate.AnnotationSet;
import gate.Document;
import org.deeplearning4j.models.word2vec.Word2Vec;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.ops.transforms.Transforms;

import java.util.List;

public class ReferenceAbsGNWWESim implements FeatCalculator<Double, TrainingExample, DocumentCtx> {
    Word2Vec gvec;

    public ReferenceAbsGNWWESim(Word2Vec gvec) {
        this.gvec = gvec;
    }

    @Override
    public MyDouble calculateFeature(TrainingExample trainingExample, DocumentCtx documentCtx, String s) {
        MyDouble value = new MyDouble(0d);
        Document rp = documentCtx.getReferenceDoc();
        Document cp = documentCtx.getCitationDoc();

        Annotation citSentence = trainingExample.getCitanceSentence();
        AnnotationSet citSentenceTokens = cp.getAnnotations("Original markups").get("Token").get(citSentence.getStartNode().getOffset(), citSentence.getEndNode().getOffset());
        List<Annotation> referenceAbstractTokens;

        INDArray referenceAbstractAVGVector = Nd4j.zeros(300);
        int referenceAbstractTokensCount = 0;
        List<Annotation> referenceAbstract;
        if ((rp.getAnnotations("Original markups").get("abstract_text") != null) && ((referenceAbstract = rp.getAnnotations("Original markups").get("abstract_text").inDocumentOrder()) != null) &&
                (referenceAbstract.size() > 0)) {
            referenceAbstractTokens = rp.getAnnotations("Original markups").get("Token").get(referenceAbstract.get(0).getStartNode().getOffset(),
                    referenceAbstract.get(referenceAbstract.size() - 1).getEndNode().getOffset()).inDocumentOrder();
        } else {
            referenceAbstractTokens = rp.getAnnotations("Original markups").get("Token").get(0l, 100l).inDocumentOrder();
        }
        //Using Broadcast to sum up all the tokens dimensions (300 dimensions for each token - row: token, column: dimension)
        for (Annotation referenceAbstractToken : referenceAbstractTokens) {
            if (referenceAbstractToken.getFeatures().get("kind").toString().equals("word")) {

                if (gvec.hasWord(referenceAbstractToken.getFeatures().get("string").toString().toLowerCase())) {
                    referenceAbstractAVGVector = referenceAbstractAVGVector.addRowVector(gvec.getWordVectorMatrix(referenceAbstractToken.getFeatures().get("string").toString().toLowerCase()));
                    referenceAbstractTokensCount++;
                }

            }
        }
        //Calculating the average by dividing each column values by the number of rows - vertical operation to form one dimensional array of the averages of columns: referenceAbstractAVGVector
        referenceAbstractAVGVector = referenceAbstractAVGVector.div(referenceAbstractTokensCount);

        INDArray citSentenceAVGVector = Nd4j.zeros(300);
        int citSentenceTokensCount = 0;
        //Using Broadcast to sum up all the tokens dimensions (300 dimensions for each token - row: token, column: dimension)
        for (Annotation citSentenceToken : citSentenceTokens) {
            if (citSentenceToken.getFeatures().get("kind").toString().equals("word")) {
                if (gvec.hasWord(citSentenceToken.getFeatures().get("string").toString().toLowerCase())) {
                    citSentenceAVGVector = citSentenceAVGVector.addRowVector(gvec.getWordVectorMatrix(citSentenceToken.getFeatures().get("string").toString().toLowerCase()));
                    citSentenceTokensCount++;
                }
            }
        }
        //Calculating the average by dividing each column values by the number of rows - vertical operation to form one dimensional array of the averages of columns: citSentenceAVGVector
        citSentenceAVGVector = citSentenceAVGVector.div(citSentenceTokensCount);

        value.setValue(Transforms.cosineSim(citSentenceAVGVector, referenceAbstractAVGVector));

        return value;
    }
}
