package org.neuinfo.foundry.common.util;

import junit.framework.TestCase;
import org.jdom2.Element;
import org.json.JSONObject;
import org.neuinfo.foundry.common.model.CinergiFormRec;

/**
 * Created by bozyurt on 8/17/15.
 */
public class TemplateISOXMLGeneratorTest extends TestCase {
    public TemplateISOXMLGeneratorTest(String name) {
        super(name);
    }

    public void testGenerate() throws Exception {
        String absText = "Geodatabase of Treaties both in force and not yet in force between the Commonwealth of Australia and the Republic of Indonesia, Independent State of Papua New Guinea, the Republic of France, New Zealand, Government of the Solomon Islands and People's Republic of East Timor";
        CinergiFormRec cfr = new CinergiFormRec();
        cfr.setResourceType("DataSet");
        cfr.setResourceTitle("2. Name/title of the resource");
        cfr.setAbstractText(absText);
        cfr.setResourceURL("4. Resource URL");
        cfr.setIndividualName("5. Your name");
        cfr.setContactEmail("6. Your email");
        cfr.setDefiningCitation("7. Defining citation");
        cfr.setResourceContributor("8. Resource contributor");
        cfr.setAlternateTitle("9. Abbrevations or synonyms");
        cfr.addGeoscienceSubdomain("10. A geoscience subdomain");
        cfr.addEquipment("LANDSAT");
        cfr.addMethod("A method");
        cfr.addPlaceName("New Zealand");
        cfr.addEarthProcess("An earth process");
        cfr.addOtherTag("An other tag");
        cfr.addDescribedFeature("a described feature");
        cfr.addSpatialExtent(new CinergiFormRec.SpatialExtent("39.0", "174.0", "-70.0", "-8.0"));
        cfr.setGeologicAge(new CinergiFormRec.GeologicAge("Cambrian", "Permian"));
        cfr.setFileFormat("CSV");

        TemplateISOXMLGenerator generator = new TemplateISOXMLGenerator();


        Element docEl = generator.createISOXMLDoc(cfr, "1");

        Utils.saveXML(docEl, "/tmp/cinergi_form_iso.xml");
        JSONObject json = cfr.toJSON();

        Utils.saveText(json.toString(2), "/tmp/cinergi_form.json");

    }
}
