package it.unive.scsr.checkers;


import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SimpleAbstractState;
import it.unive.lisa.analysis.heap.pointbased.PointBasedHeap;
import it.unive.lisa.analysis.nonrelational.value.TypeEnvironment;
import it.unive.lisa.analysis.types.InferredTypes;
import it.unive.lisa.checks.semantic.CheckToolWithAnalysisResults;
import it.unive.lisa.checks.semantic.SemanticCheck;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.numeric.Division;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;
import it.unive.scsr.Intervals;
import it.unive.scsr.Pentagons;
import it.unive.scsr.UpperBounds;

public class DivisionByZeroChecker implements
SemanticCheck<
		SimpleAbstractState<PointBasedHeap, Pentagons, TypeEnvironment<InferredTypes>>> {
	
	@Override
	public boolean visit(
			CheckToolWithAnalysisResults<SimpleAbstractState<PointBasedHeap, Pentagons, TypeEnvironment<InferredTypes>>> tool,
			CFG graph, Statement node) {
		
		if( node instanceof Division)
			checkDivision(tool, graph, (Division) node);

		
		return true;
		
	}

	private void checkDivision(
			CheckToolWithAnalysisResults<SimpleAbstractState<PointBasedHeap, Pentagons, TypeEnvironment<InferredTypes>>> tool,
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

			
							Pentagons valueState = state.getState().getValueState();


							Intervals intervalAbstractValue = valueState.getIntervals().eval((ValueExpression) s, div, state.getState());
                            UpperBounds upperboundsAbstractValue = valueState.getUpperbounds().eval((ValueExpression) s, div, state.getState());
                            for (var id : upperboundsAbstractValue) {
                                Intervals ubInterval = valueState.getIntervals().getState(id);
                                intervalAbstractValue = intervalAbstractValue.lub(ubInterval);
                            }

                            if (!intervalAbstractValue.isBottom()) {
                                if (intervalAbstractValue.interval.includes(Intervals.ZERO.interval)) {
                                    if (intervalAbstractValue.equals(Intervals.ZERO)) {
                                        tool.warn(String.format(
                                                "Division by zero detected at %s. The divisor '%s' is exactly zero: %s with upper bounds %s",
                                                div.getLocation(),
                                                divisor,
                                                intervalAbstractValue.interval,
                                                upperboundsAbstractValue.representation()
                                        ));
                                    } else {
                                        // Check for negative upper bounds
                                        if (!hasNegativeUpperBound(valueState, upperboundsAbstractValue)) {
                                            tool.warn(String.format(
                                                    "Possible division by zero detected at %s. The divisor '%s' may include zero: %s with upper bounds %s",
                                                    div.getLocation(),
                                                    divisor,
                                                    intervalAbstractValue.interval,
                                                    upperboundsAbstractValue.representation()
                                            ));
                                        } else {
                                            tool.warn("[DEBUG] Prevented a false positive due to negative upper bounds at " + div.getLocation());
                                        }
                                    }
                                }
                            }
						}
					} catch (SemanticException e) {
						e.printStackTrace();
					}
	

			}
		}
		
	}

    private boolean hasNegativeUpperBound(Pentagons pentagons, UpperBounds upperBounds) {
        return hasNegativeUpperBoundAux(pentagons, upperBounds, new HashSet<>());
    }

    /**
     * Check if there is a negative upper bound with interval whose high is negative
     * @param pentagons the pentagons abstract state
     * @param upperBounds the upper bounds to check
     * @return true if there is at least one negative upper bound, false otherwise
     */
    private boolean hasNegativeUpperBoundAux(Pentagons pentagons, UpperBounds upperBounds, Set<Identifier> seen) {
        System.out.println("[DEBUG] I'm recursing! This time on " + upperBounds.representation());
        if (upperBounds.isBottom())
            return false;
        for (var id : upperBounds) {
            if (seen.contains(id)) {
                System.out.println("[DEBUG] Already seen " + id + ", skipping to avoid cycles");
                continue;
            }
            seen.add(id);
            Intervals ubInterval = pentagons.getIntervals().getState(id);
            if (ubInterval.interval.getHigh().isNegative()) {
                return true;
            }
            if (hasNegativeUpperBoundAux(pentagons, pentagons.getUpperbounds().getState(id), seen)) {
                return true;
            }
        }
        return false;
    }

	// compute possible dynamic types / runtime types
	private Set<Type> getPossibleDynamicTypes(SymbolicExpression s, Division div,
			SimpleAbstractState<PointBasedHeap, Pentagons, TypeEnvironment<InferredTypes>> state) throws SemanticException {
		
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