package org.neuinfo.foundry.consumers.jms.consumers.plugins;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import org.apache.log4j.Logger;
import org.jdom2.Element;
import org.json.JSONArray;
import org.json.JSONObject;
import org.neuinfo.foundry.common.model.EntityInfo;
import org.neuinfo.foundry.common.util.*;
import org.neuinfo.foundry.common.util.CinergiXMLUtils.KeywordInfo;
import org.neuinfo.foundry.common.model.Keyword;
import org.neuinfo.foundry.consumers.jms.consumers.plugins.ProvenanceHelper.ProvData;
import org.neuinfo.foundry.consumers.plugin.IPlugin;
import org.neuinfo.foundry.consumers.plugin.Result;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.*;

/**
 * Created by bozyurt on 2/3/15.
 */
public class OrganizationEnhancer implements IPlugin {
    protected String workDir;
    protected String scriptPath;
    private final static Logger logger = Logger.getLogger("OrganizationEnhancer");

    @Override
    public void initialize(Map<String, String> options) throws Exception {
        this.workDir = options.get("workDir");
        Assertion.assertNotNull(this.workDir);
        File wd = new File(this.workDir);
        if (!wd.isDirectory()) {
            wd.mkdirs();
        }
        Assertion.assertTrue(wd.isDirectory());
        this.scriptPath = options.get("scriptPath");
        Assertion.assertNotNull(this.scriptPath);
        Assertion.assertTrue(new File(this.scriptPath).isDirectory());
        File scriptFile = new File(this.scriptPath, "org_enhancer.py");
        Assertion.assertNotNull(scriptFile.isFile());
    }

    @Override
    public Result handle(DBObject docWrapper) {
        try {
            DBObject originalDoc = (DBObject) docWrapper.get("OriginalDoc");
            JSONObject json = JSONUtils.toJSON((BasicDBObject) originalDoc, false);
            DBObject siDBO = (DBObject) docWrapper.get("SourceInfo");
            String srcId = siDBO.get("SourceID").toString();
            String sourceName = siDBO.get("Name").toString();
            String primaryKey = docWrapper.get("primaryKey").toString();
            Date startDate = new Date();
            XML2JSONConverter converter = new XML2JSONConverter();
            Element docEl = converter.toXML(json);
            String prefix = primaryKey.replaceAll("\\s+", "_").replaceAll("/+", "_");
            File inFile = new File(workDir, prefix + "_in.xml");
            File outFile = new File(workDir, prefix + "_out.xml");

            Utils.saveXML(docEl, inFile.getAbsolutePath());
            List<String> cmdList = new ArrayList<String>(10);
            File scriptFile = new File(this.scriptPath, "org_enhancer.py");
            cmdList.add(scriptFile.getAbsolutePath());
            cmdList.add("-i");
            cmdList.add(inFile.getAbsolutePath());
            cmdList.add("-o");
            cmdList.add(outFile.getAbsolutePath());

            ProcessBuilder pb = new ProcessBuilder(cmdList);

            Map<String, String> env = pb.environment();
            env.put("PATH", env.get("PATH") + ":" + this.scriptPath);
            logger.info("scriptPath:" + scriptPath);

            Process process = pb.start();
            BufferedReader ein = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            String line;

            BufferedReader bin = new BufferedReader(new InputStreamReader(
                    process.getInputStream()));
            while ((line = bin.readLine()) != null) {
                logger.info(line);
            }
            while ((line = ein.readLine()) != null) {
                logger.error(line);
            }

            int rc = process.waitFor();
            logger.info("rc:" + rc);

            if (rc == 0) {
                ProvData provData = new ProvData(primaryKey, ProvenanceHelper.ModificationType.Added);
                if (outFile.exists()) {
                    // load enhanced XML extract new organization keywords and add  to orgKeywords
                    KeywordData kd = addOrganizations(outFile);
                    inFile.delete();
                    if (kd.jsArr.length() > 0) {
                        DBObject data = (DBObject) docWrapper.get("Data");
                        Map<String, List<KeywordInfo>> category2KWIListMap = EnhancerUtils.getKeywordsToBeAdded(kd.jsArr, json);
                        kd.jsArr = EnhancerUtils.filter(kd.jsArr, category2KWIListMap, kd.keywordMap);
                        data.put("orgKeywords", JSONUtils.encode(kd.jsArr));
                        provData.setSourceName(sourceName).setSrcId(srcId);
                        EnhancerUtils.prepKeywordsProv(category2KWIListMap, provData);
                        ProvenanceHelper.saveEnhancerProvenance("organizationEnhancer", provData, docWrapper);
                        outFile.delete();
                        return new Result(docWrapper, Result.Status.OK_WITH_CHANGE);
                    } else {
                        outFile.delete();
                        EnhancerUtils.prepKeywordsProv(null, provData);
                        ProvenanceHelper.saveEnhancerProvenance("organizationEnhancer", provData, docWrapper);
                        return new Result(docWrapper, Result.Status.OK_WITHOUT_CHANGE);
                    }
                } else {
                    EnhancerUtils.prepKeywordsProv(null, provData);
                    ProvenanceHelper.saveEnhancerProvenance("organizationEnhancer", provData, docWrapper);
                    return new Result(docWrapper, Result.Status.OK_WITHOUT_CHANGE);
                }
            }
            inFile.delete();
            if (outFile.exists()) {
                outFile.delete();
            }
            Result r = new Result(docWrapper, Result.Status.ERROR);
            r.setErrMessage(scriptFile.getName() + " returned non zero exit code");
            return r;

        } catch (Throwable t) {
            t.printStackTrace();
            Result r = new Result(docWrapper, Result.Status.ERROR);
            r.setErrMessage(t.getMessage());
            return r;
        }
    }


    KeywordData addOrganizations(File outFile) throws Exception {
        Element docEl = Utils.loadXML(outFile.getAbsolutePath());
        Map<String, Keyword> keywordMap = new LinkedHashMap<String, Keyword>();
        List<KeywordInfo> organizationKeywords = CinergiXMLUtils.getOrganizationKeywords(docEl);
        JSONArray jsArr = new JSONArray();
        if (!organizationKeywords.isEmpty()) {

            for (KeywordInfo kwi : organizationKeywords) {
                Keyword kw = new Keyword(kwi.getTerm());
                EntityInfo ei = new EntityInfo("", "", -1, -1, kwi.getCategory());
                kw.addEntityInfo(ei);
                jsArr.put(kw.toJSON());
                keywordMap.put(kw.getTerm(), kw);
            }
        }
        KeywordData kd = new KeywordData();
        kd.jsArr = jsArr;
        kd.keywordMap = keywordMap;
        return kd;
    }

    static class KeywordData {
        JSONArray jsArr;
        Map<String, Keyword> keywordMap;

    }

    @Override
    public String getPluginName() {
        return "OrganizationEnhancer";
    }
}
