package org.neuinfo.foundry.jms.producer;

import org.apache.commons.cli.*;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.LogManager;
import org.json.JSONObject;
import org.neuinfo.foundry.common.ingestion.DocProcessingStatsService;
import org.neuinfo.foundry.common.model.Source;
import org.neuinfo.foundry.common.util.Assertion;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by bozyurt on 10/30/14.
 */
public class ManagementService {

    private PipelineTriggerHelper helper;


    public ManagementService(String queueName) {
        this.helper = new PipelineTriggerHelper(queueName);
    }

    public void startup(String configFile) throws Exception {
        this.helper.startup(configFile);
    }

    public void shutdown() {
        helper.shutdown();
    }


    void deleteDocuments(String sourceID) {
        this.helper.getDocService().deleteDocuments4Resource(this.helper.getCollectionName(), sourceID, null);
    }

    void showWorkflows(PrintWriter out) {
        this.helper.showWS(out);
    }

    void showProcessingStats(String sourceID, PrintWriter out) {
        List<DocProcessingStatsService.SourceStats> processingStats = this.helper.getProcessingStats(sourceID);
        Map<String, DocProcessingStatsService.WFStatusInfo> wfsiMap = this.helper.getWorkflowStatusInfo(sourceID, processingStats);
        for (DocProcessingStatsService.SourceStats ss : processingStats) {
            DocProcessingStatsService.WFStatusInfo wfsi = wfsiMap.get(ss.getSourceID());
            showSourceStats(ss, wfsi, out);
        }
    }

    void showSourceStats(DocProcessingStatsService.SourceStats ss, DocProcessingStatsService.WFStatusInfo wfStatusInfo, PrintWriter out) {
        StringBuilder sb = new StringBuilder(128);
        sb.append(StringUtils.rightPad(ss.getSourceID(), 15)).append(" ");
        if (wfStatusInfo != null) {
            sb.append(StringUtils.leftPad(wfStatusInfo.getStatus(), 12)).append(" ");
        } else {
            sb.append(StringUtils.leftPad("unknown", 12)).append(" ");
        }

        Map<String, Integer> statusCountMap = ss.getStatusCountMap();
        int totCount = 0;
        for (Integer count : statusCountMap.values()) {
            totCount += count;
        }
        sb.append("total:").append(StringUtils.leftPad(String.valueOf(totCount), 10)).append(" ");
        Integer finishedCount = statusCountMap.get("finished");
        Integer errorCount = statusCountMap.get("error");
        finishedCount = finishedCount == null ? 0 : finishedCount;
        errorCount = errorCount == null ? 0 : errorCount;
        sb.append("finished:").append(StringUtils.leftPad(finishedCount.toString(), 10)).append(" ");
        sb.append("error:").append(StringUtils.leftPad(errorCount.toString(), 8)).append("  ");
        for (String status : statusCountMap.keySet()) {
            if (status.equals("finished") || status.equals("error")) {
                continue;
            }
            Integer statusCount = statusCountMap.get(status);
            String s = StringUtils.leftPad(status + ":", 15) + StringUtils.leftPad(statusCount.toString(), 10);
            sb.append(s).append(" ");
        }
        out.println(sb.toString().trim());
    }

    public static void showHelp(PrintWriter out) {
        out.println("Available commands");
        out.println("\thelp - shows this message.");
        out.println("\tingest <sourceID>");
        out.println("\th - show all command history");
        out.println("\tdd <sourceID>  - delete docs for a sourceID");
        out.println("\ttrigger <sourceID> <status-2-match> <queue-2-send> [<new-status> [<new-out-status>]] (e.g. trigger nif-0000-00135 new.1 foundry.uuid.1)");
        out.println("\trun <sourceID> status:<status-2-match> step:<step-name> [on|to_end] (e.g. run nif-0000-00135 status:new.1 step:transform)");
        out.println("\tlist - lists all of the existing sources.");
        out.println("\tstatus [<sourceID>] - show processing status of data source(s)");
        out.println("\tws - show configured workflow(s)");
        out.println("\texit - exits the management client.");
    }

    public static void usage(Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("ManagementService", options);
        System.exit(1);
    }

    static boolean confirm(BufferedReader in, String message) throws IOException {
        System.out.print(message + " (y/[n])? ");
        String ans = in.readLine();
        ans = ans.trim();
        if (ans.equalsIgnoreCase("y")) {
            return true;
        }
        return false;
    }


    static void handleRun(String ans, ManagementService ms, Options options) throws Exception {
        String[] tokens = ans.split("\\s+");
        int numTokens = tokens.length;
        if (numTokens < 4) {
            usage(options);
        }

        String srcNifId = tokens[1];
        String status2Match = null;
        boolean run2TheEnd = false;
        String stepName = null;
        for (int i = 2; i < numTokens; i++) {
            String token = tokens[i];
            if (token.startsWith("status:")) {
                status2Match = token.substring(token.indexOf(':') + 1);
            } else if (token.startsWith("step:")) {
                stepName = token.substring(token.indexOf(':') + 1);
            } else if (token.equalsIgnoreCase("on") || token.equalsIgnoreCase("to_end")) {
                run2TheEnd = true;
            }
        }
        Source source = ms.helper.findSource(srcNifId);
        Assertion.assertNotNull(source);
        System.out.println("status2Match:" + status2Match + " stepName:" + stepName + " run2TheEnd:" + run2TheEnd);
        ms.helper.runPipelineSteps(source, status2Match, stepName, run2TheEnd);
    }


    public static void main(String[] args) throws Exception {
        Option help = new Option("h", "print this message");
        Option configFileOption = Option.builder("c").argName("config-file")
                .hasArg().desc("config-file e.g. cinergi-dispatcher-cfg.xml").build();

        Options options = new Options();
        options.addOption(help);
        options.addOption(configFileOption);
        CommandLineParser cli = new DefaultParser();
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

        String configFile = null;
        if (line.hasOption('c')) {
            configFile = line.getOptionValue('c');
        }

        LogManager.getLogger("org.mongodb.driver.connection").setLevel(org.apache.log4j.Level.OFF);
        LogManager.getLogger("org.mongodb.driver.management").setLevel(org.apache.log4j.Level.OFF);
        LogManager.getLogger("org.mongodb.driver.cluster").setLevel(org.apache.log4j.Level.OFF);
        LogManager.getLogger("org.mongodb.driver.protocol.insert").setLevel(org.apache.log4j.Level.OFF);
        LogManager.getLogger("org.mongodb.driver.protocol.query").setLevel(org.apache.log4j.Level.OFF);
        LogManager.getLogger("org.mongodb.driver.protocol.update").setLevel(org.apache.log4j.Level.OFF);


        ManagementService ms = new ManagementService("foundry.consumer.head");
        Set<String> history = new LinkedHashSet<String>();
        String lastCommand = null;
        try {
            ms.startup(configFile);
            PrintWriter out = new PrintWriter(System.out, true);
            out.flush();
            BufferedReader in = new BufferedReader(new InputStreamReader(System.in));

            boolean finished = false;
            while (!finished) {
                System.out.print("Foundry:>> ");
                String ans = in.readLine();
                ans = ans.trim();

                if (ans.equals("!!") && lastCommand != null) {
                    ans = lastCommand;
                    out.println("running command:" + ans);
                }
                if (ans.equals("help")) {
                    showHelp(out);
                }
                if (ans.equals("ws")) {
                    ms.showWorkflows(out);
                } else if (ans.startsWith("ingest")) {
                    String[] toks = ans.split("\\s+");
                    if (toks.length == 2) {
                        history.add(ans);
                        String srcNifId = toks[1];
                        Source source = ms.helper.findSource(srcNifId);
                        JSONObject json = ms.helper.prepareMessageBody("ingest", source);
                        ms.helper.sendMessage(json);
                        lastCommand = ans;
                    }
                } else if (ans.startsWith("run")) {
                    handleRun(ans, ms, options);
                } else if (ans.equals("history") || ans.equals("h")) {
                    for (String h : history) {
                        out.println(h);
                    }
                } else if (ans.startsWith("dd")) {
                    String[] toks = ans.split("\\s+");
                    if (toks.length == 2) {
                        String sourceID = toks[1];
                        if (confirm(in, "Do you want to delete docs for " + sourceID + "?")) {
                            ms.deleteDocuments(sourceID);
                        }
                    }
                } else if (ans.startsWith("list")) {
                    List<Source> sources = ms.helper.findSources();
                    for (Source source : sources) {
                        StringBuilder sb = new StringBuilder(128);
                        sb.append(StringUtils.rightPad(source.getResourceID(), 16)).append(" - ");
                        sb.append(source.getName());
                        out.println(sb.toString());
                    }
                    lastCommand = ans;
                } else if (ans.startsWith("trigger")) {
                    String[] toks = ans.split("\\s+");
                    if (toks.length == 4 || toks.length == 5 || toks.length == 6) {
                        String srcNifId = toks[1];
                        String status2Match = toks[2];
                        String toQueue = toks[3];
                        String newStatus = null;
                        if (toks.length >= 5) {
                            newStatus = toks[4];
                        }
                        String newOutStatus = null;
                        if (toks.length == 6) {
                            newOutStatus = toks[5];
                        }
                        Source source = ms.helper.findSource(srcNifId);
                        Assertion.assertNotNull(source);
                        ms.helper.triggerPipeline(source, status2Match, toQueue, newStatus, newOutStatus);
                        history.add(ans);
                        lastCommand = ans;
                    }
                } else if (ans.startsWith("status")) {
                    String[] toks = ans.split("\\s+");
                    if (toks.length == 1 || toks.length == 2) {
                        if (toks.length == 2) {
                            String srcNifId = toks[1];
                            ms.showProcessingStats(srcNifId, out);
                        } else {
                            ms.showProcessingStats(null, out);
                        }
                        history.add(ans);
                        lastCommand = ans;
                    }
                } else if (ans.equals("exit")) {
                    break;
                }
            }
        } finally {
            ms.shutdown();
        }
    }
}
