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

public class ReferenceAbsBWESim implements FeatCalculator<Double, TrainingExample, DocumentCtx> {
    Word2Vec babelnetvec;

    public ReferenceAbsBWESim(Word2Vec babelnetvec) {
        this.babelnetvec = babelnetvec;
    }

    @Override
    public MyDouble calculateFeature(TrainingExample trainingExample, DocumentCtx documentCtx, String s) {
        MyDouble value = new MyDouble(0d);
        Document rp = documentCtx.getReferenceDoc();
        Document cp = documentCtx.getCitationDoc();

        Annotation citSentence = trainingExample.getCitanceSentence();
        AnnotationSet citSentenceBNEntities = cp.getAnnotations("Babelnet").get("Entity").get(citSentence.getStartNode().getOffset(), citSentence.getEndNode().getOffset());
        List<Annotation> referenceAbstractBNEntities;

        INDArray referenceAbstractAVGVector = Nd4j.zeros(300);
        int referenceAbstractBNEntitiesCount = 0;
        List<Annotation> referenceAbstract;
        if ((rp.getAnnotations("Original markups").get("abstract_text") != null) && ((referenceAbstract = rp.getAnnotations("Original markups").get("abstract_text").inDocumentOrder()) != null) &&
                (referenceAbstract.size() > 0)) {
            referenceAbstractBNEntities = rp.getAnnotations("Babelnet").get("Entity").get(referenceAbstract.get(0).getStartNode().getOffset(),
                    referenceAbstract.get(referenceAbstract.size() - 1).getEndNode().getOffset()).inDocumentOrder();
        } else {
            referenceAbstractBNEntities = rp.getAnnotations("Babelnet").get("Entity").get(0l, 100l).inDocumentOrder();
        }
        //Using Broadcast to sum up all the tokens dimensions (300 dimensions for each token - row: token, column: dimension)
        for (Annotation referenceAbstractBNEntity : referenceAbstractBNEntities) {
            if (referenceAbstractBNEntity.getFeatures().get("kind").toString().equals("entity")) {
                if (babelnetvec.hasWord(referenceAbstractBNEntity.getFeatures().get("synsetID").toString().toLowerCase())) {
                    referenceAbstractAVGVector = referenceAbstractAVGVector.addRowVector(babelnetvec.getWordVectorMatrix(referenceAbstractBNEntity.getFeatures().get("synsetID").toString().toLowerCase()));
                    referenceAbstractBNEntitiesCount++;
                }
            }
        }
        //Calculating the average by dividing each column values by the number of rows - vertical operation to form one dimensional array of the averages of columns: referenceAbstractAVGVector
        referenceAbstractAVGVector = referenceAbstractAVGVector.div(referenceAbstractBNEntitiesCount);

        INDArray citSentenceAVGVector = Nd4j.zeros(300);
        int citSentenceTokensCount = 0;
        //Using Broadcast to sum up all the tokens dimensions (300 dimensions for each token - row: token, column: dimension)
        for (Annotation citSentenceBNEntity : citSentenceBNEntities) {
            if (citSentenceBNEntity.getFeatures().get("kind").toString().equals("entity")) {
                if (babelnetvec.hasWord(citSentenceBNEntity.getFeatures().get("synsetID").toString().toLowerCase())) {
                    citSentenceAVGVector = citSentenceAVGVector.addRowVector(babelnetvec.getWordVectorMatrix(citSentenceBNEntity.getFeatures().get("synsetID").toString().toLowerCase()));
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
