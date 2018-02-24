package edu.upf.taln.mining.reader;

import edu.upf.taln.dri.lib.exception.DRIexception;
import edu.upf.taln.mining.Utilities;
import gate.*;

import java.io.*;
import java.util.HashMap;
import java.util.List;

public class CollectReferencePapers {
    public static void main(String args[]) throws Exception {
        {
            if (args.length > 0) {
                String workingDir = args[0].trim();
                String pdfextURL = "http://scientmin-be-taln.s.upf.edu:8080/pdfdigest/api/pdfdigest/gate";

                try {
                    // A) IMPORTANT: Initialize Dr. Inventor Framework
                    // A.1) set the local path to the config_file previously downloaded
                    edu.upf.taln.dri.lib.Factory.setDRIPropertyFilePath(workingDir + File.separator + "DRIconfig.properties");
                    // A.2) Initialize the Dr. Inventor Text Mining Framework
                    edu.upf.taln.dri.lib.Factory.initFramework();
                } catch (DRIexception drIexception) {
                    drIexception.printStackTrace();
                }

                for (File referenceFolder : new File(workingDir + File.separator + "datasets").listFiles()) {
                    gate.Document doc = Utilities.extractXMLfromPDFWithPDFDigest(new File(referenceFolder.getPath() + File.separator + referenceFolder.getName() + ".pdf"), pdfextURL);

                    HashMap<String, Document> documents = new HashMap<String, Document>();
                    documents.put(referenceFolder.getName(), doc);

                    AnnotationSet infoAnnotations = doc.getAnnotations("Information");
                    List<Annotation> sentencesAnnotations = doc.getAnnotations("Analysis").get("Sentence").inDocumentOrder();
                    int i=1;
                    for(Annotation sentence: sentencesAnnotations)
                    {
                        FeatureMap Sfm = Factory.newFeatureMap();
                        Sfm.put("SentenceID", i);

                        infoAnnotations.add(sentence.getStartNode().getOffset(), sentence.getEndNode().getOffset(), "Sentence", Sfm);
                        i++;
                    }

                    /*documents = DRInventor.run(documents);
                    documents = Babelfy.run(documents);
                    for (String key : documents.keySet()) {
                        documents.put(key, Utilities.fillDocumentMissingLemmas(documents.get(key)));
                        documents.put(key, Utilities.fillDocumentBabelNetKind(documents.get(key)));
                    }
                    documents = ContextVectors.run(documents, workingDir);
                    for (String key : documents.keySet()) {
                        documents.put(key, Utilities.fillDocumentMissingPOS(documents.get(key)));
                    }
                    documents = Gazetteers.run(documents, workingDir);
                    documents = NGrams.run(documents, workingDir);*/

                    doc = documents.get(referenceFolder.getName());

                    PrintWriter pw = null;
                    // creating the directory succeeded
                    try {
                        pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(referenceFolder.getPath() + File.separator + referenceFolder.getName() + ".xml"), "UTF-8"));
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                    pw.println(doc.toXml());
                    pw.flush();
                    pw.close();
                    Factory.deleteResource(doc);

                    System.out.println(referenceFolder.getName() + " Done ...");

                    /*
                    if (Utilities.saveUrl(new File(referenceFolder.getPath() + File.separator + referenceFolder.getName() + ".pdf").toPath(),
                            new URL("http://aclweb.org/anthology/" + referenceFolder.getName().substring(0, 1) + "/" + referenceFolder.getName().substring(0, referenceFolder.getName().indexOf("-")) + "/" + referenceFolder.getName() + ".pdf"), 10, 10) == 0) {
                        System.out.println("Reference Paper : " + referenceFolder.getName() + " Downloaded ...");
                    } else {
                        System.out.println("Please download citing paper manually: " + referenceFolder.getName());
                    }*/
                }


            } else {
                System.out.println("Please specify main arguments");
            }
        }

    }
}
