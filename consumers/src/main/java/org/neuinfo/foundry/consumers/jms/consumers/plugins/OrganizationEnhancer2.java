package org.neuinfo.foundry.consumers.jms.consumers.plugins;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.neuinfo.foundry.common.model.EntityInfo;
import org.neuinfo.foundry.common.model.Keyword;
import org.neuinfo.foundry.common.util.CinergiXMLUtils;
import org.neuinfo.foundry.common.util.JSONPathProcessor;
import org.neuinfo.foundry.common.util.JSONUtils;
import org.neuinfo.foundry.common.util.KeywordInfo;
import org.neuinfo.foundry.consumers.common.Organization;
import org.neuinfo.foundry.consumers.plugin.IPlugin;
import org.neuinfo.foundry.consumers.plugin.Result;

import java.util.*;

/**
 * Created by bozyurt on 4/27/16.
 */
public class OrganizationEnhancer2 implements IPlugin {
    private final static Logger logger = Logger.getLogger(OrganizationEnhancer2.class);

    @Override
    public void initialize(Map<String, String> options) throws Exception {

    }

    @Override
    public Result handle(DBObject docWrapper) {
        try {
            Set<String> organizationSet = new HashSet<String>();
            List<String> orgStrings = new ArrayList<String>(10);
            DBObject originalDoc = (DBObject) docWrapper.get("OriginalDoc");
            DBObject siDBO = (DBObject) docWrapper.get("SourceInfo");
            String srcId = siDBO.get("SourceID").toString();
            String sourceName = siDBO.get("Name").toString();
            String primaryKey = docWrapper.get("primaryKey").toString();
            JSONObject json = JSONUtils.toJSON((BasicDBObject) originalDoc, false);
            JSONPathProcessor jpp = new JSONPathProcessor();
            List<Object> objects = jpp.find("$..'gmd:contact'.'gmd:CI_ResponsibleParty'.'gmd:organisationName'.'gco:CharacterString'.'_$'", json);
            if (objects != null) {
                for (Object o : objects) {
                    orgStrings.add(o.toString());
                }
            }
            jpp = new JSONPathProcessor();
            objects = jpp.find("$..'gmd:citation'.'gmd:CI_Citation'.'gmd:citedResponsibleParty'.'gmd:organisationName'.'gco:CharacterString'.'_$'",
                    json);
            if (objects != null) {
                for (Object o : objects) {
                    orgStrings.add(o.toString());
                }
            }
            // points of contacts
            jpp = new JSONPathProcessor();
            objects = jpp.find("$..'gmd:identificationInfo'.'gmd:MD_DataIdentification'.'gmd:pointOfContact'[*].'gmd:CI_ResponsibleParty'.'gmd:organisationName'.'gco:CharacterString'.'_$'",
                    json);
            if (objects != null) {
                for (Object o : objects) {
                    orgStrings.add(o.toString());
                }
            }
            List<Organization> organizations = new ArrayList<Organization>();
            for (String orgStr : orgStrings) {
                String[] toks = orgStr.split("[^a-zA-Z\\s\\d:.\\-]");
                for (String tok : toks) {
                    tok = tok.trim();
                    if (tok.startsWith(":")) {
                        tok = tok.substring(1);
                    }
                    if (!organizationSet.contains(tok)) {
                        organizationSet.add(tok);
                        String orgName = Organization.normalizeOrgName(tok);
                        logger.info("Organization: " + orgName);
                        Organization organization = Organization.validateInViaf(orgName);
                        if (organization != null) {
                            logger.info("**** Validated Organization: " + organization.getName() + " [" + organization.getUri() + "]");
                            organizations.add(organization);
                        }
                    }
                }
            }
            logger.info("==================== end of organization enhancer for " + primaryKey + " [" + sourceName + "] =========================");
            //
            String category = "Organization > Institution";
            JSONArray jsArr = new JSONArray();
            List<KeywordInfo> kwiList = new ArrayList<KeywordInfo>(10);
            for (Organization org : organizations) {
                Keyword kw = new Keyword(org.getName());
                kw.addEntityInfo(new EntityInfo("", org.getUri(), -1, -1, category));
                jsArr.put(kw.toJSON());
                kwiList.add(new KeywordInfo(org.getUri(), org.getName(), category, null, CinergiXMLUtils.KeywordType.Organization));
            }
            if (jsArr.length() > 0) {
                DBObject data = (DBObject) docWrapper.get("Data");
                data.put("orgKeywords", JSONUtils.encode(jsArr));
                ProvenanceHelper.ProvData provData = new ProvenanceHelper.ProvData(primaryKey, ProvenanceHelper.ModificationType.Added);
                Map<String, List<KeywordInfo>> category2KWIListMap = new HashMap<String, List<KeywordInfo>>(7);
                category2KWIListMap.put(category, kwiList);

                provData.setSourceName(sourceName).setSrcId(srcId);
                EnhancerUtils.prepKeywordsProv(category2KWIListMap, provData);
                ProvenanceHelper.saveEnhancerProvenance("organizationEnhancer", provData, docWrapper);
                return new Result(docWrapper, Result.Status.OK_WITH_CHANGE);
            } else {
                return new Result(docWrapper, Result.Status.OK_WITHOUT_CHANGE);
            }
        } catch (Throwable t) {
            t.printStackTrace();
            Result r = new Result(docWrapper, Result.Status.ERROR);
            r.setErrMessage(t.getMessage());
            return r;
        }
    }

    @Override
    public String getPluginName() {
        return "OrganizationEnhancer";
    }


}
