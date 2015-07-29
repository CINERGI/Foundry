package org.neuinfo.foundry.ingestor;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;
import org.json.JSONObject;
import org.neuinfo.foundry.common.util.XML2JSONConverter;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by bozyurt on 4/23/14.
 */
public class GMIXMLIngestor extends IngestorSupport {
    static final List<String> noaaList = new ArrayList<String>();

    static {
        noaaList.add("http://hydro10.sdsc.edu/metadata/NOAA_NGDC/053B250F-3EAB-4FA5-B7D0-52ED907A6526.xml");
        noaaList.add("http://hydro10.sdsc.edu/metadata/NOAA_NGDC/0006697F-0974-44DD-80A1-C6F05B250848.xml");
        noaaList.add("http://hydro10.sdsc.edu/metadata/NOAA_NGDC/096C58B9-9EE6-40EE-97A1-62DE5CF4D7BA.xml");
        noaaList.add("http://hydro10.sdsc.edu/metadata/NOAA_NGDC/12A6D38B-2431-48BA-AF5F-3C57F3E7AB75.xml");
    }


    public static JSONObject prepNOAAJsonPayload(String xmlSource) throws Exception {
        SAXBuilder builder = new SAXBuilder();
        Document doc = builder.build(xmlSource);
        Element rootEl = doc.getRootElement();

        XML2JSONConverter converter = new XML2JSONConverter();
        final JSONObject json = converter.toJSON(rootEl);

        return json;
    }

    public static void main(String[] args) throws Exception {
        IngestorSupport ingestor = new GMIXMLIngestor();

        boolean deleteRecs = false;
        if (args.length == 1) {
            if (args[0].startsWith("-del")) {
                deleteRecs = true;
                System.out.println("deleting records");
            }
        }
        try {
            ingestor.start();
            if (deleteRecs) {
                ingestor.deleteAllRecords();
            } else {
                for (String xmlSource : noaaList) {
                    JSONObject payload = prepNOAAJsonPayload(xmlSource);
                    System.out.println(payload.toString(2));

                    ingestor.insertDoc(ingestor.prepareDocument(payload));
                }
            }
        } finally {
            ingestor.shutdown();
        }
    }

}
