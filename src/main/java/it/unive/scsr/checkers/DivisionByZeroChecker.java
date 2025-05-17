package it.unive.scsr.checkers;

import java.util.HashSet;
import java.util.Set;

import it.unive.lisa.analysis.SimpleAbstractState;
import it.unive.lisa.analysis.heap.pointbased.PointBasedHeap;
import it.unive.lisa.analysis.nonrelational.value.TypeEnvironment;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.analysis.types.InferredTypes;
import it.unive.lisa.checks.semantic.CheckToolWithAnalysisResults;
import it.unive.lisa.checks.semantic.SemanticCheck;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.numeric.Division;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.scsr.Intervals;
import it.unive.scsr.checkers.divisionbyzero.Message;
import it.unive.scsr.intervals.numbers.IntervalNumber;

public class DivisionByZeroChecker
    implements SemanticCheck<
        SimpleAbstractState<
            PointBasedHeap, ValueEnvironment<Intervals>, TypeEnvironment<InferredTypes>>> {

  private record Data(CodeLocation codeLocation, SymbolicExpression expression) {}

  private final Set<Data> divisionsByZero = new HashSet<>();

  @Override
  public void beforeExecution(
      CheckToolWithAnalysisResults<
              SimpleAbstractState<
                  PointBasedHeap, ValueEnvironment<Intervals>, TypeEnvironment<InferredTypes>>>
          tool) {
    divisionsByZero.clear();
  }

  @Override
  public boolean visit(
      CheckToolWithAnalysisResults<
              SimpleAbstractState<
                  PointBasedHeap, ValueEnvironment<Intervals>, TypeEnvironment<InferredTypes>>>
          tool,
      CFG graph,
      Statement node) {
    if (node instanceof Division division) {
      checkDivision(tool, graph, division);
    }
    return true;
  }

  @Override
  public void afterExecution(
      CheckToolWithAnalysisResults<
              SimpleAbstractState<
                  PointBasedHeap, ValueEnvironment<Intervals>, TypeEnvironment<InferredTypes>>>
          tool) {
    // Write the warning using the tool provided.
    divisionsByZero.stream()
        .map(
            data -> {
              var warning = new Message.Warning("division by zero");
              var info =
                  new Message.Info(data.expression.toString(), data.codeLocation.getCodeLocation());
              return new Message(warning, info).toJson();
            })
        .forEach(tool::warn);
  }

  private void checkDivision(
      CheckToolWithAnalysisResults<
              SimpleAbstractState<
                  PointBasedHeap, ValueEnvironment<Intervals>, TypeEnvironment<InferredTypes>>>
          tool,
      CFG graph,
      Division div) {
    tool.getResultOf(graph)
        .forEach(
            result -> {
              // Get the simple abstract state (which will also be used as an oracle), terminate the
              // expression computed for the right operand of the division expression.
              var analyzer = new Analyzer<>(result);
              var state = analyzer.getStateAfter(div.getRight());
              var expressions = analyzer.getComputedExpression(div.getRight());

              // When the set of evaluated expressions is non-empty, the first one is taken.
              if (!expressions.isEmpty()) {

                analyzer
                    .reachableElements(expressions.getFirst(), div, state)
                    .forEach(
                        expression -> {
                          // For each expression reachable from the first one calculated, if it is
                          // of numeric type, the abstract domain is calculated.
                          if (Analyzer.anyNumericalType(expression, div, state)) {

                            // With the abstract domain, it is possible to know whether the right
                            // side of a division expression is a zero value.
                            var domain =
                                Analyzer.evalOrThrow(
                                    state.getValueState(),
                                    (ValueExpression) expression,
                                    div,
                                    state);
                            if (domain.interval.is(IntervalNumber.ofPrimitiveOrThrow(0))) {
                              divisionsByZero.add(
                                  new Data(expression.getCodeLocation(), expression));
                            }
                          }
                        });
              }
            });
  }
}
