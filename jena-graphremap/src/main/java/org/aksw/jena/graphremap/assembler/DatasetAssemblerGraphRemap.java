package org.aksw.jena.graphremap.assembler;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.aksw.jena.graphremap.dataset.DatasetGraphGraphRemap;
import org.aksw.jena.graphremap.model.GraphRemapConfig;
import org.aksw.jena.graphremap.model.GraphAliasEntry;
import org.apache.jena.assembler.Assembler;
import org.apache.jena.assembler.exceptions.AssemblerException;
import org.apache.jena.enhanced.EnhGraph;
import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sparql.core.assembler.DatasetAssembler;
import org.apache.jena.sparql.expr.Expr;

public class DatasetAssemblerGraphRemap
    extends DatasetAssembler
{
    @Override
    public DatasetGraph createDataset(Assembler a, Resource rawRoot) {
        GraphRemapConfig root = new GraphRemapConfig(rawRoot.asNode(), (EnhGraph)rawRoot.getModel());

        Resource baseDatasetRes = root.getBaseDataset();
        Objects.requireNonNull(baseDatasetRes, "No ja:dataset specified on " + root);
        Object obj = a.open(baseDatasetRes);

        if (!(obj instanceof Dataset)) {
            Class<?> cls = obj == null ? null : obj.getClass();
            throw new AssemblerException(root, "Expected ja:dataset to be a Dataset but instead got " + Objects.toString(cls));
        }
        Dataset baseDataset = (Dataset)obj;

        Map<String, Expr> remap = null;
        Set<GraphAliasEntry> aliases = root.listAliases().toSet();
        if (!aliases.isEmpty()) {
            remap = aliases.stream().collect(Collectors.toMap(
                GraphAliasEntry::getGraphIri,
                GraphAliasEntry::getExpr
            ));
        }

        DatasetGraph result = new DatasetGraphGraphRemap(baseDataset.asDatasetGraph(), remap);
        return result;
    }

}
