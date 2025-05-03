package it.unive.scsr.checkers;

import it.unive.lisa.analysis.AnalyzedCFG;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SimpleAbstractState;
import it.unive.lisa.analysis.heap.pointbased.PointBasedHeap;
import it.unive.lisa.analysis.nonrelational.value.TypeEnvironment;
import it.unive.lisa.analysis.types.InferredTypes;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.VariableRef;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

public class Analyzer<V extends ValueDomain<V>> {

    private final AnalyzedCFG<SimpleAbstractState<PointBasedHeap, V, TypeEnvironment<InferredTypes>>> analyzer;

    public Analyzer(AnalyzedCFG<SimpleAbstractState<PointBasedHeap, V, TypeEnvironment<InferredTypes>>> analyzer) {
        this.analyzer = analyzer;
    }

    /**
     * Compute the exit state or throw a {@link RuntimeException}.
     *
     * @return The {@link ValueDomain} contained in the exit state.
     * @throws RuntimeException If the lub operator fails.
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

    public SimpleAbstractState<PointBasedHeap, V, TypeEnvironment<InferredTypes>> getAnalysisStateAfter(Statement statement) {
        return analyzer.getAnalysisStateAfter(statement).getState();
    }

    /**
     * @param id        An identifier of a real program variable.
     * @param reference A reference to a variable of the current CFG, identified by its name.
     * @return The set of possible types that the given variable can "hold". If the types cannot be inferred, an empty
     * set is returned.
     */
    public Set<Type> getDynamicTypes(Variable id, VariableRef reference) {
        try {
            var state = getAnalysisStateAfter(reference);
            var dynamicType = state.getDynamicTypeOf(id, reference, state);

            if (!dynamicType.isUntyped()) return Set.of(dynamicType);

            return state
                    .getRuntimeTypesOf(id, reference, state)
                    .stream()
                    .filter(t -> t != Untyped.INSTANCE)
                    .collect(Collectors.toSet());
        } catch (Exception e) {
            return Collections.emptySet();
        }
    }
}
