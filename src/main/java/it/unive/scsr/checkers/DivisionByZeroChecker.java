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
import it.unive.lisa.util.numeric.MathNumber;
import it.unive.scsr.Intervals;
import it.unive.scsr.Pentagons;
import it.unive.scsr.UpperBounds;
import it.unive.scsr.checkers.OverflowChecker.NumericalSize;

public class DivisionByZeroChecker implements SemanticCheck<SimpleAbstractState<PointBasedHeap, Pentagons, TypeEnvironment<InferredTypes>>> {

	private NumericalSize size;
	private Set<String> warnedLocations;

	public DivisionByZeroChecker(NumericalSize size) {
		this.size = size;
		this.warnedLocations = new HashSet<>();
	}

	@Override
	public boolean visit(
			CheckToolWithAnalysisResults<SimpleAbstractState<PointBasedHeap, Pentagons, TypeEnvironment<InferredTypes>>> tool,
			CFG graph, Statement node) {

		if (node instanceof Division)
			checkDivision(tool, graph, (Division) node);

		return true;
	}

	private void checkDivision(
			CheckToolWithAnalysisResults<SimpleAbstractState<PointBasedHeap, Pentagons, TypeEnvironment<InferredTypes>>> tool,
			CFG graph, Division div) {

		for (var result : tool.getResultOf(graph)) {
			var state = result.getAnalysisStateAfter(div.getRight());

			Set<SymbolicExpression> reachableIds = new HashSet<>();
			Iterator<SymbolicExpression> comExprIterator = state.getComputedExpressions().iterator();
			if (comExprIterator.hasNext()) {
				SymbolicExpression divisor = comExprIterator.next();
				try {
					reachableIds
							.addAll(state.getState().reachableFrom(divisor, div, state.getState()).elements);

					for (SymbolicExpression s : reachableIds) {
						Type staticType = s.getStaticType();
						Set<Type> dynamicTypes = getPossibleDynamicTypes(s, div, state.getState());

						// TODO: implement type checks, it is required a numerical type

						if (!staticType.isNumericType() && !staticType.isUntyped())
							return;

						if (staticType.isUntyped()) {
							boolean found = false;
							for (Type type : dynamicTypes) {
								if (type.isNumericType()) {
									found = true;
									break;
								}
							}
							if (!found)
								return;
						}

						Pentagons valueState = state.getState().getValueState();

						Intervals intervalAbstractValue = valueState.getIntervals().eval((ValueExpression) s, div,
								state.getState());
						UpperBounds upperboundsAbstractValue = valueState.getUpperbounds().eval((ValueExpression) s,
								div, state.getState());

						// TODO: add checks for division by zero

						if (intervalAbstractValue.isBottom())
							continue;

						MathNumber low = intervalAbstractValue.interval.getLow();
						MathNumber high = intervalAbstractValue.interval.getHigh();

						// TODO: Check if the interval contains zero
						if (intervalAbstractValue.interval.includes(Intervals.ZERO.interval)) {
							String locationKey = div.getLocation().toString();

							// TODO: If it contains zero, check if it's exactly zero or possibly zero
							if (intervalAbstractValue.equals(Intervals.ZERO)) {
								// Divisor is exactly zero
								if (!warnedLocations.contains(locationKey + ":CRITICAL")) {
									warnedLocations.add(locationKey + ":CRITICAL");

									tool.warn(String.format(
											"Division by zero detected at %s. The divisor is exactly zero: [%s, %s]",
											div.getLocation(),
											low,
											high));
								}
							} else {
								// Divisor may include zero
								// Check if negative upper bound analysis can help
								if (!hasNegativeUpperBound(valueState, upperboundsAbstractValue)) {
									// No negative upper bound found
									if (!warnedLocations.contains(locationKey + ":WARNING")) {
										warnedLocations.add(locationKey + ":WARNING");

										tool.warn(String.format(
												"Possible division by zero detected at %s with interval [%s, %s]",
												div.getLocation(),
												low,
												high));
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

	private boolean hasNegativeUpperBoundAux(Pentagons pentagons, UpperBounds upperBounds, Set<Identifier> seen) {
		if (upperBounds.isBottom())
			return false;

		for (var id : upperBounds) {
			if (seen.contains(id)) {
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
											  SimpleAbstractState<PointBasedHeap, Pentagons, TypeEnvironment<InferredTypes>> state)
			throws SemanticException {

		Set<Type> possibleDynamicTypes = new HashSet<>();
		Type dynamicTypes = state.getDynamicTypeOf(s, div, state);
		if (dynamicTypes != null && !dynamicTypes.isUntyped()) {
			possibleDynamicTypes.add(dynamicTypes);
		} else if (dynamicTypes.isUntyped()) {
			Set<Type> runtimeTypes = state.getRuntimeTypesOf(s, div, state);
			if (runtimeTypes.stream().anyMatch(t -> t != Untyped.INSTANCE))
				for (Type t : runtimeTypes)
					possibleDynamicTypes.add(t);
		}

		return possibleDynamicTypes;

	}

}