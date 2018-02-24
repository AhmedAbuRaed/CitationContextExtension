package edu.upf.taln.mining.reader;

import gate.Annotation;

/**
 * Created by ahmed on 5/8/2016.
 */
public class TrainingExample {

    private Annotation citanceSentence;
    private Integer isImplicit;

    public TrainingExample(Annotation citanceSentence, Integer isImplicit) {
        this.citanceSentence = citanceSentence;
        this.isImplicit = isImplicit;
    }

    public Annotation getCitanceSentence() {
        return citanceSentence;
    }

    public void setCitanceSentence(Annotation citanceSentence) {
        this.citanceSentence = citanceSentence;
    }

    public Integer getIsImplicit() {
        return isImplicit;
    }

    public void setIsImplicit(Integer isImplicit) {
        this.isImplicit = isImplicit;
    }
}
