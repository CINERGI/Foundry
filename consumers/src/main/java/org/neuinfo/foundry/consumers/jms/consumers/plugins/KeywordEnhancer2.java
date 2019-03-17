package org.neuinfo.foundry.consumers.jms.consumers.plugins;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import org.jdom2.Element;
import org.json.JSONArray;
import org.json.JSONObject;
import org.neuinfo.foundry.common.model.EntityInfo;
import org.neuinfo.foundry.common.util.*;
import org.neuinfo.foundry.consumers.jms.consumers.jta.Document;
import org.neuinfo.foundry.consumers.jms.consumers.jta.Keyword;
import org.neuinfo.foundry.consumers.jms.consumers.jta.KeywordAnalyzer;
import org.neuinfo.foundry.consumers.plugin.IPlugin;
import org.neuinfo.foundry.consumers.plugin.Result;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

/**
 * Created by bozyurt on 6/9/16.
 */
public class KeywordEnhancer2 implements IPlugin {
    private String serviceURL;
    List<String> jsonPaths = new ArrayList<String>(5);
     private String configFileDir = "/data/cinergi/";
     private String stopListFile = "/data/cinergi/"+"stoplist.txt";
     private String nullIRIsFile = "/data/cinergi/"+"nulliris.txt";
     private String defaultKeywordRulesFile = "/data/cinergi/"+"keyword_rules.yml";
    KeywordAnalyzer keywordAnalyzer;
    Map<String, DefaultKeywords> defaultKeywordsMap;



    @Override
    public void initialize(Map<String, String> options) throws Exception {
        this.setServiceURL(options.get("serviceURL"));
        jsonPaths.add("$..'gmd:abstract'.'gco:CharacterString'.'_$'");
        jsonPaths.add("$..'gmd:citation'.'gmd:CI_Citation'.'gmd:title'.'gco:CharacterString'.'_$'");
        jsonPaths.add("$..'abstract'.'gco:CharacterString'.'_$'");
        jsonPaths.add("$..'title'.'gco:CharacterString'.'_$'");



        OntologyHandler handler = OntologyHandler.getInstance();
        OWLOntologyManager manager = handler.getManager();
        OWLDataFactory df = handler.getDf();
        OWLOntology extensions = handler.getExtensions();
        OWLOntology cinergi_ont = handler.getCinergi_ont();
        if (options.containsKey("serviceURL")) {
            this.setServiceURL(options.get("serviceURL"));
        }
        if (options.containsKey("configFileDir")) {
            this.setServiceURL(options.get("configFileDir"));
        }


        List<String> stoplist = Files.readAllLines(Paths.get(getStopListFile()), StandardCharsets.UTF_8);
        List<String> nullIRIs = Files.readAllLines(Paths.get(getNullIRIsFile()), StandardCharsets.UTF_8);
        LinkedHashMap<String, IRI> exceptionMap = null; // Create this using label duplicates spreadsheet

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        this.keywordAnalyzer = new KeywordAnalyzer(manager, df, cinergi_ont, extensions, gson,
                stoplist, exceptionMap, nullIRIs, getServiceURL());

        if (new File(getDefaultKeywordRulesFile()).isFile()) {
            this.defaultKeywordsMap = loadDefaultKeywordRules(getDefaultKeywordRulesFile());
        }
    }



    @Override
    public Result handle(DBObject docWrapper) {
        try {

            DBObject originalDoc = (DBObject) docWrapper.get("OriginalDoc");

            // handle edited documents (IBO)
            DBObject editedDoc = (DBObject) docWrapper.get("EditedDoc");
            JSONObject json;
            if (editedDoc != null) {
                json = JSONUtils.toJSON((BasicDBObject) editedDoc, false);
            } else {
                json = JSONUtils.toJSON((BasicDBObject) originalDoc, false);
            }
            DBObject siDBO = (DBObject) docWrapper.get("SourceInfo");
            String srcId = siDBO.get("SourceID").toString();
            String sourceName = siDBO.get("Name").toString();
            String primaryKey = docWrapper.get("primaryKey").toString();

            String docTitle = null;
            String docAbstract = null;
            JSONPathProcessor processor = new JSONPathProcessor();
            for (String jsonPath : jsonPaths) {
                List<Object> objects = processor.find(jsonPath, json);
                if (objects != null && !objects.isEmpty()) {
                    String text2Annotate = (String) objects.get(0);
                    if (!text2Annotate.equals("REQUIRED FIELD")) {
                        if (jsonPath.indexOf("title") != -1) {
                            docTitle = text2Annotate;
                        } else {
                            docAbstract = text2Annotate;
                        }
                    }
                }
            }

            XML2JSONConverter converter = new XML2JSONConverter();
            Element docEl = converter.toXML(json);
            Set<String> existingKeywords = CinergiXMLUtils.getExistingKeywords(docEl);

            if (!existingKeywords.isEmpty()) {
                StringBuilder sb = new StringBuilder(existingKeywords.size() * 30);
                sb.append(" . ");
                for (Iterator<String> it = existingKeywords.iterator(); it.hasNext(); ) {
                    sb.append(it.next());
                    if (it.hasNext()) {
                        sb.append(" , ");
                    }
                }
                String s = sb.toString().trim() + " .";
                if (docAbstract != null) {
                    docAbstract += s;
                } else {
                    docAbstract = s;
                }
            }
            // add default keywords if any
            if (defaultKeywordsMap != null) {
                DefaultKeywords defaultKeywords = defaultKeywordsMap.get(srcId);
                if (defaultKeywords != null) {
                    StringBuilder sb = new StringBuilder();
                    sb.append(' ');
                    for(Iterator<String> it =  defaultKeywords.getKeywords().iterator(); it.hasNext();) {
                        String kw = it.next();
                        sb.append(kw);
                        if (it.hasNext()) {
                            sb.append(" , ");
                        }
                    }
                    sb.append(" .");
                    docAbstract += sb.toString();
                }
            }

            Document doc = new Document();
            if (docAbstract != null) {
                doc.setText(docAbstract);
            }
            if (docTitle != null) {
                doc.setTitle(docTitle);
            }
            doc.setId(primaryKey);

            List<Keyword> keywords = keywordAnalyzer.findKeywords(doc);


            DBObject data = (DBObject) docWrapper.get("Data");
            Map<String, List<KeywordInfo>> category2KWIListMap = KeywordEnhancer2Helper.getKeywordsToBeAdded(keywords, json);
            JSONArray jsArr = new JSONArray();

            for (List<KeywordInfo> kwiList : category2KWIListMap.values()) {
                for (KeywordInfo kwi : kwiList) {
                    org.neuinfo.foundry.common.model.Keyword kw =
                            new org.neuinfo.foundry.common.model.Keyword(kwi.getTerm(), kwi.getId(), kwi.getHierarchyPath(), kwi.getCategory());
                    kw.addEntityInfo(new EntityInfo(null, kwi.getId(), -1, -1, kwi.getCategory()));
                    jsArr.put(kw.toJSON());
                }
            }
            //EnhancerUtils.useCinergiPreferredLabels(jsArr);
            data.put("keywords", JSONUtils.encode(jsArr));


            ProvenanceHelper.ProvData provData = new ProvenanceHelper.ProvData(primaryKey, ProvenanceHelper.ModificationType.Added);
            provData.setSourceName(sourceName).setSrcId(srcId).setDocTitle(docTitle);
            KeywordEnhancer2Helper.prepKeywordsProv(category2KWIListMap, provData);

            ProvenanceHelper.saveEnhancerProvenance("keywordEnhancer", provData, docWrapper);
            return new Result(docWrapper, Result.Status.OK_WITH_CHANGE);
        } catch (Throwable t) {
            t.printStackTrace();
            Result r = new Result(docWrapper, Result.Status.ERROR);
            r.setErrMessage(t.getMessage());
            return r;
        }
    }

    @Override
    public String getPluginName() {
        return "KeywordEnhancer2";
    }


    Map<String, DefaultKeywords> loadDefaultKeywordRules(String configFile) throws IOException {
        Yaml yaml = new Yaml();
        FileInputStream in = null;
        try {
            in = new FileInputStream(configFile);
            Map<String, Object> map = (Map<String, Object>) yaml.load(in);
            List<Map<String,Object>> rules = (List<Map<String, Object>>) map.get("rules");
            Map<String, DefaultKeywords> defKeywordsMap = new HashMap<String, DefaultKeywords>();
            for(Map<String,Object> ruleMap : rules) {
                String sourceID = ruleMap.get("sourceID").toString().trim();
                DefaultKeywords dk = new DefaultKeywords(sourceID);
                defKeywordsMap.put(sourceID, dk);
                List<String> keywords = (List<String>) ruleMap.get("keywords");
                for(String keyword : keywords) {
                    dk.addKeyword(keyword);
                }
            }


            return defKeywordsMap;
        } finally {
            Utils.close(in);
        }
    }

    public String getServiceURL() {
        return serviceURL;
    }

    public void setServiceURL(String serviceURL) {
        this.serviceURL = serviceURL;
    }

    public String getConfigFileDir() {
        return configFileDir;
    }

    public void setConfigFileDir(String configFileDir) {
        this.configFileDir = configFileDir;
    }

    public String getStopListFile() {
        return stopListFile;
    }

    public void setStopListFile(String stopListFile) {
        this.stopListFile = stopListFile;
    }

    public String getNullIRIsFile() {
        return nullIRIsFile;
    }

    public void setNullIRIsFile(String nullIRIsFile) {
        this.nullIRIsFile = nullIRIsFile;
    }

    public String getDefaultKeywordRulesFile() {
        return defaultKeywordRulesFile;
    }

    public void setDefaultKeywordRulesFile(String defaultKeywordRulesFile) {
        this.defaultKeywordRulesFile = defaultKeywordRulesFile;
    }

    public static class DefaultKeywords {
        String sourceID;
        List<String> keywords = new ArrayList<String>(10);

        public DefaultKeywords(String sourceID) {
            this.sourceID = sourceID;
        }

        public void addKeyword(String keyword) {
            keywords.add(keyword);
        }

        public String getSourceID() {
            return sourceID;
        }

        public List<String> getKeywords() {
            return keywords;
        }
    }

    public static void main(String[] args)  throws Exception {
        KeywordEnhancer2 kw = new KeywordEnhancer2();

        kw.loadDefaultKeywordRules("/var/data/cinergi/keyword_rules.yml");
    }
}
