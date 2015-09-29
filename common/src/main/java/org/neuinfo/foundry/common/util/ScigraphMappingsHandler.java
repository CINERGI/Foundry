package org.neuinfo.foundry.common.util;

import java.io.IOException;
import java.util.*;

/**
 * Created by bozyurt on 9/8/15.
 */
public class ScigraphMappingsHandler {
    static Map<String, List<FacetNode>> mappings = new HashMap<String, List<FacetNode>>();
    static Map<String, String> label2PreferredLabelMap = new HashMap<String, String>();
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
        text = Utils.loadAsString("/var/data/cinergi/preferredLabels.txt");
        lines = text.split("\\n");
        for (String line : lines) {
            if (line.startsWith("#")) {
                continue;
            }
            String[] toks = line.trim().split("\\s*,\\s*");
            if (toks.length == 2) {
                label2PreferredLabelMap.put(toks[0].trim(), toks[1].trim());
            }
        }
    }

    public String getPreferredLabel(String scigraphLabel) {
        return label2PreferredLabelMap.get(scigraphLabel);
    }

    public List<FacetNode> findFacetHierarchy(String thirdLevelCurrie) {
        /*
        if (thirdLevelCurrie.equals("CHEBI_24431")) {
            return Arrays.asList(new FacetNode("Chemical Entity","http://purl.obolibrary.org/obo/CHEBI_24431"));
        }
        */
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
