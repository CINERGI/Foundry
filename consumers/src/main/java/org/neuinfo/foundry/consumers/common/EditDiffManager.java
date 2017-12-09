package org.neuinfo.foundry.consumers.common;

import org.json.JSONArray;
import org.json.JSONObject;
import org.neuinfo.foundry.common.util.DiffRecord;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by bozyurt on 9/20/17.
 */
public class EditDiffManager {


    public static List<DiffRecord> extractEditDiffInformation(JSONObject json) {
        JSONArray metadataRecordLineageItems = json.getJSONArray("metadataRecordLineageItems");
        int len = metadataRecordLineageItems.length();
        List<JSONObject> uniqueItems = new ArrayList<JSONObject>(len);
        for(int i = 0; i < len; i++) {
            JSONObject item;
            if (metadataRecordLineageItems.getJSONObject(i).has("item")) {
               item =  metadataRecordLineageItems.getJSONObject(i).getJSONObject("item");
            } else {
                item = metadataRecordLineageItems.getJSONObject(i);
            }
            boolean foundSame = false;
            for(JSONObject uniqueItem : uniqueItems) {
                if (uniqueItem.similar(item)) {
                    foundSame = true;
                    break;
                }
            }
            if (!foundSame) {
                uniqueItems.add(item);
            }
        }

        List<DiffRecord> diffRecords = new ArrayList<DiffRecord>();
        for(JSONObject item : uniqueItems) {
            JSONArray metadataUpdates = item.getJSONArray("metadataUpdates");
            for(int j = 0; j < metadataUpdates.length(); j++) {
                DiffRecord diffRecord = DiffRecord.fromJSON(metadataUpdates.getJSONObject(j));
                diffRecords.add(diffRecord);
            }
        }
        return diffRecords;
    }
}
