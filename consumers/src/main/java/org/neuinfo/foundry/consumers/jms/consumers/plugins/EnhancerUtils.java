package org.neuinfo.foundry.consumers.jms.consumers.plugins;

import org.jdom2.Element;
import org.json.JSONArray;
import org.json.JSONObject;
import org.neuinfo.foundry.common.Constants;
import org.neuinfo.foundry.common.model.EntityInfo;
import org.neuinfo.foundry.common.model.Keyword;
import org.neuinfo.foundry.common.util.*;
import org.neuinfo.foundry.common.util.KeywordInfo;
import org.neuinfo.foundry.common.util.ScigraphMappingsHandler.FacetNode;
import org.neuinfo.foundry.consumers.jms.consumers.plugins.ProvenanceHelper.ProvData;

import java.io.IOException;
import java.util.*;

/**
 * Created by bozyurt on 2/4/15.
 */
public class EnhancerUtils {
    //    private static CategoryHierarchyHandler chh = CategoryHierarchyHandler.getInstance();
    private static IHierarchyHandler chh;

    static {
        try {
            chh = FacetHierarchyHandler.getInstance(Constants.SCIGRAPH_URL);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Map<String, List<KeywordInfo>> getKeywordsToBeAdded(JSONArray keywordsJson,
                                                                      JSONObject originalDocJson) throws Exception {
        Inflector inflector = new Inflector();
        Map<String, List<KeywordInfo>> category2KWIListMap = new HashMap<String, List<KeywordInfo>>(7);
        for (int i = 0; i < keywordsJson.length(); i++) {
            JSONObject kwJson = keywordsJson.getJSONObject(i);
            Keyword kw = Keyword.fromJSON(kwJson);
            String singularCCTerm = Inflector.toCamelCase(inflector.toSingular(kw.getTerm()));
            if (!singularCCTerm.equals(kw.getTerm())) {
                Keyword kwNew = new Keyword(singularCCTerm);
                for (EntityInfo ei : kw.getEntityInfos()) {
                    kwNew.addEntityInfo(ei);
                }
                kw = kwNew;
            }

            // String category = kw.getTheCategory(chh);
            for (String id : kw.getIds()) {
                List<List<FacetNode>> fnListList = ScigraphUtils.getKeywordFacetHierarchy(id, kw.getTerm());
                for (List<FacetNode> fnList : fnListList) {
                    String category = ScigraphUtils.toCinergiCategory(fnList);
                    KeywordInfo kwi = new KeywordInfo(id, kw.getTerm(), category, null);
                    List<KeywordInfo> kwiList = category2KWIListMap.get(category);
                    if (kwiList == null) {
                        kwiList = new ArrayList<KeywordInfo>(10);
                        category2KWIListMap.put(category, kwiList);
                    }
                    kwiList.add(kwi);
                }
            }
            /*
            String cinergiCategory = chh.getCinergiCategory(category.toLowerCase());
            if (cinergiCategory != null) {
                category = cinergiCategory;
            }
            KeywordInfo kwi = new KeywordInfo(kw.getTerm(), category, null);
            List<KeywordInfo> kwiList = category2KWIListMap.get(category);
            if (kwiList == null) {
                kwiList = new ArrayList<KeywordInfo>(10);
                category2KWIListMap.put(category, kwiList);
            }
            kwiList.add(kwi);
            */
        }
        if (!category2KWIListMap.isEmpty()) {
            for (List<KeywordInfo> kwiList : category2KWIListMap.values()) {
                CinergiXMLUtils.filterPlurals(kwiList);
            }
            XML2JSONConverter converter = new XML2JSONConverter();
            Element docEl = converter.toXML(originalDocJson);
            category2KWIListMap = CinergiXMLUtils.getNewKeywords(docEl, category2KWIListMap);
        }
        return category2KWIListMap;
    }

    /**
     * @param keywordsArr
     * @param category2KWIListMap
     * @param keywordMap
     * @return
     */
    public static JSONArray filter(JSONArray keywordsArr, Map<String,
            List<KeywordInfo>> category2KWIListMap,
                                   Map<String, Keyword> keywordMap) {
        JSONArray filtered = new JSONArray();
        for (int i = 0; i < keywordsArr.length(); i++) {
            JSONObject kwJson = keywordsArr.getJSONObject(i);
            Keyword kw = Keyword.fromJSON(kwJson);
            for (String id : kw.getIds()) {
                String key = ScigraphUtils.prepKeywordMapKey(kw.getTerm(), id);
                Keyword keyword = keywordMap.get(key);
                if (keyword != null) {
                    if (hasMatch(category2KWIListMap, keyword)) {
                        filtered.put(kwJson);
                        break;
                    }
                }
            }
        }
        return filtered;
    }

    public static void useCinergiPreferredLabels(JSONArray keywordsArr) {
        ScigraphMappingsHandler handler = null;
        try {
            handler = ScigraphMappingsHandler.getInstance();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (handler != null) {
            for (int i = 0; i < keywordsArr.length(); i++) {
                JSONObject kwJson = keywordsArr.getJSONObject(i);
                String term = kwJson.getString("term");
                String newLabel = handler.getPreferredLabel(term);
                if (newLabel != null && !newLabel.equals(term)) {
                    kwJson.put("term", newLabel);
                }
            }
        }
    }

    public static JSONArray filterCategories(JSONArray keywordsArr, Set<String> excludeCategorySet) {
        JSONArray filtered = new JSONArray();
        for (int i = 0; i < keywordsArr.length(); i++) {
            JSONObject kwJson = keywordsArr.getJSONObject(i);
            Keyword k = Keyword.fromJSON(kwJson);
            if (!k.hasAnyCategory(excludeCategorySet)) {
                filtered.put(kwJson);
            }
        }
        return filtered;
    }

    public static boolean hasMatch(Map<String, List<KeywordInfo>> category2KWIListMap, Keyword keyword) {
        for (String category : category2KWIListMap.keySet()) {
            List<KeywordInfo> kwiList = category2KWIListMap.get(category);
            for (KeywordInfo kwi : kwiList) {
                if (kwi.getTerm().equals(keyword.getTerm())) {
                    return true;
                }
            }
        }
        return false;
    }

    public static void prepKeywordsProv(Map<String, List<KeywordInfo>> category2KWIListMap, ProvData provData) {
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
