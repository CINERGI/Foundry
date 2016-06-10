package org.neuinfo.foundry.consumers.jms.consumers.plugins;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import org.json.JSONArray;
import org.json.JSONObject;
import org.neuinfo.foundry.common.model.EntityInfo;
import org.neuinfo.foundry.common.util.JSONPathProcessor;
import org.neuinfo.foundry.common.util.JSONUtils;
import org.neuinfo.foundry.common.util.KeywordInfo;
import org.neuinfo.foundry.consumers.jms.consumers.jta.Document;
import org.neuinfo.foundry.consumers.jms.consumers.jta.Keyword;
import org.neuinfo.foundry.consumers.jms.consumers.jta.KeywordAnalyzer;
import org.neuinfo.foundry.consumers.plugin.IPlugin;
import org.neuinfo.foundry.consumers.plugin.Result;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by bozyurt on 6/9/16.
 */
public class KeywordEnhancer2 implements IPlugin {
    String serviceURL;
    List<String> jsonPaths = new ArrayList<String>(5);
    String stopListFile = "/var/data/cinergi/stoplist.txt";
    String nullIRIsFile = "/var/data/cinergi/nulliris.txt";
    KeywordAnalyzer keywordAnalyzer;

    @Override
    public void initialize(Map<String, String> options) throws Exception {
        this.serviceURL = options.get("serviceURL");
        jsonPaths.add("$..'gmd:abstract'.'gco:CharacterString'.'_$'");
        jsonPaths.add("$..'gmd:citation'.'gmd:CI_Citation'.'gmd:title'.'gco:CharacterString'.'_$'");
        jsonPaths.add("$..'abstract'.'gco:CharacterString'.'_$'");
        jsonPaths.add("$..'title'.'gco:CharacterString'.'_$'");

        long start = System.currentTimeMillis();
        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        OWLDataFactory df = manager.getOWLDataFactory();
        manager.setSilentMissingImportsHandling(true);

        System.out.println("loading ontology");
        OWLOntology cinergi_ont = manager.loadOntologyFromOntologyDocument(
                IRI.create("http://hydro10.sdsc.edu/cinergi_ontology/cinergi.owl"));
        System.out.println("ontology loaded");
        System.out.println("Time elapsed (msecs): " + (System.currentTimeMillis() - start));

        OWLOntology extensions = null;
        for (OWLOntology o : manager.getOntologies()) {
            if (o.getOntologyID().getOntologyIRI().toString().equals(
                    "http://hydro10.sdsc.edu/cinergi_ontology/cinergiExtensions.owl")) {
                extensions = o;
            }
        }
        if (extensions == null) {
            throw new Exception("failed to gather extensions");
        }
        List<String> stoplist = Files.readAllLines(Paths.get(stopListFile), StandardCharsets.UTF_8);
        List<String> nullIRIs = Files.readAllLines(Paths.get(nullIRIsFile), StandardCharsets.UTF_8);
        LinkedHashMap<String, IRI> exceptionMap = null; // Create this using label duplicates spreadsheet

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        this.keywordAnalyzer = new KeywordAnalyzer(manager, df, cinergi_ont, extensions, gson,
                stoplist, exceptionMap, nullIRIs, serviceURL);
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

            String docTitle = null;
            String docAbstract = null;
            StringBuilder sb = new StringBuilder(2048);
            JSONPathProcessor processor = new JSONPathProcessor();
            for (String jsonPath : jsonPaths) {
                List<Object> objects = processor.find(jsonPath, json);
                if (objects != null && !objects.isEmpty()) {
                    String text2Annotate = (String) objects.get(0);
                    if (!text2Annotate.equals("REQUIRED FIELD")) {
                        sb.append(' ').append(text2Annotate);
                        if (jsonPath.indexOf("title") != -1) {
                            docTitle = text2Annotate;
                        } else {
                            docAbstract = text2Annotate;
                        }
                    }
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
                            new org.neuinfo.foundry.common.model.Keyword(kwi.getTerm(), kwi.getId(), null, kwi.getCategory());
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
}
