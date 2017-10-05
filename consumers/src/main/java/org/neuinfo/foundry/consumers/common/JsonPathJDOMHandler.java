package org.neuinfo.foundry.consumers.common;

import org.json.JSONArray;
import org.json.JSONObject;
import org.neuinfo.foundry.common.util.Assertion;
import org.neuinfo.foundry.common.util.Utils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by bozyurt on 9/20/17.
 */
public class JsonPathJDOMHandler {


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


    public static void modifyJson(List<JsonPathNode> path, DiffRecord diffRecord, JSONObject source) {
        Iterator<JsonPathNode> it = path.iterator();
        Object p = source;
        boolean update = diffRecord.getType().equalsIgnoreCase("update");
        boolean add = diffRecord.getType().equalsIgnoreCase("add");
        while (it.hasNext()) {
            JsonPathNode pathNode = it.next();
            if (it.hasNext()) {

                if (pathNode.isArray()) {
                    Object c;
                    if (add && ! ((JSONObject) p).has(pathNode.getFullName())) {
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
