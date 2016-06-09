package org.neuinfo.foundry.consumers.jms.consumers.jta;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.List;

public class Driver {

    // argument 0 is input json, argument 1 is output json, argument 2 is stoplist
    public static void main(String[] argv) throws OWLOntologyCreationException, IOException {

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


        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        OWLDataFactory df = manager.getOWLDataFactory();
        manager.setSilentMissingImportsHandling(true);

        System.out.println("loading ontology");
        OWLOntology cinergi_ont = manager.loadOntologyFromOntologyDocument(
                IRI.create("http://hydro10.sdsc.edu/cinergi_ontology/cinergi.owl"));
        System.out.println("ontology loaded");

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
        String ROOT_DIR = HOME_DIR + "/work/JSON-Text-Analyzer";
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

        long start = System.currentTimeMillis();
        // load documents
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        BufferedReader bufferedReader = new BufferedReader(new FileReader(jsonInput));

        Document[] docs = gson.fromJson(bufferedReader, Document[].class);
        List<String> stoplist = Files.readAllLines(Paths.get(stopListFile), StandardCharsets.UTF_8);
        List<String> nullIRIs = Files.readAllLines(Paths.get(nullIRIsFile), StandardCharsets.UTF_8);
        LinkedHashMap<String, IRI> exceptionMap = null; // Create this using label duplicates spreadsheet

        KeywordAnalyzer analyzer = new KeywordAnalyzer(manager, df, cinergi_ont, extensions, gson,
                stoplist, exceptionMap, nullIRIs);


        analyzer.processDocuments(docs);

        FileWriter fw = new FileWriter(jsonOutput);
        fw.write(gson.toJson(analyzer.getOutput()));

        fw.close();
        long diff = System.currentTimeMillis() - start;
        System.out.println("Elapsed time (msecs): " + diff);
    }
}
