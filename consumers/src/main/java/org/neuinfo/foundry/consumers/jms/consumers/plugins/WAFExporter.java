package org.neuinfo.foundry.consumers.jms.consumers.plugins;

import com.mongodb.DBObject;
import org.apache.log4j.Logger;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.json.JSONArray;
import org.json.JSONObject;
import org.neuinfo.foundry.common.util.CinergiXMLUtils;
import org.neuinfo.foundry.common.util.ExistingKeywordsFacetHandler;
import org.neuinfo.foundry.common.util.ISOXMLGenerator;
import org.neuinfo.foundry.common.util.Utils;
import org.neuinfo.foundry.consumers.plugin.IPlugin;
import org.neuinfo.foundry.consumers.plugin.Result;

import java.io.File;
import java.util.Map;

/**
 * Created by bozyurt on 11/25/14.
 */
public class WAFExporter implements IPlugin {
    private String outDirectory;
    private final static Logger logger = Logger.getLogger(WAFExporter.class);

    @Override
    public void initialize(Map<String, String> options) throws Exception {
        this.outDirectory = options.get("outDirectory");
    }

    @Override
    public Result handle(DBObject docWrapper) {
        try {
            DBObject procDBO = (DBObject) docWrapper.get("Processing");
            String status = procDBO.get("status").toString();
            String primaryKey = docWrapper.get("primaryKey").toString();
            DBObject siDBO = (DBObject) docWrapper.get("SourceInfo");
            String srcName = siDBO.get("Name").toString();

            ISOXMLGenerator generator = new ISOXMLGenerator();
            Element docEl = generator.generate(docWrapper);
            // also enhance existing keywords 12/01/2015
            ExistingKeywordsFacetHandler handler = new ExistingKeywordsFacetHandler(docEl);
            docEl = handler.handle();

            saveEnhancedXmlFile(primaryKey, docEl, srcName, status);
            return new Result(docWrapper, Result.Status.OK_WITHOUT_CHANGE);
        } catch (Throwable t) {
            t.printStackTrace();
            Result r = new Result(docWrapper, Result.Status.ERROR);
            r.setErrMessage(t.getMessage());
            return r;
        }
    }


    Element addSpatialExtent(Element docEl, JSONObject spatial) throws Exception {
        Namespace gmd = Namespace.getNamespace("gmd", "http://www.isotc211.org/2005/gmd");
        JSONArray boundingBoxes = spatial.getJSONArray("bounding_boxes");
        boolean hasBB = boundingBoxes.length() > 0;
        boolean hasBBFromPlaces = false;

        Element identificationInfo = docEl.getChild("identificationInfo", gmd);
        Element dataIdentification = identificationInfo.getChild("MD_DataIdentification", gmd);
        if (dataIdentification == null) {
            dataIdentification = new Element("MD_DataIdentification", gmd);
            identificationInfo.addContent(dataIdentification);
        }

        if (!hasBB) {
            JSONObject derivedBoundingBoxes = spatial.getJSONObject("derived_bounding_boxes_from_places");
            if (derivedBoundingBoxes.length() > 0) {
                for (String place : derivedBoundingBoxes.keySet()) {
                    JSONObject placeJson = derivedBoundingBoxes.getJSONObject(place);
                    JSONObject swJson = placeJson.getJSONObject("southwest");
                    JSONObject neJson = placeJson.getJSONObject("northeast");
                    String wbLongVal = String.valueOf(swJson.getDouble("lng"));
                    String sblatVal = String.valueOf(swJson.getDouble("lat"));
                    String ebLongVal = String.valueOf(neJson.getDouble("lng"));
                    String nbLatVal = String.valueOf(neJson.getDouble("lat"));
                    Element bbEl = CinergiXMLUtils.createBoundaryBox(wbLongVal, ebLongVal, sblatVal, nbLatVal, place);
                    dataIdentification.addContent(bbEl);
                }
                hasBBFromPlaces = true;
            }
        }
        if (!hasBB && !hasBBFromPlaces) {
            JSONObject derivedBoundingBoxes = spatial.getJSONObject("derived_bounding_boxes_from_derived_place");
            if (derivedBoundingBoxes.length() > 0) {
                for (String place : derivedBoundingBoxes.keySet()) {
                    JSONObject placeJson = derivedBoundingBoxes.getJSONObject(place);
                    JSONObject swJson = placeJson.getJSONObject("southwest");
                    JSONObject neJson = placeJson.getJSONObject("northeast");
                    String wbLongVal = String.valueOf(swJson.getDouble("lng"));
                    String sblatVal = String.valueOf(swJson.getDouble("lat"));
                    String ebLongVal = String.valueOf(neJson.getDouble("lng"));
                    String nbLatVal = String.valueOf(neJson.getDouble("lat"));
                    Element bbEl = CinergiXMLUtils.createBoundaryBox(wbLongVal, ebLongVal, sblatVal, nbLatVal, place);
                    dataIdentification.addContent(bbEl);
                }
            }
        }
        return docEl;
    }

    private void saveEnhancedXmlFile(String fileIdentifier, Element docEl, String srcName,
                                     String status) throws Exception {
        String sourceDirname = srcName.replaceAll("\\s+", "_");
        File sourceDir = new File(this.outDirectory, sourceDirname);
        if (status.equals("annotated.1")) {
            sourceDir = new File(sourceDir, "annotated");
        }
        if (!sourceDir.isDirectory()) {
            sourceDir.mkdirs();
        }
        fileIdentifier = fileIdentifier.replaceAll("/","__");
        fileIdentifier = fileIdentifier.replaceAll("\\.+","_");
        File enhancedXmlFile = new File(sourceDir, fileIdentifier + ".xml");
        Utils.saveXML(docEl, enhancedXmlFile.getAbsolutePath());
        logger.info("saved enhancedXmlFile to " + enhancedXmlFile);
    }


    @Override
    public String getPluginName() {
        return "WAFExporter";
    }
}
