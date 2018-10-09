package org.neuinfo.foundry.consumers.jms.consumers.plugins;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import org.cinergi.sdsc.metadata.enhancer.spatial.SpatialEnhancerResult;
import org.cinergi.sdsc.metadata.enhancer.spatial.StanfordNEDLocationFinder;
import org.jdom2.Element;
import org.json.JSONObject;
import org.neuinfo.foundry.common.util.JSONUtils;
import org.neuinfo.foundry.common.util.Utils;
import org.neuinfo.foundry.common.util.XML2JSONConverter;
import org.neuinfo.foundry.consumers.plugin.IPlugin;
import org.neuinfo.foundry.consumers.plugin.Result;

import java.io.File;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Created by bozyurt on 8/23/16.
 */
public class SpatialEnhancer2 implements IPlugin {
    private final static Logger logger = Logger.getLogger("SpatialEnhancer2");
    StanfordNEDLocationFinder finder;

    @Override
    public void initialize(Map<String, String> options) throws Exception {
        finder = new StanfordNEDLocationFinder();
        finder.startup();
    }

    @Override
    public Result handle(DBObject docWrapper) {
        try {
            //Date startDate = new Date();
            DBObject originalDoc = (DBObject) docWrapper.get("OriginalDoc");
            String primaryKey = docWrapper.get("primaryKey").toString();
            DBObject siDBO = (DBObject) docWrapper.get("SourceInfo");
            String srcId = siDBO.get("SourceID").toString();
            String sourceName = siDBO.get("Name").toString();

            JSONObject json;
            // handle edited documents (IBO)
            DBObject editedDoc = (DBObject) docWrapper.get("EditedDoc");
            if (editedDoc != null) {
                json = JSONUtils.toJSON((BasicDBObject) editedDoc, false);
            } else {
                json = JSONUtils.toJSON((BasicDBObject) originalDoc, false);
            }

            XML2JSONConverter converter = new XML2JSONConverter();
            Element docEl = converter.toXML(json);

            String xmlStr = Utils.xmlAsString(docEl);

            xmlStr = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + xmlStr;

            File file = File.createTempFile("spatial_", ".xml");
            Utils.saveText(xmlStr, file.getAbsolutePath());
            xmlStr = null;

            SpatialEnhancerResult ser = new SpatialEnhancerResult(file, finder);
            String resultStr = new ObjectMapper().writeValueAsString(ser);
            JSONObject spatialJson = new JSONObject(resultStr);
            file.delete();

            ProvenanceHelper.ProvData provData = new ProvenanceHelper.ProvData(primaryKey, ProvenanceHelper.ModificationType.Added);
            provData.setSourceName(sourceName).setSrcId(srcId);
            if (spatialJson != null) {
                logger.info("fileIdentifier:" + primaryKey);
                logger.info(spatialJson.toString(2));
                logger.info("--------------------------");
                DBObject data = (DBObject) docWrapper.get("Data");
                DBObject spatial = JSONUtils.encode(spatialJson, false);
                data.put("spatial", spatial);
                SpatialEnhancer.prepBoundingBoxProv(spatialJson, provData);
            } else {
                provData.addModifiedFieldProv("No bounding box is added.");
            }
            ProvenanceHelper.saveEnhancerProvenance("spatialEnhancer", provData, docWrapper);
            return new Result(docWrapper, Result.Status.OK_WITH_CHANGE);
        } catch (Throwable t) {
            t.printStackTrace();
            Result r = new Result(docWrapper, Result.Status.ERROR);
            r.setErrMessage(t.getMessage());
            return r;
        }
    }

    @Override
    public String getPluginName() {
        return "SpatialEnhancer2";
    }
}
