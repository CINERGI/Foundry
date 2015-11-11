package org.neuinfo.foundry.consumers.jms.consumers;

import com.mongodb.BasicDBObject;
import org.jdom2.Element;
import org.json.JSONArray;
import org.json.JSONObject;
import org.neuinfo.foundry.common.ingestion.DocumentIngestionService;
import org.neuinfo.foundry.common.ingestion.SourceIngestionService;
import org.neuinfo.foundry.common.model.DocWrapper;
import org.neuinfo.foundry.common.model.PrimaryKeyDef;
import org.neuinfo.foundry.common.model.ScicrunchResourceRec;
import org.neuinfo.foundry.common.model.ScicrunchResourceRec.UserKeyword;
import org.neuinfo.foundry.common.model.Source;
import org.neuinfo.foundry.common.util.Assertion;
import org.neuinfo.foundry.common.util.TemplateISOXMLGenerator;
import org.neuinfo.foundry.common.util.Utils;
import org.neuinfo.foundry.common.util.XML2JSONConverter;
import org.neuinfo.foundry.consumers.jms.consumers.plugins.ProvenanceHelper;
import org.neuinfo.foundry.consumers.jms.consumers.plugins.ProvenanceHelper.ProvData;
import org.neuinfo.foundry.consumers.jms.consumers.plugins.ScicrunchResourceReader;

import javax.jms.Message;
import javax.jms.MessageListener;
import java.util.*;

//import static org.mockito.Mockito.*;

/**
 * Created by bozyurt on 4/21/15.
 */
public class ScicrunchResourceIngestorConsumer extends JMSConsumerSupport implements MessageListener {
    ScicrunchResourceReader scicrunchResourceReader;

    public ScicrunchResourceIngestorConsumer(String queueName) {
        super(queueName);
        scicrunchResourceReader = new ScicrunchResourceReader();
    }


    public ScicrunchResourceReader getScicrunchResourceReader() {
        return scicrunchResourceReader;
    }

    public void setScicrunchResourceReader(ScicrunchResourceReader scicrunchResourceReader) {
        this.scicrunchResourceReader = scicrunchResourceReader;
    }

    public void handle() throws Exception {
        boolean testMode = getParam("testMode") != null;
        if (testMode) {
            /*
            Map<String, String> map = new HashMap<String, String>(17);
            map.put("Resource Name", "Cinergi 1");
            map.put("Description", "This is a dummy description with LIDAR, sediments, lakes and calderas.");
            map.put("Keywords", "theme: something , some other thing location:Australia");
            map.put("email", "iozyurt@ucsd.edu");
            ScicrunchResourceReader mockSR = mock(ScicrunchResourceReader.class);
            when(mockSR.getLastInsertedResourceID()).thenReturn(1L);
            when(mockSR.getResourceData(anyLong())).thenReturn(map);
            when(mockSR.getEmail(anyLong())).thenReturn("iozyurt@ucsd.edu");
            doNothing().when(mockSR).startup();
            doNothing().when(mockSR).shutdown();
            this.setScicrunchResourceReader(mockSR);
            */
        }


        // get resource data from Scicrunch database for Cinergi
        String batchId = Utils.prepBatchId(new Date());
        Date startDate = new Date();

        ScicrunchResourceRec srRec = null;
        try {
            scicrunchResourceReader.startup();
            Long lastInsertedResourceID = scicrunchResourceReader.getLastInsertedResourceID();
            Assertion.assertNotNull(lastInsertedResourceID);
            Map<String, String> map = scicrunchResourceReader.getResourceData(lastInsertedResourceID);
            srRec = new ScicrunchResourceRec();
            if (map.containsKey("Resource Name")) {
                srRec.setDataSetName(map.get("Resource Name"));
            }
            if (map.containsKey("Description")) {
                srRec.setDescription(map.get("Description"));
            }
            if (map.containsKey("Keywords")) {
                String keywordsStr = map.get("Keywords");
                List<UserKeyword> ukList = parseKeywords(keywordsStr);
                for (UserKeyword uk : ukList) {
                    srRec.addKeyword(uk);
                }
            }
            if (map.containsKey("email")) {
                srRec.setEmail(map.get("email"));
            }
        } finally {
            scicrunchResourceReader.shutdown();
        }
        if (srRec == null) {
            // TODO
            return;
        }
        // create a  ScicrunchResourceRec from it
        // insert or find Source
        DocumentIngestionService dis = new DocumentIngestionService();
        SourceIngestionService sis = new SourceIngestionService();
        try {
            sis.start(this.config);
            dis.start(this.config);
            Source source = prepareSource(srRec);
            source = sis.findOrAssignIDandSaveSource(source);
            dis.setSource(source);
            TemplateISOXMLGenerator generator = new TemplateISOXMLGenerator();

            Element docEl = generator.createISOXMLDoc(srRec);
            XML2JSONConverter converter = new XML2JSONConverter();
            JSONObject json = converter.toJSON(docEl);
            BasicDBObject docWrapper = dis.findDocument(json, getCollectionName());
            if (docWrapper == null) {
                DocWrapper dw = dis.prepareDocWrapper(json, batchId, source, getOutStatus());
                // save provenance
                ProvData provData = new ProvData(dw.getPrimaryKey(),
                        ProvenanceHelper.ModificationType.Ingested);
                provData.setSourceName(dw.getSourceName()).setSrcId(dw.getSourceId());
                // first cleanup any previous provenance data
                ProvenanceHelper.removeProvenance(dw.getPrimaryKey());
                ProvenanceHelper.saveIngestionProvenance("ingestion",
                        provData, startDate, dw);
                dis.saveDocument(dw, getCollectionName());
            }


        } finally {
            dis.shutdown();
            sis.shutdown();
        }
    }

    public static List<UserKeyword> parseKeywords(String keywordStr) {
        List<UserKeyword> ukList = new ArrayList<UserKeyword>(5);
        char[] carr = keywordStr.trim().toCharArray();
        int idx = 0;
        boolean inKWList = false;
        String curCategory = null;
        StringBuilder sb = new StringBuilder();
        while (idx < carr.length) {
            char c = carr[idx];
            if (!inKWList) {
                while (idx < carr.length && c != ':') {
                    sb.append(c);
                    idx++;
                    if (idx >= carr.length) {
                        break;
                    }
                    c = carr[idx];
                }
                if (c == ':') {
                    curCategory = sb.toString().trim();
                    sb.setLength(0);
                    inKWList = true;
                    idx++;
                }
            } else {
                while (idx < carr.length && c != ':' && c != ',') {
                    sb.append(c);
                    idx++;
                    if (idx >= carr.length) {
                        break;
                    }
                    c = carr[idx];
                }
                if (c == ',') {
                    String s = sb.toString().trim();
                    sb.setLength(0);
                    if (s.length() > 0) {
                        UserKeyword uk = new UserKeyword(s, curCategory);
                        ukList.add(uk);
                    }
                    idx++;
                } else if (c == ':') {
                    // FIXME Assumption: single word category
                    String s = sb.toString().trim();
                    sb.setLength(0);
                    if (s.length() > 0) {
                        int locIdx = s.lastIndexOf(' ');
                        Assertion.assertTrue(locIdx != -1);
                        String keyword = s.substring(0, locIdx).trim();
                        UserKeyword uk = new UserKeyword(keyword, curCategory);
                        ukList.add(uk);
                        curCategory = s.substring(locIdx + 1).trim();
                    }
                    idx++;
                } else if (idx == carr.length) {
                    String s = sb.toString().trim();
                    sb.setLength(0);
                    if (s.length() > 0) {
                        UserKeyword uk = new UserKeyword(s, curCategory);
                        ukList.add(uk);
                    }
                }
            }
        }
        return ukList;
    }

    public static Source prepareSource(ScicrunchResourceRec srRec) {
        Source.Builder builder = new Source.Builder("", srRec.getDataSetName());
        builder.dataSource(srRec.getDataSetName());
        JSONObject icJson = new JSONObject();
        icJson.put("ingestMethod", "Scicrunch");
        icJson.put("ingestURL", "");
        JSONObject crawlFreqJson = new JSONObject();
        crawlFreqJson.put("crawlType", "Frequency");
        crawlFreqJson.put("hours", "48");
        crawlFreqJson.put("minutes", "0");

        JSONArray jsArr = new JSONArray();
        String[] startDays = {"Sunday", "Monday", "Tuesday", "Wednesday",
                "Thursday", "Friday", "Saturday"};
        for (String startDay : startDays) {
            jsArr.put(startDay);
        }
        crawlFreqJson.put("startDays", jsArr);
        crawlFreqJson.put("startTime", "0:00");
        crawlFreqJson.put("operationEndTime", "24:00");
        icJson.put("crawlFrequency", crawlFreqJson);
        builder.ingestConfiguration(icJson);
        List<String> workflowSteps = new ArrayList<String>(3);
        workflowSteps.add("UUID Generation");
        workflowSteps.add("XML2Cinergi");
        workflowSteps.add("Index");

        PrimaryKeyDef pkDef = new PrimaryKeyDef(
                Arrays.asList("$.'gmd:MD_Metadata'.'gmd:fileIdentifier'.'gco:CharacterString'.'_$'"),
                Arrays.asList(":"), "Value");


        builder.workflowSteps(workflowSteps).primaryKey(pkDef)
                .contentSpecification(new JSONObject());

        return builder.build();
    }

    @Override
    public void onMessage(Message message) {
        try {
            handle();
        } catch (Exception x) {
            //TODO proper error handling
            x.printStackTrace();
        }
    }
}
