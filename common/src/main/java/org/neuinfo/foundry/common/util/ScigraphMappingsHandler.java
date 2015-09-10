package org.neuinfo.foundry.common.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by bozyurt on 9/8/15.
 */
public class ScigraphMappingsHandler {
    static Map<String, List<FacetNode>> mappings = new HashMap<String, List<FacetNode>>();
    private static ScigraphMappingsHandler instance;

    public static synchronized ScigraphMappingsHandler getInstance() throws IOException {
        if (instance == null) {
            instance = new ScigraphMappingsHandler();
        }
        return instance;
    }


    private ScigraphMappingsHandler() throws IOException {
        String text = Utils.loadAsString("/var/data/cinergi/mappings.txt");
        String[] lines = text.split("\\n");
        for (String line : lines) {
            String[] toks = line.trim().split("\\s+>\\s+");
            if (toks.length == 3) {
                List<FacetNode> fnList = new ArrayList<FacetNode>(3);
                for (String tok : toks) {
                    int idx = tok.indexOf("http:");
                    if (idx != -1) {
                        String label = tok.substring(0, idx).trim();
                        String id = tok.substring(idx).trim();
                        fnList.add(new FacetNode(label, id));
                    }
                }
                if (fnList.size() == 3) {
                    String key = ScigraphUtils.toCurie(fnList.get(2).getId());
                    mappings.put(key, fnList);
                }
            }
        }
    }

    public List<FacetNode> findFacetHierarchy(String thirdLevelCurrie) {
        return mappings.get(thirdLevelCurrie);
    }

    public static class FacetNode {
        final String label;
        final String id;

        public FacetNode(String label, String id) {
            this.label = label;
            this.id = id;
        }

        public String getLabel() {
            return label;
        }

        public String getId() {
            return id;
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("FacetNode{");
            sb.append("label='").append(label).append('\'');
            sb.append(", id='").append(id).append('\'');
            sb.append('}');
            return sb.toString();
        }
    }

    public static void main(String[] args) throws IOException {
        ScigraphMappingsHandler handler = ScigraphMappingsHandler.getInstance();
        List<FacetNode> facetHierarchy = handler.findFacetHierarchy("b98f3a77-397d-41d7-9507-e7a3e47210b1");
        System.out.println(facetHierarchy);

    }
}
