package edu.upf.taln.mining.feature.calculator;

import java.util.*;

import edu.upf.taln.mining.feature.context.DocumentCtx;
import edu.upf.taln.mining.reader.TrainingExample;
import edu.upf.taln.ml.feat.base.FeatCalculator;
import edu.upf.taln.ml.feat.base.MyDouble;
import gate.*;

/**
 * Created by Ahmed on 11/13/15.
 */
public class WorkNouns implements FeatCalculator<Double, TrainingExample, DocumentCtx> {
    Set<String> determinersSet = new HashSet<String>(Arrays.asList("the", "this", "that", "those", "these", "his",
            "her", "their", "such", "previous", "other"));
    Set<String> workNounsSet = new HashSet<String>(Arrays.asList("account", "algorithm", "analysis", "analyses", "approach",
            "approaches", "application", "applications", "architecture", "architectures", "characterization", "characterisation",
            "component", "components", "corpus", "corpora", "design", "designs", "evaluation", "evaluations", "example", "examples",
            "experiment", "experiments", "extension", "extensions", "evaluation", "formalism", "formalisms", "formalization",
            "formalizations", "formalization", "formalizations", "formulation", "formulations", "framework", "frameworks",
            "implementation", "implementations", "investigation", "investigations", "machinery", "machineries", "method",
            "methods", "methodology", "methodologies", "model", "models", "module", "modules", "paper", "papers", "process",
            "processes", "procedure", "procedures", "program", "programs", "prototype", "prototypes", "research", "researches",
            "result", "results", "strategy", "strategies", "system", "systems", "technique", "techniques", "theory", "theories",
            "tool", "tools", "treatment", "treatments", "work", "works"));

    @Override
    public MyDouble calculateFeature(TrainingExample trainingExample, DocumentCtx documentCtx, String s) {
        MyDouble value = new MyDouble(0d);
        Document document = documentCtx.getCitationDoc();
        Annotation sentence = trainingExample.getCitanceSentence();
        List<Annotation> tokens = document.getAnnotations("Analysis").get("Token").get(sentence.getStartNode().getOffset(), sentence.getEndNode().getOffset()).inDocumentOrder();
        for(int i=0; i<tokens.size() -1;i++)
        {
            if(determinersSet.contains(tokens.get(i).getFeatures().get("string").toString().toLowerCase()) &&
                    workNounsSet.contains(tokens.get(i+1).getFeatures().get("string").toString().toLowerCase()))
            {
                value.setValue(1d);
            }
        }
        return value;
    }
}
