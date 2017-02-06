package org.neuinfo.foundry.consumers.jms.consumers.plugins;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.jdom2.Element;
import org.json.JSONArray;
import org.json.JSONObject;
import org.neuinfo.foundry.common.util.JSONUtils;
import org.neuinfo.foundry.common.util.Utils;
import org.neuinfo.foundry.common.util.XML2JSONConverter;
import org.neuinfo.foundry.consumers.jms.consumers.plugins.ProvenanceHelper.ProvData;
import org.neuinfo.foundry.consumers.plugin.IPlugin;
import org.neuinfo.foundry.consumers.plugin.Result;

import java.net.URI;
import java.util.Map;

/**
 * Created by bozyurt on 11/25/14.
 */
public class SpatialEnhancer implements IPlugin {
    String serverURL;
    private final static Logger logger = Logger.getLogger("SpatialEnhancer");

    @Override
    public void initialize(Map<String, String> options) throws Exception {
        this.serverURL = options.get("serverURL");
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
            JSONObject json = JSONUtils.toJSON((BasicDBObject) originalDoc, false);
            XML2JSONConverter converter = new XML2JSONConverter();
            Element docEl = converter.toXML(json);

            String xmlStr = Utils.xmlAsString(docEl);
            xmlStr = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + xmlStr;
            logger.info("calling callSpatialEnhancer");
            JSONObject spatialJson = callSpatialEnhancer(serverURL, xmlStr);

            ProvData provData = new ProvData(primaryKey, ProvenanceHelper.ModificationType.Added);
            provData.setSourceName(sourceName).setSrcId(srcId);
            if (spatialJson != null) {
                logger.info("fileIdentifier:" + primaryKey);
                logger.info(spatialJson.toString(2));
                logger.info("--------------------------");
                DBObject data = (DBObject) docWrapper.get("Data");
                DBObject spatial = JSONUtils.encode(spatialJson, false);
                data.put("spatial", spatial);
                prepBoundingBoxProv(spatialJson, provData);
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

    public static void prepBoundingBoxProv(JSONObject spatial, ProvData provData) {
        boolean hasBBFromPlaces = false;
        if (spatial == null ||
                (!spatial.has("bounding_boxes") &&
                        !spatial.has("derived_bounding_boxes_from_places") &&
                        !spatial.has("derived_bounding_boxes_from_derived_place"))) {
            provData.addModifiedFieldProv("No bounding box is added.");
            return;
        }
        JSONArray boundingBoxes = null;
        if (!spatial.isNull("bounding_boxes")) {
            boundingBoxes = spatial.getJSONArray("bounding_boxes");
        }
        boolean hasBB = boundingBoxes != null && boundingBoxes.length() > 0;
        StringBuilder sb = new StringBuilder();
        if (!hasBB) {
            JSONObject derivedBoundingBoxes = spatial.getJSONObject("derived_bounding_boxes_from_places");
            if (derivedBoundingBoxes.length() > 0) {
                for (String place : derivedBoundingBoxes.keySet()) {
                    sb.setLength(0);
                    JSONObject placeJson = derivedBoundingBoxes.getJSONObject(place);
                    sb.append("Added spatial extent for ").append(place);
                    sb.append(" with value ").append(placeJson.toString());
                    provData.addModifiedFieldProv(sb.toString().trim());
                }
                hasBBFromPlaces = true;
            }
        }
        if (!hasBB && !hasBBFromPlaces) {
            JSONObject derivedBoundingBoxes = spatial.getJSONObject("derived_bounding_boxes_from_derived_place");
            if (derivedBoundingBoxes.length() > 0) {
                for (String place : derivedBoundingBoxes.keySet()) {
                    sb.setLength(0);
                    JSONObject placeJson = derivedBoundingBoxes.getJSONObject(place);
                    sb.append("Added spatial extent for ").append(place);
                    sb.append(" with value ").append(placeJson.toString());
                    provData.addModifiedFieldProv(sb.toString().trim());
                }
            }
        }
        if (provData.getModifiedList().isEmpty()) {
            provData.addModifiedFieldProv("No bounding box is added.");
        }
    }

    @Override
    public String getPluginName() {
        return "SpatialEnhancer";
    }

    public static JSONObject callSpatialEnhancer(String serverURL, String origDocXmlStr) throws Exception {
        HttpClient client = new DefaultHttpClient();

        URIBuilder builder = new URIBuilder(serverURL);
        // "http://localhost:9200/");

        URI uri = builder.build();
        logger.info("uri:" + uri);
        HttpPost httpPost = new HttpPost(uri);
        boolean ok = false;
        try {
            httpPost.addHeader("Accept", "application/json");
            httpPost.addHeader("Content-Type", "application/xml");
            StringEntity entity = new StringEntity(origDocXmlStr, "UTF-8");
            httpPost.setEntity(entity);
            final HttpResponse response = client.execute(httpPost);
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode == 200 || statusCode == 201) {
                ok = true;
                final HttpEntity responseEntity = response.getEntity();
                if (entity != null) {
                    String jsonStr = EntityUtils.toString(responseEntity);
                    return new JSONObject(jsonStr);
                }
            } else {
                logger.info(response.getStatusLine());
                logger.info(response.toString());
            }

        } finally {
            if (httpPost != null) {
                httpPost.releaseConnection();
            }
        }
        return null;
    }
}
