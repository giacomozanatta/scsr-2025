package it.unive.scsr.checkers;

import java.util.*;
import java.util.stream.Collectors;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SimpleAbstractState;
import it.unive.lisa.analysis.heap.pointbased.PointBasedHeap;
import it.unive.lisa.analysis.nonrelational.value.TypeEnvironment;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.analysis.types.InferredTypes;
import it.unive.lisa.checks.semantic.CheckToolWithAnalysisResults;
import it.unive.lisa.checks.semantic.SemanticCheck;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.Assignment;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.VariableRef;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.type.NumericType;
import it.unive.lisa.type.Type;
import it.unive.scsr.Intervals;
import it.unive.scsr.checkers.overflow.Message;
import it.unive.scsr.checkers.overflow.checkers.SizeChecker;

public class OverflowChecker
    implements SemanticCheck<
        SimpleAbstractState<
            PointBasedHeap, ValueEnvironment<Intervals>, TypeEnvironment<InferredTypes>>> {

  // Represents the exit state of an analysis, containing abstract data and the overflow level.
  public record ExitState(Intervals abstractData, SizeChecker.OverflowingLevel result) {}

  // Represents the key for an exit state, composed of a code location, a variable, and its
  // numerical size.
  public record ExitKey(CodeLocation codeLocation, Variable variable, NumericalSize size) {}

  public enum NumericalSize {
    INT8,
    INT16,
    INT32,
    UINT8,
    UINT16,
    UINT32,
    FLOAT8,
    FLOAT16,
    FLOAT32,
  }

  private final NumericalSize size;
  private final Map<ExitKey, Set<ExitState>> exitStates = new HashMap<>();

  public OverflowChecker(NumericalSize size) {
    this.size = size;
  }

  @Override
  public void beforeExecution(
      CheckToolWithAnalysisResults<
              SimpleAbstractState<
                  PointBasedHeap, ValueEnvironment<Intervals>, TypeEnvironment<InferredTypes>>>
          tool) {
    exitStates.clear();
  }

  @Override
  public boolean visit(
      CheckToolWithAnalysisResults<
              SimpleAbstractState<
                  PointBasedHeap, ValueEnvironment<Intervals>, TypeEnvironment<InferredTypes>>>
          tool,
      CFG graph,
      Statement node) {
    // Checking if each variable reference is over/under-flowing.
    if (node instanceof Assignment assignment) {
      if (assignment.getLeft() instanceof VariableRef reference) {
        checkVariableRef(tool, reference, graph);
      }
    } else {
      if (node instanceof VariableRef reference) {
        checkVariableRef(tool, reference, graph);
      }
    }

    return true;
  }

  @Override
  public void afterExecution(
      CheckToolWithAnalysisResults<
              SimpleAbstractState<
                  PointBasedHeap, ValueEnvironment<Intervals>, TypeEnvironment<InferredTypes>>>
          tool) {
    exitStates.forEach(
        (key, value) -> {
          var data =
              value.stream()
                  .filter(state -> state.result.isOverflowing())
                  .filter(state -> state.result.definitely())
                  .reduce(
                      (first, second) -> {
                        try {
                          var lub = first.abstractData.lub(second.abstractData());
                          return new ExitState(lub, first.result);
                        } catch (SemanticException e) {
                          throw new IllegalArgumentException(e);
                        }
                      });

          if (data.isPresent()) {

            // Extracts information from the key and results from the input value.
            var safeData = data.orElseThrow();
            var info =
                new Message.Info(
                    key.variable.getName(),
                    key.size.toString(),
                    key.codeLocation.getCodeLocation());
            var warning =
                new Message.Warning(
                    "definite overflow", safeData.abstractData.representation().toString());

            // Log the warnings using the provided tool.
            tool.warn(new Message(warning, info).toJson());
          }
        });
  }

  private boolean isSupportedType(final Set<Type> possibleTypes) {
    // The set of inferred types must contain at least one NumericType.
    return possibleTypes.stream().map(Type::getClass).anyMatch(NumericType.class::isAssignableFrom);
  }

  private void checkVariableRef(
      CheckToolWithAnalysisResults<
              SimpleAbstractState<
                  PointBasedHeap, ValueEnvironment<Intervals>, TypeEnvironment<InferredTypes>>>
          tool,
      VariableRef ref,
      CFG graph) {

    var id = new Variable(ref.getStaticType(), ref.getName(), ref.getLocation());
    var types =
        tool.getResultOf(graph).stream()
            .flatMap(
                result ->
                    Analyzer.inferTypes(id, ref, new Analyzer<>(result).getStateAfter(ref))
                        .stream())
            .collect(Collectors.toSet());

    // Perform analysis only for some specific types, namely those checked in the "isSupportedType"
    // method. If
    // staticType is untyped, then dynamic types are checked. If there are no supported types, no
    // analysis will be
    // performed because the inferred type is not supported.
    if (!(isSupportedType(types))) {
      return;
    }

    tool.getResultOf(graph)
        .forEach(
            result -> {
              // Computes the exit state for the specified node.
              var env = new Analyzer<>(result).exitStateOrThrow();

              if (env.knowsIdentifier(id)) {
                // Since this checker deals with the interval domain, the environment state of the
                // value must be an
                // interval.
                var intervals = env.getState(id);

                // The overflow depends on the size of NumericalSize.
                var stickiness =
                    SizeChecker.findBy(size)
                        .map(sizeChecker -> sizeChecker.isOverflowing(intervals))
                        .orElse(SizeChecker.OverflowingLevel.base());

                if (stickiness.isOverflowing()) {
                  // Add the result as an exit state.
                  var key = new ExitKey(id.getCodeLocation(), id, size);
                  var currentSet = exitStates.getOrDefault(key, new HashSet<>());
                  currentSet.add(new ExitState(intervals, stickiness));
                  exitStates.put(key, currentSet);
                }
              }
            });
  }
}
