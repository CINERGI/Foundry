package org.neuinfo.foundry.common.util;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;

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
    private static OntologyHandler instance = null;


    public synchronized static OntologyHandler getInstance() throws Exception {
        if (instance == null) {
            instance = new OntologyHandler();
        }
        return instance;
    }

    private OntologyHandler() throws Exception{
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
        List<String> facets = OWLFunctions.getFacets(this.manager, this.df);
        Collections.sort(facets);
        return facets;
    }
}
