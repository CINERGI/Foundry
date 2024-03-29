package org.neuinfo.foundry.consumers.jms.consumers.jta;

import com.google.gson.Gson;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.neuinfo.foundry.common.model.Node;
import org.neuinfo.foundry.common.util.LRUCache;
import org.neuinfo.foundry.common.util.OWLFunctions;
import org.neuinfo.foundry.common.util.Utils;
import org.semanticweb.owlapi.model.*;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;


public class KeywordAnalyzer {

    private OWLOntologyManager manager;
    private OWLDataFactory df;
    private OWLOntology cinergi, extensions;
    private LRUCache<String, Vocab> vocabCache = new LRUCache<String, Vocab>(5000);
    private Vocab nilVocab = new Vocab(null);
    private List<Output> output;
    private List<String> stoplist;
    private List<String> nullIRIs;
    private Gson gson;
    private LinkedHashMap<String, IRI> exceptionMap;
    private int counter;
    private NLPHelper nlpHelper;
    // private String SERVER_URL = "http://tikki.neuinfo.org:9000";
    private String SERVER_URL = "http://ec-scigraph.sdsc.edu:9000";

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


        String urlOut = null;
        if (stoplist.contains(input.toLowerCase())) {
            return null;
        }

        System.out.println(prefix + urlInput + suffix);
        try {
            urlOut = readURL(prefix + urlInput + suffix);
        } catch (Exception e) {
            vocabCache.put(input, nilVocab);
        }

        if (urlOut == null) {
            if (input.contains("-")) {
                // if there is a hyphen then separate it
                int i = input.indexOf("-");
                String[] substr = {input.substring(0, i), input.substring(i + 1)};
                return vocabTerm(substr[0] + " " + substr[1]);
            } else {
                // no hyphen or result, put nilVocab in cache
                vocabCache.put(input, nilVocab);
                return null;
            }
        }

        System.out.println(urlOut);
        Concept[] concepts = gson.fromJson(urlOut, Concept[].class);
        ArrayList<Concept> conceptList = new ArrayList<Concept>(Arrays.asList(concepts));
        vocab = new Vocab(conceptList);

        // preliminary check
        if (stoplist.contains(vocab.concepts.get(0).labels.get(0).toLowerCase())) {
            vocabCache.put(input, nilVocab);
            return null;
        }
        vocabCache.put(input, vocab);
        return vocab;
    }

    // returns the cinegiFacet associated with any class, returns null if there is not one
    private List<IRI> getFacetIRI(OWLClass cls, Set<IRI> visited, Node<IRI> node) {
        if (visited.contains(cls.getIRI())) {
            return null;
        }
        visited.add(cls.getIRI());

        //System.err.println(cls.getIRI());
        if (cls.getIRI().equals("http://www.w3.org/2002/07/owl#Thing")) {
            return null;
        }
        if (OWLFunctions.hasCinergiFacet(cls, extensions, df)) {
            return Arrays.asList(cls.getIRI());
        }
        if (!cls.getEquivalentClasses(manager.getOntologies()).isEmpty()) {
            for (OWLClassExpression oce : cls.getEquivalentClasses(manager.getOntologies())) // equivalencies
            {
                if (oce.getClassExpressionType().toString().equals("Class")) {
                    OWLClass equivalentClass = oce.getClassesInSignature().iterator().next();
                    if (OWLFunctions.hasCinergiFacet(equivalentClass, extensions, df)) {
                        node.addChild(equivalentClass.getIRI());
                        return Arrays.asList(equivalentClass.getIRI());
                    }
                }
            }
        }
        if (OWLFunctions.hasParentAnnotation(cls, extensions, df)) {
            List<IRI> parentIRIs = new ArrayList<IRI>(10);
            for (OWLClass c : OWLFunctions.getParentAnnotationClass(cls, extensions, df)) {
                Node<IRI> child = node.addChild(c.getIRI());
                List<IRI> parentFacet = getFacetIRI(c, visited, child);
                if (parentFacet == null) {
                    return null;
                }
                parentIRIs.addAll(parentFacet);
            }
            return parentIRIs;
        }


        if (!cls.getSuperClasses(manager.getOntologies()).isEmpty()) {
            for (OWLClassExpression oce : cls.getSuperClasses(manager.getOntologies())) // subClassOf
            {
                if (oce.getClassExpressionType().toString().equals("Class")) {
                    OWLClass cl = oce.getClassesInSignature().iterator().next();
                    if (OWLFunctions.getLabel(cl, manager, df).equals(OWLFunctions.getLabel(cls, manager, df))) {
                        continue; // skip if child of the same class
                    }
                    OWLClass owlClass = oce.getClassesInSignature().iterator().next();
                    Node<IRI> child = node.addChild(owlClass.getIRI());
                    List<IRI> retVal = getFacetIRI(owlClass, visited, child);

                    if (retVal == null) {
                        continue;
                    }
                    return retVal;
                }
            }
        }
        return null;
    }

    // given a (2nd level) cinergiFacet, returns a string of path facet2, facet1
    private String facetPath(OWLClass cls) {
        if (OWLFunctions.getParentAnnotationClass(cls, extensions, df).size() == 0) {
            System.err.println(OWLFunctions.getLabel(cls, manager, df) + " has no cinergiParent, terminating.");
            return "";
            //return null;
        }
        OWLClass cinergiParent = OWLFunctions.getParentAnnotationClass(cls, extensions, df).get(0);
        if (OWLFunctions.isTopLevelFacet(cinergiParent, extensions, df) ||
                cinergiParent.getIRI().equals(IRI.create("http://www.w3.org/2002/07/owl#Thing"))) {
            return (OWLFunctions.getLabel(cls, manager, df) + " | " + OWLFunctions.getLabel(cinergiParent, manager, df));
        } else {
            return (OWLFunctions.getLabel(cls, manager, df) + " | " + facetPath(cinergiParent));
        }
    }

    private ArrayList<Keyword> process2(String testInput, HashSet<String> visited) throws Exception {
        ArrayList<Keyword> keywords = new ArrayList<Keyword>();
        List<NLPHelper.NP> npList = nlpHelper.processText(testInput);
        for (NLPHelper.NP np : npList) {
            Tokens tok = new Tokens(np.getText());
            tok.setStart(String.valueOf(np.getStart()));
            tok.setEnd(String.valueOf(np.getEnd()));

            int numKeywords = 0;
            if (processChunk(tok, keywords, visited) == true) {
                continue;
            }
            POS[] parts = np.getPosArr();
            if (parts.length > 2) {
                // try shorter phrases (IBO)
                boolean found = false;
                for (int i = parts.length - 1; i >= 2; i--) {
                    StringBuilder sb = new StringBuilder();
                    int count = 0;
                    for (int j = 0; j < i; j++) {
                        if (isEligibleTerm(parts[j])) {
                            sb.append(parts[j].token).append(' ');
                            count++;
                        }
                    }
                    Tokens tempToken = new Tokens(tok);
                    tempToken.setToken(sb.toString().trim());
                    if (processChunk(tempToken, keywords, visited) == true) {
                        found = true;
                        numKeywords++;
                        break;
                    }
                }

                /* 
                if (found) {
                    continue;
                } 
                */

                for (int i = 1; i <= parts.length - 2; i++) {
                    StringBuilder sb = new StringBuilder();
                    int count = 0;
                    for (int j = i; j < parts.length; j++) {
                        if (isEligibleTerm(parts[j])) {
                            sb.append(parts[j].token).append(' ');
                            count++;
                        }
                    }
                    Tokens tempToken = new Tokens(tok);
                    tempToken.setToken(sb.toString().trim());
                    if (processChunk(tempToken, keywords, visited) == true) {
                        found = true;
                        numKeywords++;
                        break;
                    }
                }
                if (found) {
                    continue;
                }
            }

            for (POS p : parts) {
                if (isEligibleTerm(p)) {
                    Tokens tempToken = new Tokens(tok);
                    tempToken.setToken(p.token);
                    if (processChunk(tempToken, keywords, visited) == true) {
                        numKeywords++;
                        continue;
                    }
                }
            }

            // remove smaller keywords from the same phrase that derive from the same facet
            if (numKeywords > 1) {
                for (int i = keywords.size() - numKeywords; i < keywords.size(); i++) {
                    for (int j = i + 1; j < keywords.size(); j++) {
                        Keyword temp_i = keywords.get(i);
                        Keyword temp_j = keywords.get(j);
                        if (temp_i.getFacet()[0].equals(temp_j.getFacet()[0])) {
                            if (temp_i.getTerm().length() >= temp_j.getTerm().length()) {
                                keywords.remove(j);
                                j--;
                                numKeywords--;
                            } else {
                                System.out.println("removed " + temp_i.getTerm());
                                keywords.remove(i);
                                i--;
                                numKeywords--;
                                break;
                            }
                        }
                    }
                }
            }
        }
        return keywords;
    }

    public static boolean isEligibleTerm(POS p) {
        return (p.pos.equals("NN") || p.pos.equals("NNP") ||
                p.pos.equals("NNPS") || p.pos.equals("NNS") || p.pos.equals("JJ"));
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
        Vocab vocab = vocabTerm(t.getToken());
        if (vocab == null) {
            return false;
        }
        // visited.add(t.getToken());
        if (vocab.concepts.isEmpty()) {
            return false;
        }

        if (vocab.concepts.size() > 1) { // change this later to make use of exceptionMap TODO
            for (int i = 0; i < vocab.concepts.size(); i++) {
                if (nullIRIs.contains(vocab.concepts.get(i).uri)) {
                    vocab.concepts.remove(i);
                    i--;
                }
            }
        }
        if (vocab.concepts.isEmpty()) {
            return false;
        }
        Concept toUse = vocab.concepts.get(0);
        List<Concept> consideringToUse = new ArrayList<Concept>();
        String closestLabel = toUse.labels.get(0);
        int minDistance = 100;
        for (Concept conc : vocab.concepts) {
            for (String label : conc.labels) {
                int tempDist = Levenshtein.distance(label, t.getToken());
                if (tempDist < 2) {// within 2 changes away, add it to a consideration list
                    if (df.getOWLClass(IRI.create(conc.uri)).getSuperClasses(manager.getOntologies()).isEmpty()) {
                        // not an OWLClass, can skip
                        continue;
                    }
                    if (conc.uri.contains("obo/ENVO") || conc.uri.contains("cinergi_ontology/cinergi.owl")) {
                        consideringToUse.add(0, conc); // if its ENVO or cinergi at to beginning
                    } else {
                        consideringToUse.add(conc);
                    }
                }
                if (tempDist < minDistance) {
                    minDistance = tempDist;
                    toUse = conc; // update the concept
                    closestLabel = label; // update the label
                }
            }
        }
        if (consideringToUse.size() > 0) {
            toUse = consideringToUse.get(0);
        }
        //if input is all caps (abbreviation), check if the output is the exact same 
	if (t.getToken().equals(t.getToken().toUpperCase()))
	    if (closestLabel.equals(t.getToken()) == false)
	        return false;
        // if label is upper case, only match if case matches
        if (closestLabel.equals(closestLabel.toUpperCase()))
            if (closestLabel.equals(t.getToken()) == false)
                return false;
                                             
        OWLClass cls = df.getOWLClass(IRI.create(toUse.uri));
        // check for repeated terms
        if (visited.contains(cls.getIRI().toString())) {
            return false;
        }
        visited.add(cls.getIRI().toString());
        if (toUse.uri.contains("CHEBI"))
        {
            // filter chemical entities that cause errors
            if (t.getToken().length() <= 3) {
                return false;
            }
        }
        if (t.getToken().length() <= 2) // any input less than 2
        {
            return false;
        }
        LinkedHashSet<IRI> visitedIRI = new LinkedHashSet<IRI>();
        Node<IRI> rootNode = new Node<IRI>(cls.getIRI(), null);
        List<IRI> facetIRI = getFacetIRI(cls, visitedIRI, rootNode);
        if (facetIRI == null) {
            //System.err.println("no facet for: " + toUse.uri);
            return false;
        }


        ArrayList<String> facetLabels = new ArrayList<String>(5);
        ArrayList<String> IRIstr = new ArrayList<String>(5);
        List<String> fullHierarchies = new ArrayList<String>(5);

        Map<IRI, String> fullPathMap = prepFullPathMap(rootNode, manager, df, facetIRI);
        for (IRI firi : facetIRI) {
            String facetCSV = facetPath(df.getOWLClass(firi));
            if (facetCSV.endsWith("Thing")) {
                System.err.println("Term assigned to top level: " + facetCSV + " token:" + t.getToken());
                return false;
            }
            String facetPath = facet2Path(facetCSV);
            if (facetPath == null) {
                System.err.println("More than two level facet: " + facetCSV + " token:" + t.getToken());
                return false;
            }
            facetLabels.add(facetPath);
            IRIstr.add(firi.toString());

            String fullPath = fullPathMap.get(firi);

            String fullHierarchy = prefixFullHierarchy(fullPath, facetCSV);

            System.out.println(fullHierarchy);
            fullHierarchies.add(fullHierarchy);
        }
        //String term = t.getToken();
        String term = closestLabel;
        term = normalizeTerm(term);
        Keyword keyword = new Keyword(term, new String[]{t.getStart(), t.getEnd()},
                IRIstr.toArray(new String[IRIstr.size()]),
                facetLabels.toArray(new String[facetLabels.size()]),
                fullHierarchies.toArray(new String[fullHierarchies.size()]));
        keywords.add(keyword);
        System.out.println(keyword);

        return true; //
    }

    public static Map<IRI, String> prepFullPathMap(Node<IRI> rootNode, OWLOntologyManager manager,
                                                   OWLDataFactory df, List<IRI> facetIRI) {
        Set<IRI> facetIRISet = new HashSet<IRI>(facetIRI);
        Map<IRI, String> map = new HashMap<IRI, String>();
        List<Node<IRI>> leafNodes = Node.getLeafNodes(rootNode);
        for (Node<IRI> leaf : leafNodes) {
            StringBuilder sb = new StringBuilder();
            Node<IRI> p = leaf;
            IRI key = null;
            Set<IRI> uniqSet = new HashSet<IRI>();
            while (p != null) {
                IRI iri = p.getPayload();
                if (facetIRISet.contains(iri)) {
                    key = iri;
                }
                if (!uniqSet.contains(iri)) {
                    sb.append(OWLFunctions.getLabel(df.getOWLClass(iri), manager, df));
                    if (!p.isRoot()) {
                        sb.append(" > ");
                    }
                    uniqSet.add(iri);
                }
                p = p.getParent();
            }

            //  Assertion.assertNotNull(key);
            if (key != null) {
                map.put(key, sb.toString().trim());
            } else {
                System.out.println("No key for path:" + sb.toString().trim());
            }
        }
        return map;
    }

    public static String prefixFullHierarchy(String fullHierarchy, String facetCSV) {
        String[] tokens = facetCSV.split("\\s+\\|\\s+");
        if (tokens.length == 2) {
            if (!fullHierarchy.startsWith(tokens[1])) {
                return tokens[1] + " > " + fullHierarchy;
            }
        }
        return fullHierarchy;
    }

    public static String facet2Path(String facetStr) {
        StringBuilder sb = new StringBuilder();
        String[] tokens = facetStr.split("\\s+\\|\\s+");
        if (tokens.length == 2) {
            sb.append(tokens[1]).append(" > ").append(tokens[0]);
        } else {
            if (tokens.length > 2) {
                return null;
            }
            sb.append(facetStr);
        }

        return sb.toString().trim();
    }

    public static String normalizeTerm(String term) {
        char[] chars = term.toCharArray();
        if (!Character.isLetterOrDigit(term.charAt(0))) {
            int i = 0;
            while (i < chars.length) {
                if (Character.isLetterOrDigit(chars[i])) {
                    break;
                }
                i++;
            }
            term = term.substring(i);
        }
        chars = term.toCharArray();
        int i = term.length() - 1;
        while (i > 0) {
            char c = chars[i];
            if (c == '.' || c == '(' || c == ')' || c == ',' || c == '?' || c == ':' || c == ';') {
                i--;
            } else {
                break;
            }
        }
        if ((i + 1) < term.length()) {
            term = term.substring(0, i + 1);
        }
        return term;
    }

    public POS[] pos(Gson gson, String input) throws Exception {
        String prefix = SERVER_URL + "/scigraph/lexical/pos?text=";
        String urlInput = URLEncoder.encode(input, StandardCharsets.UTF_8.name());
        String urlOut = readURL(prefix + urlInput);

        POS[] p = gson.fromJson(urlOut, POS[].class);
        return p;
    }


    public static void main(String[] args) {
        System.out.println(normalizeTerm(".PH);"));
    }
}
