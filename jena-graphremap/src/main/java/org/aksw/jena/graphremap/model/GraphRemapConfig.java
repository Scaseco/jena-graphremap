package org.aksw.jena.graphremap.model;

import org.aksw.jena.graphremap.assembler.GraphRemapVocab;
import org.apache.jena.enhanced.EnhGraph;
import org.apache.jena.graph.Node;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.impl.ResourceImpl;
import org.apache.jena.sparql.core.assembler.DatasetAssemblerVocab;
import org.apache.jena.util.iterator.ExtendedIterator;

public class GraphRemapConfig
    extends ResourceImpl
{
    public GraphRemapConfig(Node n, EnhGraph m) {
        super(n, m);
    }

    public Resource getBaseDataset() {
        Resource baseDatasetRes = getPropertyResourceValue(DatasetAssemblerVocab.pDataset);
        return baseDatasetRes;
    }

    public ExtendedIterator<GraphAliasEntry> listAliases() {
        return listProperties(GraphRemapVocab.alias)
            .mapWith(Statement::getObject)
            .filterKeep(RDFNode::isResource)
            .mapWith(r -> new GraphAliasEntry(r.asNode(), (EnhGraph)r.getModel()));
    }
}
