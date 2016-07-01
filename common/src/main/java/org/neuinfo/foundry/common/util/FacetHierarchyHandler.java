package org.neuinfo.foundry.common.util;

import org.apache.commons.lang.StringUtils;
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
 * Created by bozyurt on 7/16/15.
 */
public class FacetHierarchyHandler implements IHierarchyHandler {
    KWNode root;
    Map<String, KWNode> categoryPath2NodeMap = new HashMap<String, KWNode>();
    Map<String, String> scigraph2CinergiCategoryMap = new HashMap<String, String>();
    Map<String, String> cinergi2ScigraphCategoryMap = new HashMap<String, String>();
    List<String> sortedCinergiCategories;
    Map<String, KWNode> label2NodeMap = new HashMap<String, KWNode>();
    static Map<String, String> exceptionMap = new HashMap<String, String>();
    Map<String, String> cinergiCategory2OntologyIdMap = new HashMap<String, String>();

    String serviceURL; // "http://tikki.neuinfo.org:9000/";
    private static FacetHierarchyHandler instance;

    static {
        exceptionMap.put("water body", "hydrologic feature");
        exceptionMap.put("observed property", "property"); //"measure");
        exceptionMap.put("named property", "property");
        exceptionMap.put("material entity", "material");
    }


    public static synchronized FacetHierarchyHandler getInstance(String serviceURL) throws Exception {
        if (instance == null) {
            instance = new FacetHierarchyHandler(serviceURL);
        }
        return instance;
    }

    private FacetHierarchyHandler(String serviceURL) throws Exception {
        this.serviceURL = serviceURL;
        getFacetHierarchy();
    }

    public void getFacetHierarchy() throws Exception {
        HttpClient client = new DefaultHttpClient();
        URIBuilder builder = new URIBuilder(this.serviceURL);
        builder.setPath("/scigraph/dynamic/facets");
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
                prepareFacetHierarchy(json, false);
            }
        } finally {
            if (httpGet != null) {
                httpGet.releaseConnection();
            }
        }
    }

    void prepareFacetHierarchy(JSONObject json, boolean verbose) {
        Map<String, KWNode> nodeMap = new HashMap<String, KWNode>();
        Map<String, KWEdge> edgeMap = new HashMap<String, KWEdge>();
        Map<String, KWEdge> objEdgeMap = new HashMap<String, KWEdge>();
        List<KWEdge> edges = new ArrayList<KWEdge>();
        JSONArray nodesArr = json.getJSONArray("nodes");
        for (int i = 0; i < nodesArr.length(); i++) {
            KWNode node = KWNode.fromJSON(nodesArr.getJSONObject(i));
            nodeMap.put(node.id, node);
        }
        JSONArray edgesArr = json.getJSONArray("edges");
        for (int i = 0; i < edgesArr.length(); i++) {
            KWEdge edge = KWEdge.fromJSON(edgesArr.getJSONObject(i));
            edgeMap.put(edge.sub, edge);
            objEdgeMap.put(edge.obj, edge);
            edges.add(edge);
        }

        List<KWNode> topLevelNodes = new LinkedList<KWNode>();
        Map<String, KWNode> topLevelNodeMap = new HashMap<String, KWNode>(27);
        Map<String, KWNode> parentNodeMap = new HashMap<String, KWNode>(27);
        for (KWNode n : nodeMap.values()) {
            if (!objEdgeMap.containsKey(n.id)) {
                topLevelNodes.add(n);
            }
            KWEdge se = edgeMap.get(n.id);
            if (se != null && se.pred.equals("subClassOf")) {
                KWNode parent = nodeMap.get(se.obj);
                if (parent != null && !parentNodeMap.containsKey(parent.id)) {
                    parentNodeMap.put(parent.id, parent);
                }
                n.setParent(parent);
                parent.addChild(n);
            }
        }
        Assertion.assertTrue(!parentNodeMap.isEmpty());
        Set<KWNode> usedNodeSet = new HashSet<KWNode>();
        for (KWNode n : parentNodeMap.values()) {
            KWNode p = n.getParent();
            if (p != null && p.getParent() == null) {
                topLevelNodeMap.put(p.id, p);
                populateUsedSet(p, usedNodeSet);
            } else if (p == null) {
                topLevelNodeMap.put(n.id, n);
                populateUsedSet(n, usedNodeSet);
            }
        }


        root = new KWNode("Root", "Root");
        for (KWNode n : topLevelNodeMap.values()) {
            root.addChild(n);
            fixHierarchy(n);
            // prepMapping(n);
        }
        for (KWNode n : nodeMap.values()) {
            if (n.label != null ) {
                label2NodeMap.put(n.label.toLowerCase(), n);
            }
        }
        fixExceptions();
        topLevelNodeMap.clear();
        for (KWNode c : root.getChildren()) {
            topLevelNodeMap.put(c.id, c);
            prepMapping(c);
        }
        if (verbose) {
            showHierarchy(topLevelNodeMap);
            System.out.println("# nodes:" + nodeMap.size() + " usedNodeSet:" + usedNodeSet.size());
            for (KWNode n : nodeMap.values()) {
                if (!usedNodeSet.contains(n)) {
                    System.out.println(n);
                }
            }
        }
    }

    private void fixHierarchy(KWNode parent) {
        if (parent.label.equalsIgnoreCase("equipment")) {
            KWNode grandParent = parent.getParent();
            if (grandParent != null) {
                grandParent.getChildren().remove(parent);
                parent.setParent(this.root);
                this.root.addChild(parent);
            }

        } else if (parent.label.equalsIgnoreCase("resource type")) {
            KWNode softwareNode = new KWNode("http://www.ebi.ac.uk/swo/SWO_0000001", "Software");
            if (parent.getChild(softwareNode.label) == null) {
                softwareNode.setParent(parent);
                parent.addChild(softwareNode);
            }
        } else if (parent.label.equalsIgnoreCase("property")) {
            KWNode otherNode = new KWNode("", "Property (Other)");
            if (parent.getChild(otherNode.label) == null) {
                otherNode.setParent(parent);
                parent.addChild(otherNode);
            }
        } else if (parent.label.equalsIgnoreCase("natural processes")) {
            KWNode grandParent = parent.getParent();
            if (grandParent != null) {
                grandParent.getChildren().remove(parent);
                for (KWNode c : parent.getChildren()) {
                    c.setParent(grandParent);
                    grandParent.addChild(c);
                }
            }
        }
        if (parent.hasChildren()) {
            List<KWNode> children = new ArrayList<KWNode>(parent.getChildren());
            for (KWNode c : children) {
                fixHierarchy(c);
            }
        }
    }

    private void fixExceptions() {
        for (String key : exceptionMap.keySet()) {
            key = exceptionMap.get(key);
            KWNode n = label2NodeMap.get(key);
            if (n != null && n.hasChildren()) {
                for (KWNode c : n.getChildren()) {
                    if ((c.id == null || c.id.isEmpty())) {
                        if (n.label.toLowerCase().equals("property")) {
                            c.id = "http://hydro10.sdsc.edu/cinergi_ontology/cinergi#namedProperty";
                        } else if (n.label.toLowerCase().equals("hydrologic feature")) {
                            c.id = "http://sweet.jpl.nasa.gov/2.3/realmHydro.owl#HydrosphereFeature";
                        }
                    }
                }
            }
        }
    }

    private void prepMapping(KWNode parent) {
        String categoryPath = prepCategoryPath(parent);
        categoryPath2NodeMap.put(categoryPath, parent);
        this.cinergi2ScigraphCategoryMap.put(categoryPath, parent.label.toLowerCase());
        this.scigraph2CinergiCategoryMap.put(parent.label.toLowerCase(), categoryPath);
        this.cinergiCategory2OntologyIdMap.put(categoryPath, parent.id);
        if (parent.hasChildren()) {
            for (KWNode c : parent.getChildren()) {
                prepMapping(c);
            }
        }
    }

    public String getOntologyId(String facetPath) {
        return this.cinergiCategory2OntologyIdMap.get(facetPath);
    }

    String prepCategoryPath(KWNode node) {
        List<String> path = new ArrayList<String>(5);
        while (node != null) {
            if (node.id.equals("Root")) {
                break;
            }
            path.add(node.label);
            node = node.parent;
        }
        if (path.isEmpty()) {
            path.add(node.label);
        }
        Collections.reverse(path);
        String categoryPath = StringUtils.join(path, " > ");
        return categoryPath;
    }


    public String getCinergiCategory(String category) {
        String key = category.toLowerCase();
        if (exceptionMap.containsKey(key)) {
            key = exceptionMap.get(key);
        }

        KWNode n = label2NodeMap.get(key);
        if (n != null && n.hasChildren()) {
            KWNode otherNode = null;
            for (KWNode c : n.getChildren()) {
                if (c.label.indexOf("Other") != -1) {
                    otherNode = c;
                    break;
                }
            }
            if (otherNode != null) {
                key = otherNode.label.toLowerCase();
            }
        }

        return this.scigraph2CinergiCategoryMap.get(key);
    }

    public String getScigraphCategory(String cinergiCategory) {
        return this.cinergi2ScigraphCategoryMap.get(cinergiCategory);
    }

    public List<String> getSortedCinergiCategories() {
        if (sortedCinergiCategories == null) {
            sortedCinergiCategories = new ArrayList<String>(this.cinergi2ScigraphCategoryMap.size());
            for (String category : cinergi2ScigraphCategoryMap.keySet()) {
                sortedCinergiCategories.add(category);
            }
            Collections.sort(sortedCinergiCategories);
        }
        return sortedCinergiCategories;
    }

    public int getPathLength2Root(String categoryPath) {
        if (categoryPath == null) {
            return -1;
        }
        KWNode node = categoryPath2NodeMap.get(categoryPath);
        if (node == null) {
            return -1;
        }
        int len = 0;
        while (node.parent != null) {
            len++;
            node = node.parent;
        }
        return len;
    }

    private void showHierarchy(Map<String, KWNode> topLevelNodeMap) {
        int level = 1;
        for (KWNode n : topLevelNodeMap.values()) {
            System.out.println(n);
            showChildren(n, level);
        }
    }

    private void showChildren(KWNode n, int level) {
        StringBuilder indent = new StringBuilder();
        for (int i = 0; i < level; i++) {
            indent.append("\t");
        }
        if (n.hasChildren()) {
            for (KWNode c : n.getChildren()) {
                System.out.println(indent.toString() + c);
                showChildren(c, level + 1);
            }
        }
    }

    private void populateUsedSet(KWNode n, Set<KWNode> usedNodeSet) {
        usedNodeSet.add(n);
        if (n.hasChildren()) {
            for (KWNode c : n.getChildren()) {
                populateUsedSet(c, usedNodeSet);
            }
        }
    }

    public static void main(String[] args) throws Exception {
        FacetHierarchyHandler handler = FacetHierarchyHandler.getInstance("http://ec-scigraph.sdsc.edu:9000/");

        List<String> sortedCinergiCategories = handler.getSortedCinergiCategories();
        System.out.println("============================");
        for (String cc : sortedCinergiCategories) {
            System.out.println(cc);
        }

        System.out.println("============================");
        for (String key : handler.cinergiCategory2OntologyIdMap.keySet()) {
            System.out.println(key + " -> " + handler.getOntologyId(key));
        }
    }

}
