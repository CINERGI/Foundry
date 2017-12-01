package org.neuinfo.foundry.common.util;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import org.bson.Document;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by bozyurt on 4/8/14.
 */
public class JSONUtils {

    /**
     * given a selector path from root (no arrays) finds a nested element
     *
     * @param dbo
     * @param selectorPath
     * @return
     */
    public static Object findNested(DBObject dbo, String selectorPath) {
        String[] toks = selectorPath.split("\\.");
        DBObject p = dbo;
        for (String tok : toks) {
            final Object o = p.get(tok);
            if (o == null) {
                return null;
            }
            if (o instanceof DBObject) {
                p = (DBObject) o;
            } else {
                break;
            }
        }
        Object foundObj = p.get(toks[toks.length - 1]);
        return foundObj;
    }

    public static JSONObject clone(JSONObject original) {
        return new JSONObject(original.toString());
    }

    public static DBObject encode(JSONArray a) {
        return encode(a, false);
    }

    public static DBObject encode(JSONObject o) {
        return encode(o, false);
    }

    public static DBObject encode(JSONArray a, boolean escape$) {
        BasicDBList result = new BasicDBList();
        try {
            for (int i = 0; i < a.length(); ++i) {
                Object o = a.get(i);
                if (o instanceof JSONObject) {
                    result.add(encode((JSONObject) o, escape$));
                } else if (o instanceof JSONArray) {
                    result.add(encode((JSONArray) o));
                } else {
                    result.add(o);
                }
            }
            return result;
        } catch (JSONException je) {
            return null;
        }
    }


    public static DBObject encode(JSONObject o, boolean escape$) {
        BasicDBObject result = new BasicDBObject();
        try {
            Iterator i = o.keys();
            while (i.hasNext()) {
                String k = (String) i.next();
                Object v = o.get(k);
                // to make MongoDB happy
                if (escape$ && k.startsWith("$")) {
                    k = "_" + k;
                }
                if (k.indexOf('.') != -1) {
                    // mongo does not allow period in a key
                    k = k.replaceAll("\\.", "");
                }
                if (v instanceof JSONArray) {
                    result.put(k, encode((JSONArray) v));
                } else if (v instanceof JSONObject) {
                    result.put(k, encode((JSONObject) v, escape$));
                } else {
                    if (v == null || v.toString().equalsIgnoreCase("null")) {
                        v = "";
                    }
                    result.put(k, v);
                }
            }
            return result;
        } catch (JSONException je) {
            return null;
        }
    }

    public static JSONObject toJSON(Document document, boolean unEscape$) throws JSONException {
        final String jsonStr = document.toJson();
        JSONObject json = new JSONObject(jsonStr);
        if (unEscape$) {
            unEscapeJson(json);
        }
        return json;
    }

    /**
     * Given a MongoDB <code>BasicDBObject</code> convert it to a JSON object unescaping <code>_$</code> back to <code>$</code>
     * if <code>unEscape$</code> is true.
     *
     * @param dbObject
     * @param unEscape$
     * @return
     */
    public static JSONObject toJSON(BasicDBObject dbObject, boolean unEscape$) throws JSONException {
        final String jsonStr = dbObject.toString();
        JSONObject json = new JSONObject(jsonStr);
        if (unEscape$) {
            unEscapeJson(json);
        }
        return json;
    }


    public static JSONArray toJSONArray(BasicDBList dbList) {
        String jsonStr = dbList.toString();
        JSONArray jsArr = new JSONArray(jsonStr);
        return jsArr;
    }

    public static void escapeJson(JSONObject parent) throws JSONException {
        Set<String> keys = new HashSet<String>(parent.keySet());
        final Iterator<String> it = keys.iterator();
        while (it.hasNext()) {
            String key = it.next();
            final Object v = parent.get(key);
            if (key.startsWith("$")) {
                parent.remove(key);
                String newKey = "_" + key;
                parent.put(newKey, v);
            }
            if (v instanceof JSONObject) {
                escapeJson((JSONObject) v);
            } else if (v instanceof JSONArray) {
                escapeJson((JSONArray) v);
            }
        }
    }

    public static void escapeJson(JSONArray parent) throws JSONException {
        int len = parent.length();
        for (int i = 0; i < len; i++) {
            final Object o = parent.get(i);
            if (o instanceof JSONObject) {
                escapeJson((JSONObject) o);
            } else if (o instanceof JSONArray) {
                escapeJson((JSONArray) o);
            }
        }
    }

    public static void unEscapeJson(JSONObject parent) throws JSONException {
        Set<String> keys = new HashSet<String>(parent.keySet());
        final Iterator<String> it = keys.iterator();
        while (it.hasNext()) {
            String key = it.next();
            final Object v = parent.get(key);
            if (key.startsWith("_$")) {
                parent.remove(key);
                String newKey = key.substring(1);
                parent.put(newKey, v);
            }
            if (v instanceof JSONObject) {
                unEscapeJson((JSONObject) v);
            } else if (v instanceof JSONArray) {
                unEscapeJson((JSONArray) v);
            }
        }
    }

    private static void unEscapeJson(JSONArray parent) throws JSONException {
        int len = parent.length();
        for (int i = 0; i < len; i++) {
            final Object o = parent.get(i);
            if (o instanceof JSONObject) {
                unEscapeJson((JSONObject) o);
            } else if (o instanceof JSONArray) {
                unEscapeJson((JSONArray) o);
            }
        }
    }

    public static void add2JSON(JSONObject json, String name, String value) {
        if (value != null) {
            json.put(name, value);
        } else {
            json.put(name, "");
        }
    }

    public static JSONObject loadFromFile(String jsonFilePath) throws IOException {
        final String jsonStr = Utils.loadAsString(jsonFilePath);
        return new JSONObject(jsonStr);
    }

    public static boolean isEqual(JSONObject js1, JSONObject js2) {
        if (js1 == js2) {
            return true;
        }
        if (js1 == null || js2 == null) {
            return false;
        }
        return js1.similar(js2);
    }

    public static String toBsonDate(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
        return sdf.format(date);
    }

}
