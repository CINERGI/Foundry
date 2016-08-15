package org.neuinfo.foundry.common.util;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.neuinfo.foundry.common.model.EntityInfo;
import org.neuinfo.foundry.common.model.Keyword;

import java.net.URI;
import java.net.URLEncoder;
import java.util.*;

/**
 * Created by bozyurt on 4/20/15.
 */
public class KeywordHierarchyHandler {
    String serviceURL; //  = "http://tikki.neuinfo.org:9000/";
    Map<String, String> lruCache = new LRUCache<String, String>(100);
    Map<String, List<String>> parentKeywordsCache = new LRUCache<String, List<String>>(1000);
    private static KeywordHierarchyHandler instance = null;


    public static KeywordHierarchyHandler getInstance(String serviceURL) {
        if (instance == null) {
            instance = new KeywordHierarchyHandler(serviceURL);
        }
        return instance;
    }

    private KeywordHierarchyHandler(String serviceURL) {
        this.serviceURL = serviceURL;
    }



    public synchronized List<String> getParentKeywords(String keyword, String id) throws Exception {
        List<String> cachedParentKeywords = parentKeywordsCache.get(id);
        if (cachedParentKeywords != null) {
            return cachedParentKeywords;
        }
        HttpClient client = new DefaultHttpClient();
        URIBuilder builder = new URIBuilder(this.serviceURL);
        //builder.setPath("/scigraph/graph/neighbors/" + id + ".json");
        builder.setPath("/scigraph/graph/neighbors");
        builder.setParameter("id", id);
        builder.setParameter("depth", "100");
        builder.setParameter("blankNodes", "false");
        builder.setParameter("relationshipType", "subClassOf");
        builder.setParameter("direction", "OUTGOING");
        URI uri = builder.build();
        HttpGet httpGet = new HttpGet(uri);
        System.out.println("uri:" + uri);
        httpGet.addHeader("Accept", "application/json");
        try {
            HttpResponse response = client.execute(httpGet);
            HttpEntity entity = response.getEntity();
            if (response.getStatusLine().getStatusCode() == 200 && entity != null) {
                String jsonStr = EntityUtils.toString(entity);
                JSONObject json = new JSONObject(jsonStr);
                // System.out.println(json.toString(2));
                // System.out.println("================");

                String hierarchy = prepHierarchy(json, keyword, id);
                System.out.println("hierarchy: " + hierarchy);
                String[] tokens = hierarchy.split(" > ");
                cachedParentKeywords = new ArrayList<String>(tokens.length - 1);
                for (String token : tokens) {
                    token = token.trim();
                    if (!token.equals(keyword)) {
                        cachedParentKeywords.add(token);
                    }
                }
                parentKeywordsCache.put(id, cachedParentKeywords);
                return cachedParentKeywords;
            }
        } finally {
            if (httpGet != null) {
                httpGet.releaseConnection();
            }
        }
        return null;
    }

    public synchronized String getKeywordFacetHierarchy(String keyword, String id) throws Exception {
        String cachedHierarchy = lruCache.get(id);
        if (cachedHierarchy != null) {
            return cachedHierarchy;
        }
//http://tikki.neuinfo.org:9000/scigraph/dynamic/http%3A%2F%2Fpurl.obolibrary.org%2Fobo%2FENVO_00002871/facets
        HttpClient client = new DefaultHttpClient();
        URIBuilder builder = new URIBuilder(this.serviceURL);
        String path = "scigraph/dynamic/" + URLEncoder.encode(id, "UTF-8") + "/facets";
        System.out.println(path);
        URI uri = new URI(this.serviceURL + path);
        HttpGet httpGet = new HttpGet(uri);
        System.out.println("uri:" + uri);
        httpGet.addHeader("Accept", "application/json");
        try {
            HttpResponse response = client.execute(httpGet);
            HttpEntity entity = response.getEntity();
            if (response.getStatusLine().getStatusCode() == 200 && entity != null) {
                String jsonStr = EntityUtils.toString(entity);
                JSONObject json = new JSONObject(jsonStr);
                //  System.out.println(json.toString(2));
                //  System.out.println("================");

                String hierarchy = prepHierarchy(json, keyword, id);
                System.out.println("hierarchy: " + hierarchy);
                lruCache.put(id, hierarchy);
                return hierarchy;
            }

        } finally {
            if (httpGet != null) {
                httpGet.releaseConnection();
            }
        }
        return null;
    }

    // tikki.neuinfo.org:9000/scigraph/graph/neighbors/Manometer.json?depth=100&blankNodes=false&relationshipType=subClassOf&direction=out
    public synchronized String getKeywordHierarchy(String keyword, String id,
                                                   FacetHierarchyHandler fhh, String cinergiCategory) throws Exception {
        String cachedHierarchy = lruCache.get(id);
        if (cachedHierarchy != null) {
            return cachedHierarchy;
        }
        String facetOntologyId = null;
        if (cinergiCategory != null) {
            facetOntologyId = fhh.getOntologyId(cinergiCategory);
        }

        HttpClient client = new DefaultHttpClient();
        URIBuilder builder = new URIBuilder(this.serviceURL);
        if (id.indexOf('#') != -1) {
            id = id.substring(id.indexOf('#') + 1);
        }

        builder.setPath("/scigraph/graph/neighbors/" + id + ".json");
        // builder.setPath("/scigraph/graph/neighbors");
        //builder.setParameter("id", id);
        builder.setParameter("depth", "100");
        builder.setParameter("blankNodes", "false");
        builder.setParameter("relationshipType", "subClassOf");
        builder.setParameter("direction", "OUTGOING");
        URI uri = builder.build();
        HttpGet httpGet = new HttpGet(uri);
        System.out.println("uri:" + uri);
        httpGet.addHeader("Accept", "application/json");
        try {
            HttpResponse response = client.execute(httpGet);
            HttpEntity entity = response.getEntity();
            if (response.getStatusLine().getStatusCode() == 200 && entity != null) {
                String jsonStr = EntityUtils.toString(entity);
                JSONObject json = new JSONObject(jsonStr);
                //System.out.println(json.toString(2));
                // System.out.println("================");

                String hierarchy = prepHierarchy(json, id, facetOntologyId);
                System.out.println("hierarchy: " + hierarchy);
                lruCache.put(id, hierarchy);
                return hierarchy;
            }
        } finally {
            if (httpGet != null) {
                httpGet.releaseConnection();
            }
        }
        return null;
    }


    String prepHierarchy(JSONObject json, String id, String facetOntologyId) {
        Map<String, KWNode> nodeMap = new HashMap<String, KWNode>(7);
        Map<String, KWEdge> edgeMap = new HashMap<String, KWEdge>(7);
        List<KWEdge> edges = new ArrayList<KWEdge>(10);
        JSONArray nodesArr = json.getJSONArray("nodes");
        for (int i = 0; i < nodesArr.length(); i++) {
            KWNode node = KWNode.fromJSON(nodesArr.getJSONObject(i));
            nodeMap.put(node.id, node);
        }
        JSONArray edgesArr = json.getJSONArray("edges");
        Set<KWEdge> startEdgeSet = new LinkedHashSet<KWEdge>();
        KWEdge startEdge = null;
        for (int i = 0; i < edgesArr.length(); i++) {
            KWEdge edge = KWEdge.fromJSON(edgesArr.getJSONObject(i));
            if (edge.sub.endsWith(id)) {
                startEdgeSet.add(edge);
                startEdge = edge;
            }
            edgeMap.put(edge.sub, edge);
            edges.add(edge);
        }
        if (startEdgeSet.size() > 1) {
            System.out.println();
        }
        Assertion.assertNotNull(startEdge);
        String pathStr = null;
        for(KWEdge se : startEdgeSet) {
            pathStr = prepPath(se, nodeMap, edgeMap, facetOntologyId);
            if (pathStr != null) {
                break;
            }
        }
        return pathStr != null ? pathStr : "";
    }

    private String prepPath(KWEdge startEdge, Map<String, KWNode> nodeMap, Map<String, KWEdge> edgeMap, String facetOntologyId) {
        StringBuilder sb = new StringBuilder();
        KWNode n = nodeMap.get(startEdge.sub);
        List<String> path = new ArrayList<String>(edgeMap.size());
        boolean foundFacet = false;
        while (n != null) {
            if (n.id.equals(facetOntologyId)) {
                foundFacet = true;
            }
            String pathPart = n.label;
            if (n.label == null || n.label.length() == 0) {
                pathPart = n.id;
                int hashIdx = pathPart.lastIndexOf('#');
                if (hashIdx != -1) {
                    pathPart = pathPart.substring(hashIdx + 1);
                }
            }

            path.add(pathPart);
            KWEdge e = edgeMap.get(startEdge.obj);
            if (e == null) {
                break;
            }
            n = nodeMap.get(e.sub);
            startEdge = e;
        }
        if (!foundFacet) {
            return null;
        }
        for (Iterator<String> it = path.iterator(); it.hasNext(); ) {
            String pathPart = it.next();
            sb.append(pathPart);
            if (it.hasNext()) {
                sb.append(" > ");
            }
        }
        return sb.toString().trim();
    }

    public void annotateEntities(String contentLocation, String text, Map<String, Keyword> keywordMap) throws Exception {
        HttpClient client = new DefaultHttpClient();
        String url = "http://ec-scigraph.sdsc.edu:9000/scigraph/annotations/entities";
        //String url = "http://tikki.neuinfo.org:9000/scigraph/annotations/entities";
        URIBuilder builder = new URIBuilder(url);
        builder.setParameter("content", text);
        // minLength=4&longestOnly=true&includeAbbrev=false&includeAcronym=false&includeNumbers=false&callback=fn
        builder.setParameter("minLength", "4");
        builder.setParameter("longestOnly", "true");
        builder.setParameter("includeAbbrev", "false");
        builder.setParameter("includeNumbers", "false");

        URI uri = builder.build();
        HttpGet httpGet = new HttpGet(uri);
        //System.out.println("uri:" + uri);
        // httpGet.addHeader("Content-Type", "application/json");
        httpGet.addHeader("Accept", "application/json");
        try {
            final HttpResponse response = client.execute(httpGet);
            final HttpEntity entity = response.getEntity();
            if (entity != null) {
                String jsonStr = EntityUtils.toString(entity);
                try {
                    //System.out.println(new JSONArray(jsonStr).toString(2));
                    //System.out.println("================");
                    JSONArray jsArr = new JSONArray(jsonStr);
                    String textLC = text.toLowerCase();
                    for (int i = 0; i < jsArr.length(); i++) {
                        final JSONObject json = jsArr.getJSONObject(i);
                        if (json.has("token")) {
                            JSONObject tokenObj = json.getJSONObject("token");
                            String id = tokenObj.getString("id");
                            final JSONArray terms = tokenObj.getJSONArray("terms");
                            if (terms.length() > 0) {
                                int start = json.getInt("start");
                                int end = json.getInt("end");
                                String term = findMatchingTerm(terms, textLC);
                                if (term != null) {
                                    Keyword keyword = keywordMap.get(term);
                                    if (keyword == null) {
                                        keyword = new Keyword(term);
                                        keywordMap.put(term, keyword);
                                    }
                                    JSONArray categories = tokenObj.getJSONArray("categories");
                                    String category = "";
                                    if (categories.length() > 0) {
                                        category = categories.getString(0);
                                    }
                                    EntityInfo ei = new EntityInfo(contentLocation, id, start, end, category);
                                    keyword.addEntityInfo(ei);
                                }
                            }
                        }
                    }
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }
        } finally {
            if (httpGet != null) {
                httpGet.releaseConnection();
            }
        }
    }

    public static String findMatchingTerm(JSONArray jsArr, String text) {
        for (int i = 0; i < jsArr.length(); i++) {
            String term = jsArr.getString(i);
            if (text.indexOf(term.toLowerCase()) != -1) {
                return term;
            }
        }
        // if no match has found return the first term
        for (int i = 0; i < jsArr.length(); i++) {
            String term = jsArr.getString(i);
            if (term != null && term.length() > 0) {
                return term;
            }
        }
        return null;
    }

    public static class KWNode {
        String id;
        String label;
        List<String> categories = new ArrayList<String>(2);
        KWNode parent;
        List<KWNode> children;

        public KWNode(String id, String label) {
            this.id = id;
            this.label = label;
        }

        public KWNode getParent() {
            return parent;
        }

        public void setParent(KWNode parent) {
            this.parent = parent;
        }

        public List<KWNode> getChildren() {
            return children;
        }

        public void addChild(KWNode child) {
            if (this.children == null) {
                this.children = new LinkedList<KWNode>();
            }
            children.add(child);
        }

        public KWNode getChild(String label) {
            if (!hasChildren()) {
                return null;
            }
            for (KWNode c : children) {
                if (c.label.equalsIgnoreCase(label)) {
                    return c;
                }
            }
            return null;
        }

        public boolean hasChildren() {
            return children != null && !children.isEmpty();
        }

        public static KWNode fromJSON(JSONObject json) {
            String id = json.getString("id");
            String label = null;
            if (json.has("lbl")) {
                Object o = json.get("lbl");
                if (o instanceof String) {
                    label = (String) o;
                }
            }
            KWNode node = new KWNode(id, label);
            if (json.has("meta")) {
                JSONObject metaJS = json.getJSONObject("meta");
                if (metaJS.has("category")) {
                    JSONArray jsArr = metaJS.getJSONArray("category");
                    for (int i = 0; i < jsArr.length(); i++) {
                        node.categories.add(jsArr.getString(i));
                    }
                }
                String cinergiPrefLabel = "http://hydro10.sdsc.edu/cinergi_ontology/cinergiExtensions.owl#cinergiPreferredLabel";
                if (metaJS.has(cinergiPrefLabel)) {
                    JSONArray jsArr = metaJS.getJSONArray(cinergiPrefLabel);
                    if (jsArr.length() > 0 && jsArr.getString(0).length() > 0) {
                        node.label = jsArr.getString(0);
                    }
                }

            }
            return node;
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("Node{");
            sb.append("id='").append(id).append('\'');
            sb.append(", label='").append(label).append('\'');
            sb.append(", categories=").append(categories);
            if (parent != null) {
                sb.append(", parent=").append(parent.id);
            }
            sb.append('}');
            return sb.toString();
        }
    }

    public static class KWEdge {
        String sub;
        String obj;
        String pred;

        public KWEdge(String sub, String obj, String pred) {
            this.sub = sub;
            this.obj = obj;
            this.pred = pred;
        }

        public static KWEdge fromJSON(JSONObject json) {
            String sub = json.getString("sub");
            String obj = json.getString("obj");
            String pred = json.getString("pred");

            return new KWEdge(sub, obj, pred);
        }
    }

    public static void main(String[] args) throws Exception {

        //KeywordHierarchyHandler handler = KeywordHierarchyHandler.getInstance("http://tikki.neuinfo.org:9000/");
        KeywordHierarchyHandler handler = KeywordHierarchyHandler.getInstance("http://ec-scigraph.sdsc.edu:9000/");

//        handler.getKeywordHierarchy("Manometer", "Manometer");
        // handler.getKeywordHierarchy("mercury","b0e515cf-ed97-4870-bdde-6c00b0c998ee");
//        handler.getKeywordHierarchy("mountain", "ENVO_00000081");

        handler.getKeywordFacetHierarchy("Proteomics", "http://purl.obolibrary.org/obo/ENVO_00002871");

    }
}
