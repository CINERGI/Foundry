package org.neuinfo.foundry.common.provenance;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import org.json.JSONObject;
import org.neuinfo.foundry.common.util.Assertion;
import org.neuinfo.foundry.common.util.JSONUtils;
import org.neuinfo.foundry.common.util.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by bozyurt on 6/1/16.
 */
public class ProvUtils {


    static void adjustIds(JSONObject eventJson, Map<String, Integer> countMap) {
        Map<String, TagIndex> tiMap = new HashMap<String, TagIndex>();
        int entityOffset = countMap.get("entity");
        int activityOffset = countMap.get("activity");
        JSONObject json = new JSONObject();
        for (String key : eventJson.keySet()) {
            if (key.equals("activity") || key.equals("entity")) {
                JSONObject o1 = eventJson.getJSONObject(key);
                for (String subKey : o1.keySet()) {
                    TagIndex ti = new TagIndex(key, subKey);
                    int localIdx = ti.getLocalIdAsInt();
                    Assertion.assertTrue(localIdx != -1);
                    if (key.equals("entity")) {
                        ti.prepGlobalIdx(localIdx + entityOffset);
                    } else {
                        ti.prepGlobalIdx(localIdx + activityOffset);
                    }
                    tiMap.put(subKey, ti);
                }
                if (key.equals("entity")) {
                    countMap.put("entity", entityOffset + o1.keySet().size());
                } else {
                    countMap.put("activity", activityOffset + o1.keySet().size());
                }
            }
        }
        for (String key : eventJson.keySet()) {
            if (key.equals("prefix")) {
                continue;
            }
            JSONObject o1 = eventJson.getJSONObject(key);
            List<String> subKeys =  new ArrayList<String>(o1.keySet());
            for(String subKey : subKeys) {
                TagIndex tagIndex = tiMap.get(subKey);
                if (tagIndex != null) {
                    Object o = o1.remove(subKey);
                    o1.put(tagIndex.globalIdx, o);
                    updateIdValues((JSONObject) o, tiMap);
                }
            }
        }
    }

   static void updateIdValues(JSONObject json, Map<String,TagIndex> tiMap) {
        List<String> keys = new ArrayList<String>(json.keySet());
        for(String key : keys) {
            Object o = json.get(key);
            if (o instanceof JSONObject) {
                updateIdValues( (JSONObject) o, tiMap);
            } else {
                String value = json.getString(key);
                TagIndex tagIndex = tiMap.get(value);
                if (tagIndex != null) {
                    json.put(key, tagIndex.globalIdx);
                }
            }
        }
    }

    static class TagIndex {
        String tag;
        String localIdx;
        String globalIdx;

        public TagIndex(String tag, String localIdx) {
            this.tag = tag;
            this.localIdx = localIdx;
        }

        public int getLocalIdAsInt() {
            int idx = localIdx.lastIndexOf("_");
            return Utils.getIntValue(localIdx.substring(idx + 1), -1);
        }

        public void prepGlobalIdx(int globalId) {
            int idx = localIdx.lastIndexOf("_");
            this.globalIdx = localIdx.substring(0, idx + 1) + globalId;
        }
    }

    public static JSONObject prepare4Viewer(BasicDBList events) {
        JSONObject combined = new JSONObject();
        Map<String, Integer> countMap = new HashMap<String, Integer>();

        countMap.put("entity", 0);
        countMap.put("activity", 0);
        for (int i = 0; i < events.size(); i++) {
            BasicDBObject provData = (BasicDBObject) events.get(i);
            JSONObject json = JSONUtils.toJSON(provData, true);
            System.out.println(json.toString(2));


            for (String key : json.keySet()) {
                if (key.equals("prefix")) {
                    continue;
                }
                System.out.println("key:" + key);
                if (!combined.has(key)) {
                    combined.put(key, new JSONObject());
                }
                JSONObject w = combined.getJSONObject(key);
                JSONObject o1 = json.getJSONObject(key);
                for (String subKey : o1.keySet()) {
                    w.put(subKey, ProvUtils.cleanup(o1.getJSONObject(subKey)));
                }
            }
        }
        return combined;
    }

    public static JSONObject cleanup(JSONObject json) {
        String[] label2Match = {"foundry:label", "foundry:UUID", "foundry:creationTime", "foundry:version", "prov:how"};
        for (int i = 0; i < label2Match.length; i++) {
            if (json.has(label2Match[i])) {
                JSONObject so = json.getJSONObject(label2Match[i]);
                String value = so.getString("$");
                json.put(label2Match[i], value);
            }
        }
        return json;
    }
}
