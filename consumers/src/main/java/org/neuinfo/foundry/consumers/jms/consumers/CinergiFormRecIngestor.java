package org.neuinfo.foundry.consumers.jms.consumers;

import com.mongodb.BasicDBObject;
import org.apache.log4j.Logger;
import org.jdom2.Element;
import org.json.JSONArray;
import org.json.JSONObject;
import org.neuinfo.foundry.common.ingestion.DocumentIngestionService;
import org.neuinfo.foundry.common.ingestion.SourceIngestionService;
import org.neuinfo.foundry.common.model.CinergiFormRec;
import org.neuinfo.foundry.common.model.DocWrapper;
import org.neuinfo.foundry.common.model.PrimaryKeyDef;
import org.neuinfo.foundry.common.model.Source;
import org.neuinfo.foundry.common.util.TemplateISOXMLGenerator;
import org.neuinfo.foundry.common.util.Utils;
import org.neuinfo.foundry.common.util.XML2JSONConverter;
import org.neuinfo.foundry.consumers.common.ServiceFactory;
import org.neuinfo.foundry.consumers.jms.consumers.plugins.ProvenanceHelper;
import org.neuinfo.foundry.consumers.jms.consumers.plugins.ProvenanceHelper.ProvData;

import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;
import java.util.*;

/**
 * Created by bozyurt on 8/13/15.
 */
public class CinergiFormRecIngestor extends JMSConsumerSupport implements MessageListener {
    private final static Logger logger = Logger.getLogger("CinergiFormRecIngestor");

    public CinergiFormRecIngestor(String queueName) {
        super(queueName);
    }

    public void handle(CinergiFormRec cfRec) throws Exception {
        ServiceFactory serviceFactory = ServiceFactory.getInstance();
        DocumentIngestionService dis = null;
        SourceIngestionService sis = null;
        try {
            dis = serviceFactory.createDocumentIngestionService();
            sis = serviceFactory.createSourceIngestionService();
            Source source = prepareSource("Cinergi");
            source = sis.findOrAssignIDandSaveSource(source);
            dis.setSource(source);
            TemplateISOXMLGenerator generator = new TemplateISOXMLGenerator();
            Date startDate = new Date();
            String batchId = Utils.prepBatchId(startDate);
            String guid = Utils.getMD5ChecksumOfString(cfRec.getResourceTitle() + cfRec.getContactEmail());
            Element docEl = generator.createISOXMLDoc(cfRec, guid);
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
            if (dis != null) {
                dis.shutdown();
            }
            if (sis != null) {
                sis.shutdown();
            }
        }
    }

    public static Source prepareSource(String dataSource) {
        Source.Builder builder = new Source.Builder("", dataSource);
        builder.dataSource(dataSource);
        JSONObject icJson = new JSONObject();
        icJson.put("ingestMethod", "form");
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
            ObjectMessage om = (ObjectMessage) message;
            String payload = (String) om.getObject();
            logger.info("payload:" + payload);
            JSONObject json = new JSONObject(payload);
            CinergiFormRec cfr = CinergiFormRec.fromJSON(json);
            handle(cfr);
        } catch (Exception x) {
            //TODO proper error handling
            x.printStackTrace();
        }
    }
}
