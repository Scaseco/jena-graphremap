package org.aksw.jena.graphremap;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.io.FileUtils;
import org.apache.jena.atlas.iterator.Iter;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.ReadWrite;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFormatter;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.system.AutoTxn;
import org.apache.jena.system.Txn;
import org.apache.jena.tdb2.assembler.VocabTDB2;
import org.apache.jena.update.UpdateExecution;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class TestDatasetAssemblerGraphRemap {

    // Initialized/destroyed by beforeClass/afterClass methods
    static Path tdb2TmpFolder;
    static Dataset dataset;

    @Test
    public void test01() {
        runTest("SELECT * FROM <urn:example:g1> FROM <urn:example:g2> { ?s ?p ?o }", 4);
    }

    @Test
    public void test02() {
        runTest("CONSTRUCT { GRAPH ?g { ?s ?p ?o } } WHERE { { SELECT * { GRAPH ?g { ?s ?p ?o } } LIMIT 10 } }", 4);
    }

    @Test
    public void testUpdate01() {
        long beforeSize, afterSize;
        try (AutoTxn txn = Txn.autoTxn(dataset, ReadWrite.WRITE)) {
            beforeSize = dataset.getDefaultModel().size();
            UpdateExecution.dataset(dataset)
                .update("WITH <urn:example:g1> DELETE { ?s ?p ?o } INSERT { ?s ?p ?o } WHERE { ?s ?p ?o . OPTIONAL { GRAPH <urn:foo> { ?s <urn:bar> ?x } } }")
                .execute();
            afterSize = dataset.getDefaultModel().size();
        }
        assertEquals(beforeSize, afterSize);
    }

    public void runTest(String queryStr, long expectedResult) {
        Query query = QueryFactory.create(queryStr);
        long actualResult = exec(query);
        Assertions.assertEquals(expectedResult, actualResult);
    }

    public long exec(Query query) {
        long result = Txn.calculateRead(dataset, () -> {
            try (QueryExecution qe = QueryExecution.create(query, dataset)) {
                long r;
                if (query.isSelectType()) {
                    ResultSet rs = qe.execSelect();
                    r = ResultSetFormatter.consume(rs);
                } else if (query.isConstructType()) {
                    r = Iter.count(qe.execConstructQuads());
                } else {
                    throw new RuntimeException("Unsupported query type");
                }
                return r;
            }
        });
        return result;
    }

    @BeforeAll
    public static void beforeClass() throws Exception {
        tdb2TmpFolder = Files.createTempDirectory("jena_from-enhancer_tdb2").toAbsolutePath();
        String assemblerStr = """
            PREFIX ja: <http://jena.hpl.hp.com/2005/11/Assembler#>
            PREFIX jgr: <https://w3id.org/aksw/jena/graphremap#>
            PREFIX tdb2: <http://jena.apache.org/2016/tdb#>
            <urn:example:root> a jgr:DatasetGraphRemap ; ja:dataset <urn:example:base> .
            <urn:example:root> jgr:alias [ jgr:graph <urn:example:all> ; jgr:expr 'true' ] .
            <urn:example:base> a tdb2:DatasetTDB2 .
            # "<urn:example:base> a ja:MemoryDataset .
            """;

        String dataStr = """
            PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
            <urn:example:g1> { <urn:example:s> a <urn:example:class> ; rdfs:label "s" }
            <urn:example:g2> { <urn:example:s> a <urn:example:class> ; rdfs:label "s" }
            """;

        Model confModel = ModelFactory.createDefaultModel();
        RDFDataMgr.read(confModel, new StringReader(assemblerStr), null, Lang.TURTLE);
        Resource tdb2Conf = confModel.getResource("urn:example:base");
        tdb2Conf.addProperty(VocabTDB2.pLocation, tdb2TmpFolder.toString());

        dataset = DatasetFactory.assemble(confModel.getResource("urn:example:root"));
        Txn.executeWrite(dataset, () -> RDFDataMgr.read(dataset, new StringReader(dataStr), null, Lang.TRIG));
    }

    @AfterAll
    public static void afterClass() throws IOException {
        try {
            if (dataset != null) {
                dataset.close();
            }
        } finally {
            if (tdb2TmpFolder != null) {
                FileUtils.deleteDirectory(tdb2TmpFolder.toFile());
            }
        }
    }
}
