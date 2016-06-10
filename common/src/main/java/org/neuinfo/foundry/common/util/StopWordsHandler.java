package org.neuinfo.foundry.common.util;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.neuinfo.foundry.common.model.EntityInfo;
import org.neuinfo.foundry.common.model.Keyword;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.util.*;

/**
 * Created by bozyurt on 7/27/15.
 */
public class StopWordsHandler {
    Set<String> stopWordSet = new HashSet<String>();
    Map<String, Phrase> phraseMap = new HashMap<String, Phrase>();
    private static StopWordsHandler instance = null;


    public synchronized static StopWordsHandler getInstance() throws Exception {
        if (instance == null) {
            instance = new StopWordsHandler();
            instance.loadStopWordsFromClassPath();
        }
        return instance;
    }


    public void postFilter(String text, Map<String, Keyword> keywordMap) {
        List<String> keys = new ArrayList<String>(keywordMap.keySet());
        if (!stopWordSet.isEmpty()) {
            for (String key : keys) {
                Keyword kw = keywordMap.get(key);
                String term = kw.getTerm();
                if (stopWordSet.contains(term) || stopWordSet.contains(term.toLowerCase())) {
                    keywordMap.remove(key);
                }
            }
        }
        keys = new ArrayList<String>(keywordMap.keySet());
        if (!phraseMap.isEmpty()) {
            for (String key : keys) {
                Keyword kw = keywordMap.get(key);
                String term = kw.getTerm();
                Phrase ph = phraseMap.get(term);
                if (ph == null) {
                    ph = phraseMap.get(term.toLowerCase());
                }
                if (ph != null) {
                    Keyword keyword = keywordMap.get(key);
                    List<EntityInfo> toBeRemoved = new LinkedList<EntityInfo>();
                    for (EntityInfo ei : keyword.getEntityInfos()) {
                        int idx = ph.phrase.indexOf(ph.term);
                        if (idx != -1) {
                            int start = ei.getStart();
                            int phraseStart = start - idx;
                            if (phraseStart >= 0 && text.indexOf(ph.phrase) == phraseStart) {
                                toBeRemoved.add(ei);
                            }
                        }
                    }
                    if (!toBeRemoved.isEmpty()) {
                        keyword.getEntityInfos().removeAll(toBeRemoved);
                    }
                    if (keyword.getEntityInfos().isEmpty()) {
                        keywordMap.remove(key);
                    }
                }
            }
        }
    }

    void loadStopWordsFromClassPath() throws IOException {
        BufferedReader bin = null;
        try {
            bin = new BufferedReader(new InputStreamReader(
                    StopWordsHandler.class.getClassLoader().getResourceAsStream("stopwords.txt")));
            StringBuilder sb = new StringBuilder(4096);
            String line;
            while ((line = bin.readLine()) != null) {
                sb.append(line).append('\n');
            }
            String content = sb.toString().trim();
            parse(content);
        } finally {
            Utils.close(bin);
        }
    }



    private void parse(String data) {
        String[] lines = data.split("\r?\n");
        for (String line : lines) {
            int idx = line.indexOf(',');
            if (idx != -1) {
                String term = line.substring(0, idx).trim();
                String phrase = line.substring(idx + 1).trim();
                idx = term.indexOf("/i");
                if (idx != -1) {
                    term = term.substring(0, idx).toLowerCase();
                }
                Phrase ph = new Phrase(term, phrase);
                phraseMap.put(term, ph);
            } else {
                String term = line.trim();
                idx = term.indexOf("/i");
                if (idx != -1) {
                    term = term.substring(0, idx);
                    this.stopWordSet.add(term.toLowerCase());
                } else {
                    this.stopWordSet.add(term);
                }
            }
        }
    }


    public static class Phrase {
        String term;
        String phrase;

        public Phrase(String term, String phrase) {
            this.term = term;
            this.phrase = phrase;
        }


    }

}
