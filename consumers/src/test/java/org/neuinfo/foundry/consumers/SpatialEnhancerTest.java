package org.neuinfo.foundry.consumers;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import junit.framework.TestCase;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.json.JSONObject;
import org.neuinfo.foundry.common.util.JSONUtils;
import org.neuinfo.foundry.common.util.Utils;
import org.neuinfo.foundry.common.util.CinergiXMLUtils;
import org.neuinfo.foundry.consumers.jms.consumers.plugins.SpatialEnhancer;
import org.neuinfo.foundry.consumers.jms.consumers.plugins.SpatialEnhancer2;
import org.neuinfo.foundry.consumers.plugin.IPlugin;
import org.neuinfo.foundry.consumers.plugin.Result;
import org.neuinfo.foundry.consumers.util.Helper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by bozyurt on 11/25/14.
 */
public class SpatialEnhancerTest extends TestCase {
    static final String HOME_DIR = System.getProperty("user.home");

    public SpatialEnhancerTest(String name) {
        super(name);
    }

    public void testGMDXMLEnhancing() throws Exception {
        String sampleDocFile = HOME_DIR + "/etc/hydro10/ScienceBase/018F7983-78AA-4368-989A-B84F4FEB36D9.xml";
        Element rootEl = Utils.loadXML(sampleDocFile);
        Namespace gmd = Namespace.getNamespace("gmd", "http://www.isotc211.org/2005/gmd");
        Namespace gco = Namespace.getNamespace("gco", "http://niem.gov/niem/external/iso-19139-gmd/draft-0.1/gco/dhs-gmo/1.0.0");
        Element identificationInfo = rootEl.getChild("identificationInfo", gmd);
        assertNotNull(identificationInfo);
        Element extent = identificationInfo.getChild("extent", gmd);
        assertNull(extent);

        String xmlDocStr = Utils.loadAsString(sampleDocFile);
        String serverUrl = "https://photon.sdsc.edu:8443/cinergi/SpatialEnhancer";
        JSONObject json = SpatialEnhancer.callSpatialEnhancer(serverUrl, xmlDocStr);

        JSONObject derivedBoundingBoxes = json.getJSONObject("derived_bounding_boxes_from_derived_place");
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
                identificationInfo.addContent(bbEl);
            }
        }


        extent = identificationInfo.getChild("extent", gmd);
        assertNotNull(extent);

        Utils.saveXML(rootEl, "/tmp/018F7983-78AA-4368-989A-B84F4FEB36D9_enhanced.xml");

    }

    public void testCallSpatialEnhancer() throws Exception {
        String sampleDocFile = HOME_DIR + "/etc/hydro10/NCDC_GIS/00C2609C-C2E2-4DFB-A9BB-E687F8B42E24.xml";
        // sampleDocFile = HOME_DIR + "/etc/hydro10/ScienceBase/01245520-5CEC-41D9-9D65-B91CD0DB436A.xml";
        //  sampleDocFile = HOME_DIR + "/etc/hydro10/ScienceBase/018F7983-78AA-4368-989A-B84F4FEB36D9.xml";
        //  sampleDocFile = "/tmp/4f4e4a55e4b07f02db62c686_foundry.xml";
        sampleDocFile = HOME_DIR + "/etc/hydro10/ScienceBase/00F67830-9DB7-4968-BE91-F5A82791B7B8.xml";

        String xmlDocStr = Utils.loadAsString(sampleDocFile);
       // String serverUrl = "https://photon.sdsc.edu:8443/cinergi/SpatialEnhancer";
      //  String serverUrl = "http://cinergi.sdsc.edu:8080/cinergi_spatial/SpatialEnhancer";
        String serverUrl = "http://photon.sdsc.edu:8080/cinergi/SpatialEnhancer";
        JSONObject json = SpatialEnhancer.callSpatialEnhancer(serverUrl, xmlDocStr);
        assertNotNull(json);
        System.out.println(json.toString(2));
    }

    public void testSpatialEnhancer() throws Exception {
        String thePrimaryKey = "043dbb8c-66de-6897-e054-00144fdd4fa6";
        thePrimaryKey = null;
        Helper helper = new Helper("");
        try {
            helper.startup("consumers-cfg.xml");

            //List<BasicDBObject> docWrappers = helper.getDocWrappers("cinergi-0001");
          //  List<BasicDBObject> docWrappers = helper.getDocWrappers("cinergi-0023");
            List<BasicDBObject> docWrappers = helper.getDocWrappers("cinergi-0013");
            IPlugin plugin = new SpatialEnhancer2();
            Map<String, String> optionMap = new HashMap<String, String>();
          //  optionMap.put("serverURL","http://photon.sdsc.edu:8080/cinergi/SpatialEnhancer");
           //  optionMap.put("serverURL","http://132.249.238.169:8080/cinergi/SpatialEnhancer");
            plugin.initialize(optionMap);
            for (BasicDBObject docWrapper : docWrappers) {
                String primaryKey = docWrapper.get("primaryKey").toString();
                if (thePrimaryKey == null || primaryKey.equalsIgnoreCase(thePrimaryKey)) {
                    Result result = plugin.handle(docWrapper);
                    if (result.getStatus() == Result.Status.OK_WITH_CHANGE) {
                        DBObject dw = result.getDocWrapper();
                        DBObject data = (DBObject) dw.get("Data");
                        JSONObject json = JSONUtils.toJSON((BasicDBObject) data, false);
                        System.out.println(json.toString(2));
                        System.out.println(">>----------------------------------------");

                    }
                   //  break;
                }
            }
        } finally {
            helper.shutdown();
        }
    }

    public static void main(String[] args) throws Exception {
        SpatialEnhancerTest test = new SpatialEnhancerTest("");
        test.testCallSpatialEnhancer();
    }
}
