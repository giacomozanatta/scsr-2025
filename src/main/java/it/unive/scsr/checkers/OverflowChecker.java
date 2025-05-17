package it.unive.scsr.checkers;

import java.util.*;

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
import it.unive.lisa.program.type.*;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.scsr.Intervals;
import it.unive.scsr.checkers.overflow.Message;
import it.unive.scsr.checkers.overflow.checkers.SizeChecker;

public class OverflowChecker
    implements SemanticCheck<
        SimpleAbstractState<
            PointBasedHeap, ValueEnvironment<Intervals>, TypeEnvironment<InferredTypes>>> {

  // Represents the exit state of an analysis, containing abstract data and the overflow level.
  public record ExitState(Intervals abstractData, SizeChecker.OverflowResult result) {}

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
    FLOAT32;

    public boolean isFloatingPoint() {
      return this.equals(FLOAT8) || this.equals(FLOAT16) || this.equals(FLOAT32);
    }

    public boolean isInteger() {
      return !isFloatingPoint();
    }
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
        checkVariableRef(tool, reference, graph, node);
      }
    } else {
      if (node instanceof VariableRef reference) {
        checkVariableRef(tool, reference, graph, node);
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
                  .filter(state -> state.result.isValuable())
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
                    safeData.result.isDefinite() ? "definite overflow" : "may overflow",
                    safeData.abstractData.representation().toString());

            // Log the warnings using the provided tool.
            tool.warn(new Message(warning, info).toJson());
          }
        });
  }

  private void checkVariableRef(
      CheckToolWithAnalysisResults<
              SimpleAbstractState<
                  PointBasedHeap, ValueEnvironment<Intervals>, TypeEnvironment<InferredTypes>>>
          tool,
      VariableRef ref,
      CFG graph,
      Statement node) {

    var id = new Variable(ref.getStaticType(), ref.getName(), ref.getLocation());

    tool.getResultOf(graph)
        .forEach(
            result -> {
              var analyzer = new Analyzer<>(result);
              var types = Analyzer.inferTypes(id, ref, analyzer.getStateAfter(ref));

              // Check if any of the types in the 'types' stream are either Float32Type or
              // Float64Type. This determines if the value might represent a single-precision or
              // double-precision floating-point number.
              var mayBeFloat =
                  types.stream()
                      .anyMatch(type -> type instanceof Float32Type || type instanceof Float64Type);

              // Check if any of the types in the 'types' stream are one of the integer types
              // (signed or unsigned 8, 16, 32, or 64 bit). This determines if the value might
              // represent a whole number.
              var mayBeInteger =
                  types.stream()
                      .anyMatch(
                          type ->
                              type instanceof Int8Type
                                  || type instanceof Int16Type
                                  || type instanceof Int32Type
                                  || type instanceof Int64Type
                                  || type instanceof UInt8Type
                                  || type instanceof UInt16Type
                                  || type instanceof UInt32Type
                                  || type instanceof UInt64Type);

              // Determine if the determined 'size' (likely representing the size of a data type) is
              // aligned with the potential underlying types. It checks if a floating-point size is
              // associated with a potentially floating-point type, or if an integer size is
              // associated with a potential integer type.
              var isTypeAligned =
                  (size.isFloatingPoint() && mayBeFloat) || (size.isInteger() && mayBeInteger);

              // Perform analysis only for some specific types. If staticType is untyped, then
              // dynamic types are checked. If there are no supported types, no analysis will be
              // performed because the inferred type is not supported.
              if (isTypeAligned) {
                // Computes the exit state for the specified node.
                var state = analyzer.getStateAfter(node);
                var env = state.getValueState();

                if (env.knowsIdentifier(id)) {

                  // Since this checker deals with the interval domain, the environment state of the
                  // value must be an interval.
                  var intervals = env.getState(id);

                  // The overflow depends on the size of NumericalSize.
                  var stickiness =
                      SizeChecker.findBy(size)
                          .map(sizeChecker -> sizeChecker.isOverflowing(intervals))
                          .orElse(null);

                  if (stickiness != null && stickiness.isValuable()) {
                    // Add the result as an exit state.
                    var key = new ExitKey(id.getCodeLocation(), id, size);
                    var currentSet = exitStates.getOrDefault(key, new HashSet<>());
                    currentSet.add(new ExitState(intervals, stickiness));
                    exitStates.put(key, currentSet);
                  }
                }
              }
            });
  }
}
