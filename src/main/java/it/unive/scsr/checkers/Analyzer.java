package it.unive.scsr.checkers;

import it.unive.lisa.analysis.AnalyzedCFG;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.SimpleAbstractState;
import it.unive.lisa.analysis.heap.pointbased.PointBasedHeap;
import it.unive.lisa.analysis.nonrelational.value.TypeEnvironment;
import it.unive.lisa.analysis.types.InferredTypes;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

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

    public List<SymbolicExpression> getComputedExpression(Statement statement) {
        var expressionsSpliterator = analyzer
                .getAnalysisStateAfter(statement)
                .getComputedExpressions()
                .spliterator();

        return StreamSupport
                .stream(expressionsSpliterator, false)
                .toList();
    }

    public Set<Type> inferTypes(SymbolicExpression expression, ProgramPoint programPoint, SemanticOracle oracle) {
        try {
            var dynamicType = oracle.getDynamicTypeOf(expression, programPoint, oracle);
            if (!dynamicType.isUntyped()) return Set.of(dynamicType);

            return oracle
                    .getRuntimeTypesOf(expression, programPoint, oracle)
                    .stream()
                    .filter(t -> t != Untyped.INSTANCE)
                    .collect(Collectors.toSet());
        } catch (Exception e) {
            return Collections.emptySet();
        }
    }
}
