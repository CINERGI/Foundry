package org.neuinfo.foundry.common.util;

import org.json.JSONObject;

/**
 * Created by bozyurt on 9/22/17.
 */
public class DiffRecord {
    String type;
    int updateSequenceNo;
    String oldValue;
    String newValue;
    String name;
    String jsonPath;


    public DiffRecord() {
    }

    public DiffRecord(String type, String jsonPath, String oldValue, String newValue) {
        this.type = type;
        this.jsonPath = jsonPath;
        this.oldValue = oldValue;
        this.newValue = newValue;
    }

    public static DiffRecord fromJSON(JSONObject json) {
        DiffRecord df = new DiffRecord();
        df.name = json.getString("name");
        df.type = json.getString("type");
        df.oldValue = json.getString("oldValue");
        df.newValue = json.getString("newValue");
        if (json.has("updatePath")) {
            df.jsonPath = json.getString("updatePath");
        } else if (json.has("deletePath")) {
            df.jsonPath = json.getString("deletePath");
        } else if (json.has("insertPath")){
            df.jsonPath = json.getString("insertPath");
        }
        df.updateSequenceNo = json.getInt("UpdateSequenceNo");
        return df;
    }

    public String getType() {
        return type;
    }

    public int getUpdateSequenceNo() {
        return updateSequenceNo;
    }

    public String getOldValue() {
        return oldValue;
    }

    public String getNewValue() {
        return newValue;
    }

    public String getName() {
        return name;
    }

    public String getJsonPath() {
        return jsonPath;
    }
}
