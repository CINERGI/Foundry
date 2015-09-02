package org.neuinfo.foundry.common.util;

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

import java.net.URI;
import java.util.*;

/**
 * Created by bozyurt on 9/1/15.
 */
public class ScigraphUtils {
    static String serviceURL = "http://tikki.neuinfo.org:9000/";
    static String annotationServiceURL = "http://tikki.neuinfo.org:9000/scigraph/annotations/entities";
    private final static Logger logger = Logger.getLogger(ScigraphUtils.class);

    public static void annotateEntities(String contentLocation, String text, Map<String, Keyword> keywordMap) throws Exception {
        HttpClient client = new DefaultHttpClient();
        URIBuilder builder = new URIBuilder(annotationServiceURL);
        builder.setParameter("content", text);
        // minLength=4&longestOnly=true&includeAbbrev=false&includeAcronym=false&includeNumbers=false&callback=fn
        builder.setParameter("minLength", "4");
        builder.setParameter("longestOnly", "true");
        builder.setParameter("includeAbbrev", "false");
        builder.setParameter("includeNumbers", "false");

        URI uri = builder.build();
        HttpGet httpGet = new HttpGet(uri);
        System.out.println("uri:" + uri);
        // httpGet.addHeader("Content-Type", "application/json");
        httpGet.addHeader("Accept", "application/json");
        try {
            final HttpResponse response = client.execute(httpGet);
            final HttpEntity entity = response.getEntity();
            if (entity != null) {
                String jsonStr = EntityUtils.toString(entity);
                try {
                    System.out.println(new JSONArray(jsonStr).toString(2));
                    System.out.println("================");
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
                                    for (int j = 1; j < categories.length(); j++) {
                                        ei.addOtherCategory(categories.getString(j));
                                    }
                                    keyword.addEntityInfo(ei);
                                }
                            }
                        }
                    }
                } catch (Throwable t) {
                    logger.error("annotateEntities", t);
                }
            }
        } finally {
            if (httpGet != null) {
                httpGet.releaseConnection();
            }
        }
    }

    public static String toCurie(String ontologyId) {
        String id = ontologyId;
        int idx = ontologyId.lastIndexOf('/');
        if (idx != -1) {
            id = ontologyId.substring(idx + 1);
        }

        if (id.indexOf('#') != -1) {
            id = id.substring(id.indexOf('#') + 1);
        }
        return id;
    }

    public static List<OntologyPath> getiKeywordCinergiHierarchy(String id) throws Exception {
        return getKeywordHierarchy(id, "cinergiParent");
    }

    public static List<OntologyPath> getKeywordHierarchy(String id) throws Exception {
        return getKeywordHierarchy(id, "subClassOf");
    }

    public static List<OntologyPath> getKeywordHierarchy(String id, String relationshipType) throws Exception {
        //   String cachedHierarchy = lruCache.get(id);
        //   if (cachedHierarchy != null) {
        //       return cachedHierarchy;
        //   }
        //   String facetOntologyId = null;
        //   if (cinergiCategory != null) {
        //       facetOntologyId = fhh.getOntologyId(cinergiCategory);
        //   }

        List<OntologyPath> opList = null;
        HttpClient client = new DefaultHttpClient();
        URIBuilder builder = new URIBuilder(serviceURL);
        if (id.indexOf('#') != -1) {
            id = id.substring(id.indexOf('#') + 1);
        }

        builder.setPath("/scigraph/graph/neighbors");
        builder.setParameter("id", toCurie(id));
        builder.setParameter("depth", "100");
        builder.setParameter("blankNodes", "false");
        //builder.setParameter("relationshipType", "cinergiParent");
        builder.setParameter("relationshipType", relationshipType);
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
                //  System.out.println(json.toString(2));
                //  System.out.println("================");

                opList = prepHierarchy(json, id);
                for (OntologyPath op : opList) {
                    System.out.println(op);
                }
                System.out.println("--------------------------------------");
                //System.out.println("hierarchy: " + hierarchy);
                // lruCache.put(id, hierarchy);
                //return hierarchy;
            }
        } finally {
            if (httpGet != null) {
                httpGet.releaseConnection();
            }
        }
        return opList;
    }

    public static List<OntologyPath> prepHierarchy(JSONObject json, String id) {
        Map<String, KWNode> nodeMap = new HashMap<String, KWNode>(7);
        Map<String, KWEdge> edgeMap = new HashMap<String, KWEdge>(7);
        List<KWEdge> edges = new ArrayList<KWEdge>(10);
        List<OntologyPath> opList = new LinkedList<OntologyPath>();
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
        for (KWEdge se : startEdgeSet) {
            OntologyPath op = prepPath(se, nodeMap, edgeMap);
            if (!op.path.isEmpty()) {
                opList.add(op);
            }
        }
        return opList;
    }

    private static OntologyPath prepPath(KWEdge startEdge, Map<String, KWNode> nodeMap, Map<String, KWEdge> edgeMap) {
        OntologyPath op = new OntologyPath();
        KWNode n = nodeMap.get(startEdge.sub);
        while (n != null) {
            op.addToPath(n);
            KWEdge e = edgeMap.get(startEdge.obj);
            if (e == null) {
                break;
            }
            n = nodeMap.get(e.sub);
            startEdge = e;
        }
        return op;
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

    public static class OntologyPath {
        List<KWNode> path = new LinkedList<KWNode>();

        public void addToPath(KWNode node) {
            path.add(node);
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            for (Iterator<KWNode> it = path.iterator(); it.hasNext(); ) {
                KWNode n = it.next();
                String pathPart = n.label;
                if (n.label == null || n.label.length() == 0) {
                    pathPart = n.id;
                    int hashIdx = pathPart.lastIndexOf('#');
                    if (hashIdx != -1) {
                        pathPart = pathPart.substring(hashIdx + 1);
                    }
                }
                sb.append(pathPart);
                if (it.hasNext()) {
                    sb.append(" > ");
                }
            }
            return sb.toString();
        }
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
        String text = "Ocean floor under polar ice cap";
        Map<String, Keyword> kwMap = new HashMap<String, Keyword>(7);
        ScigraphUtils.annotateEntities("", text, kwMap);
        for (Keyword kw : kwMap.values()) {
            Set<String> ids = kw.getIds();
            for (String id : ids) {
                getKeywordHierarchy(id);
            }
        }

    }
}
