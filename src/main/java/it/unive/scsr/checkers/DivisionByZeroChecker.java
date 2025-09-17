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
import it.unive.lisa.analysis.value.ValueDomain;
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
import it.unive.scsr.Pentagons;
import it.unive.scsr.checkers.OverflowChecker.NumericalSize;

public class DivisionByZeroChecker<V extends ValueDomain<V>> implements
SemanticCheck<
		SimpleAbstractState<PointBasedHeap, V, TypeEnvironment<InferredTypes>>> {
	
	
	private NumericalSize size;
	
	public DivisionByZeroChecker(NumericalSize size) {
		this.size = size;
	}

	@Override
	public boolean visit(
			CheckToolWithAnalysisResults<SimpleAbstractState<PointBasedHeap, V, TypeEnvironment<InferredTypes>>> tool,
			CFG graph, Statement node) {
		
		if( node instanceof Division)
			checkDivision(tool, graph, (Division) node);

		
		return true;
		
	}

	private void checkDivision(
			CheckToolWithAnalysisResults<SimpleAbstractState<PointBasedHeap, V, TypeEnvironment<InferredTypes>>> tool,
			CFG graph, Division div) {

		for (AnalyzedCFG<SimpleAbstractState<PointBasedHeap, V,
				TypeEnvironment<InferredTypes>>> result : tool.getResultOf(graph)) {
			AnalysisState<
			SimpleAbstractState<PointBasedHeap, V,
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
						
			
							boolean isNumerical = types.stream().anyMatch(t -> 
								t.toString().contains("int") || 
								t.toString().contains("float") || 
								t.toString().contains("double") ||
								t.toString().contains("number"));
							
							if (!isNumerical && !types.isEmpty())
								continue;
			
							Intervals intervalAbstractValue = extractAndEvalIntervals(state.getState().getValueState(), (ValueExpression) s, div, state.getState());
							
							if (intervalAbstractValue != null && !intervalAbstractValue.isBottom()) {
								if (intervalAbstractValue.interval != null && 
									intervalAbstractValue.interval.getLow().compareTo(it.unive.lisa.util.numeric.MathNumber.ZERO) <= 0 &&
									intervalAbstractValue.interval.getHigh().compareTo(it.unive.lisa.util.numeric.MathNumber.ZERO) >= 0) {
									tool.warnOn(div, "Possible division by zero");
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
			SimpleAbstractState<PointBasedHeap, V, TypeEnvironment<InferredTypes>> state) throws SemanticException {
		
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

	@SuppressWarnings("unchecked")
	private Intervals extractAndEvalIntervals(V valueState, ValueExpression expr, Division div, SimpleAbstractState<PointBasedHeap, V, TypeEnvironment<InferredTypes>> state) throws SemanticException {
		if (valueState instanceof ValueEnvironment) {
			ValueEnvironment<Intervals> env = (ValueEnvironment<Intervals>) valueState;
			return env.eval(expr, div, state);
		} else if (valueState instanceof Pentagons) {
			Pentagons pentagons = (Pentagons) valueState;
			return pentagons.getIntervals().eval(expr, div, state);
		}
		return new Intervals().bottom();
	}

}