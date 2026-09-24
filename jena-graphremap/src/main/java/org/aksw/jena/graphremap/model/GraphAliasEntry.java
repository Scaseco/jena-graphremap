package org.aksw.jena.graphremap.model;

import java.util.Optional;

import org.aksw.jena.graphremap.assembler.GraphRemapVocab;
import org.apache.jena.enhanced.EnhGraph;
import org.apache.jena.graph.Node;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.impl.ResourceImpl;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.util.ExprUtils;

public class GraphAliasEntry
    extends ResourceImpl
{
    public GraphAliasEntry(Node n, EnhGraph m) {
        super(n, m);
    }

    public String getGraphIri() {
        return Optional.ofNullable(getPropertyResourceValue(GraphRemapVocab.graph))
            .map(Resource::getURI).orElse(null);
    }

    public Expr getExpr() {
        String str = Optional.ofNullable(getProperty(GraphRemapVocab.expr))
            .map(Statement::getObject)
            .map(RDFNode::asLiteral)
            .map(Literal::getLexicalForm).orElse(null);
        Expr expr = ExprUtils.parse(str);
        return expr;
    }
}
