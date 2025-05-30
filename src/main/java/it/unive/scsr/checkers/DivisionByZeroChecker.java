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
import it.unive.lisa.type.NumericType;
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

							// TODO: implement type checks, it is required a numerical type
							if (!isNumericalType(types) || !isAnyTypeCompatible(types, size))
								continue;

							String varName = s.toString(); // fallback default
			
							ValueEnvironment<Intervals> valueState = state.getState().getValueState();
							
							Intervals intervalAbstractValue = valueState.eval((ValueExpression) s, div, state.getState());
							
							// TODO: add checks for division by zero
							if (intervalAbstractValue != null && !intervalAbstractValue.isBottom() && intervalAbstractValue.interval != null) {
								MathNumber low = intervalAbstractValue.interval.getLow();
								MathNumber high = intervalAbstractValue.interval.getHigh();

								if (intervalAbstractValue.interval.is(0) || intervalAbstractValue.isZero()) {
									String msg = "DivByZero Checker for size: " + size + " generated: DEFINITE division by zero ERROR on variable '" + varName + "': divisor has range [" + low + ", " + high + "]";
									tool.warnOn(div, msg);
									System.out.println(msg);
								} else if (low.leq(MathNumber.ZERO) && high.geq(MathNumber.ZERO)) {
									String msg = "DivByZero Checker for size: " + size + " generated: POSSIBLE division by zero ERROR on variable '" + varName + "': divisor has range [" + low + ", " + high + "]";
									tool.warnOn(div, msg);
									System.out.println(msg);
								}
							}
						}
					} catch (SemanticException e) {
						e.printStackTrace();
					}
			}
		}
	}

	private boolean isCompatibleWithSize(Type type, NumericalSize size) {
		if (type == null || type.isUntyped() || !(type instanceof NumericType))
			return false;

		NumericType numType = (NumericType) type;

		if (size == NumericalSize.FLOAT8 || size == NumericalSize.FLOAT16 || size == NumericalSize.FLOAT32)
			return !numType.isIntegral();

		if (size == NumericalSize.INT8 || size == NumericalSize.INT16 || size == NumericalSize.INT32
				|| size == NumericalSize.UINT8 || size == NumericalSize.UINT16 || size == NumericalSize.UINT32)
			return numType.isIntegral();

		return false;
	}

	private boolean isAnyTypeCompatible(Set<Type> types, NumericalSize size) {
		for (Type t : types)
			if (isCompatibleWithSize(t, size))
				return true;
		return false;
	}

	private boolean isNumericalType(Set<Type> types) {
		for (Type t : types)
			if (t.isNumericType())
				return true;
		return false;
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