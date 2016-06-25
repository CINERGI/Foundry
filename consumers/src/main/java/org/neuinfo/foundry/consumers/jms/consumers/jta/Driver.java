package org.neuinfo.foundry.consumers.jms.consumers.jta;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.neuinfo.foundry.common.util.Assertion;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.SystemOutDocumentTarget;
import org.semanticweb.owlapi.model.*;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;

public class Driver {

    // argument 0 is input json, argument 1 is output json, argument 2 is stoplist
    public static void main(String[] argv) throws Exception {

		/*
         * load ontology
		 * read in json from the input file document by document
		 * 
		 * for each document, do POS search on title + text
		 * find noun phrases and adjectives 
		 *  
		 * output annotated keywords into output file
		 * 
		 */

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
            System.err.println("failed to gather extensions");
            System.exit(1);
        }

        String HOME_DIR = System.getProperty("user.home");
        String ROOT_DIR = HOME_DIR + "/dev/java/Foundry/data";
        String jsonInput = ROOT_DIR + "/czo_contents.json";
        String jsonOutput = ROOT_DIR + "/czo_contents_out.json";
        String stopListFile = ROOT_DIR + "/stoplist.txt";
        String nullIRIsFile = ROOT_DIR + "/nulliris.txt";
        if (argv.length == 4) {
            jsonInput = argv[0];
            jsonOutput = argv[1];
            stopListFile = argv[2];
            nullIRIsFile = argv[3];
        }

        List<String> stoplist = Files.readAllLines(Paths.get(stopListFile), StandardCharsets.UTF_8);
        List<String> nullIRIs = Files.readAllLines(Paths.get(nullIRIsFile), StandardCharsets.UTF_8);
        LinkedHashMap<String, IRI> exceptionMap = null; // Create this using label duplicates spreadsheet
       // doAnalyze(manager, df, cinergi_ont, extensions, jsonInput, jsonOutput, stoplist, nullIRIs, exceptionMap);

        String title = "Temporal and spatial trends of chloride and sodium in groundwater in New Hampshire, 1960â€“2011";
        String text = "Data on concentrations of chloride and sodium in groundwater in New Hampshire were assembled from various State and Federal agencies and organized into a database. This report provides documentation of many assumptions and limitations of disparate data that were collected to meet wide-ranging objectives and investigates temporal and spatial trends of the data. Data summaries presented in this report and analyses performed for this study needed to take into account the 27 percent of chloride and 5 percent of sodium data that were censored (less than a reporting limit) at multiple reporting limits that systematically decreased over time. Throughout New Hampshire, median concentrations of chloride were significantly greater during 2000-2011 than in every decade since the 1970s, and median concentrations of sodium were significantly greater during 2000-2011 than during the 1990s. Results of summary statistics showed that the 50th, 75th, and 90th percentiles of the median concentrations of chloride and sodium by source (well) from Rockingham and Strafford counties were the highest in the State; and the 75th and 90th percentiles from Carroll, Coos, and Grafton counties were the lowest. Large increases in median concentrations of chloride and sodium for individual wells after 1995 compared with concentrations for years before were found in parts of Belknap and Rockingham counties and in small clusters within Carroll, Hillsborough, and Merrimack counties";

        title = "Meteorological Data from the Jemez CZO MidElevation PLZ-ZOB Site.";
        text = "Standard meteorological data are being collected at the PLZ-ZOB site using a suite of Campbell Scientific Instruments. The data is downloaded semi-regularly on-site. The variable being continuously monitored are air pressure, air temperature, relative humidity, wind speed and direction, and precipiration and recorded to the datalogger every 30 minutes.";


        text = "The Real-Time Hydrology Network provides integrated observation from bedrock to boundarylayer of the Shale Hills Susquehanna Critical Zone Observatory watershed.  \"Off-the-shelf? Internet Protocol(IP) compliant climate stations, eddy covariance flux stations, " +
                "stream gauging, soil moisture profilers, andpressure transducers for monitoring groundwater levels comprise a series of real-time " +
                "Internet-accessiblesensor arrays that support research and educational efforts investigating interactions between the atmosphere,surface and subsurface terrestrial processes, and the riverine hydrologic system. CITATION:";
        title = "Shale Hills Susquehanna Critical Zone Observatory Real-Time Hydrology Network.";

        text = "The overall goal of this effort is to monitor the surface climate of the upper Luquillo " +
                "Mountains.  This station provides basic hourly and daily climate data that is comparable to the Bisley and El Verde stations at lower elevations.  It also provides the long-term reference data that supplements shorter-term, high frequency measurements.; PUBLICATIONS: Weaver, P.L., E. Medina, D. Pool, K. Dugget, J. Gonzales-Liboy, E. Cuvas, 1986. Ecological Observations in the Cloud Forests of the Luquillo Mountains in Puerto Rico. Biotropica 18(1):69-82";
        title = "East Peak Climate ; IITF East Peak Daily Climate 2000 to 2011";

        text += " . Hydrology .";
        testIt(text, title, manager, df, cinergi_ont, extensions, stoplist, nullIRIs);
         System.exit(1);

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        BufferedReader bufferedReader = new BufferedReader(new FileReader(jsonInput));

        Document[] docs = gson.fromJson(bufferedReader, Document[].class);
        // for testing
        docs = Arrays.copyOf(docs, 10);
        /*
        // filter
        Document theDoc = null;
        for(Document d : docs) {
           if (d.getId().equals("{90a6ea7f-b6e0-3cd1-b62d-dd52493fb928}")) {
               theDoc = d;
               break;
           }
        }
        Assertion.assertNotNull(theDoc);
        docs = new Document[] {theDoc};
        */
        testIt(docs, manager, df, cinergi_ont, extensions, stoplist, nullIRIs);
    }

    public static void testIt(Document[] docs, OWLOntologyManager manager, OWLDataFactory df, OWLOntology cinergi_ont,
                              OWLOntology extensions,List<String> stoplist, List<String> nullIRIs ) throws Exception {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        KeywordAnalyzer analyzer = new KeywordAnalyzer(manager, df, cinergi_ont, extensions, gson,
                stoplist, null, nullIRIs, null);
        for(Document doc : docs) {
            System.out.println(doc.getId());
            List<Keyword> keywords = analyzer.findKeywords(doc);
            System.out.println("Keywords\n----------------");
            for (Keyword k : keywords) {
                System.out.println(k);
            }
        }
    }
    public static void testIt(String text, String title, OWLOntologyManager manager, OWLDataFactory df, OWLOntology cinergi_ont,
                              OWLOntology extensions,List<String> stoplist, List<String> nullIRIs ) throws Exception {
        Document doc = new Document();
        doc.setId("1");
        doc.setTitle(title);
        doc.setText(text);
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        KeywordAnalyzer analyzer = new KeywordAnalyzer(manager, df, cinergi_ont, extensions, gson,
                stoplist, null, nullIRIs, null);
        List<Keyword> keywords = analyzer.findKeywords(doc);
        System.out.println("Keywords\n----------------");
        for(Keyword k : keywords) {
            System.out.println(k);
        }
    }

    public static void doAnalyze(OWLOntologyManager manager, OWLDataFactory df, OWLOntology cinergi_ont,
                                 OWLOntology extensions, String jsonInput, String jsonOutput,
                                 List<String> stoplist, List<String> nullIRIs,
                                 LinkedHashMap<String, IRI> exceptionMap) throws IOException {
        long start;// load documents
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        BufferedReader bufferedReader = new BufferedReader(new FileReader(jsonInput));

        Document[] docs = gson.fromJson(bufferedReader, Document[].class);
        // for testing
        docs = Arrays.copyOf(docs, 10);
        start = System.currentTimeMillis();
        KeywordAnalyzer analyzer = new KeywordAnalyzer(manager, df, cinergi_ont, extensions, gson,
                stoplist, exceptionMap, nullIRIs, null);


        analyzer.processDocuments(docs);

        FileWriter fw = new FileWriter(jsonOutput);
        fw.write(gson.toJson(analyzer.getOutput()));

        fw.close();
        long diff = System.currentTimeMillis() - start;
        System.out.println("Elapsed time (msecs): " + diff);
    }
}
