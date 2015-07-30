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

import java.net.URI;
import java.util.*;

import org.neuinfo.foundry.common.util.KeywordHierarchyHandler.Edge;
import org.neuinfo.foundry.common.util.KeywordHierarchyHandler.Node;

/**
 * Created by bozyurt on 7/16/15.
 */
public class FacetHierarchyHandler implements IHierarchyHandler{
    Node root;
    Map<String,Node> categoryPath2NodeMap = new HashMap<String, Node>();
    Map<String, String> scigraph2CinergiCategoryMap = new HashMap<String, String>();
    Map<String, String> cinergi2ScigraphCategoryMap = new HashMap<String, String>();
    List<String> sortedCinergiCategories;
    Map<String,Node> label2NodeMap = new HashMap<String, Node>();
    static Map<String,String> exceptionMap = new HashMap<String, String>();

    String serviceURL = "http://tikki.neuinfo.org:9000/";
    private static FacetHierarchyHandler instance;
    static {
        exceptionMap.put("water body","hydrosphere feature");
        exceptionMap.put("observed property","property"); //"measure");
        exceptionMap.put("named property","property");
        exceptionMap.put("material entity","material");
    }


    public static synchronized FacetHierarchyHandler getInstance() throws Exception {
        if (instance == null) {
            instance = new FacetHierarchyHandler();
        }
        return instance;
    }
    private FacetHierarchyHandler() throws Exception{
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
                prepareFacetHierarchy(json);
            }
        } finally {
            if (httpGet != null) {
                httpGet.releaseConnection();
            }
        }
    }

    void prepareFacetHierarchy(JSONObject json) {
        Map<String, Node> nodeMap = new HashMap<String, Node>();
        Map<String, Edge> edgeMap = new HashMap<String, Edge>();
        Map<String, Edge> objEdgeMap = new HashMap<String, Edge>();
        List<Edge> edges = new ArrayList<Edge>();
        JSONArray nodesArr = json.getJSONArray("nodes");
        for (int i = 0; i < nodesArr.length(); i++) {
            Node node = Node.fromJSON(nodesArr.getJSONObject(i));
            nodeMap.put(node.id, node);
        }
        JSONArray edgesArr = json.getJSONArray("edges");
        for (int i = 0; i < edgesArr.length(); i++) {
            Edge edge = Edge.fromJSON(edgesArr.getJSONObject(i));
            edgeMap.put(edge.sub, edge);
            objEdgeMap.put(edge.obj, edge);
            edges.add(edge);
        }

        List<Node> topLevelNodes = new LinkedList<Node>();
        Map<String, Node> topLevelNodeMap = new HashMap<String, Node>(27);
        Map<String, Node> parentNodeMap = new HashMap<String, Node>(27);
        for (Node n : nodeMap.values()) {
            if (!objEdgeMap.containsKey(n.id)) {
                topLevelNodes.add(n);
            }
            Edge se = edgeMap.get(n.id);
            if (se != null && se.pred.equals("subClassOf")) {
                Node parent = nodeMap.get(se.obj);
                if (parent != null && !parentNodeMap.containsKey(parent.id)) {
                    parentNodeMap.put(parent.id, parent);
                }
                n.setParent(parent);
                parent.addChild(n);
            }
        }
        Assertion.assertTrue(!parentNodeMap.isEmpty());
        Set<Node> usedNodeSet = new HashSet<Node>();
        for (Node n : parentNodeMap.values()) {
            Node p = n.getParent();
            if (p != null && p.getParent() == null) {
                topLevelNodeMap.put(p.id, p);
                populateUsedSet(p, usedNodeSet);
            } else if (p == null) {
                topLevelNodeMap.put(n.id, n);
                populateUsedSet(n, usedNodeSet);
            }
        }


        root = new Node("Root", "Root");
        for(Node n : topLevelNodeMap.values()) {
            root.addChild(n);
            fixHierarchy(n);
            prepMapping(n);
        }
        for(Node n : nodeMap.values()) {
            label2NodeMap.put(n.label.toLowerCase(), n);
        }

        topLevelNodeMap.clear();
        for(Node c : root.getChildren()) {
            topLevelNodeMap.put(c.id, c);
        }

        showHierarchy(topLevelNodeMap);
        System.out.println("# nodes:" + nodeMap.size() + " usedNodeSet:" + usedNodeSet.size());
        for (Node n : nodeMap.values()) {
            if (!usedNodeSet.contains(n)) {
                System.out.println(n);
            }
        }
    }

    private void fixHierarchy(Node parent) {
        if (parent.label.equalsIgnoreCase("equipment")) {
            Node grandParent = parent.getParent();
            if (grandParent != null) {
                grandParent.getChildren().remove(parent);
                parent.setParent(this.root);
                this.root.addChild(parent);
            }

        } else if (parent.label.equalsIgnoreCase("resource type")) {
            Node softwareNode = new Node("http://www.ebi.ac.uk/swo/SWO_0000001","Software");
            if (parent.getChild(softwareNode.label) == null) {
                softwareNode.setParent(parent);
                parent.addChild(softwareNode);
            }
        } else if (parent.label.equalsIgnoreCase("property")) {
            Node otherNode = new Node("","Property (Other)");
            if (parent.getChild(otherNode.label) == null) {
                otherNode.setParent(parent);
                parent.addChild(otherNode);
            }
        }
        if (parent.hasChildren()) {
            List<Node> children = new ArrayList<Node>(parent.getChildren());
            for (Node c : children) {
                   fixHierarchy(c);
            }
        }
    }

    private void prepMapping(Node parent) {
        String categoryPath = prepCategoryPath(parent);
        categoryPath2NodeMap.put(categoryPath, parent);
        this.cinergi2ScigraphCategoryMap.put(categoryPath, parent.label.toLowerCase());
        this.scigraph2CinergiCategoryMap.put(parent.label.toLowerCase(), categoryPath);
        if (parent.hasChildren()) {
            for(Node c : parent.getChildren()) {
                prepMapping(c);
            }
        }
    }


    String prepCategoryPath(Node node) {
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
        String key = category;
        if (exceptionMap.containsKey(key)) {
            key = exceptionMap.get(key);
        }

        Node n = label2NodeMap.get(key);
        if (n != null && n.hasChildren()) {
            Node otherNode = null;
            for(Node c : n.getChildren()) {
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
            for(String category : cinergi2ScigraphCategoryMap.keySet()) {
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
        Node node = categoryPath2NodeMap.get(categoryPath);
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

    private void showHierarchy(Map<String, Node> topLevelNodeMap) {
        int level = 1;
        for (Node n : topLevelNodeMap.values()) {
            System.out.println(n);
            showChildren(n, level);
        }
    }

    private void showChildren(Node n, int level) {
        StringBuilder indent = new StringBuilder();
        for(int i = 0; i < level; i++) {
            indent.append("\t");
        }
        if (n.hasChildren()) {
            for (Node c : n.getChildren()) {
                System.out.println(indent.toString() + c);
                showChildren(c, level + 1);
            }
        }
    }

    private void populateUsedSet(Node n, Set<Node> usedNodeSet) {
        usedNodeSet.add(n);
        if (n.hasChildren()) {
            for (Node c : n.getChildren()) {
                populateUsedSet(c, usedNodeSet);
            }
        }
    }

    public static void main(String[] args) throws Exception {
        FacetHierarchyHandler handler = FacetHierarchyHandler.getInstance();

    }

}
