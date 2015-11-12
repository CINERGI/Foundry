package org.neuinfo.foundry.consumers.common;

import bnlpkit.util.FileUtils;
import bnlpkit.util.GenUtils;
import opennlp.tools.chunker.ChunkerME;
import opennlp.tools.chunker.ChunkerModel;
import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTaggerME;
import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.filter.Filters;
import org.jdom2.input.SAXBuilder;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;
import org.neuinfo.foundry.common.model.Keyword;
import org.neuinfo.foundry.common.util.*;

import java.io.*;
import java.util.*;

/**
 * Created by bozyurt on 11/9/15.
 */
public class PhraseKWDetector {
    SentenceDetectorME sentenceDetector;
    ChunkerME chunker;
    POSTaggerME posTagger;
    TokenizerME tokenizer;
    Inflector inflector = new Inflector();
    private static Namespace gmd = Namespace.getNamespace("gmd", "http://www.isotc211.org/2005/gmd");
    private static Namespace gco = Namespace.getNamespace("gco", "http://www.isotc211.org/2005/gco");


    public void handle(File rootDir) throws Exception {
        Map<String, NPWrapper> npMap = new HashMap<String, NPWrapper>();
        File[] files = rootDir.listFiles();
        for (File f : files) {
            if (f.isFile() && f.getName().endsWith(".xml")) {
                ISOText isoText = extractText(f);
                if (isoText.abstractText != null || isoText.title != null) {
                    if (isoText.abstractText != null && isoText.abstractText.indexOf("heat flux") != -1) {
                        System.out.println(org.neuinfo.foundry.common.util.Utils.formatText(isoText.abstractText, 100));
                        System.out.println("======================");
                        extractNPs(isoText.abstractText, npMap);
                        Map<String, List<String>> keyword2OntologyIdsMap = prepKeyword2OntologyIdsMap(isoText);
                        List<NPWrapper> npwList1 = new ArrayList<NPWrapper>(npMap.values());
                        associateWithOntologyIds(npwList1, keyword2OntologyIdsMap);

                        /*
                        String abs = isoText.abstractText;
                        Map<String, Keyword> keywordMap = new HashMap<String, Keyword>();
                        ScigraphUtils.annotateEntities(null, abs, keywordMap);
                        for (Keyword kw : keywordMap.values()) {
                            System.out.println(kw);
                        } */
                    }
                }
            }
        }

        List<NPWrapper> npwList = new ArrayList<NPWrapper>(npMap.values());
        Collections.sort(npwList, new Comparator<NPWrapper>() {
            @Override
            public int compare(NPWrapper o1, NPWrapper o2) {
                return o2.count - o1.count;
            }
        });
        System.out.println("=======================================");
        for (NPWrapper npw : npwList) {
            System.out.println(npw);
        }
    }

    Map<String, List<String>> prepKeyword2OntologyIdsMap(ISOText isoText) throws Exception {
        Map<String, List<String>> map = new HashMap<String, List<String>>();
        StringBuilder sb = new StringBuilder(1024);
        if (isoText.title != null) {
            sb.append(isoText.title).append(" \n");
        }
        if (isoText.abstractText != null) {
            sb.append(isoText.abstractText);
        }
        String text = sb.toString();
        Map<String, Keyword> keywordMap = new HashMap<String, Keyword>();
        ScigraphUtils.annotateEntities(null, text, keywordMap, false);
        for (Keyword kw : keywordMap.values()) {
            List<String> ontologyIds = new ArrayList<String>(kw.getIds());
            String term = kw.getTerm().toLowerCase();
            if (term.indexOf(' ') != -1) {
                String[] toks = term.split("\\s+");
                for (String tok : toks) {
                    if (!map.containsKey(tok)) {
                        map.put(tok, ontologyIds);
                    }
                }
            }
            map.put(term, ontologyIds);
        }
        return map;
    }

    void associateWithOntologyIds(List<NPWrapper> npwList, Map<String, List<String>> kw2OntoloyIdsMap) {
        for(NPWrapper npw : npwList) {
            String phrase = npw.np.getPhrase().toLowerCase();
            if (kw2OntoloyIdsMap.containsKey(phrase)) {
                List<String> ontologyIds = kw2OntoloyIdsMap.get(phrase);
                for(String ontId : ontologyIds) {
                    npw.np.addOntologyId(ontId);
                }
            } else  {
                for(Token tok : npw.np.toks) {
                    String key = tok.token.toLowerCase();
                    if (kw2OntoloyIdsMap.containsKey(key)) {
                        List<String> ontologyIds = kw2OntoloyIdsMap.get(key);
                        for(String ontId : ontologyIds) {
                            tok.addOntologyId(ontId);
                        }
                    }
                }
            }
        }
    }

    public void initialize() throws IOException {
        InputStream in = null;
        try {
            in = getClass().getClassLoader().getResourceAsStream("en-sent.bin");
            SentenceModel sm = new SentenceModel(in);
            this.sentenceDetector = new SentenceDetectorME(sm);
        } finally {
            FileUtils.close(in);
        }

        try {
            in = getClass().getClassLoader().getResourceAsStream("opennlp/models/en-chunker.bin");
            ChunkerModel cm = new ChunkerModel(in);
            this.chunker = new ChunkerME(cm);
        } finally {
            FileUtils.close(in);
        }

        try {
            in = getClass().getClassLoader().getResourceAsStream("opennlp/models/en-pos-maxent.bin");
            POSModel pm = new POSModel(in);
            this.posTagger = new POSTaggerME(pm);
        } finally {
            FileUtils.close(in);
        }

        try {
            in = getClass().getClassLoader().getResourceAsStream("en-token.bin");
            TokenizerModel tm = new TokenizerModel(in);
            this.tokenizer = new TokenizerME(tm);
        } finally {
            FileUtils.close(in);
        }
    }

    public void extractNPs(String text, Map<String, NPWrapper> npMap) {
        String[] sentences = this.sentenceDetector.sentDetect(text);
        int idx = 1;
        for (String sentence : sentences) {
            String[] tokens = this.tokenizer.tokenize(sentence);
            String[] pos = this.posTagger.tag(tokens);
            String[] tags = this.chunker.chunk(tokens, pos);

            System.out.println(idx + ": " + GenUtils.formatText(sentence, 100));
            System.out.print("\t");
            for (String ct : tags) {
                System.out.print(ct + ' ');
            }
            System.out.println();
            System.out.print("\t");
            for (String posTag : pos) {
                System.out.print(posTag + ' ');
            }
            System.out.println();
            List<NP> npList = findNPsInSentence(tokens, pos, tags);
            if (!npList.isEmpty()) {
                System.out.println(npList);
                for (NP np : npList) {
                    String phrase = np.getPhrase();
                    String key = inflector.toSingular(phrase).toLowerCase();
                    NPWrapper npw = npMap.get(key);
                    if (npw == null) {
                        npw = new NPWrapper(np);
                        npMap.put(key, npw);
                    }
                    npw.incr();
                }
            }
            idx++;
        }
    }

    List<NP> findNPsInSentence(String[] tokens, String[] pos, String[] tags) {
        List<NP> npList = new LinkedList<NP>();
        NP np = new NP();
        for (int i = 0; i < tags.length; i++) {
            if (tags[i].equals("B-NP")) {
                if (!np.toks.isEmpty()) {
                    npList.add(np);
                    np = new NP();
                }
                if (isEligible(pos[i])) {
                    np.addTok(new Token(tokens[i], i, pos[i]));
                }
            } else if (tags[i].equals("I-NP")) {
                if (isEligible(pos[i])) {
                    np.addTok(new Token(tokens[i], i, pos[i]));
                }
            }
        }
        if (!np.toks.isEmpty()) {
            npList.add(np);
        }
        // remove single token phrases
        for (Iterator<NP> it = npList.iterator(); it.hasNext(); ) {
            np = it.next();
            if (np.toks.size() == 1) {
                it.remove();
            }
        }

        return npList;
    }

    public static boolean isEligible(String posTag) {
        return posTag.equals("NNS") || posTag.equals("NNP") || posTag.equals("NN") || posTag.equals("CC") || posTag.equals("JJ");
        // return !posTag.equals("DT") && !posTag.equals("JJ") && !posTag.equals("RB") && !posTag.equals("VBG");
    }


    public ISOText extractText(File isoXmlFile) throws Exception {
        SAXBuilder builder = new SAXBuilder();
        BufferedReader in = null;
        Document doc = null;
        try {
            in = org.neuinfo.foundry.common.util.Utils.newUTF8CharSetReader(isoXmlFile.getAbsolutePath());
            doc = builder.build(in);
        } finally {
            org.neuinfo.foundry.common.util.Utils.close(in);
        }
        XPathFactory factory = XPathFactory.instance();

        String abstractText = null;
        String title = null;
        XPathExpression<Element> expr = factory.compile("//gmd:abstract",
                Filters.element(), null, gmd);
        List<Element> absEls = expr.evaluate(doc);
        if (!absEls.isEmpty()) {
            abstractText = absEls.get(0).getChildTextTrim("CharacterString", gco);
            if (abstractText.equalsIgnoreCase("required field")) {
                abstractText = null;
            }
        }

        expr = factory.compile("//gmd:title", Filters.element(), null, gmd);
        List<Element> titleEls = expr.evaluate(doc);
        if (!titleEls.isEmpty()) {
            title = titleEls.get(0).getChildTextTrim("CharacterString", gco);
            if (title.equalsIgnoreCase("required field")) {
                title = null;
            }
        }
        return new ISOText(abstractText, title);
    }

    public static class NPWrapper {
        final NP np;
        int count = 0;

        public NPWrapper(NP np) {
            this.np = np;
        }

        public void incr() {
            count++;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder("NPWrapper::{");
            sb.append("np=").append(np.getPhrase());
            if (np.hasOntologyId()) {
                sb.append(" * ");
            }
            sb.append(", count=").append(count);
            sb.append('}');
            return sb.toString();
        }
    }

    public static class NP {
        List<Token> toks = new LinkedList<Token>();
        String phrase;
        List<String> ontologyIds = new LinkedList<String>();

        public void addTok(Token tok) {
            toks.add(tok);
        }

        public String getPhrase() {
            if (phrase == null && !toks.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                for (Iterator<Token> it = toks.iterator(); it.hasNext(); ) {
                    Token tok = it.next();
                    sb.append(tok.token);
                    if (it.hasNext()) {
                        sb.append(' ');
                    }
                }
                phrase = sb.toString();
            }
            return phrase;
        }

        public void addOntologyId(String ontologyId) {
            if (!ontologyIds.contains(ontologyId)) {
                ontologyIds.add(ontologyId);
            }
        }

        public boolean hasOntologyId() {
            if (!ontologyIds.isEmpty()) {
                return true;
            }
            for (Token tok : toks) {
                if (tok.hasOntologyId()) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("NP::{");
            sb.append("phrase='").append(getPhrase()).append('\'');
            sb.append('}');
            return sb.toString();
        }
    }

    public static class Token {
        String token;
        int locIdx;
        String posTag;
        List<String> ontologyIds = new LinkedList<String>();

        public Token(String token, int locIdx, String posTag) {
            this.token = token;
            this.locIdx = locIdx;
            this.posTag = posTag;
        }

        public void addOntologyId(String ontologyId) {
            if (!ontologyIds.contains(ontologyId)) {
                ontologyIds.add(ontologyId);
            }
        }

        public boolean hasOntologyId() {
            return !ontologyIds.isEmpty();
        }
    }

    static class ISOText {
        String abstractText;
        String title;

        public ISOText(String abstractText, String title) {
            this.abstractText = abstractText;
            this.title = title;
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("ISOText::{");
            sb.append("abstractText='").append(abstractText);
            sb.append("', title='").append(title).append("'}");
            return sb.toString();
        }
    }

    public static void main(String[] args) throws Exception {
        PhraseKWDetector detector = new PhraseKWDetector();
        detector.initialize();
        detector.handle(new File("/var/data/cinergi/waf/hydro10.sdsc.edu/metadata/ScienceBase_WAF_dump"));

    }
}
