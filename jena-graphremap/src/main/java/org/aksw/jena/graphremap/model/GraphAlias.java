package org.aksw.jena.graphremap.model;

import java.util.Optional;

import org.aksw.jena.graphremap.assembler.FromAsFilterVocab;
import org.apache.jena.enhanced.EnhGraph;
import org.apache.jena.graph.Node;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.impl.ResourceImpl;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.util.ExprUtils;

public class GraphAlias
    extends ResourceImpl
{
    public GraphAlias(Node n, EnhGraph m) {
        super(n, m);
    }

    public String getGraphIri() {
        return Optional.ofNullable(getPropertyResourceValue(FromAsFilterVocab.graph))
            .map(Resource::getURI).orElse(null);
    }

    public Expr getExpr() {
        String str = Optional.ofNullable(getPropertyResourceValue(FromAsFilterVocab.graph))
            .map(Resource::getURI).orElse(null);
        Expr expr = ExprUtils.parse(str);
        return expr;
    }
}
