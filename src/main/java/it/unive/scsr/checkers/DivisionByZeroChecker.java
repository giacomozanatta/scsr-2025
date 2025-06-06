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
import it.unive.scsr.Interval;
import it.unive.scsr.Intervals;
import it.unive.scsr.Pentagons;
import it.unive.lisa.symbolic.value.Identifier;


public class DivisionByZeroChecker implements
SemanticCheck<
		SimpleAbstractState<PointBasedHeap, ValueEnvironment<Intervals>, TypeEnvironment<InferredTypes>>> {

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

							// ADDED: implement type checks, it is required a numerical type
							if (types.isEmpty()) {
								tool.warnOn(div, "No possible dynamic types for " + s.toString());
								return;
							}

							else {
								// check if any dynamic types are numeric types and return only if none are using map filter
								Set<Type> numericDynamicTypes = types.stream().filter(t -> t.isNumericType()).collect(java.util.stream.Collectors.toSet());
								if (numericDynamicTypes.isEmpty()) {
									tool.warnOn(div, "Variable " + s.toString() + " does not have any possible numeric types");
									return;
								}
							}

							Object vs = state.getState().getValueState();
							Intervals finalInterval = Intervals.TOP;
							
							if (vs instanceof ValueEnvironment<?>) {
								@SuppressWarnings("unchecked")
								ValueEnvironment<Intervals> valueState = (ValueEnvironment<Intervals>) vs;
								finalInterval = valueState.eval((ValueExpression) s, div, state.getState());
							}
						
							else if (vs instanceof Pentagons) {
								Pentagons p = (Pentagons) vs;
								
								ValueEnvironment<Intervals> ie = p.getInterval();
								Intervals raw = ie.eval((ValueExpression) s, div, state.getState());
								if (raw == null || raw.isBottom())
									return;
								if ((s instanceof Identifier)) {
									Intervals reduced = raw;
									for (Identifier b : p.getUpperBounds().getState((Identifier) s)) {
										Intervals ib = ie.getState(b);
										//if (ib != null && !ib.isBottom()) 
											reduced = reduced.glb(new Intervals(MathNumber.MINUS_INFINITY, ib.interval.getHigh()));
										tool.warnOn(div, "Reduced bounds are now: " + reduced.interval.getLow() + " to " + reduced.interval.getHigh());
									}
									finalInterval = reduced;
								}
								else { 
									finalInterval = raw;
								}
							}

							if (finalInterval != null && finalInterval.interval != null) {
								if (finalInterval.isNonBottomSingletonWithValue(0)) {
									tool.warnOn(div, "[DEFINITE] Division by zero detected: divisor " + s + " is exactly 0");
									return;
								} else if (finalInterval.interval.intersects(Interval.ZERO)) {
									MathNumber lo = finalInterval.interval.getLow();
									MathNumber hi = finalInterval.interval.getHigh();
									tool.warnOn(div, "[POSSIBLE] Division by zero: divisor " + s 
										+ " may be 0 (in [" + lo + ", " + hi + "])");
									return;
								}
							}
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