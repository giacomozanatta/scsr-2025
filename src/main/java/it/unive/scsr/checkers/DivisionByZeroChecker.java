package it.unive.scsr.checkers;


import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

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
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.numeric.Division;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;
import it.unive.lisa.util.numeric.MathNumber;
import it.unive.scsr.Intervals;
import it.unive.scsr.checkers.OverflowChecker.NumericalSize;

public class DivisionByZeroChecker implements
SemanticCheck<
		SimpleAbstractState<PointBasedHeap, ValueEnvironment<Intervals>, TypeEnvironment<InferredTypes>>> {
	
	
	private NumericalSize size;
	
	public DivisionByZeroChecker(NumericalSize size) {
		this.size = size;
	}

	@Override
	public boolean visit(
			CheckToolWithAnalysisResults<SimpleAbstractState<PointBasedHeap, ValueEnvironment<Intervals>, TypeEnvironment<InferredTypes>>> tool,
			CFG graph, Statement node) {
		
		if( node instanceof Division)
			checkDivision(tool, graph, (Division) node);

		
		return true;
		
	}

	private void checkDivision(
			CheckToolWithAnalysisResults<SimpleAbstractState<PointBasedHeap, ValueEnvironment<Intervals>, TypeEnvironment<InferredTypes>>> tool,
			CFG graph, Division div) {

		for (AnalyzedCFG<SimpleAbstractState<PointBasedHeap, ValueEnvironment<Intervals>,
				TypeEnvironment<InferredTypes>>> result : tool.getResultOf(graph)) {
			AnalysisState<
			SimpleAbstractState<PointBasedHeap, ValueEnvironment<Intervals>,
					TypeEnvironment<InferredTypes>>> state = result.getAnalysisStateAfter(div.getRight());
			
			Set<SymbolicExpression> reachableIds = new HashSet<>();
			Iterator<SymbolicExpression> comExprIterator = state.getComputedExpressions().iterator();
			if(comExprIterator.hasNext()) {
				SymbolicExpression divisor = comExprIterator.next();
					try {
						reachableIds
								.addAll(state.getState().reachableFrom(divisor, div, state.getState()).elements);
						
						for (SymbolicExpression s : reachableIds) {

							Set<Type> types = getPossibleDynamicTypes(s, div, state.getState());
// MY CODE STARTS HERE:
							// TYPE CHECKER
							boolean numericFound = types.stream().anyMatch(Type::isNumericType);
							if (!numericFound) {
							// If the type doesn't belong to numeric type
								tool.warnOn(div, "DivisionByZeroChecker: the operand at '" + div.getLocation() + "' must have a numeric type, found " + types);
								System.err.println("DEBUG: DivisionByZeroChecker: the operand at '" + div.getLocation() + "' must have a numeric type, found " + types);
								continue;
							}

							// DIVISION BY ZERO CHECKER
							ValueEnvironment<Intervals> valueState = state.getState().getValueState();
							Intervals intervalAbstractValue = valueState.eval((ValueExpression) s, div, state.getState());

							// Checks if interval is null
							if (intervalAbstractValue == null) {
								tool.warnOn(div, "DivisionByZeroChecker: intervalAbstractValue is null, skipping...");
								continue;
							}
							// Checks if interval is bottom
							if (intervalAbstractValue.isBottom()) {
								tool.warnOn(div, "DivisionByZeroChecker: intervalAbstractValue is bottom, skipping...");
								continue;
							}

							MathNumber low = intervalAbstractValue.interval.getLow();
							MathNumber high = intervalAbstractValue.interval.getHigh();

							if (low.equals(MathNumber.ZERO) && high.equals(MathNumber.ZERO)) {
								tool.warnOn(div, "DivisionByZeroChecker: DEFINITE division by zero detected!");
								System.err.println("DEBUG: Definite division by zero at " + div.getLocation());
							} else if (low.compareTo(MathNumber.ZERO) <= 0 && high.compareTo(MathNumber.ZERO) >= 0) {
								tool.warnOn(div, "DivisionByZeroChecker: POTENTIAL division by zero detected (interval includes 0)!");
								System.err.println("DEBUG: Potential division by zero at " + div.getLocation());
							}
						}
// END OF MY CODE
					} catch (SemanticException e) {
						e.printStackTrace();
					}


			}
		}

	}

	// compute possible dynamic types / runtime types
	private Set<Type> getPossibleDynamicTypes(SymbolicExpression s, Division div,
			SimpleAbstractState<PointBasedHeap, ValueEnvironment<Intervals>, TypeEnvironment<InferredTypes>> state) throws SemanticException {
		
		Set<Type> possibleDynamicTypes = new HashSet<>();
		Type dynamicTypes = state.getDynamicTypeOf(s, div, state);
		if(dynamicTypes != null && !dynamicTypes.isUntyped()) {
			possibleDynamicTypes.add(dynamicTypes);
		} else if(dynamicTypes.isUntyped()){
			Set<Type> runtimeTypes = state.getRuntimeTypesOf(s, div, state);
			if(runtimeTypes.stream().anyMatch(t -> t != Untyped.INSTANCE))
				for( Type t : runtimeTypes)
					possibleDynamicTypes.add(t);
		}
		
		return possibleDynamicTypes;
	
	}
	

}