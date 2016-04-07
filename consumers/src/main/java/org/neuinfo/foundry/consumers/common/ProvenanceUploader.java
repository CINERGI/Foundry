package org.neuinfo.foundry.consumers.common;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import org.apache.commons.cli.*;
import org.json.JSONObject;
import org.neuinfo.foundry.common.util.Assertion;
import org.neuinfo.foundry.common.util.JSONUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by bozyurt on 4/7/15.
 */
public class ProvenanceUploader {
    String configFile = "cinergi-consumers-cfg.xml";

    public ProvenanceUploader() {
    }

    public String getConfigFile() {
        return configFile;
    }

    public void setConfigFile(String configFile) {
        this.configFile = configFile;
    }

    public static void fixVersionNumbers(JSONObject json, int order) {
        if (order == 0) {
            return;
        }
        String en1Version = null;
        String en2Version = null;
        if (order == 1) {
            en1Version = "1.1";
            en2Version = "1.2";
        } else if (order == 2) {
            en1Version = "1.2";
            en2Version = "1.3";
        } else if (order == 3) {
            en1Version = "1.3";
            en2Version = "1.4";
        }
        Assertion.assertNotNull(en1Version);
        Assertion.assertNotNull(en2Version);

        JSONObject entity = json.getJSONObject("entity");
        JSONObject en1 = entity.getJSONObject("foundry:en_1");
        en1.getJSONObject("foundry:version").put("$", en1Version);

        JSONObject en2 = entity.getJSONObject("foundry:en_2");
        en2.getJSONObject("foundry:version").put("$", en2Version);
    }

    public void addProvenanceChains2ProvServer(String sourceID) throws Exception {
        Helper helper = new Helper("");
        try {
            ProvenanceClient pc = new ProvenanceClient();
            helper.startup(configFile);
//            List<BasicDBObject> docWrappers = helper.getDocWrappers(sourceID);
            List<String> oidList = helper.getDocWrapperIds(sourceID);
            int count = 0;
            for (String oid : oidList) {
                DBObject docWrapper = helper.getDocWrapper(oid);
                String primaryKey = docWrapper.get("primaryKey").toString();
                BasicDBObject history = (BasicDBObject) docWrapper.get("History");
                BasicDBObject prov = (BasicDBObject) history.get("prov");
                if (prov == null) {
                    System.out.println("Skipping " + primaryKey + " for missing provenance");
                    continue;
                }

                BasicDBList events = (BasicDBList) prov.get("events");
                if (events.size() >= 4) {
                    pc.deleteProvenance(primaryKey);
                    for (int i = 0; i < 4; i++) {
                        BasicDBObject provData = (BasicDBObject) events.get(i);
                        JSONObject json = JSONUtils.toJSON(provData, true);
                        //fixVersionNumbers(json, i);

                        System.out.println(json.toString(2));
                        System.out.println("-----------------------");
                        pc.saveProvenance(json);
                    }
                    count++;
                    System.out.println();
                    System.out.println("=======================================");

                }
                if (count > 2) {
                    // break;
                }
            }
        } finally {
            helper.shutdown();
        }
    }

    public static void usage(Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("ProvenanceUploader", options);
        System.exit(1);
    }

    public static void main(String[] args) throws Exception {
        Option help = new Option("h", "print this message");
        Option configFileOption = OptionBuilder.withArgName("config-file")
                .hasArg().withDescription("config-file e.g. cinergi-consumers-cfg.xml").create('c');
        Option sourceIdOption = OptionBuilder.withArgName("sourceId(s)")
                .hasArg().isRequired(true)
                .withDescription("A comma separated list of sourceIDs (e.g cinergi-0014)").create('s');
        Options options = new Options();
        options.addOption(help);
        options.addOption(configFileOption);
        options.addOption(sourceIdOption);
        CommandLineParser cli = new GnuParser();
        CommandLine line = null;
        try {
            line = cli.parse(options, args);
        } catch (Exception x) {
            System.err.println(x.getMessage());
            usage(options);
        }

        if (line.hasOption("h")) {
            usage(options);
        }

        ProvenanceUploader pu = new ProvenanceUploader();
        String sourceIdsStr = line.getOptionValue('s');
        String[] sourceIDs = sourceIdsStr.split("\\s*,\\s*");

        if (line.hasOption('c')) {
            String configFile = line.getOptionValue('c');
            pu.setConfigFile(configFile);
        }

        for (String sourceID : sourceIDs) {
            System.out.println("Adding provenance for " + sourceID);
            pu.addProvenanceChains2ProvServer(sourceID);
        }

        // pu.addProvenanceChains2ProvServer("cinergi-0002");
        // pu.addProvenanceChains2ProvServer("cinergi-0003");
        // pu.addProvenanceChains2ProvServer("cinergi-0004");
        // pu.addProvenanceChains2ProvServer("cinergi-0005");
        // pu.addProvenanceChains2ProvServer("cinergi-0006");
        // pu.addProvenanceChains2ProvServer("cinergi-0007");
    }
}
