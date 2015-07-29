package org.neuinfo.foundry.consumers.jms.consumers.plugins;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import org.json.JSONObject;
import org.neuinfo.foundry.consumers.common.Utils;
import org.neuinfo.foundry.consumers.plugin.IPlugin;
import org.neuinfo.foundry.consumers.plugin.Result;

import java.util.Map;

/**
 * Created by bozyurt on 11/20/14.
 */
public class CinergiElasticSearchIndexPreparer implements IPlugin {
    private String serverURL;
    private String indexPath;

    @Override
    public void initialize(Map<String, String> options) throws Exception {
        this.serverURL = options.get("serverURL");
        this.indexPath = options.get("indexPath");
    }

    @Override
    public Result handle(DBObject docWrapper) {
        try {
            BasicDBObject procDBO = (BasicDBObject) docWrapper.get("Processing");
            String docId = procDBO.getString("docId");
            BasicDBObject data = (BasicDBObject) docWrapper.get("Data");
            BasicDBObject metaData = (BasicDBObject) data.get("metaData");
            String jsonDocStr;
            JSONObject js = new JSONObject();
            js.put("content", new JSONObject(metaData.toString()));
            jsonDocStr = js.toString();
            boolean ok = Utils.send2ElasticSearch(jsonDocStr, docId, indexPath, serverURL);
            if (ok) {
                return new Result(docWrapper, Result.Status.OK_WITHOUT_CHANGE);
            } else {
                Result r = new Result(docWrapper, Result.Status.ERROR);
                r.setErrMessage("Error indexing document with docId:" + docId);
                return r;
            }
        } catch (Throwable t) {
            t.printStackTrace();
            Result r = new Result(docWrapper, Result.Status.ERROR);
            r.setErrMessage(t.getMessage());
            return r;
        }
    }

    @Override
    public String getPluginName() {
        return "CinergiElasticSearchIndexPreparer";
    }
}
