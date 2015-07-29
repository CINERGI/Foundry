package org.neuinfo.foundry.consumers.plugin;

import com.mongodb.DBObject;
import org.json.JSONObject;

import java.util.Map;

/**
 * Created by bozyurt on 10/28/14.
 */
public interface IPlugin {

    public void initialize(Map<String,String> options) throws Exception;

    public Result handle(DBObject docWrapper);

    public String getPluginName();

}
