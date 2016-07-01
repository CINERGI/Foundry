package org.neuinfo.foundry.common.util;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.json.JSONArray;
import org.json.JSONObject;
import org.neuinfo.foundry.common.Constants;
import org.neuinfo.foundry.common.model.Keyword;

import java.util.*;

/**
 * Created by bozyurt on 6/10/16.
 */
public class ISOXMLGenerator2 {
    private Namespace gmd = Namespace.getNamespace("gmd", "http://www.isotc211.org/2005/gmd");
    private Namespace gmi = Namespace.getNamespace("gmi", "http://www.isotc211.org/2005/gmi");
    private Namespace xlink = Namespace.getNamespace("xlink", "http://www.w3.org/1999/xlink");
    private Namespace gmx = Namespace.getNamespace("gmx", "http://www.isotc211.org/2005/gmx");

    public Element generate(DBObject docWrapper) throws Exception {
        DBObject originalDoc = (DBObject) docWrapper.get("OriginalDoc");

        DBObject data = (DBObject) docWrapper.get("Data");
        DBObject spatial = (DBObject) data.get("spatial");
        JSONObject originalDocJson = JSONUtils.toJSON((BasicDBObject) originalDoc, false);
        XML2JSONConverter converter = new XML2JSONConverter();
        Element docEl = converter.toXML(originalDocJson);
        // MI_Metadata
        if (!docEl.getName().equals("MI_Metadata")) {
            docEl.setName("MI_Metadata");
            docEl.setNamespace(gmi);
            if (docEl.getNamespace("gmd") == null) {
                docEl.addNamespaceDeclaration(gmd);
            }
        }
        if (docEl.getNamespace("xlink") == null) {
            docEl.addNamespaceDeclaration(xlink);
        }
        if (docEl.getNamespace("gmx") == null) {
            docEl.addNamespaceDeclaration(gmx);
        }
        if (spatial != null) {
            JSONObject spatialJson = JSONUtils.toJSON((BasicDBObject) spatial, false);
            Object bbObj = spatialJson.get("bounding_boxes");
            if ((bbObj instanceof JSONArray)) {
                docEl = ISOXMLGeneratorSupport.addSpatialExtent(docEl, spatialJson);
            }
        }

        Map<String, List<KeywordInfo>> category2KWIListMap = new HashMap<String, List<KeywordInfo>>(7);
        if (data.containsField("orgKeywords")) {
            DBObject kwDBO = (DBObject) data.get("orgKeywords");
            JSONArray jsArr = JSONUtils.toJSONArray((BasicDBList) kwDBO);
            for (int i = 0; i < jsArr.length(); i++) {
                JSONObject kwJson = jsArr.getJSONObject(i);
                Keyword kw = Keyword.fromJSON(kwJson);
                Set<String> categories = kw.getCategories();
                if (categories.size() == 1) {
                    String category = categories.iterator().next();
                    Set<String> ids = kw.getIds();
                    if (!ids.isEmpty()) {
                        KeywordInfo kwi = new KeywordInfo(ids.iterator().next(),
                                kw.getTerm(), category, null,
                                CinergiXMLUtils.KeywordType.Organization);
                        List<KeywordInfo> kwiList = category2KWIListMap.get(category);
                        if (kwiList == null) {
                            kwiList = new ArrayList<KeywordInfo>(10);
                            category2KWIListMap.put(category, kwiList);
                        }
                        kwiList.add(kwi);
                    }
                }
            }
        }


        if (data.containsField("keywords")) {
            // need to fix bad IDs in the database:
            FacetHierarchyHandler fhh = FacetHierarchyHandler.getInstance(Constants.SCIGRAPH_URL);

            DBObject kwDBO = (DBObject) data.get("keywords");
            JSONArray jsArr = JSONUtils.toJSONArray((BasicDBList) kwDBO);
            for (int i = 0; i < jsArr.length(); i++) {
                JSONObject kwJson = jsArr.getJSONObject(i);
                Keyword keyword = Keyword.fromJSON(kwJson);
                String category = keyword.getFacetHierarchy();

                //KeywordInfo kwi = new KeywordInfo(keyword.getOntId(), keyword.getTerm(), category, keyword.getFullHierarchy());
                String ontId = fhh.getOntologyId(category);
                if ( ontId == null || ontId.isEmpty() ) {
                    ontId = keyword.getOntId();
                }
                KeywordInfo kwi = new KeywordInfo(ontId, keyword.getTerm(), category, keyword.getFullHierarchy());
                List<KeywordInfo> kwiList = category2KWIListMap.get(category);
                if (kwiList == null) {
                    kwiList = new ArrayList<KeywordInfo>(10);
                    category2KWIListMap.put(category, kwiList);
                }
                if (!kwiList.contains(kwi)) {
                    kwiList.add(kwi);
                }
            }
        }
        if (!category2KWIListMap.isEmpty()) {
            docEl = ISOXMLGeneratorSupport.addKeywords(docEl, category2KWIListMap, docWrapper);
        }
        // fix anchor problem if exists
        docEl = ISOXMLFixer.fixAnchorProblem(docEl);
        return docEl;
    }
}
