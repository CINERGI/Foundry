
package org.cinergi.sdsc.metadata.enhancer.spatial;

import edu.stanford.nlp.ie.AbstractSequenceClassifier;
import edu.stanford.nlp.ie.crf.CRFClassifier;
import edu.stanford.nlp.ling.CoreLabel;
import org.apache.log4j.Logger;

import java.util.HashSet;
import java.util.Set;


public class StanfordNEDLocationFinder {
    AbstractSequenceClassifier<CoreLabel> classifier;

    private static Logger log = Logger.getLogger("StanfordNEDLocationFinder");
    private static String serializedClassifier = "english.all.3class.distsim.crf.ser.gz";

    public void startup() throws Exception {
        classifier = CRFClassifier.getClassifier(serializedClassifier);
    }

    public Set<String> getLocationsFromText(String text) throws Exception {
        Set<String> locations = new HashSet<String>();

        String[] example = new String[1];
        example[0] = text;


        String xml = classifier.classifyWithInlineXML(text);
        String start = "<LOCATION>";
        String end = "</LOCATION>";

        int p = xml.indexOf(start);
        int q = xml.indexOf(end);

        while (p != -1 && q != -1) {

            String loc = xml.substring(p + 10, q);
            locations.add(loc);

            xml = xml.substring(q + 11);
            p = xml.indexOf(start);
            q = xml.indexOf(end);
        }
        return locations;
    }


    private static String parseLocation(String line) {
        return line.split(",")[0];
    }

}