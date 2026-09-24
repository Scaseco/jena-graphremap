# Jena Graph Remap

**Jena Graph Remap** is a plugin for [Apache Jena](https://jena.apache.org/) that rewrites
SPARQL `FROM` / `FROM NAMED` (dataset description) clauses into `GRAPH` blocks with
`FILTER` statements, and can remap specific graph IRIs to arbitrary SPARQL expressions
(e.g. `true`, another IRI, or a `REGEX` test).

The plugin is configured declaratively through
[Jena assemblers](https://jena.apache.org/documentation/assembler/) and works transparently
with any query engine (in-memory, TDB2, ...) — including as a plugin for
[Apache Jena Fuseki](https://jena.apache.org/fuseki/).

It is a standalone factor-out of the former JenaX "From-Enhancer" plugin.

- Repository: https://github.com/Scaseco/jena-graphremap
- License: Apache 2.0
- Maven: `org.aksw.jena.graphremap:jena-graphremap`

## Maven

```xml
<dependency>
    <groupId>org.aksw.jena.graphremap</groupId>
    <artifactId>jena-graphremap</artifactId>
    <version>0.9.0-SNAPSHOT</version>
</dependency>
```

Snapshots are published to the AKSW snapshot repository:

```xml
<repository>
    <id>aksw-snapshots</id>
    <url>https://maven.aksw.org/archiva/repository/snapshots</url>
</repository>
```

## How it works

When a query is executed against a dataset assembled with this plugin, its
`FROM` / `FROM NAMED` clauses are rewritten into the query pattern before the query is
handed to the base engine. For example,

```sparql
SELECT * FROM <urn:example:g1> FROM <urn:example:g2> { ?s ?p ?o }
```

is rewritten to

```sparql
SELECT * {
  GRAPH ?__dg__0 { ?s ?p ?o }
  FILTER (?__dg__0 IN (<urn:example:g1>, <urn:example:g2>))
}
```

Optimizations:

- A single `FROM <iri>` with a constant IRI becomes a plain `GRAPH <iri> { ... }` (no filter).
- If the rewritten filter reduces to `true` (e.g. the graph was remapped to the expression
  `true`), no `FILTER` is emitted and the `GRAPH ?g { ... }` block matches all graphs.

`FROM NAMED <iri>` clauses constrain the graph variable of existing `GRAPH ?g { ... }`
blocks in the query pattern in the same way.

The fresh variables introduced by the rewrite use the prefix `__dg__` (collision-checked
against the pattern's variables) and are internal only: `SELECT *` queries keep returning
the original result variables.

SPARQL updates are not rewritten and work unchanged against the wrapped dataset.

### Graph remapping (aliases)

Specific graph IRIs can be remapped to arbitrary SPARQL expressions via assembler aliases.
The semantics of an expression depend on the number of variables it mentions:

- **0 variables:** treated as a constant matched against the graph name by value.
  The boolean constant `true` matches all graphs, `false` matches none; an IRI constant
  (e.g. `<http://dbpedia.org/>`) remaps the graph to that IRI.
- **1 variable:** the variable (e.g. `?g` in `REGEX(str(?g), '[0-9]+$')`) is substituted
  with the graph name and the resulting boolean expression is evaluated.
- **> 1 variable:** rejected with an `IllegalStateException`.

Constant and single-variable conditions are combined into a disjunction (constants via
`IN` / `=`).

## Assembler configuration

Vocabulary (prefix `jgr:`):

```turtle
PREFIX jgr: <https://w3id.org/aksw/jena/graphremap#>
```

| Term | Role |
|------|------|
| `jgr:DatasetGraphRemap` | Assembler type of the wrapping dataset |
| `ja:dataset` | The base dataset (assembled as usual) |
| `jgr:alias` | Alias entry: `jgr:graph <iri>` plus `jgr:expr "<expression>"` |

`jgr:expr` is a plain string literal; it is parsed as a SPARQL expression when the dataset
is assembled, so syntax errors surface at that point.

Minimal example with an in-memory base dataset:

```turtle
PREFIX ja:  <http://jena.hpl.hp.com/2005/11/Assembler#>
PREFIX jgr: <https://w3id.org/aksw/jena/graphremap#>

<urn:example:root> a jgr:DatasetGraphRemap ;
    ja:dataset <urn:example:base> ;

    # Remap <urn:example:all> to "match all graphs"
    jgr:alias [ jgr:graph <urn:example:all> ; jgr:expr "true" ] ;

    # Remap <urn:example:dbpedia> to the DBpedia IRI
    jgr:alias [ jgr:graph <urn:example:dbpedia> ; jgr:expr "<http://dbpedia.org/>" ] ;

    # Remap <urn:example:regex> to a test on the graph name
    jgr:alias [ jgr:graph <urn:example:regex> ; jgr:expr "regex(str(?g), '[0-9]+$')" ] .

<urn:example:base> a ja:MemoryDataset .
```

The same configuration works with a TDB2 base dataset:

```turtle
PREFIX tdb2: <http://jena.apache.org/2016/tdb#>

<urn:example:base> a tdb2:DatasetTDB2 ;
    tdb2:location "/path/to/tdb2" .
```

## Example: Java

No explicit initialization is required: the assembler and the query engine factory are
registered automatically via Jena's subsystem lifecycle SPI (service loader).

```java
Model confModel = ModelFactory.createDefaultModel();
RDFDataMgr.read(confModel, new StringReader(assemblerStr), null, Lang.TURTLE);
Resource tdb2Conf = confModel.getResource("urn:example:base");
tdb2Conf.addProperty(VocabTDB2.pLocation, "/path/to/tdb2");

// Assemble the wrapping dataset
Dataset dataset = DatasetFactory.assemble(confModel.getResource("urn:example:root"));

// Load data: two named graphs with two triples each
String data = """
    <urn:example:g1> { <urn:example:s> a <urn:example:class> ; rdfs:label "s" }
    <urn:example:g2> { <urn:example:s> a <urn:example:class> ; rdfs:label "s" }
    """;
Txn.executeWrite(dataset, () -> RDFDataMgr.read(dataset, new StringReader(data), null, Lang.TRIG));

// The FROM clauses are rewritten to filters before execution
Query query = QueryFactory.create(
    "SELECT * FROM <urn:example:g1> FROM <urn:example:g2> { ?s ?p ?o }");
try (QueryExecution qe = QueryExecution.create(query, dataset)) {
    ResultSet rs = qe.execSelect();
    ResultSetFormatter.print(rs, System.out, null); // 4 rows
}
```

## Using with Fuseki

A self-contained plugin JAR (shaded, Jena dependencies excluded) can be built with:

```sh
# from the repository root
make fuseki-plugin
# or, in the jena-graphremap-pkg-fuseki-plugin module
mvn -Pbundle clean package
```

This produces `jena-graphremap-fuseki-plugin-<version>.jar`.
Place it into the `webapp/WEB-INF/lib` directory of your Fuseki installation and restart
Fuseki. The plugin then registers itself automatically, and datasets can be configured
with `jgr:DatasetGraphRemap` assemblers as described above.

## Implementation

| Class | Role |
|-------|------|
| `org.aksw.jena.graphremap.init.InitJenaGraphRemap` | Jena subsystem lifecycle hook (level 2345); registers the assembler and the query engine factory |
| `org.aksw.jena.graphremap.assembler.DatasetAssemblerGraphRemap` | Assembler interpreter for `jgr:DatasetGraphRemap` |
| `org.aksw.jena.graphremap.model.GraphRemapConfig` / `GraphAliasEntry` | Reads `ja:dataset` and the `jgr:alias` entries |
| `org.aksw.jena.graphremap.dataset.DatasetGraphGraphRemap` | Marker wrapper around the base `DatasetGraph`, carrying the graph-to-expression map |
| `org.aksw.jena.graphremap.engine.QueryEngineFactoryFromAsFilter` | `QueryEngineFactory` that rewrites the query and delegates to the base engine |
| `org.aksw.jena.graphremap.engine.ElementTransformDatasetDescription` | The actual `FROM` / `FROM NAMED` to `GRAPH` + `FILTER` rewrite |
| `org.aksw.jena.graphremap.util.DynamicDatasetUtils` | Unwraps `DynamicDatasetGraph` so the engine factory still matches |

## Building

Requirements: Java 21, Maven (Jena `6.3.0-SNAPSHOT`, JUnit 6).

```sh
mvn clean install
```

The test suite (`jena-graphremap/src/test`) covers:

- `FROM` clause rewriting against a TDB2 dataset (SELECT and CONSTRUCT queries)
- A SPARQL update (DELETE/INSERT round trip) leaving the data intact
