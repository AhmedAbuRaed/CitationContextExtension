package edu.upf.taln.mining;

import gate.*;
import gate.creole.ResourceInstantiationException;
import gate.util.GateException;
import gate.util.InvalidOffsetException;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by Ahmed on 3/28/17.
 */
public class Test {
    static public void main(String[] args) {
        //use reference ACL ID J96-2004 for testing
        /*String referenceACLName = args[1];
        String corpusDir = args[2];*/

        /*HashMap<String, gate.Document> documents = Utilities.getDocuments(referenceACLName, corpusDir, "RAW");
        Utilities.EnrichGATEdocument(documents.get("W99-0508"), referenceACLName, corpusDir);*/
        /*
        HashMap<String, gate.Document> richDocuments = Utilities.getDocuments(referenceACLName, corpusDir, "RICH");
        Document document = annotateBabelnetAnnotations(richDocuments.get("W99-0508"));
        richDocuments.put("W99-0508", document);

        PrintWriter pw = null;

        try {
            pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(corpusDir + File.separator + referenceACLName + File.separator +
                    document.getName().substring(0, document.getName().indexOf("_")) + "_RICH.xml"), "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        pw.println(document.toXml());
        pw.flush();
        pw.close();*/

        try {
            Gate.init();
        } catch (GateException e) {
            e.printStackTrace();
        }
        /*
        List<String> references = Arrays.asList("A92-1018", "C98-2122", "D07-1031", "J90-1003", "J93-1007", "J96-2004", "N03-1003", "N04-1035", "N06-1020", "P02-1053", "P04-1015", "P04-1035", "P04-1041", "P05-1045", "P07-1033", "P90-1034", "W02-1011", "W04-1013", "W05-0909", "W06-1615");

        File referencesFolder = new File("D:\\Research\\UPF\\Projects\\CitationContextExtension\\datasets");

        for(String ref: references)
        {
            Utilities.generateCorpusGATEDocs(ref, referencesFolder.getPath());
        }*/
        try {
            List<Annotation> referenceAbstract;
            Document document = Factory.newDocument(new URL("file:///D:\\Research\\UPF\\Projects\\CitationContextExtension\\datasets\\J96-2004/J96-2004.xml"));
            if ((document.getAnnotations("Original markups").get("abstract_text") != null)
                    && ((referenceAbstract = document.getAnnotations("Original markups").get("abstract_text").inDocumentOrder()) != null)
                    && (referenceAbstract.size() > 0)) {
                System.out.print("Ok");
            }
        } catch (ResourceInstantiationException e) {
            e.printStackTrace();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }


    }
}
