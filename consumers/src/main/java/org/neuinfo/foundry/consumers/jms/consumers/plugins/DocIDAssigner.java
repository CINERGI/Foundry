package org.neuinfo.foundry.consumers.jms.consumers.plugins;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import org.apache.log4j.Logger;
import org.neuinfo.foundry.consumers.plugin.IPlugin;
import org.neuinfo.foundry.consumers.plugin.Result;

import java.util.Map;
import java.util.UUID;

/**
 * Created by bozyurt on 12/7/15.
 */
public class DocIDAssigner implements IPlugin {
    private final static Logger log = Logger.getLogger(DocIDAssigner.class);

    @Override
    public void initialize(Map<String, String> options) throws Exception {
        // no-op
    }

    @Override
    public Result handle(DBObject docWrapper) {
        try {
            BasicDBObject pi = (BasicDBObject) docWrapper.get("Processing");
            UUID uuid = UUID.randomUUID();
            pi.put("docId", uuid.toString());

            return new Result(docWrapper, Result.Status.OK_WITH_CHANGE);
        } catch (Throwable t) {
            log.error("handle", t);
            t.printStackTrace();
            Result r = new Result(docWrapper, Result.Status.ERROR);
            r.setErrMessage(t.getMessage());
            return r;
        }
    }

    @Override
    public String getPluginName() {
        return "DocIDAssigner";
    }
}
