package org.neuinfo.foundry.consumers.jms.consumers.plugins;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import org.json.JSONObject;
import org.neuinfo.foundry.common.util.JSONUtils;
import org.neuinfo.foundry.consumers.common.Constants;
import org.neuinfo.foundry.consumers.common.Utils;
import org.neuinfo.foundry.consumers.plugin.IPlugin;
import org.neuinfo.foundry.consumers.plugin.Result;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by bozyurt on 11/20/14.
 */
public class XML2CinergiPlugin implements IPlugin {
    @Override
    public void initialize(Map<String, String> options) throws Exception {
        // no op
    }

    @Override
    public Result handle(DBObject docWrapper) {
        try {
            String objectId = docWrapper.get(Constants.MONGODB_ID_FIELD).toString();
            DBObject originalDoc = (DBObject) docWrapper.get("OriginalDoc");
            DBObject pi = (DBObject) docWrapper.get("Processing");
            JSONObject json = JSONUtils.toJSON((BasicDBObject) originalDoc, false);
            JSONObject cinergi = processHandler(json, objectId);
            if (cinergi != null) {
                // set CINERGI ID as the docId (UUID)
                String docId = (String) pi.get("docId");
                JSONObject cinergiMDJson = cinergi.getJSONObject("cmd:CINERGI_MetadataObject");
                cinergiMDJson.put("cmd:CINERGI_ID", docId);

                DBObject data = (DBObject) docWrapper.get("Data");
                DBObject cinergiMD = JSONUtils.encode(cinergi, false);
                data.put("metaData", cinergiMD);
                return new Result(docWrapper, Result.Status.OK_WITH_CHANGE);
            } else {
                Result r = new Result(docWrapper, Result.Status.ERROR);
                r.setErrMessage("Error in XML to CINERGI enhancer");
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
        return "XML2Cinergi";
    }

    JSONObject processHandler(JSONObject originalDoc, String objectId) throws Exception {
        List<String> cmdList = new ArrayList<String>(10);
        cmdList.add("xml-to-cinergi");
        File inputFile = Utils.prepareInputFile("xml", originalDoc, objectId);
        File outFile = Utils.prepareOutFile("json", objectId);
        cmdList.add("-i");
        cmdList.add(inputFile.getAbsolutePath());
        cmdList.add("-o");
        cmdList.add(outFile.getAbsolutePath());
        cmdList.add("-h"); // FIXME for hydro data only

        ProcessBuilder pb = new ProcessBuilder(cmdList);

        Process process = pb.start();
        BufferedReader bin = new BufferedReader(new InputStreamReader(
                process.getErrorStream()));
        String line;
        while ((line = bin.readLine()) != null) {
            System.out.println(line);
        }

        int rc = process.waitFor();
        System.out.println("rc:" + rc);
        inputFile.delete();
        if (rc == 0) {
            JSONObject jsonObject = JSONUtils.loadFromFile(outFile.getAbsolutePath());
            // System.out.println(jsonObject.toString(2));
            outFile.delete();
            return jsonObject;
        }
        return null;
    }
}
