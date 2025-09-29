package it.unive.scsr.checkers;

import java.util.Set;
import java.util.stream.Collectors;

import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.AnalyzedCFG;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SimpleAbstractState;
import it.unive.lisa.analysis.heap.pointbased.PointBasedHeap;
import it.unive.lisa.analysis.nonrelational.value.TypeEnvironment;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.analysis.types.InferredTypes;
import it.unive.lisa.checks.semantic.CheckToolWithAnalysisResults;
import it.unive.lisa.checks.semantic.SemanticCheck;
import it.unive.lisa.program.annotations.Annotation;
import it.unive.lisa.program.annotations.matcher.AnnotationMatcher;
import it.unive.lisa.program.annotations.matcher.BasicAnnotationMatcher;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeMember;
import it.unive.lisa.program.cfg.Parameter;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.call.CFGCall;
import it.unive.lisa.program.cfg.statement.call.Call;
import it.unive.lisa.program.cfg.statement.call.UnresolvedCall;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.util.StringUtilities;
import it.unive.scsr.TaintThreeLevels;

// -- refactored version --

// public class TaintThreeLevelsChecker_REF implements

public class TaintThreeLevelsChecker implements
		SemanticCheck<
				SimpleAbstractState<PointBasedHeap, ValueEnvironment<TaintThreeLevels>, TypeEnvironment<InferredTypes>>> {

	public static final Annotation SENSITIVE_SINK_ANNOTATION = new Annotation("lisa.taint.Sink");

	public static final AnnotationMatcher SENSITIVE_SINK_MATCHER = new BasicAnnotationMatcher(
			SENSITIVE_SINK_ANNOTATION);
			
	// Alias for generic complexity
	// private static final String GENERIC_STATE_ALIAS = "SimpleAbstractState<PointBasedHeap, ValueEnvironment<TaintThreeLevels>, TypeEnvironment<InferredTypes>>";

	@Override
	public boolean visit(CheckToolWithAnalysisResults<SimpleAbstractState<PointBasedHeap, ValueEnvironment<TaintThreeLevels>, TypeEnvironment<InferredTypes>>> tool,
			CFG graph, Statement statement) {
		
		// Guard clause
		if (!(statement instanceof UnresolvedCall)) {
			return true;
		}

		UnresolvedCall call = (UnresolvedCall) statement;
		
		// Result iteration for current CFG
		for (AnalyzedCFG<SimpleAbstractState<PointBasedHeap, ValueEnvironment<TaintThreeLevels>, TypeEnvironment<InferredTypes>>> result : tool.getResultOf(call.getCFG())) {
			try {
				processCall(tool, call, result);
			} catch (SemanticException e) {
				System.err.println("Semantic error while checking " + call + ": " + e.getMessage());
				e.printStackTrace(System.err);
			}
		}
		return true;
	}

	// Verify call destination
	private void processCall(
			CheckToolWithAnalysisResults<SimpleAbstractState<PointBasedHeap, ValueEnvironment<TaintThreeLevels>, TypeEnvironment<InferredTypes>>> tool,
			UnresolvedCall unresolvedCall,
			AnalyzedCFG<SimpleAbstractState<PointBasedHeap, ValueEnvironment<TaintThreeLevels>, TypeEnvironment<InferredTypes>>> result) throws SemanticException {
		
		Call resolved = tool.getResolvedVersion(unresolvedCall, result);
		if (resolved == null || !(resolved instanceof CFGCall)) {
			return; 
		}

		CFGCall cfgCall = (CFGCall) resolved;
		for (CodeMember target : cfgCall.getTargets()) {
			checkSinkParameters(tool, unresolvedCall, cfgCall, target, result);
		}
	}

	// Detect sink(s)
	private void checkSinkParameters(
			CheckToolWithAnalysisResults<SimpleAbstractState<PointBasedHeap, ValueEnvironment<TaintThreeLevels>, TypeEnvironment<InferredTypes>>> tool,
			UnresolvedCall originalCall, CFGCall resolvedCall, CodeMember target,
			AnalyzedCFG<SimpleAbstractState<PointBasedHeap, ValueEnvironment<TaintThreeLevels>, TypeEnvironment<InferredTypes>>> result) throws SemanticException {
		
		Parameter[] formals = target.getDescriptor().getFormals();
		
		for (int i = 0; i < formals.length; i++) {
			if (formals[i].getAnnotations().contains(SENSITIVE_SINK_MATCHER)) {
				analyzeTaintedArgument(tool, originalCall, resolvedCall, formals[i], i, result);
			}
		}
	}

	// CORE LOGIC
	// Check if the argument is tainted
	private void analyzeTaintedArgument(
			CheckToolWithAnalysisResults<SimpleAbstractState<PointBasedHeap, ValueEnvironment<TaintThreeLevels>, TypeEnvironment<InferredTypes>>> tool,
			UnresolvedCall originalCall, CFGCall resolvedCall, Parameter sinkParameter, int paramIndex,
			AnalyzedCFG<SimpleAbstractState<PointBasedHeap, ValueEnvironment<TaintThreeLevels>, TypeEnvironment<InferredTypes>>> result) throws SemanticException {
		
		AnalysisState<SimpleAbstractState<PointBasedHeap, ValueEnvironment<TaintThreeLevels>, TypeEnvironment<InferredTypes>>> state = result.getAnalysisStateAfter(originalCall.getParameters()[paramIndex]);
		ValueEnvironment<TaintThreeLevels> valueState = state.getState().getValueState();

		Set<SymbolicExpression> reachableExprs = state.getComputedExpressions().elements.stream()
				.flatMap(expr -> {
					try {
						return state.getState().reachableFrom(expr, originalCall, state.getState()).elements.stream();
					} catch (SemanticException e) {
						return java.util.stream.Stream.empty();
					}
				})
				.collect(Collectors.toSet());

		for (SymbolicExpression expr : reachableExprs) {
			TaintThreeLevels taint = valueState.eval((ValueExpression) expr, originalCall, state.getState());
			
			if (taint.isAlwaysTainted()) {
				String message = String.format(
						"[DEFINITE] A definitely tainted value reaches this sink. The %s parameter ('%s') of %s receives a tainted value.",
						StringUtilities.ordinal(paramIndex + 1),
						sinkParameter.getName(),
						resolvedCall.getFullTargetName()
				);
				tool.warnOn(originalCall, message);
			} else if (taint.isPossiblyTainted()) {
				String message = String.format(
						"[POSSIBLE] A possibly tainted value may reach this sink. The %s parameter ('%s') of %s might receive a tainted value.",
						StringUtilities.ordinal(paramIndex + 1),
						sinkParameter.getName(),
						resolvedCall.getFullTargetName()
				);
				tool.warnOn(originalCall, message);
			}
		}
	}
}