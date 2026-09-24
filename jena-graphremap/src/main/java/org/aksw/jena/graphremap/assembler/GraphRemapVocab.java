package org.aksw.jena.graphremap.assembler;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;

public class GraphRemapVocab {
    public static final String NS = GraphRemapTerms.NS;

    public static String getURI() { return NS; }

    public static final Resource DatasetGraphRemap = ResourceFactory.createResource(GraphRemapTerms.DatasetGraphRemap);
    public static final Property alias = ResourceFactory.createProperty(GraphRemapTerms.alias);
    public static final Property graph = ResourceFactory.createProperty(GraphRemapTerms.graph);
    public static final Property expr = ResourceFactory.createProperty(GraphRemapTerms.expr);
}
