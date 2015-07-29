package org.neuinfo.foundry.consumers.jms.consumers.plugins;

import org.neuinfo.foundry.common.model.Keyword;
import org.neuinfo.foundry.common.util.LRUCache;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Created by bozyurt on 5/21/15.
 */
public class KeywordEnhancerCache {
    private static KeywordEnhancerCache instance = null;
    Map<String, List<Keyword>> relatedKeywordsCache = Collections.synchronizedMap(new LRUCache<String, List<Keyword>>(5000));
    Map<String, List<String>> parentKeywordsCache =  Collections.synchronizedMap(new LRUCache<String, List<String>>(5000));

    private KeywordEnhancerCache() {
    }

    public synchronized static KeywordEnhancerCache getInstance() {
        if (instance == null) {
            instance = new KeywordEnhancerCache();
        }
        return instance;
    }

    public List<Keyword> getRelatedKeywords(String ontologyId) {
        return relatedKeywordsCache.get(ontologyId);
    }

    public void putRelatedKeywords(String ontologyId, List<Keyword> keywords) {
        relatedKeywordsCache.put(ontologyId, keywords);
    }

    public List<String> getParentKeywords(String ontologyId) {
        return parentKeywordsCache.get(ontologyId);
    }

    public void putParentKeywords(String ontologyId, List<String> keywords) {
        parentKeywordsCache.put(ontologyId, keywords);
    }

}
