package org.neuinfo.foundry.consumers;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import junit.framework.TestCase;
import org.json.JSONObject;
import org.neuinfo.foundry.common.provenance.ProvenanceRec;
import org.neuinfo.foundry.common.util.JSONUtils;
import org.neuinfo.foundry.consumers.common.ProvenanceClient;
import org.neuinfo.foundry.consumers.common.ConsumerUtils;
import org.neuinfo.foundry.consumers.jms.consumers.plugins.ProvenanceHelper;
import org.neuinfo.foundry.consumers.util.Helper;

import javax.xml.datatype.DatatypeConfigurationException;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Created by bozyurt on 12/4/14.
 */
public class ProvenanceTests extends TestCase {
    public ProvenanceTests(String name) {
        super(name);
    }

    public void testProvSubmit() throws Exception {
        ProvenanceRec.Builder builder = new ProvenanceRec.Builder("http://example.org", "foundry");

        String fileIdentifier = "52d969c0e4b08fdd52822380";
        String entityUUID = fileIdentifier + "_v1";
        String docCreationTime = ConsumerUtils.getTimeInProvenanceFormat();

        String docId = builder.entityWithAttr("UUID=" + entityUUID, "creationTime=" + docCreationTime).getLastGeneratedId();
        String activityId = builder.activityWithAttr("ingestion", docCreationTime, ConsumerUtils.getTimeInProvenanceFormat()).getLastGeneratedId();

        ProvenanceRec provenanceRec = builder.used(activityId, docId).wasGeneratedBy(docId, activityId).build();

        System.out.println(provenanceRec.asJSON());
        ProvenanceClient pc = new ProvenanceClient();
        //  pc.saveProvenance(provenanceRec);
    }

    public void testProvSubmit2() throws Exception {
        ProvenanceRec provenanceRec = getProvenanceRec("52d969c0e4b08fdd528223678");
        ProvenanceClient pc = new ProvenanceClient();
        pc.saveProvenance(provenanceRec);
    }

    public void testProvSubmit3() throws Exception {
        ProvenanceRec provenanceRec = getProvenanceRec("52d969c0e4b08fdd528223800");
        ProvenanceClient pc = new ProvenanceClient();
        pc.saveProvenance(provenanceRec);
    }

    private ProvenanceRec getProvenanceRec(String entityUUID) throws DatatypeConfigurationException {
        ProvenanceRec.Builder builder = new ProvenanceRec.Builder("http://example.org", "foundry");

        String docCreationTime = ConsumerUtils.getTimeInProvenanceFormat();
        String startUUID = UUID.randomUUID().toString();
        String inDocId = builder.entityWithAttr("UUID=" + startUUID, "creationTime=" + docCreationTime, "sourceName=ScienceBase").getLastGeneratedId();
        String outDocId = builder.entityWithAttr("UUID=" + entityUUID, "creationTime=" + docCreationTime).getLastGeneratedId();
        String activityId = builder.activityWithAttr("ingestion", docCreationTime, ConsumerUtils.getTimeInProvenanceFormat()).getLastGeneratedId();

        ProvenanceRec provenanceRec = builder.used(activityId, inDocId).wasGeneratedBy(outDocId, activityId).build();

        System.out.println(provenanceRec.asJSON());
        return provenanceRec;
    }

    public void testProvQ1() throws Exception {
        //String[] fidArr = {"52d969c0e4b08fdd52822387", "52d969c0e4b08fdd52822380"};
//        String[] fidArr = {"52d969c0e4b08fdd528223678", "52d969c0e4b08fdd528223800"};
       // String[] fidArr = {"51220a00e4b0b37542fda86e"}; //  "gov.noaa.nodc:7600529"};
        String[] fidArr = {"3592f7bc37ea27adea06455fbf7dfd87"};
        // 51220a00e4b0b37542fda86e
        for (String fileIdentifier : fidArr) {
            String entityUUID = fileIdentifier;
            ProvenanceClient pc = new ProvenanceClient();
            pc.getProvenance(entityUUID);
            System.out.println("--------------------");
        }
    }

    public void testProvSubmitC4() throws Exception {
        String entityUUID = "52d969c0e4b08fdd528223800";
        // entityUUID = "52d969c0e4b08fdd528223678";
        ProvenanceRec.Builder builder = new ProvenanceRec.Builder("http://example.org", "foundry");
        String docCreationTime = ConsumerUtils.getTimeInProvenanceFormat();

        String inDocId = builder.entityWithAttr("UUID=" + entityUUID, "sourceName=ScienceBase").getLastGeneratedId();
        String outDocId = builder.entityWithAttr("UUID=" + entityUUID).getLastGeneratedId();
        String activityId = builder.activityWithAttr("spatialEnhancer", docCreationTime, ConsumerUtils.getTimeInProvenanceFormat()).getLastGeneratedId();

        ProvenanceRec provenanceRec = builder.used(activityId, inDocId)
                .wasDerivedFrom(outDocId, inDocId, activityId)
                .wasGeneratedBy(outDocId, activityId).build();

        System.out.println(provenanceRec.asJSON());
        ProvenanceClient pc = new ProvenanceClient();
        pc.saveProvenance(provenanceRec);
    }

    public void testProvDelete() throws Exception {
        ProvenanceClient pc = new ProvenanceClient();
        pc.deleteProvenance("4");
    }


    public void testProvenanceHelper() throws Exception {
        ProvenanceHelper.ProvData pd = new ProvenanceHelper.ProvData("abc38747", ProvenanceHelper.ModificationType.Added);

        pd.setSourceName("ScienceBase").setSrcId("cinergi-0001").addModifiedFieldProv("Added keyword x");

        ProvenanceHelper.saveEnhancerProvenance("keywordEnhancer", pd, null);
    }

    public void testProvenanceHelperIngest() throws Exception {
        ProvenanceHelper.ProvData pd = new ProvenanceHelper.ProvData("abc38747", ProvenanceHelper.ModificationType.Added);

        pd.setSourceName("ScienceBase").setSrcId("cinergi-0001").addModifiedFieldProv("Ingested abc38747");

        ProvenanceHelper.saveIngestionProvenance("ingestion", pd, new Date(), null);
    }

    public void testProvenanceHelperDelete() throws Exception {
        // ProvenanceHelper.removeProvenance("abc38747");
        ProvenanceClient pc = new ProvenanceClient();
        pc.getProvenance("abc38747");
    }

    public void testDeleteProvenanceChain() throws Exception {
        ProvenanceClient pc = new ProvenanceClient();
        pc.deleteProvenance("51220a00e4b0b37542fda86e");
    }

    public void testAddProvenanceChain() throws Exception {
        Helper helper = new Helper("");
        try {
            helper.startup("cinergi-consumers-cfg.xml");
            ProvenanceClient pc = new ProvenanceClient();
            List<BasicDBObject> docWrappers = helper.getDocWrappers("cinergi-0001");
            for (BasicDBObject docWrapper : docWrappers) {
                // 51220a00e4b0b37542fda86e
                String primaryKey = docWrapper.get("primaryKey").toString();
                if (primaryKey.equals("51220a00e4b0b37542fda86e")) {
                    BasicDBObject history = (BasicDBObject) docWrapper.get("History");
                    BasicDBObject prov = (BasicDBObject) history.get("prov");

                    BasicDBList events = (BasicDBList) prov.get("events");
                    for (int i = 0; i < events.size(); i++) {
                        BasicDBObject provData = (BasicDBObject) events.get(i);
                        JSONObject json = JSONUtils.toJSON(provData, true);
                        System.out.println(json.toString(2));
                        System.out.println("-----------------------");
                        // pc.saveProvenance(json);
                    }
                    break;
                }
            }

        } finally {
            helper.shutdown();
        }
    }
}
