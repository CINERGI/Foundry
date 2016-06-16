package org.neuinfo.foundry.consumers.jms.consumers.plugins;

import org.jdom2.Element;
import org.json.JSONArray;
import org.json.JSONObject;
import org.neuinfo.foundry.common.util.*;
import org.neuinfo.foundry.consumers.jms.consumers.jta.Keyword;

import java.util.*;

/**
 * Created by bozyurt on 6/9/16.
 */
public class KeywordEnhancer2Helper {

    public static Map<String, List<KeywordInfo>> getKeywordsToBeAdded(List<Keyword> keywordList,
                                                                      JSONObject originalDocJson) throws Exception {
        Inflector inflector = new Inflector();
        Map<String, List<KeywordInfo>> category2KWIListMap = new HashMap<String, List<KeywordInfo>>(7);
        for (Keyword keyword : keywordList) {
            String singularCCTerm = Inflector.toCamelCase(inflector.toSingular(keyword.getTerm()));
            if (!singularCCTerm.equals(keyword.getTerm())) {
                Keyword kwNew = new Keyword(singularCCTerm, keyword.getSpan(),
                        keyword.getOntID(), keyword.getFacet(), keyword.getFullHierarchy());

                keyword = kwNew;
            }
/*
            List<ScigraphMappingsHandler.FacetNode> fnList = ScigraphUtils.findFacetHierarchyGivenFacet(keyword.getFacet());
            if (fnList != null) {
                String category = ScigraphUtils.toCinergiCategory(fnList);
                // full hierarchy for web service
                String fullHierarchyPath = keyword.getFullHierarchy();
                if (category.indexOf(" > ") != -1) {
                    int idx = category.lastIndexOf(" > ");
                    fullHierarchyPath = category.substring(0, idx).trim() + " > " + fullHierarchyPath;
                }

                KeywordInfo kwi = new KeywordInfo(keyword.getOntID(), keyword.getTerm(), category, fullHierarchyPath);
                List<KeywordInfo> kwiList = category2KWIListMap.get(category);
                if (kwiList == null) {
                    kwiList = new ArrayList<KeywordInfo>(10);
                    category2KWIListMap.put(category, kwiList);
                }
                kwiList.add(kwi);
            }
*/
            for(int i = 0; i < keyword.getFacet().length; i++) {
                String category = keyword.getFacet()[i];
                String fullHierarchyPath = keyword.getFullHierarchy()[i];
                 KeywordInfo kwi = new KeywordInfo(keyword.getOntID()[i], keyword.getTerm(), category, fullHierarchyPath);
                List<KeywordInfo> kwiList = category2KWIListMap.get(category);
                if (kwiList == null) {
                    kwiList = new ArrayList<KeywordInfo>(10);
                    category2KWIListMap.put(category, kwiList);
                }
                kwiList.add(kwi);

            }
        } // for keywords
        if (!category2KWIListMap.isEmpty()) {
            for (List<KeywordInfo> kwiList : category2KWIListMap.values()) {
                CinergiXMLUtils.filterPlurals(kwiList);
            }
        }
        return category2KWIListMap;
    }



    public static void prepKeywordsProv(Map<String, List<KeywordInfo>> category2KWIListMap, ProvenanceHelper.ProvData provData) {
        if (category2KWIListMap == null || category2KWIListMap.isEmpty()) {
            provData.addModifiedFieldProv("No keywords are added");
            return;
        }
        for (String category : category2KWIListMap.keySet()) {
            List<KeywordInfo> kwiList = category2KWIListMap.get(category);
            StringBuilder sb = new StringBuilder(128);
            sb.append("Added keywords ");
            for (Iterator<KeywordInfo> iter = kwiList.iterator(); iter.hasNext(); ) {
                KeywordInfo kwi = iter.next();
                sb.append(kwi.getTerm());
                if (iter.hasNext()) {
                    sb.append(',');
                }
            }
            sb.append(" for category ").append(category);
            provData.addModifiedFieldProv(sb.toString().trim());
        }
    }
}
