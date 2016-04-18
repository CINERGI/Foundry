package org.neuinfo.foundry.consumers.jms.consumers.plugins;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.neuinfo.foundry.common.model.EntityInfo;
import org.neuinfo.foundry.common.model.Keyword;
import org.neuinfo.foundry.common.util.*;
import org.neuinfo.foundry.common.util.KeywordInfo;
import org.neuinfo.foundry.consumers.jms.consumers.plugins.ProvenanceHelper.ProvData;
import org.neuinfo.foundry.consumers.plugin.IPlugin;
import org.neuinfo.foundry.consumers.plugin.Result;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

/**
 * Created by bozyurt on 12/8/14.
 */
public class KeywordEnhancer implements IPlugin {
    String serviceURL = "http://tikki.neuinfo.org:9000/scigraph/annotations/entities";
    List<String> jsonPaths = new ArrayList<String>(5);
    // NERKeywordEnhancer nerKeywordEnhancer;
    KeywordHierarchyHandler keywordHierarchyHandler;
    StopWordsHandler stopWordsHandler;
    boolean useNER = false;
    KeywordEnhancerCache cache = KeywordEnhancerCache.getInstance();
    //    Map<String, List<Keyword>> relatedKeywordsCache = new HashMap<String, List<Keyword>>();
    private final static Logger logger = Logger.getLogger(KeywordEnhancer.class);

    @Override
    public void initialize(Map<String, String> options) throws Exception {
        if (options.containsKey("serviceURL")) {
            this.serviceURL = options.get("serviceURL");
        }

        this.useNER = options.containsKey("useNER") ? Boolean.parseBoolean(options.get("useNER")) : false;

        jsonPaths.add("$..'gmd:abstract'.'gco:CharacterString'.'_$'");
        jsonPaths.add("$..'gmd:title'.'gco:CharacterString'.'_$'");
        jsonPaths.add("$..'abstract'.'gco:CharacterString'.'_$'");
        jsonPaths.add("$..'title'.'gco:CharacterString'.'_$'");

        //  this.nerKeywordEnhancer = NERKeywordEnhancer.getInstance();
        this.keywordHierarchyHandler = KeywordHierarchyHandler.getInstance();
        this.stopWordsHandler = StopWordsHandler.getInstance();
    }

    public boolean isUseNER() {
        return useNER;
    }

    public void setUseNER(boolean useNER) {
        this.useNER = useNER;
    }

    @Override
    public Result handle(DBObject docWrapper) {
        try {
            DBObject originalDoc = (DBObject) docWrapper.get("OriginalDoc");
            JSONObject json = JSONUtils.toJSON((BasicDBObject) originalDoc, false);
            DBObject siDBO = (DBObject) docWrapper.get("SourceInfo");
            String srcId = siDBO.get("SourceID").toString();
            String sourceName = siDBO.get("Name").toString();
            String primaryKey = docWrapper.get("primaryKey").toString();
            Date startDate = new Date();
            JSONPathProcessor processor = new JSONPathProcessor();
            Map<String, Keyword> keywordMap = new LinkedHashMap<String, Keyword>();
            List<String> text2AnnotateList = new ArrayList<String>(2);
            String docTitle = null;
            for (String jsonPath : jsonPaths) {
                List<Object> objects = processor.find(jsonPath, json);
                if (objects != null && !objects.isEmpty()) {
                    String text2Annotate = (String) objects.get(0);
                    if (!text2Annotate.equals("REQUIRED FIELD")) {
                        ScigraphUtils.annotateEntities(jsonPath, text2Annotate, keywordMap, false);
                        text2AnnotateList.add(text2Annotate);
                        if (jsonPath.indexOf("title") != -1) {
                            docTitle = text2Annotate;
                        }
                    }
                }
            }
            JSONArray jsArr = new JSONArray();
            Set<String> allowedCategorySet = new HashSet<String>();
            if (useNER) {
                allowedCategorySet.add("dataCenter");
                allowedCategorySet.add("platform");
            } else {
                allowedCategorySet.add("dataCenter");
                allowedCategorySet.add("instrument");
                allowedCategorySet.add("theme");
                allowedCategorySet.add("platform");
            }
            // add any related terms also
            // List<Keyword> mainKeywords = new ArrayList<Keyword>(keywordMap.values());


            for (Keyword keyword : keywordMap.values()) {
                // if (keyword.hasCategory() && keyword.hasAnyCategory(allowedCategorySet)) {
                // no category exclusions 04/22/2015
                JSONObject kwJson = keyword.toJSON();
                jsArr.put(kwJson);
                if (logger.isDebugEnabled()) {
                    logger.debug(kwJson.toString(2));
                    logger.debug("---------------------------");
                }
            }
            if (useNER) {
                /*
                Map<String, Keyword> nerKeywordsMap = this.nerKeywordEnhancer.findKeywords(text2AnnotateList);
                for (Keyword kw : nerKeywordsMap.values()) {
                    // skip location category
                    if (!kw.hasCategory("location")) {
                        JSONObject kwJson = kw.toJSON();
                        jsArr.put(kwJson);
                        logger.info("NER:" + kwJson.toString(2));
                        logger.info("---------------------------");
                    }
                }
                keywordMap.putAll(nerKeywordsMap);
                */
            }
            if (jsArr.length() > 0) {
                // apply Cinergi specific stop word/phrase filtering
                for (String text2Annotate : text2AnnotateList) {
                    stopWordsHandler.postFilter(text2Annotate, keywordMap);
                }


                Set<String> excludeCategorySet = new HashSet<String>(3);
                excludeCategorySet.add("platform");
                DBObject data = (DBObject) docWrapper.get("Data");
                Map<String, List<KeywordInfo>> category2KWIListMap = EnhancerUtils.getKeywordsToBeAdded(jsArr, json);
                jsArr = EnhancerUtils.filter(jsArr, category2KWIListMap, keywordMap);
                jsArr = EnhancerUtils.filterCategories(jsArr, excludeCategorySet);
                // do final mapping of keywords to Cinergi preferred labels
                EnhancerUtils.useCinergiPreferredLabels(jsArr);
                data.put("keywords", JSONUtils.encode(jsArr));

                ProvData provData = new ProvData(primaryKey, ProvenanceHelper.ModificationType.Added);
                provData.setSourceName(sourceName).setSrcId(srcId).setDocTitle(docTitle);
                EnhancerUtils.prepKeywordsProv(category2KWIListMap, provData);
                ProvenanceHelper.saveEnhancerProvenance("keywordEnhancer", provData, docWrapper);
                return new Result(docWrapper, Result.Status.OK_WITH_CHANGE);
            } else {
                return new Result(docWrapper, Result.Status.OK_WITHOUT_CHANGE);
            }
        } catch (Throwable t) {
            t.printStackTrace();
            Result r = new Result(docWrapper, Result.Status.ERROR);
            r.setErrMessage(t.getMessage());
            return r;
        }
    }


    @Override
    public String getPluginName() {
        return "KeywordEnhancer";
    }


    void addHierarchyKeywords(String ontologyId, String keyword, String category, Map<String, Keyword> keywordMap) throws Exception {
        List<String> parentKeywords = cache.getParentKeywords(ontologyId);
        if (parentKeywords == null) {
            parentKeywords = this.keywordHierarchyHandler.getParentKeywords(keyword, ontologyId);
            if (parentKeywords != null) {
                cache.putParentKeywords(ontologyId, parentKeywords);
            } else {
                cache.putParentKeywords(ontologyId, new ArrayList<String>(0));
            }
        }
        if (parentKeywords != null) {
            for (String pk : parentKeywords) {
                if (pk.equalsIgnoreCase("location of")) {
                    continue;
                }
                Keyword kw = new Keyword(pk);
                kw.addEntityInfo(new EntityInfo("", "", -1, -1, category));
                if (!keywordMap.containsKey(kw.getTerm())) {
                    keywordMap.put(kw.getTerm(), kw);
                }
            }
        }
    }

    public void getRelatedKeywords(String ontologyId, Map<String, Keyword> keywordMap) throws Exception {
        List<Keyword> keywords = this.cache.getRelatedKeywords(ontologyId);
        if (keywords != null) {
            for (Keyword kw : keywords) {
                if (!keywordMap.containsKey(kw.getTerm())) {
                    keywordMap.put(kw.getTerm(), kw);
                }
            }
        } else {
            keywords = new ArrayList<Keyword>(20);
            getRelatedTermsForRelation(ontologyId, keywordMap, "RO_0000052", keywords);
            getRelatedTermsForRelation(ontologyId, keywordMap, "provides", keywords);
            getRelatedTermsForRelation(ontologyId, keywordMap, "cinergi_ro.owlSynonym", keywords);
            getRelatedTermsForRelation(ontologyId, keywordMap, "has_topic", keywords);
            getRelatedTermsForRelation(ontologyId, keywordMap, "IAO_0000136", keywords);
            getRelatedTermsForRelation(ontologyId, keywordMap, "studies", keywords);
            this.cache.putRelatedKeywords(ontologyId, keywords);
        }
    }

    private void getRelatedTermsForRelation(String ontologyId, Map<String, Keyword> keywordMap,
                                            String relationType, List<Keyword> keywords)
            throws URISyntaxException, IOException {
        HttpClient client = new DefaultHttpClient();
        URIBuilder builder = new URIBuilder("http://tikki.neuinfo.org:9000/scigraph/graph/neighbors");
        builder.setParameter("id", ontologyId);
        builder.setParameter("depth", "2");
        builder.setParameter("blankNodes", "true");
        builder.setParameter("direction", "BOTH");
        builder.setParameter("relationshipType", relationType);
        URI uri = builder.build();
        HttpGet httpGet = new HttpGet(uri);
        System.out.println("uri:" + uri);
        httpGet.addHeader("Accept", "application/json");
        try {
            final HttpResponse response = client.execute(httpGet);
            final HttpEntity entity = response.getEntity();
            if (entity != null && response.getStatusLine().getStatusCode() == 200) {
                String jsonStr = EntityUtils.toString(entity);
                try {
                    JSONObject json = new JSONObject(jsonStr);
                    //  System.out.println(json.toString(2));
                    // System.out.println("================");
                    JSONArray nodesArr = json.getJSONArray("nodes");
                    for (int i = 0; i < nodesArr.length(); i++) {
                        Neighbour n = Neighbour.fromJSON(nodesArr.getJSONObject(i));
                        if (!n.getId().startsWith("_") && !Utils.isEmpty(n.getLabel())
                                && !n.getCategories().isEmpty()) {
                            if (n.getId().startsWith("http://purl.obolibrary.org/obo/RO_")) {
                                continue;
                            }
                            Keyword kw = new Keyword(n.getLabel());
                            EntityInfo ei = new EntityInfo("", n.getId(), -1, -1, n.getCategories().get(0));
                            kw.addEntityInfo(ei);
                            for (int j = 1; j < n.getCategories().size(); j++) {
                                ei.addOtherCategory(n.getCategories().get(j));
                            }
                            if (!keywordMap.containsKey(kw.getTerm())) {
                                keywordMap.put(kw.getTerm(), kw);
                                keywords.add(kw);
                            }
                        }
                    }
                } catch (Throwable t) {
                    logger.error("getRelatedTermsForRelation", t);
                }
            }
        } finally {
            if (httpGet != null) {
                httpGet.releaseConnection();
            }
        }
    }

    public static class Neighbour {
        String id;
        String label;
        List<String> categories = new ArrayList<String>(1);

        public Neighbour(String id, String label) {
            this.id = id;
            this.label = label;
        }

        public static Neighbour fromJSON(JSONObject json) {
            String id = json.getString("id");
            String label = null;
            if (json.has("lbl") && !json.isNull("lbl")) {
                label = json.getString("lbl");
            }
            Neighbour n = new Neighbour(id, label);
            JSONObject metaObj = json.getJSONObject("meta");
            if (metaObj.has("category")) {
                JSONArray jsArr = metaObj.getJSONArray("category");
                for (int i = 0; i < jsArr.length(); i++) {
                    n.categories.add(jsArr.getString(i));
                }
            }
            return n;
        }

        public String getId() {
            return id;
        }

        public String getLabel() {
            return label;
        }

        public List<String> getCategories() {
            return categories;
        }
    }


}
