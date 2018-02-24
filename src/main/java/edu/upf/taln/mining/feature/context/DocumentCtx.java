package edu.upf.taln.mining.feature.context;

import gate.Document;

/**
 * Created by ahmed on 5/7/2016.
 */
public class DocumentCtx {
    private Document cp;
    private Document rp;

    public DocumentCtx(Document cpDocument, Document rpDocument) {
        this.setCitationDoc(cpDocument);
        this.setReferenceDoc(rpDocument);
    }

    public DocumentCtx(Document cpDocument) {
        this.setCitationDoc(cpDocument);
    }

    public Document getCitationDoc() {
        return cp;
    }

    public void setCitationDoc(Document cp) {
        this.cp = cp;
    }

    public Document getReferenceDoc() {
        return rp;
    }

    public void setReferenceDoc(Document rp) {
        this.rp = rp;
    }
}
