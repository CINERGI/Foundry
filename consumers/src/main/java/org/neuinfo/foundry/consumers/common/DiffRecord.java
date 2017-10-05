package org.neuinfo.foundry.consumers.common;

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
    String updatePath;


    public static DiffRecord fromJSON(JSONObject json) {
        DiffRecord df = new DiffRecord();
        df.name = json.getString("name");
        df.type = json.getString("type");
        df.oldValue = json.getString("oldValue");
        df.newValue = json.getString("newValue");
        if (json.has("updatePath")) {
            df.updatePath = json.getString("updatePath");
        } else {
            df.updatePath = json.getString("insertPath");
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

    public String getUpdatePath() {
        return updatePath;
    }
}
