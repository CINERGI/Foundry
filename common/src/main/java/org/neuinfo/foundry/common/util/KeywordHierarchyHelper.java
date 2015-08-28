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
import org.neuinfo.foundry.common.util.KeywordHierarchyHandler.KWEdge;
import org.neuinfo.foundry.common.util.KeywordHierarchyHandler.KWNode;

import java.net.URI;
import java.util.*;

/**
 * Created by bozyurt on 8/28/15.
 */
public class KeywordHierarchyHelper {
    String serviceURL = "http://tikki.neuinfo.org:9000/";
    static Map<String, String> lruCache = Collections.synchronizedMap(new LRUCache<String, String>(1000));

    public String getImmediateParent(String ontologyId) throws Exception {
        String immediateParent = lruCache.get(ontologyId);
        if (immediateParent != null) {
            return immediateParent;
        }
        HttpClient client = new DefaultHttpClient();
        URIBuilder builder = new URIBuilder(this.serviceURL);
        builder.setPath("/scigraph/graph/neighbors");
        builder.setParameter("id", ontologyId);
        builder.setParameter("depth", "1");
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
                System.out.println(json.toString(2));
                System.out.println("================");
                immediateParent = prepHierarchy(json, ontologyId);
                lruCache.put(ontologyId, immediateParent);
            }
        } finally {
            if (httpGet != null) {
                httpGet.releaseConnection();
            }
        }
        return null;

    }

    String prepHierarchy(JSONObject json, String id) {
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
        Assertion.assertTrue(startEdgeSet.size() == 1);
        Assertion.assertNotNull(startEdge);
        KWNode n = nodeMap.get(startEdge.obj);
        String pathPart = n.label;
        if (n.label == null || n.label.length() == 0) {
            pathPart = n.id;
            int hashIdx = pathPart.lastIndexOf('#');
            if (hashIdx != -1) {
                pathPart = pathPart.substring(hashIdx + 1);
            }
        }
        return pathPart;
    }

    public static void main(String[] args) throws Exception {
        KeywordHierarchyHelper helper = new KeywordHierarchyHelper();
        System.out.println(helper.getImmediateParent("ENVO_00002871"));
    }

}
