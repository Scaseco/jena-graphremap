package org.aksw.jena.graphremap.init;

import org.aksw.jena.graphremap.assembler.DatasetAssemblerGraphRemap;
import org.aksw.jena.graphremap.assembler.GraphRemapVocab;
import org.aksw.jena.graphremap.engine.QueryEngineFactoryFromAsFilter;
import org.apache.jena.assembler.Assembler;
import org.apache.jena.assembler.assemblers.AssemblerGroup;
import org.apache.jena.sparql.core.assembler.AssemblerUtils;
import org.apache.jena.sparql.core.assembler.DatasetAssembler;
import org.apache.jena.sparql.engine.QueryEngineRegistry;
import org.apache.jena.sys.JenaSubsystemLifecycle;

public class InitJenaGraphRemap
    implements JenaSubsystemLifecycle
{
    public static final int LEVEL = 2345;

    @Override
    public void start() {
        init();
    }

    @Override
    public void stop() {
        // Nothing to do
    }

    /** The plugin is loaded late with level {@value #LEVEL} so that the query engine factory
     * is added closer to the front of the registry */
    @Override
    public int level() {
        return LEVEL;
    }

    public static void init() {
        QueryEngineRegistry.addFactory(new QueryEngineFactoryFromAsFilter());
        registerWith(Assembler.general());
    }

    static void registerWith(AssemblerGroup g) {
        AssemblerUtils.register(g, GraphRemapVocab.DatasetGraphRemap, new DatasetAssemblerGraphRemap(), DatasetAssembler.getGeneralType());
    }
}
