package edu.upf.taln.mining.feature.calculator;

import edu.upf.taln.ml.feat.base.FeatCalculator;
import edu.upf.taln.ml.feat.base.MyString;
import edu.upf.taln.mining.feature.context.DocumentCtx;
import edu.upf.taln.mining.reader.TrainingExample;

/**
 * Class: o_n_p, x
 *
 * @author Ahmed AbuRa'ed
 */
public class ClassGetter implements FeatCalculator<String, TrainingExample, DocumentCtx> {
    @Override
    public MyString calculateFeature(TrainingExample obj, DocumentCtx docs, String ClassGetter) {
            MyString Value = new MyString("x");

            if (obj != null && obj.getIsImplicit() != null && obj.getIsImplicit() >= 1) {
                Value.setValue("o_n_p");
            }

            return Value;
    }
}
