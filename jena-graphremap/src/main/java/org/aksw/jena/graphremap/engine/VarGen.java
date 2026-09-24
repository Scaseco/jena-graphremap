package org.aksw.jena.graphremap.engine;

import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.apache.jena.sparql.core.Var;

public class VarGen {
    public static Stream<Var> of(String baseMarker) {
        return IntStream.range(0, Integer.MAX_VALUE)
            .mapToObj(i -> baseMarker +i)
            .map(Var::alloc);
    }

}
