package org.neuinfo.foundry.consumers.jms.consumers.jta;

import com.google.gson.Gson;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.neuinfo.foundry.common.util.LRUCache;
import org.neuinfo.foundry.common.util.Utils;
import org.semanticweb.owlapi.model.*;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;


public class KeywordAnalyzer {

    private OWLOntologyManager manager;
    private OWLDataFactory df;
    private OWLOntology cinergi, extensions;
    private LRUCache<String, Vocab> vocabCache = new LRUCache<String, Vocab>(5000);
    private Vocab nilVocab = new Vocab();
    private List<Output> output;
    private List<String> stoplist;
    private List<String> nullIRIs;
    private Gson gson;
    private LinkedHashMap<String, IRI> exceptionMap;
    private int counter;
    private NLPHelper nlpHelper;
    private String SERVER_URL = "http://tikki.neuinfo.org:9000";

    public KeywordAnalyzer(OWLOntologyManager manager, OWLDataFactory df, OWLOntology ont,
                           OWLOntology extensions, Gson gson, List<String> stoplist,
                           LinkedHashMap<String, IRI> exceptionMap, List<String> nullIRIs, String serviceURL) throws IOException {
        output = new ArrayList<Output>();
        this.manager = manager;
        this.df = df;
        this.extensions = extensions;
        this.gson = gson;
        this.stoplist = stoplist;
        this.exceptionMap = exceptionMap;
        this.nullIRIs = nullIRIs;
        counter = 0;

        // IBO
        this.nlpHelper = new NLPHelper();
        if (serviceURL != null) {
            SERVER_URL = serviceURL;
        }
    }

    public List<Output> getOutput() {
        return output;
    }


    public List<Keyword> findKeywords(Document doc) throws UnsupportedEncodingException {
        StringBuilder sb = new StringBuilder(2048);
        if (!Utils.isEmpty(doc.getTitle())) {
            sb.append(doc.getTitle()).append(' ');
        }
        if (!Utils.isEmpty(doc.getText())) {
            sb.append(doc.getText());
        }
        String text = sb.toString().trim();
        ArrayList<Keyword> keywords = new ArrayList<Keyword>();
        if (text.length() == 0) {
            return keywords;
        }
        HashSet<String> visited = new HashSet<String>();
        try {
            // keywords = process(text, visited); // ORIG
            keywords = process2(text, visited);
        } catch (Exception e1) {
            e1.printStackTrace();
        }

        return keywords;
    }

    public void processDocument(Document doc) throws UnsupportedEncodingException {

        String text = doc.getTitle() + " " + doc.getText();

        System.out.println("processing: " + doc.getTitle());

        ArrayList<Keyword> keywords = new ArrayList<Keyword>();
        HashSet<String> visited = new HashSet<String>();
        try {
            // keywords = process(text, visited); // ORIG
            keywords = process2(text, visited);
        } catch (Exception e1) {
            e1.printStackTrace();
        }

        if (!keywords.isEmpty()) {
            addDocumentToOutput(doc, keywords.toArray(new Keyword[keywords.size()]));
        } else {
            System.err.println(doc.getTitle() + ": " + "no keywords");
        }
    }

    private void addDocumentToOutput(Document doc, Keyword[] keywords) {

        Output toAdd = new Output();
        toAdd.setKeyword(keywords);
        toAdd.setId(doc.getId());
        toAdd.setText(doc.getText());
        toAdd.setTitle(doc.getTitle());

        output.add(toAdd);

    }

    public void processDocuments(Document[] docs) throws UnsupportedEncodingException {

        for (Document doc : docs) {
            processDocument(doc);
        }
    }

    public String readURL(String urlString) throws IOException {
        String jsonStr = null;
        HttpClient client = new DefaultHttpClient();

        HttpGet httpGet = new HttpGet(urlString);

        httpGet.addHeader("Content-Type", "application/json;charset=utf-8");
        try {
            HttpResponse response = client.execute(httpGet);

            HttpEntity entity = response.getEntity();
            if (entity != null) {
                jsonStr = EntityUtils.toString(entity);

            }
            if (response.getStatusLine().getStatusCode() == 404
                    || response.getStatusLine().getStatusCode() == 406) {
                jsonStr = null;
            }
            //System.out.println(jsonStr);

        } finally {
            httpGet.releaseConnection();
        }
        return jsonStr;
    }

    public Vocab vocabTerm(String input) throws UnsupportedEncodingException {
        if (input == null) {
            return null;
        }
        Vocab vocab = vocabCache.get(input);
        if (vocab != null) {
            if (vocab == nilVocab) {
                return null;
            }
            return vocab;
        }
        String prefix = SERVER_URL + "/scigraph/vocabulary/term/";
        String suffix = "?limit=10&searchSynonyms=true&searchAbbreviations=false&searchAcronyms=false";
        String urlInput = URLEncoder.encode(input, StandardCharsets.UTF_8.name()).replace("+", "%20");


        String urlOut;
        if (stoplist.contains(input.toLowerCase())) {
            return null;
        }
        //System.out.println(prefix+urlInput+suffix);
        try {
            urlOut = readURL(prefix + urlInput + suffix);
            if (urlOut == null) {
                vocabCache.put(input, nilVocab);
                return null;
            }
        } catch (Exception e) {
            vocabCache.put(input, nilVocab);
            return null;
        }
        vocab = gson.fromJson(urlOut, Vocab.class);

        // preliminary check
        if (stoplist.contains(vocab.concepts.get(0).labels.get(0).toLowerCase())) {
            vocabCache.put(input, nilVocab);
            return null;
        }
        vocabCache.put(input, vocab);
        return vocab;
    }

    // returns the cinegiFacet associated with any class, returns null if there is not one
    private IRI getFacetIRI(OWLClass cls, HashSet<IRI> visited) {
        if (visited.contains(cls.getIRI())) {
            return null;
        }
        visited.add(cls.getIRI());

        //System.err.println(cls.getIRI());
        if (cls.getIRI().equals("http://www.w3.org/2002/07/owl#Thing")) {
            return null;
        }
        if (OWLFunctions.hasCinergiFacet(cls, extensions, df)) {
            return cls.getIRI();
        }
        if (OWLFunctions.hasParentAnnotation(cls, extensions, df)) {
            return getFacetIRI(OWLFunctions.getParentAnnotationClass(cls, extensions, df), visited);
        }
        if (!cls.getEquivalentClasses(manager.getOntologies()).isEmpty()) {
            for (OWLClassExpression oce : cls.getEquivalentClasses(manager.getOntologies())) // equivalencies
            {
                if (oce.getClassExpressionType().toString().equals("Class")) {
                    IRI retVal = getFacetIRI(oce.getClassesInSignature().iterator().next(), visited);
                    if (retVal == null) {
                        continue;
                    }
                    return retVal;
                }
            }
        }

        if (!cls.getSuperClasses(manager.getOntologies()).isEmpty()) {
            for (OWLClassExpression oce : cls.getSuperClasses(manager.getOntologies())) // subClassOf
            {
                if (oce.getClassExpressionType().toString().equals("Class")) {
                    OWLClass cl = oce.getClassesInSignature().iterator().next();
                    {
                        if (OWLFunctions.getLabel(cl, manager, df).equals(OWLFunctions.getLabel(cls, manager, df)))
                            continue; // skip if child of the same class
                    }
                    IRI retVal = getFacetIRI(oce.getClassesInSignature().iterator().next(), visited);

                    if (retVal == null) {
                        continue;
                    }
                    return retVal;
                }
            }
        }
        return null;
    }


    private ArrayList<Keyword> process2(String testInput, HashSet<String> visited) throws Exception {
        ArrayList<Keyword> keywords = new ArrayList<Keyword>();
        List<NLPHelper.NP> npList = nlpHelper.processText(testInput);
        for (NLPHelper.NP np : npList) {
            Tokens tok = new Tokens(np.getText());
            tok.setStart(String.valueOf(np.getStart()));
            tok.setEnd(String.valueOf(np.getEnd()));
            if (processChunk(tok, keywords, visited) == true) {
                continue;
            }
            POS[] parts = np.getPosArr();
            for (POS p : parts) {
                if (p.pos.equals("NN") || p.pos.equals("NNP") ||
                        p.pos.equals("NNPS") || p.pos.equals("NNS") || p.pos.equals("JJ")) {
                    // if there is a hyphen
                    if (p.token.contains("-")) {
                        // if there is a hyphen in the array of POS, then
                        // break it into separate parts and process them individually
                        int i = p.token.indexOf("-");
                        String[] substr = {p.token.substring(0, i), p.token.substring(i + 1)};
                        Tokens tempToken = new Tokens(substr[0] + " " + substr[1]);
                        if (processChunk(tempToken, keywords, visited) == true) // see if the phrase with a space replacing the hyphen exists
                        {
                            continue;
                        }
                    } else // doesnt contain a hyphen
                    {
                        // call vocab Term search for each of these tokens
                        Tokens tempToken = new Tokens(tok);
                        tempToken.setToken(p.token);
                        if (processChunk(tempToken, keywords, visited) == true) {
                            continue;
                        }
                    }
                }
            }

        }
        return keywords;
    }

    private ArrayList<Keyword> process(String testInput, HashSet<String> visited) throws Exception {
        String url = URLEncoder.encode(testInput, StandardCharsets.UTF_8.name());
        String chunks = SERVER_URL + "/scigraph/lexical/chunks?text=";
        //System.out.println(chunks+url);
        String json = readURL(chunks + url);

        ArrayList<Keyword> keywords = new ArrayList<Keyword>();

        // each result from chunking
        Tokens[] tokens = gson.fromJson(json, Tokens[].class);

        for (Tokens tok : tokens) // each chunk t
        {
            boolean used = false;
            if (processChunk(tok, keywords, visited) == true) {
                used = true;
                continue;
            }
            //FIXME calls REST for EVERY chunk
            POS[] parts = pos(gson, tok.getToken());

            for (POS p : parts) {
                //Vocab vocab;
                //System.out.println(p.token + " " + p.pos);
                if (p.pos.equals("NN") || p.pos.equals("NNP") ||
                        p.pos.equals("NNPS") || p.pos.equals("NNS") || p.pos.equals("JJ")) {
                    //System.out.println(p.token);
                    // if there is a hyphen
                    if (p.token.contains("-")) {
                        // if there is a hyphen in the array of POS, then
                        // break it into separate parts and process them individually
                        int i = p.token.indexOf("-");
                        String[] substr = {p.token.substring(0, i), p.token.substring(i + 1)};
                        Tokens tempToken = new Tokens(substr[0] + " " + substr[1]);
                        if (processChunk(tempToken, keywords, visited) == true) // see if the phrase with a space replacing the hyphen exists
                        {
                            used = true;
                            continue;
                        }
                    } else // doesnt contain a hyphen
                    {
                        // call vocab Term search for each of these tokens
                        Tokens tempToken = new Tokens(tok);
                        tempToken.setToken(p.token);
                        if (processChunk(tempToken, keywords, visited) == true) {
                            used = true;
                            continue;
                        }
                        // if the result of vocabulary/term is all caps, ignore it unless the pos search was all caps as well

		/*    	    	for (String label : vocab.concepts.get(0).labels) // comparing it to label of concept
                        {
			    	    	if (label.equals(p.token))
			    	    	{
			    	    		writer.printf("%-60s\t%s\n", t.token, label);
			    	    		used = true;
			    	    		break;
			    	    	}
			    	    	else if (label.contains(p.token.toUpperCase()) || p.token.toUpperCase().contains(label))
			    	    	{	
			    	    		// do nothing since the token was referenced incorrectly
			    	    		used = true;
			    	    		break;
			    	    	}
		    	    	}
		    	    	if (!printed)
		    	    	{	
		    	    		if (!p.token.toUpperCase().equals(p.token) 
		    	    				&& !vocab.concepts.get(0).labels.get(0).toUpperCase().equals(vocab.concepts.get(0).labels.get(0))) // last check if token and label are not both caps
		    	    		{	
			    	    		writer.printf("%-60s\t%s\n", t.token, vocab.concepts.get(0).labels.get(0));
			    	    		printed =  true;
		    	    		}
		    	    	}
		    */
                    }
                }
            }
        }
        return keywords;
    }

    // takes a token (phrase and span), a reference of keywords to add to, and a
    private boolean processChunk(Tokens t, ArrayList<Keyword> keywords, HashSet<String> visited) throws Exception {

        if (visited.contains(t.getToken())) // this token has already been used
        {
            return false;
        }
        Vocab vocab = vocabTerm(t.getToken());
        if (vocab == null) {
            return false;
        }
        visited.add(t.getToken());
        if (vocab.concepts.isEmpty()) {
            return false;
        }

        Concept toUse = vocab.concepts.get(0); // TODO find the concept that matched the token
        if (vocab.concepts.size() > 1) { // change this later to make use of exceptionMap TODO
            for (int i = 0; i < vocab.concepts.size(); i++) {
                if (nullIRIs.contains(vocab.concepts.get(i).uri)) {
                    vocab.concepts.remove(i);
                    i--;
                }
            }
            if (vocab.concepts.isEmpty()) {
                return false;
            }
            toUse = vocab.concepts.get(0);
        }
        HashSet<IRI> visitedIRI = new HashSet<IRI>();

        OWLClass cls = df.getOWLClass(IRI.create(toUse.uri));
        if (toUse.uri.contains("CHEBI") && t.getToken().length() <= 3) // filter chemical entities that cause errors
        {
            return false;
        }
        IRI facetIRI = getFacetIRI(cls, visitedIRI);
        if (facetIRI == null) {
            //System.err.println("no facet for: " + toUse.uri);
            return false;
        }
        // if equipement
        if (facetIRI.toString().contentEquals("http://sweet.jpl.nasa.gov/2.3/matrEquipment.owl#Equipment")) {
            boolean foundMatch = false;
            for (String lbl : toUse.labels
                    ) {
                if (lbl.contentEquals(t.getToken())) {
                    foundMatch = true;
                }
            }
            if (!foundMatch) return false;
        }

        if (facetIRI.toString().contentEquals("http://hydro10.sdsc.edu/cinergi_ontology/observation#Observation")) {
            return false;
        }

        keywords.add(new Keyword(t.getToken(), new String[]{t.getStart(), t.getEnd()}, facetIRI.toString(),
                OWLFunctions.getLabel(df.getOWLClass(facetIRI), manager, df)));

        return true; //

    }

    public POS[] pos(Gson gson, String input) throws Exception {
        String prefix = SERVER_URL + "/scigraph/lexical/pos?text=";
        String urlInput = URLEncoder.encode(input, StandardCharsets.UTF_8.name());
        String urlOut = readURL(prefix + urlInput);

        POS[] p = gson.fromJson(urlOut, POS[].class);
        return p;
    }


}
