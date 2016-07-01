package org.neuinfo.foundry.ingestor.ws.dm;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.wordnik.swagger.annotations.*;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.filter.Filters;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;
import org.json.JSONArray;
import org.json.JSONObject;
import org.neuinfo.foundry.common.Constants;
import org.neuinfo.foundry.common.model.EntityInfo;
import org.neuinfo.foundry.common.model.Keyword;
import org.neuinfo.foundry.common.util.*;
import org.neuinfo.foundry.ingestor.ws.MongoService;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.*;
import java.util.*;

/**
 * Created by bozyurt on 7/17/14.
 */
@Path("cinergi/docs")
@Api(value = "cinergi/docs", description = "Metadata Documents")
public class DocumentResource {
    private static String theApiKey = "72b6afb31ba46a4e797c3f861c5a945f78dfaa81";
    static FacetHierarchyHandler fhh;

    static {
        try {
            fhh = FacetHierarchyHandler.getInstance(Constants.SCIGRAPH_URL);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Path("/{resourceId}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {@ApiResponse(code = 500, message = "An internal error occurred during the retrieval of document ids")})
    @ApiOperation(value = "Retrieve all document ids for the given resource ID",
            notes = "",
            response = String.class)
    public Response getDocumentIdsForResource(@ApiParam(value = "The resource ID for the harvest source", required = true) @PathParam("resourceId") String resourceId) {
        MongoService mongoService = null;
        try {
            mongoService = new MongoService();

            Set<String> statusSet = new HashSet<>(7);
            statusSet.add("finished");
            JSONArray documentIds4Source = mongoService.findDocumentIds4Source(resourceId, statusSet);
            JSONObject json = new JSONObject();
            json.put("documentIds", documentIds4Source);
            String jsonStr = json.toString(2);
            return Response.ok(jsonStr).build();
        } catch (Exception x) {
            x.printStackTrace();
            return Response.serverError().build();
        } finally {
            if (mongoService != null) {
                mongoService.shutdown();
            }
        }
    }

    @Path("/{resourceId}/{docId}")
    @GET
    @Produces(MediaType.APPLICATION_XML)
    @ApiResponses(value = {@ApiResponse(code = 500, message = "An internal error occurred during the document retrieval"),
            @ApiResponse(code = 404, message = "No metadata document is found with the given resource ID and document ID")})
    @ApiOperation(value = "Retrieve the original document as XML",
            notes = "",
            response = String.class)
    public Response getDocument(@ApiParam(value = "The resource ID for the harvest source", required = true) @PathParam("resourceId") String resourceId,
                                @ApiParam(value = "The document ID for the metadata document", required = true) @PathParam("docId") String docId) {
        String xmlStr = null;
        MongoService mongoService = null;
        try {
            mongoService = new MongoService();

            BasicDBObject docWrapper = mongoService.findTheDocument(resourceId, docId);
            if (docWrapper == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("No document with id:" + docId + " is found in the source " + resourceId).build();
            }

            ISOXMLGenerator2 generator = new ISOXMLGenerator2();

            Element docEl = generator.generate(docWrapper);
            XMLOutputter xmlOutputter = new XMLOutputter(Format.getPrettyFormat());

            Document doc = new Document();
            doc.setRootElement(docEl);
            StringWriter sw = new StringWriter(16000);
            xmlOutputter.output(doc, sw);

            xmlStr = sw.toString();
        } catch (Exception x) {
            x.printStackTrace();
        } finally {
            if (mongoService != null) {
                mongoService.shutdown();
            }
        }
        return Response.ok(xmlStr).build();
    }

    @Path("/keyword/hierarchies/")
    @GET
    @Produces("application/json")
    @ApiResponses(value = {@ApiResponse(code = 500, message = "An internal error occurred during the keyword hierarchy processing"),
            @ApiResponse(code = 404, message = "No metadata document is found with the given document ID")})
    @ApiOperation(value = "Retrieve keywords and their ontology hierarchies for a ISO metadata document",
            notes = "",
            response = String.class)
    public Response getKeywordHierarchies(
            @ApiParam(value = "The document ID for the metadata document", required = true) @QueryParam("id") String docId) {
        String jsonStr = "";
        MongoService mongoService = null;
        try {
            mongoService = new MongoService();
            System.out.println("docId:" + docId);
            BasicDBObject docWrapper = mongoService.findTheDocument(docId);
            if (docWrapper == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("No document with id:" + docId + " is not found!").build();
            }

            JSONObject result = new JSONObject();
            BasicDBObject data = (BasicDBObject) docWrapper.get("Data");
            BasicDBList enhancedKeywords = (BasicDBList) data.get("enhancedKeywords");
            JSONArray keywordsArr = new JSONArray();
            result.put("keywords", keywordsArr);
            if (enhancedKeywords != null) {
                for(Object o : enhancedKeywords) {
                    JSONObject keywordJson = JSONUtils.toJSON((BasicDBObject) o, true);
                    String term = keywordJson.getString("term");
                    String hierarchy = "Unassigned";
                    if (  keywordJson.has("hierarchyPath") ){
                         hierarchy = keywordJson.getString("hierarchyPath");
                    }
                    JSONObject khJson = new JSONObject();
                    khJson.put("keyword", term);
                    khJson.put("hierarchy", hierarchy);
                    keywordsArr.put(khJson);
                }
            }
            jsonStr = result.toString(2);
        } catch (Exception x) {
            x.printStackTrace();
            return Response.serverError().build();
        } finally {
            if (mongoService != null) {
                mongoService.shutdown();
            }
        }
        return Response.ok(jsonStr).build();
    }


    @Path("/keyword/hierarchiesOld/")
    @GET
    @Produces("application/json")
    @ApiResponses(value = {@ApiResponse(code = 500, message = "An internal error occurred during the keyword hierarchy processing"),
            @ApiResponse(code = 404, message = "No metadata document is found with the given document ID")})
    @ApiOperation(value = "Retrieve keywords and their ontology hierarchies for a ISO metadata document",
            notes = "",
            response = String.class)
    public Response getKeywordHierarchiesOld(
            @ApiParam(value = "The document ID for the metadata document", required = true) @QueryParam("id") String docId) {
        String jsonStr;
        MongoService mongoService = null;
        try {
            mongoService = new MongoService();
            System.out.println("docId:" + docId);
            BasicDBObject docWrapper = mongoService.findTheDocument(docId);
            if (docWrapper == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("No document with id:" + docId + " is not found!").build();
            }
            KeywordHierarchyHandler handler = KeywordHierarchyHandler.getInstance(Constants.SCIGRAPH_URL);
            JSONObject result = new JSONObject();
            BasicDBObject data = (BasicDBObject) docWrapper.get("Data");
            BasicDBList keywords = (BasicDBList) data.get("keywords");
            JSONArray keywordsArr = new JSONArray();
            result.put("keywords", keywordsArr);
            IHierarchyHandler chh = FacetHierarchyHandler.getInstance(Constants.SCIGRAPH_URL);
            if (keywords != null && !keywords.isEmpty()) {
                for (Object o : keywords) {
                    JSONObject keywordJson = JSONUtils.toJSON((BasicDBObject) o, true);
                    //JSONObject kwhJson = prepHierarchyForKeyword(keywordJson, handler, chh);
                    JSONObject kwhJson = prepHierarchyForKeyword2(keywordJson);
                    if (kwhJson.has("hierarchy")) {
                        keywordsArr.put(kwhJson);
                    }
                }
            }
            // also add existing keywords
            BasicDBObject origDocDBO = (BasicDBObject) docWrapper.get("OriginalDoc");
            JSONObject origDocJson = JSONUtils.toJSON(origDocDBO, false);
            XML2JSONConverter converter = new XML2JSONConverter();
            Element docEl = converter.toXML(origDocJson);
            // Utils.saveXML(docEl, "/tmp/xpath_test.xml");

            List<Keyword> existingKeywords = getExistingKeywords(docEl);
            for (Keyword kw : existingKeywords) {
                JSONObject kwJson = kw.toJSON();
                JSONObject kwhJson = prepHierarchyForKeyword2(kwJson);
                if (kwhJson.has("hierarchy")) {
                    keywordsArr.put(kwhJson);
                }
            }
            /*
            List<JSONObject> jsonObjects = prepHierarchies(docEl, handler, chh);
            for (JSONObject js : jsonObjects) {
                keywordsArr.put(js);
            }
            */

            jsonStr = result.toString(2);
        } catch (Exception x) {
            x.printStackTrace();
            return Response.serverError().build();
        } finally {
            if (mongoService != null) {
                mongoService.shutdown();
            }
        }

        return Response.ok(jsonStr).build();
    }

    List<Element> findElementsWithName(Element rootEl, String selElemName) {
        List<Element> selectList = new LinkedList<Element>();
        collectElements(rootEl, selectList, selElemName);
        return selectList;
    }

    void collectElements(Element parentEl, List<Element> selectList, String selElemName) {
        if (parentEl.getName().equals(selElemName)) {
            selectList.add(parentEl);
        } else {
            List<Element> children = parentEl.getChildren();
            for (Element child : children) {
                collectElements(child, selectList, selElemName);
            }
        }
    }

    List<Keyword> getExistingKeywords(Element docEl) throws Exception {
        Set<String> existingKeywords = CinergiXMLUtils.getExistingKeywords(docEl);
        if (existingKeywords.isEmpty()) {
            return Collections.emptyList();
        }
        StringBuilder sb = new StringBuilder(existingKeywords.size() * 30);
        for (Iterator<String> it = existingKeywords.iterator(); it.hasNext(); ) {
            sb.append(it.next());
            if (it.hasNext()) {
                sb.append(" , ");
            }
        }
        Set<String> existingKeywordsLC = new HashSet<String>();
        for (String ekw : existingKeywords) {
            existingKeywordsLC.add(ekw.toLowerCase());
        }
        Map<String, Keyword> keywordMap = new HashMap<String, Keyword>();

        ScigraphUtils.annotateEntities(null, sb.toString(), keywordMap, false);
        List<Keyword> filteredKeywordList = new ArrayList<Keyword>(keywordMap.size());
        for (Keyword kw : keywordMap.values()) {
            if (existingKeywordsLC.contains(kw.getTerm().toLowerCase())) {
                filteredKeywordList.add(kw);
            }
        }
        return filteredKeywordList;
    }


    List<JSONObject> prepHierarchies(Element docEl, KeywordHierarchyHandler handler,
                                     IHierarchyHandler chh) {
        List<JSONObject> jsonList = new ArrayList<JSONObject>(5);
        Map<String, Namespace> nsMap = new HashMap<String, Namespace>();
        List<Namespace> namespacesInScope = docEl.getNamespacesInScope();
        for (Namespace ns : namespacesInScope) {
            nsMap.put(ns.getPrefix(), ns);
        }
        XPathFactory xpathFactory = XPathFactory.instance();
        XPathExpression<Element> expr = null;

        Namespace ns = nsMap.get("gmd");
        boolean geoscienceAustralia = false;
        if (ns != null) {
            expr = xpathFactory.compile("//gmd:MD_Keywords",
                    Filters.element(), null, ns);
        } else if (nsMap.containsKey("gmi")) {
            ns = nsMap.get("gmi");
            expr = xpathFactory.compile("//gmi:MD_Keywords",
                    Filters.element(), null, ns);
        } else {
            geoscienceAustralia = true;
            // get default namespace
            // nsMap.put("gmi", Namespace.getNamespace("gmi", "http://www.isotc211.org/2005/gmi"));
            Namespace gmi = Namespace.getNamespace("gmi", "http://www.isotc211.org/2005/gmi");
            ns = nsMap.get("");
            expr = xpathFactory.compile("//gmi:MD_Keywords",
                    Filters.element(), null, gmi);
        }
        Assertion.assertNotNull(ns);
        Document doc = new Document(docEl);
        List<Element> elements = expr.evaluate(doc);
        if (geoscienceAustralia) {
            elements = findElementsWithName(docEl, "MD_Keywords");
        }
        if (elements != null && !elements.isEmpty()) {

            Namespace gcoNS = nsMap.get("gco");
            for (Element el : elements) {
                List<Element> children = el.getChildren("keyword", ns);
                for (Element child : children) {
                    String keywordStr = child.getChildTextTrim("CharacterString", gcoNS);
                    Map<String, Keyword> kwMap = new HashMap<String, Keyword>(7);
                    try {
                        handler.annotateEntities(null, keywordStr, kwMap);
                        if (!kwMap.isEmpty()) {
                            Keyword theKW = null;
                            int maxPathLen = -10;
                            for (Keyword kw : kwMap.values()) {
                                if (kw.hasCategory()) {
                                    for (String category : kw.getCategories()) {
                                        String categoryPath = chh.getCinergiCategory(category.toLowerCase());
                                        int pathLength2Root = chh.getPathLength2Root(categoryPath);
                                        if (pathLength2Root > maxPathLen) {
                                            maxPathLen = pathLength2Root;
                                            theKW = kw;
                                        }
                                    }
                                }
                            }
                            if (theKW != null) {
                                EntityInfo ei = theKW.getTheCategoryEntityInfo(chh);
                                if (ei != null && !Utils.isEmpty(ei.getId())) {
                                    String ontologyID = getOntologyID(ei.getId());
                                    String keywordHierarchy = handler.getKeywordHierarchy(keywordStr, ontologyID, fhh, null);
                                    if (keywordHierarchy != null && keywordHierarchy.length() > 0) {

                                        JSONObject json = new JSONObject();
                                        json.put("keyword", keywordStr);
                                        json.put("hierarchy", keywordHierarchy);
                                        jsonList.add(json);
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return jsonList;
    }


    JSONObject prepHierarchyForKeyword2(JSONObject keywordJson) {
        JSONObject json = new JSONObject();
        Keyword keyword = Keyword.fromJSON(keywordJson);
        for (EntityInfo ei : keyword.getEntityInfos()) {
            if (!Utils.isEmpty(ei.getId())) {

                try {
                    List<String> facetHierarchies = ScigraphUtils.getKeywordFacetHierarchies4WS(ei.getId());
                    if (!facetHierarchies.isEmpty()) {
                        json.put("keyword", keyword.getTerm());
                        json.put("hierarchy", facetHierarchies.get(0));
                        return json;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return json;
    }

    JSONObject prepHierarchyForKeyword(JSONObject keywordJson, KeywordHierarchyHandler handler,
                                       IHierarchyHandler chh) {
        JSONObject json = new JSONObject();
        Keyword keyword = Keyword.fromJSON(keywordJson);
        EntityInfo ei = keyword.getTheCategoryEntityInfo(chh);
        if (ei != null && !Utils.isEmpty(ei.getId())) {
            String ontologyID = getOntologyID(ei.getId());
            try {
                String[] toks = ei.getCategory().split("\\s+>\\s+");
                String lastTerm = toks[toks.length - 1];
                String cinergiCategory = chh.getCinergiCategory(lastTerm.toLowerCase());

                String keywordHierarchy = handler.getKeywordHierarchy(keyword.getTerm(), ontologyID,
                        (FacetHierarchyHandler) chh, cinergiCategory);
                if (keywordHierarchy != null && keywordHierarchy.length() > 0) {
                    json.put("keyword", keyword.getTerm());
                    json.put("hierarchy", keywordHierarchy);
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            Map<String, Keyword> kwMap = new HashMap<String, Keyword>(7);
            try {
                handler.annotateEntities(null, keyword.getTerm(), kwMap);
                if (!kwMap.isEmpty()) {
                    Keyword kw = kwMap.values().iterator().next();
                    ei = kw.getTheCategoryEntityInfo(chh);
                    if (ei != null && !Utils.isEmpty(ei.getId())) {
                        String ontologyID = getOntologyID(ei.getId());
                        String keywordHierarchy = handler.getKeywordHierarchy(keyword.getTerm(), ontologyID,
                                (FacetHierarchyHandler) chh, null);
                        if (keywordHierarchy != null && keywordHierarchy.length() > 0) {
                            json.put("keyword", keyword.getTerm());
                            json.put("hierarchy", keywordHierarchy);
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return json;
    }

    public static String getOntologyID(String id) {
        int idx = id.lastIndexOf('/');
        if (idx != -1) {
            return id.substring(idx + 1);
        }
        return id;
    }


    //    /**
//     * @param nifId        NIF ID for the source
//     * @param docId        Document ID
//     * @param batchId      batchId for the ingestion set in the format of <code>YYYYMMDD</code>
//     * @param in           The xml file to ingest
//     * @param ingestStatus One of <code>start</code> or <code>end</code>. Used to indicate the start and end of group of documents ingested in a batch.
//     * @return
//     */
/*
    @Path("docs/{nifId}/{docId}")
    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Response saveDocument(@PathParam("nifId") String nifId,
                                 @PathParam("docId") String docId,
                                 @FormDataParam("batchId") String batchId,
                                 @FormDataParam("file") InputStream in,
                                 @FormDataParam("ingestStatus") String ingestStatus,
                                 @FormDataParam("apiKey") String apiKey

    ) {
        if (batchId == null || ingestStatus == null || apiKey == null) {
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).build());
        }
        System.out.println("batchId:" + batchId);
        System.out.println("nifId:" + nifId);
        if (apiKey == null || !apiKey.equals(theApiKey)) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }
        String dataSource = "";
        MongoService mongoService = null;
        try {
            String xmlStr = toXMLString(in);
            mongoService = MongoService.getInstance();
            if (mongoService.hasDocument(nifId, docId)) {
                return Response.status(Response.Status.FOUND).
                        entity("A document with id " + docId +
                                " for source " + nifId + " already exists!").build();
            }

            final Source source = mongoService.findSource(nifId);
            if (source == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("No source with the nifId:" + nifId).build();
            }
            final List<BatchInfo> batchInfos = source.getBatchInfos();


            if (batchInfos.isEmpty() && ingestStatus.equals("in_process")) {
                return Response.serverError().build();
            } else {
                //TODO
            }
            XML2JSONConverter converter = new XML2JSONConverter();
            SAXBuilder builder = new SAXBuilder();
            Document doc = builder.build(new StringReader(xmlStr));
            Element rootEl = doc.getRootElement();

            final JSONObject json = converter.toJSON(rootEl);

            mongoService.saveDocument(json, batchId, source.getResourceID(), source.getName(), true,
                    source, docId);


            if (ingestStatus.equals("start")) {
                mongoService.beginBatch(nifId, dataSource, batchId, true);
            } else if (ingestStatus.equals("in_process")) {
                mongoService.updateBatch(nifId, dataSource, batchId, true);
            } else if (ingestStatus.equals("end")) {
                mongoService.updateBatch(nifId, dataSource, batchId, true);
                mongoService.endBatch(nifId, "", batchId);
            }
            String jsonStr = json.toString(2);
            // System.out.println("json:\n" + jsonStr);
            // System.out.println("=============================");


            return Response.ok(jsonStr).build();
        } catch (Exception e) {
            e.printStackTrace();
            if (mongoService != null) {
                if (ingestStatus.equals("start")) {
                    mongoService.beginBatch(nifId, dataSource, batchId, false);
                } else {
                    mongoService.updateBatch(nifId, dataSource, batchId, false);
                }
            }
            return Response.serverError().build();
        }

        // return Response.ok("{}").build();

    }
*/
    private String toXMLString(InputStream in) throws IOException {
        StringBuilder sb = new StringBuilder(16000);
        BufferedReader bin;
        try {
            bin = new BufferedReader(new InputStreamReader(in));
            String line;
            while ((line = bin.readLine()) != null) {
                sb.append(line).append('\n');
            }
            return sb.toString();
        } finally {
            Utils.close(in);
        }
    }

}
