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
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;
import it.unive.lisa.util.numeric.MathNumber;
import it.unive.scsr.Intervals;
import it.unive.scsr.Pentagons;
import it.unive.scsr.UpperBounds;
import it.unive.scsr.checkers.OverflowChecker.NumericalSize;

public class DivisionByZeroChecker implements SemanticCheck <SimpleAbstractState<PointBasedHeap, Pentagons, TypeEnvironment<InferredTypes>>> {

private NumericalSize size;
private Set<String> warnedLocations; // Track warned locations to avoid duplicates

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
		// Takes the result of the expression on the divisor, like in expr1 / expr2 it takes expr2
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
						if (!found)
							return;
					}

					Pentagons valueState = state.getState().getValueState();

					Intervals intervalAbstractValue = valueState.getIntervals().eval((ValueExpression) s, div,
							state.getState());
					UpperBounds upperboundsAbstractValue = valueState.getUpperbounds().eval((ValueExpression) s,
							div, state.getState());

					// TODO: implement division by zero check using intervals
					// We can't check a bottom value
					if (intervalAbstractValue.isBottom())
						continue;

					// Skip completely unbounded intervals (no useful information)
					MathNumber low = intervalAbstractValue.interval.getLow();
					MathNumber high = intervalAbstractValue.interval.getHigh();

					if (low.isMinusInfinity() && high.isPlusInfinity()) {
						continue;
					}

					// TODO: Check if the interval contains zero (interval.includes(Intervals.ZERO.interval))
					if (intervalAbstractValue.interval.includes(Intervals.ZERO.interval)) {
						String locationKey = div.getLocation().toString();

						// Avoid duplicate warnings
						if (!warnedLocations.contains(locationKey)) {
							warnedLocations.add(locationKey);

							// TODO: If it contains zero, check if it's exactly zero or possibly zero
							if (intervalAbstractValue.equals(Intervals.ZERO)) {
								// Divisor is exactly zero - CRITICAL
								tool.warn(String.format(
										"[CRITICAL] Division by zero detected at %s. " +
												"The divisor '%s' is exactly zero: %s, Upper bounds: %s",
										div.getLocation(),
										divisor,
										intervalAbstractValue.representation(),
										upperboundsAbstractValue.representation()));
							} else {
								// Divisor may include zero - WARNING
								tool.warn(String.format(
										"[WARNING] Possible division by zero detected at %s. " +
												"The divisor '%s' may include zero: %s, Upper bounds: %s",
										div.getLocation(),
										divisor,
										intervalAbstractValue.representation(),
										upperboundsAbstractValue.representation()));
							}
						}
					}

					// TODO: Report appropriate warnings using tool.warn()
					// (Already implemented above)
				}
			} catch (SemanticException e) {
				e.printStackTrace();
			}

		}
	}

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