package it.unive.scsr.checkers;

import java.util.HashSet;

import it.unive.lisa.analysis.AnalyzedCFG;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SimpleAbstractState;
import it.unive.lisa.analysis.heap.pointbased.PointBasedHeap;
import it.unive.lisa.analysis.nonrelational.value.TypeEnvironment;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.analysis.types.InferredTypes;
import it.unive.lisa.checks.semantic.CheckToolWithAnalysisResults;
import it.unive.lisa.checks.semantic.SemanticCheck;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.numeric.Division;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.scsr.Intervals;
import it.unive.scsr.intervals.numbers.IntervalNumber;

import static it.unive.scsr.utils.Logging.defaultLogger;

public class DivisionByZeroChecker implements SemanticCheck<SimpleAbstractState<PointBasedHeap, ValueEnvironment<Intervals>, TypeEnvironment<InferredTypes>>> {

    @Override
    public void beforeExecution(CheckToolWithAnalysisResults<SimpleAbstractState<PointBasedHeap, ValueEnvironment<Intervals>, TypeEnvironment<InferredTypes>>> tool) {
        SemanticCheck.super.beforeExecution(tool);
    }

    @Override
    public boolean visit(CheckToolWithAnalysisResults<SimpleAbstractState<PointBasedHeap, ValueEnvironment<Intervals>, TypeEnvironment<InferredTypes>>> tool, CFG graph, Statement node) {
        if (node instanceof Division division) checkDivision(tool, graph, division);
        return true;
    }

    @Override
    public void afterExecution(CheckToolWithAnalysisResults<SimpleAbstractState<PointBasedHeap, ValueEnvironment<Intervals>, TypeEnvironment<InferredTypes>>> tool) {
        SemanticCheck.super.afterExecution(tool);
    }

    private void checkDivision(CheckToolWithAnalysisResults<SimpleAbstractState<PointBasedHeap, ValueEnvironment<Intervals>, TypeEnvironment<InferredTypes>>> tool, CFG graph, Division div) {

        for (AnalyzedCFG<SimpleAbstractState<PointBasedHeap, ValueEnvironment<Intervals>, TypeEnvironment<InferredTypes>>> result : tool.getResultOf(graph)) {

            var analyzer = new Analyzer<>(result);
            var state = analyzer.getStateAfter(div.getRight());
            var expressions = analyzer.getComputedExpression(div.getRight());

            if (!expressions.isEmpty()) {

                var divisor = expressions.getFirst();

                try {

                    var reachableIds = new HashSet<>(state.reachableFrom(divisor, div, state).elements);

                    for (SymbolicExpression symbolicExpression : reachableIds) {
                        if (Analyzer.anyNumericalType(symbolicExpression, div, state)) {

                            ValueEnvironment<Intervals> valueState = state.getValueState();
                            var intervals = valueState.eval((ValueExpression) symbolicExpression, div, state);

                            if (intervals.interval.is(IntervalNumber.ofPrimitiveOrThrow(0))) {
                                System.out.println(symbolicExpression.getCodeLocation().getCodeLocation());
                                System.out.println(symbolicExpression);
                                System.out.println(intervals.representation());
                            }
                        }
                    }
                } catch (SemanticException e) {
                    defaultLogger.warning(e.getMessage());
                }
            }
        }
    }
}
