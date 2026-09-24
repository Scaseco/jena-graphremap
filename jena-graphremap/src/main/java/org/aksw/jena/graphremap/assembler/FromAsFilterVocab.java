package org.aksw.jena.graphremap.assembler;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;

public class FromAsFilterVocab {
    public static final String NS = FromAsFilterTerms.NS;

    public static String getURI() { return NS; }

    public static final Resource DatasetGraphRemap = ResourceFactory.createResource(NS + "DatasetGraphRemap");
    public static final Property mapping = ResourceFactory.createProperty(NS + "mapping");
    public static final Property alias = ResourceFactory.createProperty(NS + "alias");
    public static final Property graph = ResourceFactory.createProperty(NS + "graph");
    //public static final Property baseDataset = ResourceFactory.createProperty(FromAsFilterTerms.baseDataset);

}
