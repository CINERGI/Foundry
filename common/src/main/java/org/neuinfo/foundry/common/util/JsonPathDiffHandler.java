package org.neuinfo.foundry.common.util;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.*;

/**
 * Created by bozyurt on 9/20/17.
 */
public class JsonPathDiffHandler {


    public static List<JsonPathNode> parseJsonPath(String diffJsonPath) {
        String[] tokens = diffJsonPath.split("\\.");
        List<JsonPathNode> path = new ArrayList<JsonPathNode>(tokens.length);
        int i = 0;
        while (i < tokens.length) {
            String token = tokens[i];
            int idx = Integer.MIN_VALUE;
            if ((i + 1) < tokens.length && Utils.isNumber(tokens[i + 1])) {
                idx = Utils.getIntValue(tokens[i + 1], Integer.MIN_VALUE);
                i += 2;
            } else {
                i++;
            }

            path.add(new JsonPathNode(token, idx != Integer.MIN_VALUE ? idx : null));
        }
        return path;
    }


    public static void createUpdateKeywordDiffJson(List<JsonPathNode> path, DiffRecord diffRecord,
                                                   JSONObject source, JSONArray dest) {
        Iterator<JsonPathNode> it = path.iterator();
        JsonPathNode firstNode = it.next();
        Assertion.assertTrue(firstNode.getLocalName().equals("enhancedKeywords") && firstNode.isArray());
        JSONArray jsArr = source.getJSONArray(firstNode.getFullName());
        JSONObject sourceCloneJson = JSONUtils.clone(jsArr.getJSONObject(firstNode.arrIdx));
        List<JsonPathNode> relativePath = path.subList(1, path.size());
        modifyJson(relativePath, diffRecord, sourceCloneJson);

        //System.out.println(sourceCloneJson.toString(2));
        JSONObject diffWrapper = new JSONObject();
        diffWrapper.put("enhancedKeyword", sourceCloneJson);
        diffWrapper.put("diffType", diffRecord.getType());
        diffWrapper.put("diffPath",diffRecord.getJsonPath());
        if (dest.length() == 0) {
            dest.put(diffWrapper);
        } else {
            String refKey = prepEnhancedKeywordKey(sourceCloneJson);
            int theIdx = -1;
            for(int i = 0; i < dest.length(); i++) {
                String key = prepEnhancedKeywordKey(dest.getJSONObject(i).getJSONObject("enhancedKeyword"));
                if (key.equals(refKey)) {
                    theIdx = i;
                    break;
                }
            }
            if (theIdx != -1) {
                dest.put(theIdx, diffWrapper);
            } else {
                dest.put(diffWrapper);
            }
        }
    }

    public static String prepEnhancedKeywordKey(JSONObject ekJson) {
        StringBuilder sb = new StringBuilder(100);
        sb.append( ekJson.getString("id")).append(':');
        sb.append( ekJson.getString("term")).append(':');
        sb.append(ekJson.getString("hierarchyPath"));
        return sb.toString();
    }

    public static void modifyJson(List<JsonPathNode> path, DiffRecord diffRecord, JSONObject source) {
        Iterator<JsonPathNode> it = path.iterator();
        Object p = source;
        boolean update = diffRecord.getType().equalsIgnoreCase("update");
        boolean add = diffRecord.getType().equalsIgnoreCase("add");
        boolean delete = diffRecord.getType().equalsIgnoreCase("delete");
        while (it.hasNext()) {
            JsonPathNode pathNode = it.next();
            if (it.hasNext()) {

                if (pathNode.isArray()) {
                    Object c;
                    if (add && !((JSONObject) p).has(pathNode.getFullName())) {
                        JSONArray arr = new JSONArray();
                        ((JSONObject) p).put(pathNode.getFullName(), arr);
                        c = arr;
                    } else {
                        c = ((JSONObject) p).get(pathNode.getFullName());
                    }
                    Object o;
                    if (c instanceof JSONArray) {
                        if (pathNode.arrIdx < 0 && add) {
                            JSONObject item = new JSONObject();
                            ((JSONArray) c).put(item);
                            o = item;
                        } else {
                            o = ((JSONArray) c).get(pathNode.arrIdx);
                        }
                    } else {
                        o = c;
                    }
                    if (update) {
                        Assertion.assertNotNull(o, pathNode.getFullName());
                    }
                    p = o;
                } else {
                    JSONObject c;
                    if (add && !((JSONObject) p).has(pathNode.getFullName())) {
                        c = new JSONObject();
                        ((JSONObject) p).put(pathNode.getFullName(), c);
                    } else {
                        c = ((JSONObject) p).getJSONObject(pathNode.getFullName());
                    }
                    if (update) {
                        Assertion.assertNotNull(c, pathNode.getFullName());
                    }
                    p = c;
                }
            } else {
                // last element
                if (update) {
                    if (pathNode.isArray()) {
                        JSONArray a = ((JSONObject) p).getJSONArray(pathNode.getFullName());
                        a.put(pathNode.arrIdx, diffRecord.getNewValue());
                    } else {
                        // String value = ((JSONObject) p).getString(pathNode.getFullName());
                        ((JSONObject) p).put(pathNode.getFullName(), diffRecord.getNewValue());
                    }
                } else {
                    if (add) {
                        ((JSONObject) p).put(pathNode.getFullName(), diffRecord.getNewValue());
                    } else if (delete) {
                        Assertion.assertTrue(p instanceof JSONObject);
                        if (pathNode.isArray()) {
                            JSONArray a = ((JSONObject) p).getJSONArray(pathNode.getFullName());
                            a.remove(pathNode.arrIdx);
                        } else {
                            ((JSONObject) p).remove(pathNode.getFullName());
                        }
                    }
                }
            }
        }
    }

    static boolean setField(JSONObject json, String newValue) {
        if (json.keySet().size() > 1) {
            return false;
        }
        JSONObject p = json;
        do {
            Object o = p.get(p.keys().next());
            if (o instanceof JSONObject) {
                p = (JSONObject) o;
                if (p.keySet().size() != 1) {
                    return false;
                }
            } else {
                if (o instanceof String) {
                    p.put(p.keys().next(), newValue);
                    return true;
                }
            }
        } while (p != null);
        return false;
    }


    public static Map<String, KeywordDiffRec> extractKeywordDiffRecords(JSONArray keywordDiffArray) {
        Map<String, KeywordDiffRec> map = new HashMap<String, KeywordDiffRec>();
        for (int i = 0; i < keywordDiffArray.length(); i++){
            JSONObject json = keywordDiffArray.getJSONObject(i);
            KeywordDiffRec rec = KeywordDiffRec.fromJSON(json);
            StringBuilder sb = new StringBuilder();
            String key = sb.append(rec.getTerm()).append(':').append(rec.getId()).toString();
            map.put(key, rec);
        }
        return map;
    }


    public  static KeywordDiffRec findMatching(String keyword, String ontId, Map<String, KeywordDiffRec> map) {
        StringBuilder sb = new StringBuilder();
        String key = sb.append(keyword).append(':').append(ontId).toString();
        if (map.containsKey(key)) {
            return map.remove(key);
        }
        return null;
    }


    public static boolean shouldRemoveKeyword(KeywordDiffRec keywordDiffRec) {
        if (!keywordDiffRec.isValidation() && keywordDiffRec.diffType.equals("update")) {
            return true;
        }
        return false;
    }

    public static boolean shouldAddKeyword(KeywordDiffRec keywordDiffRec) {
        if (keywordDiffRec.isValidation() && keywordDiffRec.getDiffType().equals("add")) {
            if (!Utils.isEmpty(keywordDiffRec.getCategory()) && !Utils.isEmpty(keywordDiffRec.getHierarchyPath())) {
                return true;
            }
        }
        return false;
    }


    public static class KeywordDiffRec {
        String id;
        String term;
        String category;
        String hierarchyPath;
        String type;
        String lastChangedDate;
        boolean validation;
        String diffType;
        String diffPath;

        public KeywordDiffRec() {
        }

        public String getId() {
            return id;
        }

        public String getTerm() {
            return term;
        }

        public String getCategory() {
            return category;
        }

        public String getHierarchyPath() {
            return hierarchyPath;
        }

        public String getType() {
            return type;
        }

        public String getLastChangedDate() {
            return lastChangedDate;
        }

        public boolean isValidation() {
            return validation;
        }

        public String getDiffType() {
            return diffType;
        }

        public String getDiffPath() {
            return diffPath;
        }

        public static KeywordDiffRec fromJSON(JSONObject json) {
            String diffType = json.getString("diffType");
            String diffPath = json.getString("diffPath");
            JSONObject kwJSON = json.getJSONObject("enhancedKeyword");
            String id = kwJSON.getString("id");
            KeywordDiffRec rec = new KeywordDiffRec();
            rec.id = id;
            rec.diffType = diffType;
            rec.diffPath = diffPath;
            rec.term = kwJSON.getString("term");
            rec.category = kwJSON.getString("category");
            rec.type = kwJSON.getString("type");
            rec.hierarchyPath = kwJSON.getString("hierarchyPath");
            rec.lastChangedDate = kwJSON.getString("lastChangedDate");
            rec.validation = true;
            if (kwJSON.has("validation")) {
                rec.validation = kwJSON.getBoolean("validation");
            }
            return rec;
        }


    }


    public static class JsonPathNode {
        String localName;
        String namespace;
        Integer arrIdx = null;

        public JsonPathNode(String pathNodeStr, Integer arrIdx) {
            this.arrIdx = arrIdx;
            int idx = pathNodeStr.indexOf(':');
            if (idx != -1) {
                this.namespace = pathNodeStr.substring(0, idx);
                this.localName = pathNodeStr.substring(idx + 1);
            } else {
                this.localName = pathNodeStr;
            }
        }

        public String getLocalName() {
            return localName;
        }

        public String getNamespace() {
            return namespace;
        }

        public boolean hasNamespace() {
            return namespace != null;
        }

        public boolean isArray() {
            return arrIdx != null;
        }

        public String getFullName() {
            if (namespace != null) {
                return namespace + ":" + localName;
            } else {
                return localName;
            }
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("JsonPathNode{");
            sb.append("localName='").append(localName).append('\'');
            sb.append(", namespace='").append(namespace).append('\'');
            if (arrIdx >= 0) {
                sb.append(", idx=").append(arrIdx);
            }
            sb.append('}');
            return sb.toString();
        }
    }


    public static void main(String[] args) {
        List<JsonPathNode> path = parseJsonPath("OriginalDoc.gmd:MD_Metadata.gmd:identificationInfo.gmd:MD_DataIdentification.gmd:descriptiveKeywords.0.gmd:MD_Keywords.gmd:keyword.5.gco:CharacterString._$");

        for (JsonPathNode jpn : path) {
            System.out.println(jpn);
        }
    }
}
