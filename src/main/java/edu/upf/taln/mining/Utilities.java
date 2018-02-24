package edu.upf.taln.mining;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.upf.taln.dri.lib.demo.Util;
import edu.upf.taln.mining.feature.calculator.*;
import edu.upf.taln.mining.feature.context.DocumentCtx;
import edu.upf.taln.mining.reader.ACLMetaData;
import edu.upf.taln.mining.reader.BabelnetSynset;
import edu.upf.taln.mining.reader.TrainingExample;
import edu.upf.taln.ml.feat.*;
import edu.upf.taln.ml.feat.exception.FeatSetConsistencyException;
import edu.upf.taln.ml.feat.exception.FeatureException;
import gate.*;
import gate.creole.ExecutionException;
import gate.creole.ResourceInstantiationException;
import gate.persist.PersistenceException;
import gate.util.InvalidOffsetException;
import gate.util.OffsetComparator;
import gate.util.persistence.PersistenceManager;
import org.apache.commons.lang3.tuple.Pair;

import org.deeplearning4j.models.word2vec.Word2Vec;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.media.multipart.file.FileDataBodyPart;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import weka.core.converters.ArffSaver;

import javax.net.ssl.SSLHandshakeException;
import javax.ws.rs.client.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Created by Ahmed on 3/15/17.
 */
public class Utilities {
    public static CorpusController application;
    static int babelnetQueryCounter = 0;

    public static void generateCorpusGATEDocs(String referenceACLName, String corpusDir) {
        Document document;

        try {
            String html = "http://cl.awaisathar.com/citation-context-corpus/" + referenceACLName + ".html";
            document = Jsoup.connect(html).maxBodySize(0).get();
            Elements dstPaperData = document.select(".dstPaperData");

            //Reference Paper Data
            String referenceACLNumber = dstPaperData.get(0).childNode(0).attributes().get("text").trim();
            String referenceTitle = dstPaperData.get(0).childNode(1).childNode(0).attributes().get("text");
            String referenceAuthors = dstPaperData.get(0).childNode(2).childNode(0).attributes().get("text");

            //Citing Papers Data
            Elements tables = document.getElementsByTag("table");
            Elements srcPapers = tables.select(".srcPaper");
            int dataNotFoundCounter = 1;
            for (Element srcPaper : srcPapers) {
                StringBuilder paperText = new StringBuilder();
                int paperIndex = 0;
                HashMap<Integer, String> sentenceMap = new HashMap<Integer, String>();
                HashMap<Integer, Pair> sentencePairMap = new HashMap<Integer, Pair>();
                HashMap<Integer, String> sentenceClassMap = new HashMap<Integer, String>();

                Elements srcPaperLines = srcPaper.getElementsByClass("line");


                for (Element srcPaperLine : srcPaperLines) {
                    String classText = srcPaperLine.attributes().get("class");

                    if (!classText.equals("line srcData")) {
                        String[] titleText = srcPaperLine.attributes().get("title").split("\t");
                        sentenceMap.put(Integer.valueOf(titleText[0].split(":")[0]), titleText[1] + "\r\n ");
                        sentenceClassMap.put(Integer.valueOf(titleText[0].split(":")[0]), classText.replaceAll("line", "").trim());
                        sentencePairMap.put(Integer.valueOf(titleText[0].split(":")[0]), Pair.of(paperIndex, paperIndex - 1 + sentenceMap.get(Integer.valueOf(titleText[0].split(":")[0])).length()));
                        paperIndex += (sentenceMap.get(Integer.valueOf(titleText[0].split(":")[0])).length());
                        paperText.append(sentenceMap.get(Integer.valueOf(titleText[0].split(":")[0])));
                    }
                }

                gate.Document doc = Factory.newDocument(paperText.toString());

                AnnotationSet formalAnnotations = doc.getAnnotations("Formal_Citations");
                AnnotationSet informalAnnotations = doc.getAnnotations("InFormal_Citations");
                AnnotationSet excludeAnnotations = doc.getAnnotations("Excluded");
                AnnotationSet infoAnnotations = doc.getAnnotations("Information");

                for (Integer sClass : sentenceClassMap.keySet()) {
                    if (sentenceClassMap.get(sClass).equals("p")) {
                        FeatureMap fm = Factory.newFeatureMap();
                        fm.put("Class", "p");
                        fm.put("SentenceID", sClass);

                        informalAnnotations.add(Long.parseLong(sentencePairMap.get(sClass).getLeft().toString()), Long.parseLong(sentencePairMap.get(sClass).getRight().toString()), "PositiveCitation", fm);
                    } else if (sentenceClassMap.get(sClass).equals("n")) {
                        FeatureMap fm = Factory.newFeatureMap();
                        fm.put("Class", "n");
                        fm.put("SentenceID", sClass);

                        informalAnnotations.add(Long.parseLong(sentencePairMap.get(sClass).getLeft().toString()), Long.parseLong(sentencePairMap.get(sClass).getRight().toString()), "NegativeCitation", fm);
                    } else if (sentenceClassMap.get(sClass).equals("o")) {
                        FeatureMap fm = Factory.newFeatureMap();
                        fm.put("Class", "o");
                        fm.put("SentenceID", sClass);

                        informalAnnotations.add(Long.parseLong(sentencePairMap.get(sClass).getLeft().toString()), Long.parseLong(sentencePairMap.get(sClass).getRight().toString()), "ObjectiveCitation", fm);
                    } else if (sentenceClassMap.get(sClass).equals("pc")) {
                        FeatureMap fm = Factory.newFeatureMap();
                        fm.put("Class", "pc");
                        fm.put("SentenceID", sClass);

                        formalAnnotations.add(Long.parseLong(sentencePairMap.get(sClass).getLeft().toString()), Long.parseLong(sentencePairMap.get(sClass).getRight().toString()), "PositiveCitation", fm);
                    } else if (sentenceClassMap.get(sClass).equals("nc")) {
                        FeatureMap fm = Factory.newFeatureMap();
                        fm.put("Class", "nc");
                        fm.put("SentenceID", sClass);

                        formalAnnotations.add(Long.parseLong(sentencePairMap.get(sClass).getLeft().toString()), Long.parseLong(sentencePairMap.get(sClass).getRight().toString()), "NegativeCitation", fm);
                    } else if (sentenceClassMap.get(sClass).equals("oc")) {
                        FeatureMap fm = Factory.newFeatureMap();
                        fm.put("Class", "oc");
                        fm.put("SentenceID", sClass);

                        formalAnnotations.add(Long.parseLong(sentencePairMap.get(sClass).getLeft().toString()), Long.parseLong(sentencePairMap.get(sClass).getRight().toString()), "ObjectiveCitation", fm);
                    } else if (sentenceClassMap.get(sClass).equals("x")) {
                        FeatureMap fm = Factory.newFeatureMap();
                        fm.put("Class", "x");
                        fm.put("SentenceID", sClass);

                        excludeAnnotations.add(Long.parseLong(sentencePairMap.get(sClass).getLeft().toString()), Long.parseLong(sentencePairMap.get(sClass).getRight().toString()), "Excluded", fm);
                    } else if (sentenceClassMap.get(sClass).equals("xc")) {
                        FeatureMap fm = Factory.newFeatureMap();
                        fm.put("Class", "xc");
                        fm.put("SentenceID", sClass);

                        excludeAnnotations.add(Long.parseLong(sentencePairMap.get(sClass).getLeft().toString()), Long.parseLong(sentencePairMap.get(sClass).getRight().toString()), "Excluded", fm);
                    }
                }

                for (Integer id : sentenceMap.keySet()) {
                    FeatureMap Sfm = Factory.newFeatureMap();
                    Sfm.put("SentenceID", id);

                    infoAnnotations.add(Long.parseLong(sentencePairMap.get(id).getLeft().toString()), Long.parseLong(sentencePairMap.get(id).getRight().toString()), "Sentence", Sfm);
                }

                //Annotate document features with the Reference Paper Data
                FeatureMap referenceFM = Factory.newFeatureMap();
                referenceFM.put("referenceACLNumber", referenceACLNumber);
                referenceFM.put("referenceTitle", referenceTitle);
                referenceFM.put("referenceAuthors", referenceAuthors);
                doc.setFeatures(referenceFM);

                Elements srcData = srcPaper.getElementsByClass("line srcData");
                String citingPaper = srcData.get(0).attributes().get("title").split("\r\n")[0].trim();

                File referenceFolder = new File(corpusDir + File.separator + referenceACLName);
                referenceFolder.mkdir();

                PrintWriter pw = null;

                // creating the directory succeeded
                try {
                    if (!citingPaper.equals("Data not found")) {
                        pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(corpusDir + File.separator + referenceACLName + File.separator +
                                citingPaper + ".xml"), "UTF-8"));
                    } else {
                        pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(corpusDir + File.separator + referenceACLName + File.separator +
                                citingPaper.replaceAll(" ", "") + "-" + dataNotFoundCounter + ".xml"), "UTF-8"));
                        dataNotFoundCounter++;
                    }
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                pw.println(doc.toXml());
                pw.flush();
                pw.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ResourceInstantiationException e) {
            e.printStackTrace();
        } catch (InvalidOffsetException e) {
            e.printStackTrace();
        }
    }

    public static void generateDRInventorEnrichedGATEDocument(gate.Document document, String referenceACLName, String corpusDir) {
        System.out.println(document.getName() + " Start applying DR. Inventer annotations on the document " + document.getName() + "...");
        document = applyDRInventer(document);
        System.out.println(document.getName() + " DR. Inventer annotations applied ...");

        System.out.println(document.getName() + " Start applying Babelnet annotations on the document " + document.getName() + "...");
        document = annotateBabelnetAnnotations(document);
        System.out.println(document.getName() + " Babelnet annotations applied ...");

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
        pw.close();
    }

    public static void generatePreProcessedEnrichedGATEDocument(gate.Document document, String referenceACLName, String corpusDir) {
        System.out.println(referenceACLName + " Start applying Token Lemmas Babelnet Kind Filling on the documents ...");
        document = Utilities.applyTokenLemmasBNKindFilling(document);
        System.out.println(referenceACLName + " Token Lemmas Babelnet Kinds Filled ...");
        System.out.println(referenceACLName + " Start applying Vectors annotations on the documents ...");
        document = Utilities.applyGappVectors(document, corpusDir);
        System.out.println(referenceACLName + " Vectors annotations applied ...");
        System.out.println(referenceACLName + " Start applying CoRefChains on the documents ...");
        document = Utilities.applyCoRefChainsConnectionsonVectors(document);
        System.out.println(referenceACLName + " Done applying CoRefChains on the documents ...");
        System.out.println(referenceACLName + " Start applying Normalized Vectors annotations on the documents ...");
        document = Utilities.applyGappNormalizedVectors(document, corpusDir);
        System.out.println(referenceACLName + " Normalized Vectors annotations applied ...");
        PrintWriter pw = null;

        try {
            pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(corpusDir + File.separator + referenceACLName + File.separator +
                    document.getName().substring(0, document.getName().indexOf("_")) + "PREPROCESSED.xml"), "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        pw.println(document.toXml());
        pw.flush();
        pw.close();
    }

    public static String getPage(String pageURL, Integer N) throws Exception {
        URL url = null;
        try {
            url = new URL(pageURL);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        StringBuffer stringBuffer = new StringBuffer();

        for (int i = 0; i < N; i++) {

            try {
                InputStream inputStream = url.openStream();  // throws an IOException

                int s = 0;

                DataInputStream dataInputStream = new DataInputStream(new BufferedInputStream(inputStream));

                while ((s = dataInputStream.read()) != -1) {
                    stringBuffer.append((char) s);

                }

            } catch (IOException e) {
                continue;
            }

            return stringBuffer.toString();
        }

        throw new Exception("Failed to download after " + N + " attepmts");
    }

    public static gate.Document annotateBabelnetAnnotations(gate.Document document) {

        URL url;
        InputStream inputStream = null;
        DataInputStream dataInputStream;

        AnnotationSet documentSentences = document.getAnnotations("Information").get("Sentence");
        AnnotationSet documentBabelnet = document.getAnnotations("Babelnet");
        documentBabelnet.clear();

        List annotationsList = new ArrayList((AnnotationSet) documentSentences);
        Collections.sort(annotationsList, new OffsetComparator());

        Long start = 0l, end = 0l, size = 0l;

        for (Object documentSentenceAnnotation : annotationsList) {
            try {
                Annotation documentSentence = (Annotation) documentSentenceAnnotation;
                if (((documentSentence.getEndNode().getOffset() - documentSentence.getStartNode().getOffset()) + size <= 1700) &&
                        ((documentSentence.getEndNode().getOffset() - documentSentence.getStartNode().getOffset()) <= 1000) &&
                        (!documentSentence.equals(annotationsList.get(annotationsList.size() - 1)))) {
                    end = documentSentence.getEndNode().getOffset();
                    size = end - start;
                } else {
                    if (documentSentence.equals(annotationsList.get(annotationsList.size() - 1))) {
                        end = documentSentence.getEndNode().getOffset();
                    }
                    if (start == end) {
                        continue;
                    }

                    String babelQuery = null;
                    try {
                        babelQuery = getPage("http://babelfy.io/v1/disambiguate?text=" + URLEncoder.encode(document.getContent().getContent(start, end).toString(), "UTF-8") + "&lang=EN&key=3567f537-c43b-4176-ac95-6f10dc7becef", 5);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    System.out.println("Processing context: " + babelQuery);
                    //System.out.println("Json Response: " + stringBuffer.toString());

                    babelnetQueryCounter++;
                    System.out.println("queryCounter: " + babelnetQueryCounter);

                    ObjectMapper mapper = new ObjectMapper();
                    BabelnetSynset[] synsets = mapper.readValue(babelQuery, BabelnetSynset[].class);

                    for (BabelnetSynset synset : synsets) {
                        BabelnetSynset.TokenFragment tokenFragment = synset.getTokenFragment();
                        BabelnetSynset.CharFragment charFragment = synset.getCharFragment();

                        Long tokenFragmentStart = Long.parseLong(tokenFragment.getStart());
                        Long tokenFragmentEnd = Long.parseLong(tokenFragment.getEnd());
                        Long charFragmentStart = start + Long.parseLong(charFragment.getStart());
                        Long charFragmentEnd = start + Long.parseLong(charFragment.getEnd()) + 1L;

                        FeatureMap fm = Factory.newFeatureMap();
                        fm.put("babelnetURL", synset.getBabelNetURL());
                        fm.put("coherenceScore", synset.getCoherenceScore());
                        fm.put("dbpediaURL", synset.getDBpediaURL());
                        fm.put("globalScore", synset.getGlobalScore());
                        fm.put("numTokens", (tokenFragmentEnd - tokenFragmentStart) + 1);
                        fm.put("score", synset.getScore());
                        fm.put("source", synset.getSource());
                        fm.put("synsetID", synset.getBabelSynsetID());

                        documentBabelnet.add(charFragmentStart, charFragmentEnd, "Entity", fm);
                    }
                    if ((documentSentence.getEndNode().getOffset() - documentSentence.getStartNode().getOffset()) <= 1000) {
                        start = documentSentence.getStartNode().getOffset();
                        end = documentSentence.getEndNode().getOffset();
                        size = end - start;
                    } else {
                        start = documentSentence.getEndNode().getOffset() + 1;
                        end = documentSentence.getEndNode().getOffset() + 1;
                        size = 0l;
                    }

                }

            } catch (InvalidOffsetException e) {
                e.printStackTrace();
            } catch (JsonMappingException e) {
                e.printStackTrace();
            } catch (JsonParseException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return document;
    }

    public static Pair<gate.Document, Integer> annotateBabelnetAnnotations(gate.Document document, Integer queryCounter) {

        URL url;
        InputStream inputStream = null;
        DataInputStream dataInputStream;
        String s;

        AnnotationSet documentSentences = document.getAnnotations("Analysis").get("Sentence");
        AnnotationSet documentBabelnet = document.getAnnotations("Babelnet");
        documentBabelnet.clear();

        List annotationsList = new ArrayList((AnnotationSet) documentSentences);
        Collections.sort(annotationsList, new OffsetComparator());

        Long start = 0l, end = 0l, size = 0l;

        for (Object documentSentenceAnnotation : annotationsList) {
            try {
                Annotation documentSentence = (Annotation) documentSentenceAnnotation;
                if (((documentSentence.getEndNode().getOffset() - documentSentence.getStartNode().getOffset()) + size <= 1700) &&
                        ((documentSentence.getEndNode().getOffset() - documentSentence.getStartNode().getOffset()) <= 1000) &&
                        (!documentSentence.equals(annotationsList.get(annotationsList.size() - 1)))) {
                    end = documentSentence.getEndNode().getOffset();
                    size = end - start;
                } else {
                    if (documentSentence.equals(annotationsList.get(annotationsList.size() - 1))) {
                        end = documentSentence.getEndNode().getOffset();
                    }
                    if (start == end) {
                        continue;
                    }

                    url = new URL("http://babelfy.io/v1/disambiguate?text=" + URLEncoder.encode(document.getContent().getContent(start, end).toString(), "UTF-8") + "&lang=EN&key=3567f537-c43b-4176-ac95-6f10dc7becef");
                    inputStream = url.openStream();         // throws an IOException
                    dataInputStream = new DataInputStream(new BufferedInputStream(inputStream));
                    StringBuffer stringBuffer = new StringBuffer();
                    while ((s = dataInputStream.readLine()) != null) {
                        stringBuffer.append(s);
                    }
                    System.out.println("Processing context: " + document.getContent().getContent(start, end).toString());
                    //System.out.println("Json Response: " + stringBuffer.toString());

                    queryCounter++;
                    System.out.println("queryCounter: " + queryCounter);

                    ObjectMapper mapper = new ObjectMapper();
                    BabelnetSynset[] synsets = mapper.readValue(stringBuffer.toString(), BabelnetSynset[].class);

                    for (BabelnetSynset synset : synsets) {
                        BabelnetSynset.TokenFragment tokenFragment = synset.getTokenFragment();
                        BabelnetSynset.CharFragment charFragment = synset.getCharFragment();

                        Long tokenFragmentStart = Long.parseLong(tokenFragment.getStart());
                        Long tokenFragmentEnd = Long.parseLong(tokenFragment.getEnd());
                        Long charFragmentStart = start + Long.parseLong(charFragment.getStart());
                        Long charFragmentEnd = start + Long.parseLong(charFragment.getEnd()) + 1L;

                        FeatureMap fm = Factory.newFeatureMap();
                        fm.put("babelnetURL", synset.getBabelNetURL());
                        fm.put("coherenceScore", synset.getCoherenceScore());
                        fm.put("dbpediaURL", synset.getDBpediaURL());
                        fm.put("globalScore", synset.getGlobalScore());
                        fm.put("numTokens", (tokenFragmentEnd - tokenFragmentStart) + 1);
                        fm.put("score", synset.getScore());
                        fm.put("source", synset.getSource());
                        fm.put("synsetID", synset.getBabelSynsetID());

                        documentBabelnet.add(charFragmentStart, charFragmentEnd, "Entity", fm);
                    }
                    if ((documentSentence.getEndNode().getOffset() - documentSentence.getStartNode().getOffset()) <= 1000) {
                        start = documentSentence.getStartNode().getOffset();
                        end = documentSentence.getEndNode().getOffset();
                        size = end - start;
                    } else {
                        start = documentSentence.getEndNode().getOffset() + 1;
                        end = documentSentence.getEndNode().getOffset() + 1;
                        size = 0l;
                    }

                }

            } catch (InvalidOffsetException e) {
                e.printStackTrace();
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return Pair.of(document, queryCounter);
    }

    public static HashMap<String, ACLMetaData> loadACLMetaData(String workingDir) {
        HashMap<String, ACLMetaData> loadedACLMetaData = new HashMap<String, ACLMetaData>();
        File ACLMetaDataFile = null;
        ACLMetaDataFile = new File(workingDir + File.separator + "acl-metadata.txt");
        BufferedReader readerACLMetaData = null;
        try {
            String line;
            readerACLMetaData = new BufferedReader(
                    new InputStreamReader(
                            new FileInputStream(ACLMetaDataFile), "UTF-8"));

            ACLMetaData metaData = new ACLMetaData();
            while ((line = readerACLMetaData.readLine()) != null) {
                if (!line.equals("")) {

                    String[] catLine = line.split("=", 2);
                    String cat = catLine[0].trim();
                    String value = catLine[1].trim().replaceAll("\\{|\\}", "");

                    if (cat.equals("id")) {
                        metaData.setId(value);
                    } else if (cat.equals("author")) {
                        ArrayList<String> authors = new ArrayList<String>();
                        for (String author : value.split(";")) {
                            authors.add(author);
                        }

                        metaData.setAuthors(authors);
                    } else if (cat.equals("title")) {
                        metaData.setTitle(value);
                    } else if (cat.equals("venue")) {
                        metaData.setVenue(value);
                    } else if (cat.equals("year")) {
                        metaData.setYear(value);
                    }
                } else {
                    loadedACLMetaData.put(metaData.getId(), metaData);
                    metaData = new ACLMetaData();
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                readerACLMetaData.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return loadedACLMetaData;
    }

    public static HashMap<String, gate.Document> getDocuments(String referenceACLName, String corpusDir, String type) {
        gate.Document doc;
        HashMap<String, gate.Document> documents = new HashMap<String, gate.Document>();
        File refFolder = new File(corpusDir /*+ File.separator + referenceACLName*/);
        try {
            doc = Factory.newDocument(new URL("file:///" + refFolder.getPath() + File.separator + referenceACLName + ".xml"), "UTF-8");
            documents.put(referenceACLName, doc);

        } catch (ResourceInstantiationException e) {
            e.printStackTrace();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        try {
            for (File document : refFolder.listFiles()) {
                if (document.getName().endsWith("_" + type + ".xml")) {
                    doc = Factory.newDocument(new URL("file:///" + document.getPath()), "UTF-8");
                    documents.put(document.getName().substring(0, document.getName().lastIndexOf("_")/*.indexOf("-", document.getName().indexOf("-") + 1)*/), doc);
                }
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (ResourceInstantiationException e) {
            e.printStackTrace();
        }

        return documents;
    }

    public static gate.Document applyDRInventer(gate.Document document) {
        gate.Document processedDocument = document;
        PrintStream out = System.out;
        System.setOut(new PrintStream(new OutputStream() {
            @Override
            public void write(int b) throws IOException {
            }
        }));
        try {
            processedDocument = Util.enrichSentences((gate.Document) document, "Information", "Sentence");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            System.setOut(out);
        }
        return processedDocument;
    }

    public static gate.Document applyTokenLemmasBNKindFilling(gate.Document document) {
        for (Annotation tokenAnnotation : document.getAnnotations("Analysis").get("Token")) {
            FeatureMap Tfm = tokenAnnotation.getFeatures();
            if (!Tfm.containsKey("lemma")) {
                if (Tfm.containsKey("string")) {
                    Tfm.put("lemma", Tfm.get("string"));
                } else {
                    try {
                        String value = String.valueOf(document.getContent().getContent(tokenAnnotation.getStartNode().getOffset(), tokenAnnotation.getEndNode().getOffset()));
                        Tfm.put("lemma", value.toLowerCase());
                    } catch (InvalidOffsetException e) {
                        e.printStackTrace();
                    }
                }
            }

            for (Annotation annotation : document.getAnnotations("Babelnet").get("Entity")) {
                FeatureMap fm = annotation.getFeatures();
                if (!fm.containsKey("kind")) {
                    fm.put("kind", "entity");
                }
            }
        }
        return document;
    }

    public static gate.Document applyGappVectors(gate.Document document, String workingDir) {
        try {
            // load the GAPP
            application = (CorpusController) PersistenceManager.loadObjectFromFile(new File(workingDir + File.separator + "ACLSUMM_VECTORS.gapp"));

            Corpus corpus = null;
            corpus = Factory.newCorpus("");
            corpus.add(document);
            application.setCorpus(corpus);
            application.execute();

            Factory.deleteResource(corpus);

        } catch (IOException e) {
            e.printStackTrace();
        } catch (PersistenceException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (ResourceInstantiationException e) {
            e.printStackTrace();
        }
        return document;
    }

    public static gate.Document applyCoRefChainsConnectionsonVectors(gate.Document document) {
        //Adding CorefChains to the CP Vector_Norms.
        AnnotationSet analysis = document.getAnnotations("Analysis");
        AnnotationSet vectors = analysis.get("Vector");
        AnnotationSet coRefChains = document.getAnnotations("CorefChains");
        ArrayList checkedCoRefChainsTypes = new ArrayList();

        for (Annotation coref : coRefChains) {
            String corefType = coref.getType();

            AnnotationSet coRefChainsTypes = coRefChains.get(corefType);
            FeatureMap NOMINALTypefilter = Factory.newFeatureMap();
            FeatureMap PRONOMINALTypefilter = Factory.newFeatureMap();

            for (Annotation coRefChainsType : coRefChainsTypes) {
                if (!checkedCoRefChainsTypes.contains(corefType)) {
                    NOMINALTypefilter.put("type", "NOMINAL");
                    PRONOMINALTypefilter.put("type", "PRONOMINAL");
                    AnnotationSet SelectedNOMINALTypes = coRefChainsTypes.get(corefType, NOMINALTypefilter);
                    AnnotationSet SelectedPRONOMINALTypes = coRefChainsTypes.get(corefType, PRONOMINALTypefilter);
                    if (SelectedNOMINALTypes.size() > 0 && SelectedPRONOMINALTypes.size() > 0) {
                        for (Annotation SelectedNOMINALType : SelectedNOMINALTypes) {
                            AnnotationSet SelectedNOMINALTypeVectors = vectors.get(SelectedNOMINALType.getStartNode().getOffset(), SelectedNOMINALType.getEndNode().getOffset());
                            if (SelectedNOMINALTypeVectors.size() > 0) {
                                Annotation SelectedNOMINALTypeVector = SelectedNOMINALTypeVectors.iterator().next();
                                for (Annotation SelectedPRONOMINALType : SelectedPRONOMINALTypes) {
                                    AnnotationSet SelectedPRONOMINALTypeVectors = vectors.get(SelectedPRONOMINALType.getStartNode().getOffset(), SelectedPRONOMINALType.getEndNode().getOffset());
                                    if (SelectedPRONOMINALTypeVectors.size() > 0) {
                                        Annotation cpSelectedPRONOMINALTypeVector = SelectedPRONOMINALTypeVectors.iterator().next();
                                        if (SelectedNOMINALTypeVector.getStartNode().getOffset() != cpSelectedPRONOMINALTypeVector.getStartNode().getOffset()) {
                                            AnnotationSet cpNOMINALTypeTokens = analysis.get("Token").get(SelectedNOMINALType.getStartNode().getOffset(),
                                                    SelectedNOMINALType.getEndNode().getOffset());
                                            for (Annotation token : cpNOMINALTypeTokens) {
                                                if (SelectedNOMINALTypeVector.getFeatures().containsKey(token.getFeatures().get("string"))) {
                                                    cpSelectedPRONOMINALTypeVector.getFeatures().put(token.getFeatures().get("string"), SelectedNOMINALTypeVector.getFeatures().get(token.getFeatures().get("string")));
                                                }
                                            }
                                        }
                                    } else {
                                        System.out.println("Could not find the  Vector of PRONOMINALType");
                                    }
                                }
                            } else {
                                System.out.println("Could not find the  Vector of NOMINALType");
                            }
                        }
                    }
                    checkedCoRefChainsTypes.add(corefType);
                }
            }
        }

        return document;
    }

    public static gate.Document applyGappNormalizedVectors(gate.Document document, String workingDir) {
        try {
            // load the GAPP
            application = (CorpusController) PersistenceManager.loadObjectFromFile(new File(workingDir + File.separator + "ACLSUMM_NORMALIZED_VECTORS.gapp"));

            Corpus corpus = null;

            corpus = Factory.newCorpus("");
            corpus.add(document);
            application.setCorpus(corpus);
            application.execute();

            Factory.deleteResource(corpus);

        } catch (IOException e) {
            e.printStackTrace();
        } catch (PersistenceException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (ResourceInstantiationException e) {
            e.printStackTrace();
        }
        return document;
    }

    public static Set<String> detectAcronyms(HashMap<String, gate.Document> documents, HashMap<String, ACLMetaData> ACLMetaDataMap) {
        Set<String> Acronyms = new HashSet<String>();
        for (String key : documents.keySet()) {
            gate.Document document = documents.get(key);
            ACLMetaData referenceMetaData = ACLMetaDataMap.get(document.getFeatures().get("referenceACLNumber"));
            String year = null;
            Iterator sentencesIterator = document.getAnnotations("Information").get("Sentence").iterator();
            while (sentencesIterator.hasNext()) {
                try {
                    Annotation sentence = (Annotation) sentencesIterator.next();

                    FormalCitation formalCitation = new FormalCitation(0, ACLMetaDataMap);
                    TrainingExample trainingExample = new TrainingExample(sentence, 0);
                    DocumentCtx documentCtx = new DocumentCtx(document);

                    if (formalCitation.calculateFeature(trainingExample, documentCtx, "FormalCitation").getValue() == 1) {

                        List<Annotation> tokensList = document.getAnnotations("Analysis").get("Token").get(sentence.getStartNode().getOffset(), sentence.getEndNode().getOffset()).inDocumentOrder();
                        AnnotationSet citationMarkers = document.getAnnotations("Analysis").get("CitMarker").get(sentence.getStartNode().getOffset(), sentence.getEndNode().getOffset());
                        if (citationMarkers != null) {

                            Iterator citationMarkersIterator = citationMarkers.iterator();
                            while (citationMarkersIterator.hasNext()) {
                                Annotation citationMarker = (Annotation) citationMarkersIterator.next();
                                AnnotationSet years = document.getAnnotations("Analysis").get("Year").get(citationMarker.getStartNode().getOffset(), citationMarker.getEndNode().getOffset());
                                if (years.size() > 0) {
                                    try {
                                        year = document.getContent().getContent(years.iterator().next().getStartNode().getOffset(), years.iterator().next().getEndNode().getOffset()).toString();
                                    } catch (InvalidOffsetException e) {
                                        e.printStackTrace();
                                    }

                                    String citationMarkerText = citationMarker.getFeatures().get("bibReference").toString();
                                    if (citationMarkerText.contains(" and ")) {
                                        String fAuthor = citationMarkerText.split(" ")[0];
                                        String sAuthor = citationMarkerText.split(" ")[2].replaceAll(",", "");
                                        if (referenceMetaData.getAuthors().size() > 1) {
                                            if (referenceMetaData.getYear().equals(year) && referenceMetaData.getAuthors().get(0).split(",")[0].trim().equals(fAuthor)
                                                    && referenceMetaData.getAuthors().get(1).split(",")[0].trim().equals(sAuthor)) {
                                                for (int i = 0; i < tokensList.size(); i++) {
                                                    if (tokensList.get(i).getStartNode().getOffset() == citationMarker.getStartNode().getOffset()) {
                                                        int j = i - 1;
                                                        int k = 4;
                                                        while (j >= 0 && k > 0) {
                                                            if (tokensList.get(j).getFeatures().get("string").toString().matches("^[A-Z][A-Z0-9]{2,}$")) {
                                                                Acronyms.add(tokensList.get(j).getFeatures().get("string").toString());
                                                            }
                                                            k--;
                                                            j--;
                                                        }
                                                    } else if (tokensList.get(i).getEndNode().getOffset() == citationMarker.getEndNode().getOffset()) {
                                                        int j = i + 1;
                                                        int k = 4;
                                                        while (j < tokensList.size() && k > 0) {
                                                            if (tokensList.get(j).getFeatures().get("string").toString().matches("^[A-Z][A-Z0-9]{2,}$")) {
                                                                Acronyms.add(tokensList.get(j).getFeatures().get("string").toString());
                                                            }
                                                            k--;
                                                            j++;
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    } else {
                                        String fAuthor = citationMarkerText.split(" ")[0];
                                        if (referenceMetaData.getAuthors().size() > 0) {
                                            if (referenceMetaData.getYear().equals(year) && referenceMetaData.getAuthors().get(0).split(",")[0].equals(fAuthor)) {
                                                for (int i = 0; i < tokensList.size(); i++) {
                                                    if (tokensList.get(i).getStartNode().getOffset() == citationMarker.getStartNode().getOffset()) {
                                                        int j = i - 1;
                                                        int k = 4;
                                                        while (j >= 0 && k > 0) {
                                                            if (tokensList.get(j).getFeatures().get("string").toString().matches("^[A-Z][A-Z0-9]{2,}$")) {
                                                                Acronyms.add(tokensList.get(j).getFeatures().get("string").toString());
                                                            }
                                                            k--;
                                                            j--;
                                                        }
                                                    } else if (tokensList.get(i).getEndNode().getOffset() == citationMarker.getEndNode().getOffset()) {
                                                        int j = i + 1;
                                                        int k = 4;
                                                        while (j < tokensList.size() && k > 0) {
                                                            if (tokensList.get(j).getFeatures().get("string").toString().matches("^[A-Z][A-Z0-9]{2,}$")) {
                                                                Acronyms.add(tokensList.get(j).getFeatures().get("string").toString());
                                                            }
                                                            k--;
                                                            j++;
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return Acronyms;
    }

    public static Set<String> detectLexicalHooks(String workingDirectory, HashMap<String, ACLMetaData> ACLMetaDataMap) {
        Set<String> lexicalHooks = new HashSet<String>();
        HashMap<String, Integer> occurrences = new HashMap<String, Integer>();
        File corpus = new File(workingDirectory + File.separator + "datasets");
        for (File clusterFolder : corpus.listFiles()) {
            for (File file : clusterFolder.listFiles()) {
                if (!file.getName().equals(clusterFolder.getName() + ".xml")) {
                    gate.Document document = null;
                    try {
                        document = Factory.newDocument(new URL("file:///" + file.getPath()));
                    } catch (ResourceInstantiationException e) {
                        e.printStackTrace();
                    } catch (MalformedURLException e) {
                        e.printStackTrace();
                    }
                    ACLMetaData referenceMetaData = ACLMetaDataMap.get(document.getFeatures().get("referenceACLNumber"));
                    String year = null;
                    Iterator sentencesIterator = document.getAnnotations("Information").get("Sentence").iterator();
                    while (sentencesIterator.hasNext()) {
                        try {
                            Annotation sentence = (Annotation) sentencesIterator.next();

                            FormalCitation formalCitation = new FormalCitation(0, ACLMetaDataMap);
                            TrainingExample trainingExample = new TrainingExample(sentence, 0);
                            DocumentCtx documentCtx = new DocumentCtx(document);

                            if (formalCitation.calculateFeature(trainingExample, documentCtx, "FormalCitation").getValue() == 1) {

                                List<Annotation> tokensList = document.getAnnotations("Analysis").get("Token").get(sentence.getStartNode().getOffset(), sentence.getEndNode().getOffset()).inDocumentOrder();
                                AnnotationSet citationMarkers = document.getAnnotations("Analysis").get("CitMarker").get(sentence.getStartNode().getOffset(), sentence.getEndNode().getOffset());
                                if (citationMarkers != null) {

                                    Iterator citationMarkersIterator = citationMarkers.iterator();
                                    while (citationMarkersIterator.hasNext()) {
                                        Annotation citationMarker = (Annotation) citationMarkersIterator.next();
                                        AnnotationSet years = document.getAnnotations("Analysis").get("Year").get(citationMarker.getStartNode().getOffset(), citationMarker.getEndNode().getOffset());
                                        if (years.size() > 0) {
                                            try {
                                                year = document.getContent().getContent(years.iterator().next().getStartNode().getOffset(), years.iterator().next().getEndNode().getOffset()).toString();
                                            } catch (InvalidOffsetException e) {
                                                e.printStackTrace();
                                            }

                                            String citationMarkerText = citationMarker.getFeatures().get("bibReference").toString();
                                            if (citationMarkerText.contains(" and ")) {
                                                String fAuthor = citationMarkerText.split(" ")[0];
                                                String sAuthor = citationMarkerText.split(" ")[2].replaceAll(",", "");

                                                if (referenceMetaData.getAuthors().size() > 1) {
                                                    if (referenceMetaData.getYear().equals(year) && referenceMetaData.getAuthors().get(0).split(",")[0].trim().equals(fAuthor)
                                                            && referenceMetaData.getAuthors().get(1).split(",")[0].trim().equals(sAuthor)) {
                                                        for (int i = 1; i < tokensList.size(); i++) {
                                                            AnnotationSet tokenCitations = document.getAnnotations("Analysis").get("CitMarker").get(tokensList.get(i).getStartNode().getOffset(),
                                                                    tokensList.get(i).getEndNode().getOffset());
                                                            if (tokenCitations.size() == 0) {
                                                                if (tokensList.get(i).getFeatures().get("string").toString().matches("^[A-Z][A-Za-z0-9]{2,}$")
                                                                        && !tokensList.get(i - 1).getFeatures().get("string").toString().equals(".")) {
                                                                    if (occurrences.containsKey(tokensList.get(i).getFeatures().get("string").toString())) {
                                                                        occurrences.put(tokensList.get(i).getFeatures().get("string").toString(), occurrences.get(tokensList.get(i).getFeatures().get("string").toString()) + 1);
                                                                    } else {
                                                                        occurrences.put(tokensList.get(i).getFeatures().get("string").toString(), 1);
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            } else {
                                                String fAuthor = citationMarkerText.split(" ")[0];
                                                if (referenceMetaData.getAuthors().size() > 0) {
                                                    if (referenceMetaData.getYear().equals(year) && referenceMetaData.getAuthors().get(0).split(",")[0].equals(fAuthor)) {
                                                        for (int i = 1; i < tokensList.size(); i++) {
                                                            AnnotationSet tokenCitations = document.getAnnotations("Analysis").get("CitMarker").get(tokensList.get(i).getStartNode().getOffset(),
                                                                    tokensList.get(i).getEndNode().getOffset());
                                                            if (tokenCitations.size() == 0) {
                                                                if (tokensList.get(i).getFeatures().get("string").toString().matches("^[A-Z][A-Za-z0-9]{2,}$")
                                                                        && !tokensList.get(i - 1).getFeatures().get("string").toString().equals(".")) {
                                                                    if (occurrences.containsKey(tokensList.get(i).getFeatures().get("string").toString())) {
                                                                        occurrences.put(tokensList.get(i).getFeatures().get("string").toString(), occurrences.get(tokensList.get(i).getFeatures().get("string").toString()) + 1);
                                                                    } else {
                                                                        occurrences.put(tokensList.get(i).getFeatures().get("string").toString(), 1);
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
					     gate.Factory.deleteResource(document);
                }
            }
            int max = 0;
            String LH = "";
            for (String key : occurrences.keySet()) {
                if (occurrences.get(key) > max) {
                    LH = key;
                    max = occurrences.get(key);
                }
            }
            lexicalHooks.add(LH);
        }
        return lexicalHooks;
    }

    public static gate.Document annotateWord2VecAnnotations(gate.Document document, Word2Vec word2Vec, String annotationSetName) {
        Iterator tokensIterator = document.getAnnotations("Analysis").get("Token").iterator();
        AnnotationSet annotationSet = document.getAnnotations("Word2Vec");

        while (tokensIterator.hasNext()) {
            Annotation token = (Annotation) tokensIterator.next();
            if (token.getFeatures().get("kind").toString().equals("word")) {

                if (word2Vec.hasWord(token.getFeatures().get("string").toString().toLowerCase())) {
                    double[] wordVector = word2Vec.getWordVector(token.getFeatures().get("string").toString().toLowerCase());
                    if (wordVector != null) {
                        FeatureMap fm = Factory.newFeatureMap();
                        StringBuilder vector = new StringBuilder();
                        for (int i = 0; i < wordVector.length; i++) {
                            vector.append(wordVector[i]);
                            vector.append(" ");
                        }
                        fm.put(token.getFeatures().get("string").toString().toLowerCase(), vector.toString().trim());
                        try {
                            annotationSet.add(token.getStartNode().getOffset(), token.getEndNode().getOffset(), annotationSetName, fm);
                        } catch (InvalidOffsetException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }

        return document;
    }

    public static gate.Document fillDocumentMissingLemmas(gate.Document document) {
        for (Annotation annotation : document.getAnnotations("Analysis").get("Token")) {
            FeatureMap fm = annotation.getFeatures();
            if (!fm.containsKey("lemma")) {
                if (fm.containsKey("string")) {
                    fm.put("lemma", fm.get("string"));
                } else {
                    try {
                        String value = String.valueOf(document.getContent().getContent(annotation.getStartNode().getOffset(), annotation.getEndNode().getOffset()));
                        fm.put("lemma", value.toLowerCase());
                    } catch (InvalidOffsetException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return document;
    }

    public static gate.Document fillDocumentBabelNetKind(gate.Document document) {
        for (Annotation annotation : document.getAnnotations("Babelnet").get("Entity")) {
            FeatureMap fm = annotation.getFeatures();
            if (!fm.containsKey("kind")) {
                fm.put("kind", "entity");
            }
        }
        return document;
    }

    public static gate.Document fillDocumentMissingPOS(gate.Document document) {
        for (Annotation annotation : document.getAnnotations("Analysis").get("Token")) {
            FeatureMap fm = annotation.getFeatures();
            if (!fm.containsKey("category")) {
                fm.put("category", "-LRB-");
            }
        }
        return document;
    }

    public static void exportGATEDocuments(HashMap<String, gate.Document> processedRCDocuments, String
            rfolder, String outputFolder, String extension) {
        PrintWriter pw = null;
        File ref = new File(outputFolder + File.separator + rfolder);

        // attempt to create the directory here
        //ref.mkdirs();

        for (String docKey : processedRCDocuments.keySet()) {
            if (ref.exists()) {
                // creating the directory succeeded
                try {
                    pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(outputFolder + File.separator
                            + rfolder + "/" + docKey + "_" + extension + ".xml"), "UTF-8"));
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                pw.println(processedRCDocuments.get(docKey).toXml());
                pw.flush();
                pw.close();
                Factory.deleteResource(processedRCDocuments.get(docKey));
            } else {
                // creating the directory failed
                System.out.println("failed trying to create the directory");
            }
        }

    }

    public static Set<String> getStopWordsSet(String workingDir) {
        Set<String> stopWords = new HashSet<String>();
        BufferedReader readerStopWords = null;
        try {
            String line;
            readerStopWords = new BufferedReader(
                    new InputStreamReader(
                            new FileInputStream(workingDir + File.separator + "full-stop-words.lst"), "UTF-8"));
            while ((line = readerStopWords.readLine()) != null) {
                if (!line.equals("")) {
                    stopWords.add(line.toLowerCase().trim());
                }
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return stopWords;
    }

    public static FeatureSet<TrainingExample, DocumentCtx> generateFeatureSet(HashMap<String, gate.Document> documents,
                                                                              HashMap<String, ACLMetaData> metaDataACLMap,
                                                                              Word2Vec gvec, Word2Vec aclvec, Word2Vec bnvec,
                                                                              Set<String> stopWords, Set<String> acronyms, Set<String> lexicalHooks) {

        FeatureSet<TrainingExample, DocumentCtx> featSet = new FeatureSet<TrainingExample, DocumentCtx>();

        // Adding document identifier
        try {
            featSet.addFeature(new NumericW<TrainingExample, DocumentCtx>("FORMAL_CITATION0", new FormalCitation(0, metaDataACLMap)));
            featSet.addFeature(new NumericW<TrainingExample, DocumentCtx>("FORMAL_CITATIONm1", new FormalCitation(-1, metaDataACLMap)));
            featSet.addFeature(new NumericW<TrainingExample, DocumentCtx>("AUTHOR_NAME", new AuthorName()));
            featSet.addFeature(new NumericW<TrainingExample, DocumentCtx>("ACRONYMS", new Acronyms(acronyms)));
            featSet.addFeature(new NumericW<TrainingExample, DocumentCtx>("WORK_NOUNS", new WorkNouns()));
            featSet.addFeature(new NumericW<TrainingExample, DocumentCtx>("PRONOUNS", new Pronouns()));
            featSet.addFeature(new NumericW<TrainingExample, DocumentCtx>("CONNECTORS", new Connectors()));
            featSet.addFeature(new NumericW<TrainingExample, DocumentCtx>("SECTION_MARKERS0", new SectionMarkers(0)));
            featSet.addFeature(new NumericW<TrainingExample, DocumentCtx>("SECTION_MARKERSm1", new SectionMarkers(-1)));
            featSet.addFeature(new NumericW<TrainingExample, DocumentCtx>("SECTION_MARKERp1", new SectionMarkers(1)));
            featSet.addFeature(new NumericW<TrainingExample, DocumentCtx>("CITATION_LISTS", new CitationLists(metaDataACLMap)));
            featSet.addFeature(new NumericW<TrainingExample, DocumentCtx>("LEXICAL_HOOKS", new LexicalHooks(lexicalHooks)));

            featSet.addFeature(new NumericW<TrainingExample, DocumentCtx>("CauseEffectExis", new CauseEffectExis()));
            featSet.addFeature(new NumericW<TrainingExample, DocumentCtx>("CorefChain", new CorefChain(metaDataACLMap)));
            featSet.addFeature(new NumericW<TrainingExample, DocumentCtx>("FormalCitNumber", new FormalCitNumber()));
            featSet.addFeature(new NumericW<TrainingExample, DocumentCtx>("GAZActionCount", new GAZActionCount()));
            featSet.addFeature(new NumericW<TrainingExample, DocumentCtx>("GAZConceptCount", new GAZConceptCount()));
            featSet.addFeature(new NumericW<TrainingExample, DocumentCtx>("HighestProbFacet", new HighestProbFacet()));
            featSet.addFeature(new NumericW<TrainingExample, DocumentCtx>("PositionDistance", new PositionDistance(metaDataACLMap)));
            featSet.addFeature(new NumericW<TrainingExample, DocumentCtx>("ReferenceAbsBCVSim", new ReferenceAbsBCVSim()));
            featSet.addFeature(new NumericW<TrainingExample, DocumentCtx>("ReferenceAbsBWESim", new ReferenceAbsBWESim(bnvec)));
            featSet.addFeature(new NumericW<TrainingExample, DocumentCtx>("ReferenceAbsGNWWESim", new ReferenceAbsGNWWESim(gvec)));
            featSet.addFeature(new NumericW<TrainingExample, DocumentCtx>("ReferenceAbsWCVSim", new ReferenceAbsWCVSim()));
            featSet.addFeature(new NumericW<TrainingExample, DocumentCtx>("ReferenceAbsWWESim", new ReferenceAbsWWESim(aclvec)));
            featSet.addFeature(new NumericW<TrainingExample, DocumentCtx>("TitleTokens", new TitleTokens(stopWords)));

            featSet.addFeature(new StringW<TrainingExample, DocumentCtx>("SENTENCELEMMAS_STRING", new SentenceNGramsStrings("LemmasNGrams", "1-gram")));
            featSet.addFeature(new StringW<TrainingExample, DocumentCtx>("SENTENCEBIGRAMLEMMAS_STRING", new SentenceNGramsStrings("LemmasNGrams", "2-gram")));
            featSet.addFeature(new StringW<TrainingExample, DocumentCtx>("SENTENCETRIGRAMLEMMAS_STRING", new SentenceNGramsStrings("LemmasNGrams", "3-gram")));

            featSet.addFeature(new StringW<TrainingExample, DocumentCtx>("SENTENCEPOS_STRING", new SentenceNGramsStrings("POSNGrams", "1-gram")));
            featSet.addFeature(new StringW<TrainingExample, DocumentCtx>("SENTENCEBIGRAMPOS_STRING", new SentenceNGramsStrings("POSNGrams", "2-gram")));
            featSet.addFeature(new StringW<TrainingExample, DocumentCtx>("SENTENCETRIGRAMPOS_STRING", new SentenceNGramsStrings("POSNGrams", "3-gram")));

            Set<String> classValues = new HashSet<String>();

            classValues.add("o_n_p");
            classValues.add("x");

            // Class feature (lasts)
            featSet.addFeature(new NominalW<TrainingExample, DocumentCtx>("class", classValues, new ClassGetter()));

        } catch (FeatureException e) {
            System.out.println("Error instantiating feature generation template.");
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return featSet;
    }

    public static void FeatureSetToARFF(FeatureSet<TrainingExample, DocumentCtx> featureSet, String outputPath, String outputInstancesType, String version) {
        try {
            ArffSaver saver = new ArffSaver();
            saver.setInstances(FeatUtil.wekaInstanceGeneration(featureSet, outputInstancesType + " LASTUS_v_" + version));
            saver.setFile(new File(outputPath + File.separator + "implicitCitationsDetection_" + outputInstancesType + "_v_" + version + ".arff"));
            saver.writeBatch();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (FeatSetConsistencyException e) {
            e.printStackTrace();
        }
    }

    /**
     * Downloads from a (http/https) URL and saves to a file.
     * Does not consider a connection error an Exception. Instead it returns:
     * <p>
     * 0=ok
     * 1=connection interrupted, timeout (but something was read)
     * 2=not found (FileNotFoundException) (404)
     * 3=server error (500...)
     * 4=could not connect: connection timeout (no internet?) java.net.SocketTimeoutException
     * 5=could not connect: (server down?) java.net.ConnectException
     * 6=could not resolve host (bad host, or no internet - no dns)
     * 7=small content probably html page instead of PDF
     * 8=content length less than threshold
     *
     * @param file               File to write. Parent directory will be created if necessary
     * @param url                http/https url to connect
     * @param secsConnectTimeout Seconds to wait for connection establishment
     * @param secsReadTimeout    Read timeout in seconds - trasmission will abort if it freezes more than this
     * @return See above
     * @throws IOException Only if URL is malformed or if could not create the file
     */
    public static int saveUrl(final Path file, final URL url,
                              int secsConnectTimeout, int secsReadTimeout) throws IOException {
        Files.createDirectories(file.getParent()); // make sure parent dir exists , this can throw exception
        URLConnection conn = url.openConnection(); // can throw exception if bad url
        if (secsConnectTimeout > 0) conn.setConnectTimeout(secsConnectTimeout * 1000);
        if (secsReadTimeout > 0) conn.setReadTimeout(secsReadTimeout * 1000);
        int ret = 0;
        boolean somethingRead = false;

        try (InputStream is = conn.getInputStream()) {
            if (conn.getContentLengthLong() < 8192) {
                return 8;
            }
            try (BufferedInputStream in = new BufferedInputStream(is); OutputStream fout = Files
                    .newOutputStream(file)) {
                final byte data[] = new byte[8192];
                int count;
                while ((count = in.read(data)) > 0) {
                    somethingRead = true;
                    fout.write(data, 0, count);
                }
            }
        } catch (SSLHandshakeException ssl) {
            return -1;
        } catch (java.io.IOException e) {
            int httpcode = 999;
            try {
                httpcode = ((HttpURLConnection) conn).getResponseCode();
            } catch (Exception ee) {
                return -1;
            }
            if (somethingRead && e instanceof java.net.SocketTimeoutException) ret = 1;
            else if (e instanceof FileNotFoundException && httpcode >= 400 && httpcode < 500) ret = 2;
            else if (httpcode >= 400 && httpcode < 600) ret = 3;
            else if (e instanceof java.net.SocketTimeoutException) ret = 4;
            else if (e instanceof java.net.ConnectException) ret = 5;
            else if (e instanceof java.net.UnknownHostException) ret = 6;
            else /*throw e*/ return -1;
        } catch (Exception ex) {
            return -1;
        }
        return ret;
    }

    public static gate.Document extractXMLfromPDFWithPDFDigest(File filePath, String pdfextURL) throws Exception {
        // local variables
        org.glassfish.jersey.client.ClientConfig clientConfig = null;
        Client client = null;
        WebTarget webTarget = null;
        Invocation.Builder invocationBuilder = null;
        Response response = null;
        FileDataBodyPart fileDataBodyPart = null;
        FormDataMultiPart formDataMultiPart = null;
        int responseCode;
        String responseMessageFromServer = null;
        String responseString = null;

        try {
            // invoke service after setting necessary parameters
            clientConfig = new org.glassfish.jersey.client.ClientConfig();
            clientConfig.register(MultiPartFeature.class);
            client = ClientBuilder.newClient(clientConfig);
            System.out.println("pdfextURL: " + pdfextURL);

            String pdfextURL_service = pdfextURL;
            System.out.println("pdfextURL_service: " + pdfextURL_service);
            webTarget = client.target(pdfextURL_service);

            // set file upload values
            fileDataBodyPart = new FileDataBodyPart("file", filePath, MediaType.APPLICATION_OCTET_STREAM_TYPE);
            formDataMultiPart = new FormDataMultiPart();
            formDataMultiPart.bodyPart(fileDataBodyPart);

            // invoke service
            invocationBuilder = webTarget.request();
            //          invocationBuilder.header("Authorization", "Basic " + authorization);
            response = invocationBuilder.post(Entity.entity(formDataMultiPart, MediaType.MULTIPART_FORM_DATA));

            // get response code
            responseCode = response.getStatus();
            System.out.println("Response code: " + responseCode);

            if (response.getStatus() != 200) {
                System.out.println("ERROR: Failed with HTTP error code : " + responseCode);
            }

            // get response message
            responseMessageFromServer = response.getStatusInfo().getReasonPhrase();
            System.out.println("ResponseMessageFromServer: " + responseMessageFromServer);

            // get response string
            responseString = response.readEntity(String.class);
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            // release resources, if any
            fileDataBodyPart.cleanup();
            formDataMultiPart.cleanup();
            formDataMultiPart.close();
            response.close();
            client.close();
            if (responseString == null || responseString.equals("")) {
                return null;
            }
        }
        return gate.Factory.newDocument(responseString);
    }

    public static FeatureMap combineNormalizedVectors(FeatureMap normalizedVector1, FeatureMap
            normalizedVector2) {
        FeatureMap combineNormalizedVector = Factory.newFeatureMap();
        for (Object key : normalizedVector1.keySet()) {
            if (normalizedVector2.containsKey(key)) {
                combineNormalizedVector.put(key, String.valueOf((new Double(normalizedVector1.get(key).toString()) + new Double(normalizedVector2.get(key).toString())) / 2.0));
            } else {
                combineNormalizedVector.put(key, String.valueOf((new Double(normalizedVector1.get(key).toString()) / 2.0)));
            }
        }

        for (Object key : normalizedVector2.keySet()) {
            if (!normalizedVector1.containsKey(key)) {
                combineNormalizedVector.put(key, String.valueOf((new Double(normalizedVector2.get(key).toString()) / 2.0)));
            }
        }
        return combineNormalizedVector;
    }
}
