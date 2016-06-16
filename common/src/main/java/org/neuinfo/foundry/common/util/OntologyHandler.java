package org.neuinfo.foundry.common.util;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by bozyurt on 6/15/16.
 */
public class OntologyHandler {
    final private OWLOntologyManager manager;
    final private OWLDataFactory df;
    final private OWLOntology cinergi_ont;
    private OWLOntology extensions;
    private List<String> facetPaths;
    private static OntologyHandler instance = null;


    public synchronized static OntologyHandler getInstance() throws Exception {
        if (instance == null) {
            instance = new OntologyHandler();
        }
        return instance;
    }

    private OntologyHandler() throws Exception {
        long start = System.currentTimeMillis();
        manager = OWLManager.createOWLOntologyManager();
        df = manager.getOWLDataFactory();
        manager.setSilentMissingImportsHandling(true);
        System.out.println("loading ontology");
        this.cinergi_ont = manager.loadOntologyFromOntologyDocument(
                IRI.create("http://hydro10.sdsc.edu/cinergi_ontology/cinergi.owl"));
        System.out.println("ontology loaded");
        System.out.println("Time elapsed (msecs): " + (System.currentTimeMillis() - start));

        for (OWLOntology o : manager.getOntologies()) {
            if (o.getOntologyID().getOntologyIRI().toString().equals(
                    "http://hydro10.sdsc.edu/cinergi_ontology/cinergiExtensions.owl")) {
                extensions = o;
            }
        }
        if (extensions == null) {
            throw new Exception("failed to gather extensions");
        }
        start = System.currentTimeMillis();
        List<String> facets = OWLFunctions.getFacets(this.manager, this.df);
        this.facetPaths = new ArrayList<String>(facets.size());
        for (String f : facets) {
            String[] tokens = f.split("\\s*,\\s*");
            if (tokens.length == 2) {
                StringBuilder sb = new StringBuilder();
                sb.append(tokens[1]).append(" > ").append(tokens[0]);
                facetPaths.add(sb.toString());
            }
        }
        Collections.sort(facetPaths);
        System.out.println("getFacets:: Time elapsed (msecs): " + (System.currentTimeMillis() - start));
    }


    public OWLOntologyManager getManager() {
        return manager;
    }

    public OWLDataFactory getDf() {
        return df;
    }

    public OWLOntology getCinergi_ont() {
        return cinergi_ont;
    }

    public OWLOntology getExtensions() {
        return extensions;
    }

    public List<String> getAllFacets() {
        return this.facetPaths;
    }


    public static void main(String[] args) throws Exception {

        OntologyHandler handler = OntologyHandler.getInstance();


        List<String> allFacets = handler.getAllFacets();
        for (String f : allFacets) {
            System.out.println(f);
        }


    }
}
