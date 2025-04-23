package it.unive.scsr.checkers;

import it.unive.lisa.analysis.AnalyzedCFG;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SimpleAbstractState;
import it.unive.lisa.analysis.heap.pointbased.PointBasedHeap;
import it.unive.lisa.analysis.nonrelational.value.TypeEnvironment;
import it.unive.lisa.analysis.types.InferredTypes;
import it.unive.lisa.analysis.value.ValueDomain;

public class Analyzer<V extends ValueDomain<V>> {

    private final AnalyzedCFG<SimpleAbstractState<PointBasedHeap, V, TypeEnvironment<InferredTypes>>> analyzer;

    public Analyzer(AnalyzedCFG<SimpleAbstractState<PointBasedHeap, V, TypeEnvironment<InferredTypes>>> analyzer) {
        this.analyzer = analyzer;
    }

    /**
     * Compute the exit state or throw a {@link RuntimeException}.
     * @throws RuntimeException If the lub operator fails.
     * @return The {@link ValueDomain} contained in the exit state.
     */
    public V exitStateOrThrow() {
        try {
            return analyzer
                    .getExitState()
                    .getState()
                    .getValueState();
        } catch (SemanticException e) {
            throw new RuntimeException(e);
        }
    }
}
