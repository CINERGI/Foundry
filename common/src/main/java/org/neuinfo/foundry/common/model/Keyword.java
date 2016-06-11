package org.neuinfo.foundry.common.model;

import org.json.JSONArray;
import org.json.JSONObject;
import org.neuinfo.foundry.common.util.IHierarchyHandler;

import java.util.*;

/**
 * Created by bozyurt on 2/11/15.
 */
public class Keyword {
    private String term;
    private String ontId;
    private String fullHierarchy;
    private String facetHierarchy;

    List<EntityInfo> entityInfos = new LinkedList<EntityInfo>();

    public Keyword(String term) {
        this.term = term;
    }

    public Keyword(String term, String ontId, String fullHierarchy, String facetHierarchy) {
        this.term = term;
        this.ontId = ontId;
        this.fullHierarchy = fullHierarchy;
        this.facetHierarchy = facetHierarchy;
    }

    public void addEntityInfo(EntityInfo ei) {
        entityInfos.add(ei);
    }

    public String getTerm() {
        return term;
    }

    public List<EntityInfo> getEntityInfos() {
        return entityInfos;
    }

    public String getOntId() {
        return ontId;
    }

    public String getFullHierarchy() {
        return fullHierarchy;
    }

    public String getFacetHierarchy() {
        return facetHierarchy;
    }

    public Set<String> getCategories() {
        Set<String> categories = new HashSet<String>(7);
        for (EntityInfo ei : entityInfos) {
            if (ei.getCategory().length() > 0) {
                categories.add(ei.getCategory());
            }
        }
        return categories;
    }

    public boolean hasCategory() {
        for (EntityInfo ei : entityInfos) {
            if (ei.getCategory().length() > 0) {
                return true;
            }
        }
        return false;
    }

    public Set<String> getIds() {
        Set<String> idSet = new LinkedHashSet<String>();
        for (EntityInfo ei : entityInfos) {
            if (ei.getId() != null) {
                idSet.add(ei.getId());
            }
        }
        return idSet;
    }

    /**
     * returns the most specific category determined heuristically
     *
     * @param handler
     * @return the most specific category determined heuristically
     */
    public String getTheCategory(IHierarchyHandler handler) {
        String category = null;
        if (entityInfos.size() == 1) {
            return entityInfos.get(0).getCategory();
        }
        int maxPathLen = -10;
        for (EntityInfo ei : entityInfos) {
            if (ei.getCategory().length() > 0) {
                String categoryPath = handler.getCinergiCategory(ei.getCategory().toLowerCase());
                int pathLength2Root = handler.getPathLength2Root(categoryPath);
                if (pathLength2Root > maxPathLen) {
                    maxPathLen = pathLength2Root;
                    category = ei.getCategory();
                }
                if (ei.hasOtherCategories()) {
                    for (String cat : ei.getOtherCategories()) {
                        if (cat.length() > 0) {
                            categoryPath = handler.getCinergiCategory(cat.toLowerCase());
                            pathLength2Root = handler.getPathLength2Root(categoryPath);
                            if (pathLength2Root > maxPathLen) {
                                maxPathLen = pathLength2Root;
                                category = cat;
                            }
                        }
                    }
                }
            }
        }
        if (category == null) {
            for (EntityInfo ei : entityInfos) {
                if (ei.getCategory().length() > 0) {
                    category = ei.getCategory();
                }
            }
        }
        return category;
    }

    public EntityInfo getTheCategoryEntityInfo(IHierarchyHandler handler) {
        EntityInfo theEI = null;
        if (entityInfos.size() == 1) {
            return entityInfos.get(0);
        }
        if (hasCategory()) {
            int maxPathLen = -10;
            for (EntityInfo ei : entityInfos) {
                if (ei.getCategory().length() > 0) {
                    String categoryPath = handler.getCinergiCategory(ei.getCategory().toLowerCase());
                    int pathLength2Root = handler.getPathLength2Root(categoryPath);
                    if (pathLength2Root > maxPathLen) {
                        maxPathLen = pathLength2Root;
                        theEI = ei;
                    }
                }
                if (ei.hasOtherCategories()) {
                    for (String category : ei.getOtherCategories()) {
                        if (category.length() > 0) {
                            String categoryPath = handler.getCinergiCategory(category.toLowerCase());
                            int pathLength2Root = handler.getPathLength2Root(categoryPath);
                            if (pathLength2Root > maxPathLen) {
                                maxPathLen = pathLength2Root;
                                theEI = ei;
                            }
                        }
                    }
                }
            }
        }

        if (theEI == null) {
            for (EntityInfo ei : entityInfos) {
                if (ei.getCategory().length() > 0) {
                    theEI = ei;
                }
            }
        }
        return theEI;
    }

    public boolean hasAnyCategory(Set<String> allowedSet) {
        for (EntityInfo ei : entityInfos) {
            if (ei.getCategory().length() > 0) {
                if (allowedSet.contains(ei.getCategory())) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean hasCategory(String category) {
        for (EntityInfo ei : entityInfos) {
            if (ei.getCategory().length() > 0) {
                if (ei.getCategory().equals(category)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Keyword{");
        sb.append("term='").append(term).append('\'');
        sb.append(", entityInfos=").append(entityInfos);
        sb.append('}');
        return sb.toString();
    }

    public JSONObject toJSON() {
        JSONObject js = new JSONObject();
        js.put("term", term);
        if (ontId != null) {
            js.put("ontId", ontId);
        }
        if (fullHierarchy != null) {
            js.put("fullHierarchy", fullHierarchy);
        }
        if (facetHierarchy != null) {
            js.put("facetHierarchy", facetHierarchy);
        }
        JSONArray jsArr = new JSONArray();
        js.put("entityInfos", jsArr);
        for (EntityInfo ei : entityInfos) {
            jsArr.put(ei.toJSON());
        }
        return js;
    }

    public static Keyword fromJSON(JSONObject json) {
        String term = json.getString("term");
        String ontId = json.has("ontId") ? json.getString("ontId") : null;
        String facet = json.has("fullHierarchy") ? json.getString("fullHierarchy") : null;
        String facetHierarchy = json.has("facetHierarchy") ? json.getString("facetHierarchy") : null;
        Keyword kw = new Keyword(term, ontId, facet, facetHierarchy);
        JSONArray jsArr = json.getJSONArray("entityInfos");

        for (int i = 0; i < jsArr.length(); i++) {
            JSONObject js = jsArr.getJSONObject(i);
            kw.addEntityInfo(EntityInfo.fromJSON(js));
        }
        return kw;
    }
}
