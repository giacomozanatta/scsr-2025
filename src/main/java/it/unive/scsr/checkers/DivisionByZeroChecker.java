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

		for (var result : tool.getResultOf(graph)) {
            // Takes the result of the expression on the divisor, like in expr1 / expr2 it takes expr2
			var state = result.getAnalysisStateAfter(div.getRight());
			
			Set<SymbolicExpression> reachableIds = new HashSet<>();
			Iterator<SymbolicExpression> comExprIterator = state.getComputedExpressions().iterator();
			if(comExprIterator.hasNext()) {
				SymbolicExpression divisor = comExprIterator.next();
					try {
						reachableIds
								.addAll(state.getState().reachableFrom(divisor, div, state.getState()).elements);
						
						for (SymbolicExpression s : reachableIds) {
                            Type staticType = s.getStaticType();
							Set<Type> dynamicTypes = getPossibleDynamicTypes(s, div, state.getState());

                            // It's a different type, ignore
                            if (!staticType.isNumericType() && !staticType.isUntyped())
                                return;

                            // Handle dynamic types
                            if (staticType.isUntyped()) {
                                boolean found = false;
                                for (Type type : dynamicTypes) {
                                    if (type.isNumericType()) {
                                        found = true;
                                        break;
                                    }
                                }

                                // Even the dynamic type is not numeric
                                if  (!found)
                                    return;
                            }

			
							ValueEnvironment<Intervals> valueState = state.getState().getValueState();
							
							Intervals intervalAbstractValue = valueState.eval((ValueExpression) s, div, state.getState());

                            if (!intervalAbstractValue.isBottom()) {
                                if (intervalAbstractValue.interval.includes(Intervals.ZERO.interval)) {
                                    if (intervalAbstractValue.equals(Intervals.ZERO)) {
                                        tool.warn("Division by zero detected in " + div + " at " + div.getLocation() +
                                                ". The divisor is exactly zero: " + intervalAbstractValue.interval);
                                    } else {
                                        tool.warn("Possible division by zero detected in " + div + " at " + div.getLocation() +
                                                ". The divisor may include zero: " + intervalAbstractValue.interval);
                                    }
                                }
                            }

							// TODO: add checks for division by zero
						}
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