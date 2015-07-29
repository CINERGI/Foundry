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

import java.net.URI;
import java.net.URL;
import java.util.*;

/**
 * Created by bozyurt on 7/27/15.
 */
public class StopWordsHandler {
    String stopwordsUrl;
    Set<String> stopWordSet = new HashSet<String>();
    Map<String, Phrase> phraseMap = new HashMap<String, Phrase>();
    private static StopWordsHandler instance = null;


    public synchronized static StopWordsHandler getInstance(String stopwordsUrl) throws Exception {
        if (instance == null) {
            instance = new StopWordsHandler(stopwordsUrl);
            instance.loadStopwords();
        }
        return instance;
    }

    public synchronized static StopWordsHandler getInstance() {
        if (instance == null) {
            throw  new RuntimeException("StopWordsHandler is not properly initialized!");
        }
        return instance;
    }



    private StopWordsHandler(String stopwordsUrl) {
        this.stopwordsUrl = stopwordsUrl;
    }

    public void postFilter(String text, Map<String, Keyword> keywordMap) {
        List<String> terms = new ArrayList<String>(keywordMap.keySet());
        if (!stopWordSet.isEmpty()) {
            for (String term : terms) {
                if (stopWordSet.contains(term.toLowerCase())) {
                    keywordMap.remove(term);
                }
            }
        }
        terms = new ArrayList<String>(keywordMap.keySet());
        if (!phraseMap.isEmpty()) {
            for (String term : terms) {
                Phrase ph = phraseMap.get(term.toLowerCase());
                if (ph != null) {
                    Keyword keyword = keywordMap.get(term);
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
                        keywordMap.remove(term);
                    }
                }
            }
        }
    }

    public void loadStopwords() throws Exception {
        URIBuilder builder = new URIBuilder(this.stopwordsUrl);
        URI uri = builder.build();
        if (this.stopwordsUrl.startsWith("file://")) {
            String data = Utils.loadAsString(this.stopwordsUrl.substring(7));
            parse(data);
        } else {
            HttpClient client = new DefaultHttpClient();
            HttpGet httpGet = new HttpGet(uri);
            try {
                HttpResponse response = client.execute(httpGet);
                HttpEntity entity = response.getEntity();
                if (response.getStatusLine().getStatusCode() == 200 && entity != null) {
                    String data = EntityUtils.toString(entity);
                    parse(data);
                }
            } finally {
                if (httpGet != null) {
                    httpGet.releaseConnection();
                }
            }
        }

    }

    private void parse(String data) {
        String[] lines = data.split("\r?\n");
        for (String line : lines) {
            int idx = line.indexOf(',');
            if (idx != -1) {
                String term = line.substring(0, idx).trim().toLowerCase();
                String phrase = line.substring(idx + 1).trim().toLowerCase();
                Phrase ph = new Phrase(term, phrase);
                phraseMap.put(term, ph);
            } else {
                this.stopWordSet.add(line.trim().toLowerCase());
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
