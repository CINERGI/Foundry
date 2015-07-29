package org.neuinfo.foundry.common.model;

import com.mongodb.DBObject;
import org.json.JSONObject;
import org.neuinfo.foundry.common.util.Utils;

/**
 * Created by bozyurt on 5/27/14.
 */
public class BatchInfo {
    private final String batchId;
    private int ingestedCount;
    private int submittedCount;
    private Status status = Status.NOT_STARTED;

    public BatchInfo(String batchId) {
        this.batchId = batchId;
    }

    public BatchInfo(String batchId, Status status) {
        this.batchId = batchId;
        this.status = status;
    }

    public String getBatchId() {
        return batchId;
    }

    public int getIngestedCount() {
        return ingestedCount;
    }

    public int getSubmittedCount() {
        return submittedCount;
    }

    public Status getStatus() {
        return status;
    }

    public void setIngestedCount(int ingestedCount) {
        this.ingestedCount = ingestedCount;
    }

    public void setSubmittedCount(int submittedCount) {
        this.submittedCount = submittedCount;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public JSONObject toJSON() {
        JSONObject js = new JSONObject();
        js.put("batchId", batchId);
        js.put("status", status.getCode());
        js.put("ingestedCount", ingestedCount);
        js.put("submittedCount", submittedCount);
        return js;
    }

    public static BatchInfo fromDbObject(DBObject dbo) {
        String batchId = (String) dbo.get("batchId");
        BatchInfo bi = new BatchInfo(batchId);
        int statusCode = Utils.getIntValue(dbo.get("status"), 0);
        bi.status = Status.fromCode(statusCode);
        bi.submittedCount = Utils.getIntValue(dbo.get("submittedCount"), 0);
        bi.ingestedCount = Utils.getIntValue(dbo.get("ingestedCount"), 0);

        return bi;
    }
}
