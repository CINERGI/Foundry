package org.neuinfo.foundry.common.util;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
import org.jdom2.Element;
import org.neuinfo.foundry.common.model.CinergiFormRec;
import org.neuinfo.foundry.common.model.ScicrunchResourceRec;

import java.io.File;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by bozyurt on 4/15/15.
 */
public class TemplateISOXMLGenerator {
    VelocityEngine ve;

    public TemplateISOXMLGenerator() {
        ve = new VelocityEngine();
        ve.setProperty(RuntimeConstants.RESOURCE_LOADER, "classpath");
        ve.setProperty("classpath.resource.loader.class", ClasspathResourceLoader.class.getName());
        ve.init();
    }

    public Element createISOXMLDoc(CinergiFormRec cfRec, String primaryKey) throws Exception {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        VelocityContext ctx = new VelocityContext();
        ctx.put("guid", primaryKey);

        ctx.put("resourceType", StringEscapeUtils.escapeXml(cfRec.getResourceType()));
        ctx.put("resourceTitle", StringEscapeUtils.escapeXml(cfRec.getResourceTitle()));
        ctx.put("abstract", StringEscapeUtils.escapeXml(cfRec.getAbstractText()));
        ctx.put("resourceURL",  StringEscapeUtils.escapeXml(cfRec.getResourceURL()));
        ctx.put("individualName", StringEscapeUtils.escapeXml(cfRec.getIndividualName()));
        ctx.put("contactEmail",  StringEscapeUtils.escapeXml(cfRec.getContactEmail()));
        ctx.put("definingCitation", StringEscapeUtils.escapeXml(cfRec.getDefiningCitation()));
        ctx.put("resourceContributor", StringEscapeUtils.escapeXml(cfRec.getResourceContributor()));
        ctx.put("alternateTitle", StringEscapeUtils.escapeXml(cfRec.getAlternateTitle()));
        ctx.put("geoscienceSubdomains", cfRec.getGeoscienceSubdomains());
        ctx.put("equipments", cfRec.getEquipments());
        ctx.put("methods", cfRec.getMethods());
        ctx.put("earthProcesses", cfRec.getEarthProcesses());
        ctx.put("describedFeatures", cfRec.getDescribedFeatures());
        ctx.put("otherTags", cfRec.getOtherTags());
        ctx.put("placeNames", cfRec.getPlaceNames());
        ctx.put("temporalExtent", cfRec.getTemporalExtent());
        ctx.put("spatialExtents", cfRec.getSpatialExtents());
        ctx.put("hasExtent", cfRec.getTemporalExtent() != null
                || !cfRec.getSpatialExtents().isEmpty() || cfRec.getGeologicAge() != null);
        ctx.put("editDate", sdf.format(new Date()));
        ctx.put("geologicAge", cfRec.getGeologicAge());
        ctx.put("fileFormat", StringEscapeUtils.escapeXml(cfRec.getFileFormat()));
        ctx.put("lineage", StringEscapeUtils.escapeXml(cfRec.getLineage()));
        Template template = null;
        template = ve.getTemplate("iso_template.vm");

        StringWriter sw = new StringWriter(16000);
        template.merge(ctx, sw);

        System.out.println(sw.toString());
        File tempFile = File.createTempFile("form_", ".xml");
        Utils.saveText(sw.toString(), tempFile.getAbsolutePath());
        System.out.println("saved " + tempFile);

        Element rootEl = Utils.readXML(sw.toString());

        return rootEl;
    }

    public Element createISOXMLDoc(ScicrunchResourceRec resourceRec) throws Exception {
        Map<String, CategoryKeywords> cwMap = new LinkedHashMap<String, CategoryKeywords>();
        for (ScicrunchResourceRec.UserKeyword uk : resourceRec.getKeywords()) {
            CategoryKeywords cw = cwMap.get(uk.getCategory());
            if (cw == null) {
                cw = new CategoryKeywords(uk.getCategory());
                cwMap.put(uk.getCategory(), cw);
            }
            cw.addTerm(uk.getTerm());
        }

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        VelocityContext ctx = new VelocityContext();
        // Assumption: One document per Scicrunch entered resource
        String guid = Utils.getMD5ChecksumOfString(resourceRec.getDataSetName());

        ctx.put("dataSetTitle", resourceRec.getDataSetName());
        ctx.put("description", resourceRec.getDescription());
        ctx.put("email", resourceRec.getEmail());
        ctx.put("orgLongName", "TBD");
        ctx.put("contactEmail", resourceRec.getEmail());
        ctx.put("editDate", sdf.format(new Date()));
        ctx.put("guid", guid);

        ctx.put("individualName", "");
        ctx.put("czoOrgName", "");
        ctx.put("czoRole", "");
        ctx.put("phone", "");
        ctx.put("deliveryPoint", "");
        ctx.put("city", "");
        ctx.put("state", "");
        ctx.put("zip", "");
        ctx.put("email", "");


        if (!cwMap.isEmpty()) {
            List<CategoryKeywords> cwList = new ArrayList<CategoryKeywords>(cwMap.values());
            ctx.put("categoryKeywords", cwList);
        }


        Template template = null;
        template = ve.getTemplate("metadata19115_template.vm");

        StringWriter sw = new StringWriter(16000);
        template.merge(ctx, sw);

        System.out.println(sw.toString());

        Element rootEl = Utils.readXML(sw.toString());

        return rootEl;
    }

    public static class CategoryKeywords {
        String category;
        List<String> terms = new ArrayList<String>(5);

        public CategoryKeywords(String category) {
            this.category = category;
        }

        public void addTerm(String term) {
            if (!terms.contains(term)) {
                terms.add(term);
            }
        }

        public String getCategory() {
            return category;
        }

        public List<String> getTerms() {
            return terms;
        }
    }


    public static void main(String[] args) throws Exception {
        ScicrunchResourceRec ssrr = new ScicrunchResourceRec();
        ssrr.setEmail("iozyurt@ucsd.edu");
        ssrr.setDataSetName("Some DataSet");
        ssrr.setDescription("This is the placeholder for abstract for the abstract of this CINERGI resource");
        ssrr.addKeyword(new ScicrunchResourceRec.UserKeyword("lake", "Water body"));
        ssrr.addKeyword(new ScicrunchResourceRec.UserKeyword("reservoir", "Water body"));
        ssrr.addKeyword(new ScicrunchResourceRec.UserKeyword("mercury", "Chemical"));
        ssrr.addKeyword(new ScicrunchResourceRec.UserKeyword("Manometer", "Device"));

        TemplateISOXMLGenerator generator = new TemplateISOXMLGenerator();

        generator.createISOXMLDoc(ssrr);

    }
}
