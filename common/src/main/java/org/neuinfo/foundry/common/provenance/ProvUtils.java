package org.neuinfo.foundry.common.provenance;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import org.json.JSONObject;
import org.neuinfo.foundry.common.util.JSONUtils;

/**
 * Created by bozyurt on 6/1/16.
 */
public class ProvUtils {

    public static JSONObject prepare4Viewer(BasicDBList events) {
        JSONObject combined = new JSONObject();
        for (int i = 0; i < events.size(); i++) {
            BasicDBObject provData = (BasicDBObject) events.get(i);
            JSONObject json = JSONUtils.toJSON(provData, true);
            for (String key : json.keySet()) {
                if (key.equals("prefix")) {
                    continue;
                }
                if (!combined.has(key)) {
                    combined.put(key, new JSONObject());
                }
                JSONObject w = combined.getJSONObject(key);
                JSONObject o1 = json.getJSONObject(key);
                String subKey = o1.keySet().iterator().next();
                w.put(subKey, ProvUtils.cleanup(o1.getJSONObject(subKey)));
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
