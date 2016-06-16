package org.neuinfo.foundry.common.util;

import java.io.IOException;
import java.util.*;

/**
 * Created by bozyurt on 9/8/15.
 */
public class ScigraphMappingsHandler {
    static Map<String, List<FacetNode>> mappings = new HashMap<String, List<FacetNode>>();
    /** */
    static Map<String, List<FacetNode>> mappings2 = new HashMap<String, List<FacetNode>>();

    static Map<String, String> label2PreferredLabelMap = new HashMap<String, String>();
    private static ScigraphMappingsHandler instance;

    public static synchronized ScigraphMappingsHandler getInstance() throws IOException {
        if (instance == null) {
            instance = new ScigraphMappingsHandler();
        }
        return instance;
    }


    private ScigraphMappingsHandler() throws IOException {
        // String text = Utils.loadAsString("/var/data/cinergi/mappings.txt");
        String text = Utils.loadAsStringFromClasspath("mappings.txt");
        String[] lines = text.split("\\n");
        for (String line : lines) {
            String[] toks = line.trim().split("\\s+>\\s+");
            if (toks.length == 3) {
                List<FacetNode> fnList = new ArrayList<FacetNode>(3);
                for (String tok : toks) {
                    int idx = tok.indexOf("http:");
                    if (idx == -1) {
                        // try https
                        idx = tok.indexOf("https:");
                    }
                    if (idx != -1) {
                        String label = tok.substring(0, idx).trim();
                        String id = tok.substring(idx).trim();
                        fnList.add(new FacetNode(label, id));
                    }
                }
                if (fnList.size() == 3) {
                    String key = ScigraphUtils.toCurie(fnList.get(2).getId());
                    mappings.put(key, fnList);
                    String key1 = ScigraphUtils.toCurie(fnList.get(1).getLabel());
                    mappings2.put(key1, fnList);
                    key1 = ScigraphUtils.toCurie(fnList.get(0).getLabel());
                    List<FacetNode> nfnList = Arrays.asList( fnList.get(0));
                    mappings2.put(key1, nfnList);
                } else {
                    System.err.println("line:" + line);
                }
            }
        }
        //text = Utils.loadAsString("/var/data/cinergi/preferredLabels.txt");
        text = Utils.loadAsStringFromClasspath("preferredLabels.txt");
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


    /**
     *
     * @param facet
     * @return
     */
    public List<FacetNode> findFacetHierarchyGivenFacet(String facet) {
        return mappings2.get(facet);
    }

    public List<String> getSortedCinergiFacets() {
        Set<FacetNodes> facetNodesSet = new HashSet<FacetNodes>();
        for (List<FacetNode> fnList : this.mappings.values()) {
            FacetNodes fn = new FacetNodes(fnList.subList(0,2));
            if (!facetNodesSet.contains(fn)) {
                facetNodesSet.add(fn);
            }
        }
        List<String> cinergiFacetStrings = new ArrayList<String>(facetNodesSet.size());
        for (FacetNodes facetNodes : facetNodesSet) {
            List<FacetNode> fnList = facetNodes.getFnList();
            StringBuilder sb = new StringBuilder(80);
            for (Iterator<FacetNode> iter = fnList.iterator(); iter.hasNext(); ) {
                String label = iter.next().getLabel();
                label = getPreferredLabel(label) != null ? getPreferredLabel(label) : label;
                sb.append(label);
                if (iter.hasNext()) {
                    sb.append(" > ");
                }
            }
            cinergiFacetStrings.add(sb.toString());
            // also add top level
            String label = fnList.get(0).getLabel();
            label = getPreferredLabel(label) != null ? getPreferredLabel(label) : label;
            if (!cinergiFacetStrings.contains(label)) {
                cinergiFacetStrings.add(label);
            }
        }
        Collections.sort(cinergiFacetStrings);
        return cinergiFacetStrings;
    }

    static class FacetNodes {
        final List<FacetNode> fnList;

        public FacetNodes(List<FacetNode> fnList) {
            this.fnList = fnList;
        }

        public List<FacetNode> getFnList() {
            return fnList;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            FacetNodes that = (FacetNodes) o;
            if (fnList.size() != that.fnList.size()) {
                return false;
            }
            for (int i = 0; i < fnList.size(); i++) {
                if (!fnList.get(i).equals(that.fnList.get(i))) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public int hashCode() {
            return fnList != null ? fnList.hashCode() : 0;
        }
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

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            FacetNode facetNode = (FacetNode) o;

            if (!label.equals(facetNode.label)) return false;
            return id.equals(facetNode.id);

        }

        @Override
        public int hashCode() {
            int result = label.hashCode();
            result = 31 * result + id.hashCode();
            return result;
        }
    }

    public static void main(String[] args) throws IOException {
        ScigraphMappingsHandler handler = ScigraphMappingsHandler.getInstance();
        List<FacetNode> facetHierarchy = handler.findFacetHierarchy("b98f3a77-397d-41d7-9507-e7a3e47210b1");
        System.out.println(facetHierarchy);

        List<String> sortedCinergiFacets = handler.getSortedCinergiFacets();
        for (String s : sortedCinergiFacets) {
            System.out.println("\t" + s);
        }

    }
}
