package org.neuinfo.foundry.consumers.jms.consumers;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import org.bson.types.ObjectId;
import org.json.JSONObject;
import org.neuinfo.foundry.common.ingestion.DocumentIngestionService;
import org.neuinfo.foundry.common.model.DocWrapper;
import org.neuinfo.foundry.common.model.Source;
import org.neuinfo.foundry.common.util.Assertion;
import org.neuinfo.foundry.common.util.JSONUtils;
import org.neuinfo.foundry.consumers.jms.consumers.plugins.ProvenanceHelper;
import org.neuinfo.foundry.consumers.jms.consumers.plugins.ProvenanceHelper.ProvData;
import org.neuinfo.foundry.consumers.plugin.Ingestable;
import org.neuinfo.foundry.consumers.plugin.Ingestor;
import org.neuinfo.foundry.consumers.plugin.Result;

import javax.jms.JMSException;
import javax.jms.MessageListener;
import java.util.Date;

/**
 * Created by bozyurt on 10/28/14.
 */
public class GenericIngestionConsumer extends ConsumerSupport implements Ingestable {
    private Ingestor ingestor;

    public GenericIngestionConsumer(String queueName) {
        super(queueName);
    }

    @Override
    public void handleMessages(MessageListener listener) throws JMSException {
        try {
            handle();
        } catch (Exception x) {
            x.printStackTrace();
        }
    }

    void handle() throws Exception {
        String srcNifId = ingestor.getOption("srcNifId");
        String dataSource = ingestor.getOption("dataSource");
        String batchId = ingestor.getOption("batchId");
        String includeFile = ingestor.getOption("includeFile");


        DocumentIngestionService dis = new DocumentIngestionService();
        MessagePublisher messagePublisher = null;
        try {
            ingestor.startup();
            dis.start(this.config);
            messagePublisher = new MessagePublisher(this.config.getBrokerURL());
            Source source = dis.findSource(srcNifId, dataSource);
            Assertion.assertNotNull(source, "Cannot find source for sourceID:" + srcNifId);
            dis.setSource(source);
            int submittedCount = 0;
            int ingestedCount = 0;
            dis.beginBatch(source, batchId);

            while (ingestor.hasNext()) {
                try {
                    Date startDate = new Date();
                    Result result = ingestor.prepPayload();
                    if (result.getStatus() == Result.Status.OK_WITH_CHANGE) {
                        BasicDBObject document = dis.findDocument(result.getPayload(), getCollectionName());

                        if (document != null) {
                            String updateOutStatus = getIngestor().getOption("updateOutStatus");
                            Assertion.assertNotNull(updateOutStatus);
                            // find difference and update OriginalDoc
                            BasicDBObject origDocDBO = (BasicDBObject) document.get("OriginalDoc");
                            JSONObject origDocJS = JSONUtils.toJSON(origDocDBO, false);
                            JSONObject payload = result.getPayload();

                            DBObject pi = (DBObject) document.get("Processing");

                            if (JSONUtils.isEqual(origDocJS, payload)) {
                                String status = (String) pi.get("status");
                                if (includeFile != null || (status != null && status.equals("error"))) {
                                    // the previous doc processing ended with error
                                    // or doc needs to be reprocessed, so start over
                                    DocWrapper dw = dis.prepareDocWrapper(result.getPayload(), batchId, source,
                                            getOutStatus());
                                    // save provenance
                                    ProvData provData = new ProvData(dw.getPrimaryKey(),
                                            ProvenanceHelper.ModificationType.Ingested);
                                    provData.setSourceName(dw.getSourceName()).setSrcId(dw.getSourceId());

                                    // first cleanup any previous provenance data
                                    ProvenanceHelper.removeProvenance(dw.getPrimaryKey());
                                    ProvenanceHelper.saveIngestionProvenance("ingestion",
                                            provData, startDate, dw);
                                    // delete previous record first
                                    dis.removeDocument(document, getCollectionName());
                                    ObjectId oid = dis.saveDocument(dw, getCollectionName());
                                    messagePublisher.sendMessage(oid.toString(), getOutStatus());
                                } else {
                                    pi.put("status", "finished");
                                    dis.updateDocument(document, getCollectionName(), batchId);

                                }
                            } else {
                                DBObject dbObject = JSONUtils.encode(payload, true);
                                document.put("OriginalDoc", dbObject);
                                // update CrawlDate
                                document.put("CrawlDate", JSONUtils.toBsonDate(new Date()));
                                pi.put("status", updateOutStatus);
                                dis.updateDocument(document, getCollectionName(), batchId);
                                String oidStr = document.get("_id").toString();
                                messagePublisher.sendMessage(oidStr, updateOutStatus);
                            }

                        } else {
                            DocWrapper dw = dis.prepareDocWrapper(result.getPayload(), batchId, source, getOutStatus());

                            // DocWrapper dw = dis.saveDocument(result.getPayload(), batchId,
                            //        source, getOutStatus(), getCollectionName());
                            // save provenance
                            ProvData provData = new ProvData(dw.getPrimaryKey(), ProvenanceHelper.ModificationType.Ingested);
                            provData.setSourceName(dw.getSourceName()).setSrcId(dw.getSourceId());
                            // first cleanup any previous provenance data
                            ProvenanceHelper.removeProvenance(dw.getPrimaryKey());
                            ProvenanceHelper.saveIngestionProvenance("ingestion",
                                    provData, startDate, dw);
                            ObjectId oid = dis.saveDocument(dw, getCollectionName());
                            messagePublisher.sendMessage(oid.toString(), getOutStatus());
                        }
                        ingestedCount++;
                    }
                } catch (Exception x) {
                    x.printStackTrace();

                } finally {
                    submittedCount++;
                }
            }

            dis.endBatch(source, batchId, ingestedCount, submittedCount);
        } finally {
            dis.shutdown();
            ingestor.shutdown();
            if (messagePublisher != null) {
                messagePublisher.close();
            }
        }

    }

    @Override
    public void setIngestor(Ingestor ingestor) {
        this.ingestor = ingestor;
    }

    @Override
    public Ingestor getIngestor() {
        return this.ingestor;
    }


}
