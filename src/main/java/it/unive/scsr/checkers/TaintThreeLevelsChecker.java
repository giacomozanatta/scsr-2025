package it.unive.scsr.checkers;

import java.util.HashSet;

import it.unive.lisa.analysis.AnalyzedCFG;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SimpleAbstractState;
import it.unive.lisa.analysis.heap.pointbased.PointBasedHeap;
import it.unive.lisa.analysis.nonrelational.value.TypeEnvironment;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.program.annotations.matcher.AnnotationMatcher;
import it.unive.lisa.program.annotations.matcher.BasicAnnotationMatcher;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeMember;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.call.CFGCall;
import it.unive.lisa.program.cfg.statement.call.UnresolvedCall;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.type.Type;
import it.unive.lisa.util.StringUtilities;
import it.unive.scsr.TaintThreeLevels;
import it.unive.lisa.analysis.types.InferredTypes;
import it.unive.lisa.checks.semantic.CheckToolWithAnalysisResults;
import it.unive.lisa.checks.semantic.SemanticCheck;
import it.unive.lisa.program.annotations.Annotation;

public class TaintThreeLevelsChecker
    implements SemanticCheck<
        SimpleAbstractState<
            PointBasedHeap, ValueEnvironment<TaintThreeLevels>, TypeEnvironment<InferredTypes>>> {

  private final boolean filterFirstThis;

  /**
   * @param filterFirstThis Allow taint analysis to be disabled if the first parameter is a
   *     reference type and its name is <code>this</code>. Not an elegant solution, but at least
   *     avoid some pedantic warnings for this checker.
   */
  public TaintThreeLevelsChecker(boolean filterFirstThis) {
    this.filterFirstThis = filterFirstThis;
  }

  /** Sink annotation. */
  public static final Annotation SINK_ANNOTATION = new Annotation("lisa.taint.Sink");

  /** Sink matcher. */
  public static final AnnotationMatcher SINK_MATCHER = new BasicAnnotationMatcher(SINK_ANNOTATION);

  @Override
  public boolean visit(
      CheckToolWithAnalysisResults<
              SimpleAbstractState<
                  PointBasedHeap,
                  ValueEnvironment<TaintThreeLevels>,
                  TypeEnvironment<InferredTypes>>>
          tool,
      CFG graph,
      Statement node) {

    if (!(node instanceof UnresolvedCall call)) return true;

    try {
      for (AnalyzedCFG<
              SimpleAbstractState<
                  PointBasedHeap,
                  ValueEnvironment<TaintThreeLevels>,
                  TypeEnvironment<InferredTypes>>>
          result : tool.getResultOf(call.getCFG())) {

        var resolved = tool.getResolvedVersion(call, result);
        if (resolved == null) System.err.println("Error");

        if (resolved instanceof CFGCall cfg) {
          for (CodeMember n : cfg.getTargets()) {
            var parameters = n.getDescriptor().getFormals();
            for (int i = 0; i < parameters.length; i++)
              if (parameters[i].getAnnotations().contains(SINK_MATCHER)) {
                var state = result.getAnalysisStateAfter(call.getParameters()[i]);
                var reachableIds = new HashSet<SymbolicExpression>();

                for (SymbolicExpression e : state.getComputedExpressions()) {
                  reachableIds.addAll(
                      state.getState().reachableFrom(e, node, state.getState()).elements);
                }

                for (SymbolicExpression s : reachableIds) {
                  ValueEnvironment<TaintThreeLevels> valueState = state.getState().getValueState();

                  // Filter taint checks for "this" reference.
                  if (filterFirstThis) {
                    var types = Analyzer.inferTypes(s, call, state.getState());
                    if (types.stream().allMatch(Type::isReferenceType)
                        && s.toString().equals("this")) {
                      continue;
                    }
                  }

                  if (valueState
                      .eval((ValueExpression) s, node, state.getState())
                      .isAlwaysTainted())
                    tool.warnOn(
                        call,
                        "[DEFINITE] The value passed for the "
                            + StringUtilities.ordinal(i + 1)
                            + " parameter of this call is always tainted, and it reaches the sink at parameter '"
                            + parameters[i].getName()
                            + "' of "
                            + resolved.getFullTargetName());
                  else if (valueState
                      .eval((ValueExpression) s, node, state.getState())
                      .isPossiblyTainted())
                    tool.warnOn(
                        call,
                        "[POSSIBLE] The value passed for the "
                            + StringUtilities.ordinal(i + 1)
                            + " parameter of this call may be tainted, and it reaches the sink at parameter '"
                            + parameters[i].getName()
                            + "' of "
                            + resolved.getFullTargetName());
                }
              }
          }
        }
      }
    } catch (SemanticException e) {
      System.err.println("Cannot check " + node);
      e.printStackTrace(System.err);
    }

    return true;
  }
}
