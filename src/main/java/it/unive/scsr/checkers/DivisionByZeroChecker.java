package it.unive.scsr.checkers;


import it.unive.lisa.analysis.AnalyzedCFG;
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
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.util.numeric.MathNumber;
import it.unive.scsr.Intervals;
import it.unive.scsr.Pentagons;

public class DivisionByZeroChecker implements
SemanticCheck<
		SimpleAbstractState<PointBasedHeap, Pentagons, TypeEnvironment<InferredTypes>>> {


	public DivisionByZeroChecker() {

		// Find division operations in the CFG
	}	@Override
	public boolean visit(
			CheckToolWithAnalysisResults<SimpleAbstractState<PointBasedHeap, Pentagons, TypeEnvironment<InferredTypes>>> tool,
			CFG graph, Statement node) {
		
		// Perform check only if is a division
		if( node instanceof Division)
			checkDivision(tool, graph, (Division) node);

		
		return true;
		
	}
	// CORE LOGIC
	private void checkDivision(
			CheckToolWithAnalysisResults<SimpleAbstractState<PointBasedHeap, Pentagons, TypeEnvironment<InferredTypes>>> tool,
			CFG graph, Division div) {

		// Get the divisor (right hand side)
		it.unive.lisa.program.cfg.statement.Expression divisorExpr = div.getRight();
		
		for (AnalyzedCFG<SimpleAbstractState<PointBasedHeap, Pentagons,
				TypeEnvironment<InferredTypes>>> result : tool.getResultOf(graph)) {
			
			try {
				// Get state BEFORE the division operation
				it.unive.lisa.analysis.AnalysisState<SimpleAbstractState<PointBasedHeap, Pentagons, TypeEnvironment<InferredTypes>>> analysisStateBefore = result.getAnalysisStateBefore(div);
				if (analysisStateBefore == null) continue;
				SimpleAbstractState<PointBasedHeap, Pentagons, TypeEnvironment<InferredTypes>> stateBefore = analysisStateBefore.getState();
				if (stateBefore == null) continue;
				
				SymbolicExpression symbolicDivisor = null;
				for (SymbolicExpression expr : analysisStateBefore.getComputedExpressions()) {
					if (expr.getCodeLocation().equals(divisorExpr.getLocation())) {
						symbolicDivisor = expr;
						break;
					}
				}
				if (symbolicDivisor == null) continue;
				
				// Evaluation of divisor abstract interval
				Intervals divisorInterval = evaluateSymbolicExpression(symbolicDivisor, stateBefore);
				if (divisorInterval != null && divisorInterval.interval != null) {
					// Check if the interval includes zero
					if (divisorInterval.interval.getLow().compareTo(MathNumber.ZERO) <= 0 && 
						divisorInterval.interval.getHigh().compareTo(MathNumber.ZERO) >= 0) {
						tool.warnOn(div, "Potential division by zero: divisor may be zero");
					}
				}
			} catch (Exception e) {
				continue;
			}
		}
	}
	// Evaluation
	private Intervals evaluateSymbolicExpression(SymbolicExpression expr, SimpleAbstractState<PointBasedHeap, Pentagons, TypeEnvironment<InferredTypes>> state) {
		// Case 1: it's a variable
		if (expr instanceof Variable) {
			Variable var = (Variable) expr;
			Object valueState = state.getValueState();
			// If using pentagons
			if (valueState instanceof Pentagons) {
				Pentagons pentagonsState = (Pentagons) valueState;
				if (pentagonsState.getIntervals() != null) {
					return pentagonsState.getIntervals().getState(var);
				}
			// If using intervals
			} else if (valueState instanceof ValueEnvironment) {
				@SuppressWarnings("unchecked")
				ValueEnvironment<Intervals> intervalsState = (ValueEnvironment<Intervals>) valueState;
				return intervalsState.getState(var);
			}
		// Case 2: it's a binary expr
		} else if (expr instanceof BinaryExpression) {
			BinaryExpression binExpr = (BinaryExpression) expr;
			SymbolicExpression left = binExpr.getLeft();
			SymbolicExpression right = binExpr.getRight();
			Intervals leftInterval = evaluateSymbolicExpression(left, state);
			Intervals rightInterval = evaluateSymbolicExpression(right, state);
			// Eval. binary expr
			if (leftInterval != null && rightInterval != null) {
				try {
					return leftInterval.evalBinaryExpression(binExpr.getOperator(), leftInterval, rightInterval, null, null);
				} catch (it.unive.lisa.analysis.SemanticException e) {
					return null;
				}
			}
		}
		// For other types
		return null;
	}
}