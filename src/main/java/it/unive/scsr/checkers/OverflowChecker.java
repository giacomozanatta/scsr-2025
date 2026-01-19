package it.unive.scsr.checkers;

import java.util.HashSet;
import java.util.Set;

import it.unive.lisa.analysis.AnalyzedCFG;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SimpleAbstractState;
import it.unive.lisa.analysis.heap.pointbased.PointBasedHeap;
import it.unive.lisa.analysis.nonrelational.value.TypeEnvironment;
import it.unive.lisa.analysis.types.InferredTypes;
import it.unive.lisa.checks.semantic.CheckToolWithAnalysisResults;
import it.unive.lisa.checks.semantic.SemanticCheck;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.statement.Assignment;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.VariableRef;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;
import it.unive.lisa.util.numeric.MathNumber;
import it.unive.scsr.Intervals;
import it.unive.scsr.Pentagons;
import it.unive.scsr.UpperBounds;

public class OverflowChecker implements SemanticCheck <SimpleAbstractState<PointBasedHeap, Pentagons, TypeEnvironment<InferredTypes>>> {

public enum NumericalSize {
	INT8,   // signed integer 8-bit: -128 to 127
	INT16,  // signed integer 16-bit: -32768 to 32767
	INT32,  // signed integer 32-bit: -2147483648 to 2147483647
	UINT8,  // unsigned integer 8-bit: 0 to 255
	UINT16, // unsigned integer 16-bit: 0 to 65535
	UINT32, // unsigned integer 32-bit: 0 to 4294967295
	FLOAT8, // signed float 8-bit (simplified)
	FLOAT16, // signed float 16-bit
	FLOAT32, // signed float 32-bit
}

private NumericalSize size;
private Set<String> warnedLocations; // Track warned locations to avoid duplicates

public OverflowChecker(NumericalSize size) {
	this.size = size;
	this.warnedLocations = new HashSet<>();
}

@Override
public boolean visit(
		CheckToolWithAnalysisResults<SimpleAbstractState<PointBasedHeap, Pentagons, TypeEnvironment<InferredTypes>>> tool,
		CFG graph, Statement node) {

	if (node instanceof Assignment) {
		Assignment assignment = (Assignment) node;
		Expression leftExpression = assignment.getLeft();

		// Checking if each variable reference is over/under-flowing
		if (leftExpression instanceof VariableRef) {
			checkVariableRef(tool, (VariableRef) leftExpression, graph, node);
		}

	} else {
		// Checking if each variable reference is over/under-flowing
		if (node instanceof VariableRef) {
			checkVariableRef(tool, (VariableRef) node, graph, node);
		}
	}

	return true;
}

private void checkVariableRef(
		CheckToolWithAnalysisResults<SimpleAbstractState<PointBasedHeap, Pentagons, TypeEnvironment<InferredTypes>>> tool,
		VariableRef varRef, CFG graph, Statement node) {
	Variable id = new Variable(varRef.getStaticType(), varRef.getName(), varRef.getLocation());

	Type staticType = id.getStaticType();
	Set<Type> dynamicTypes = getPossibleDynamicTypes(tool, graph, node, id, varRef);

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

	for (var result : tool.getResultOf(graph)) {
		var state = result.getAnalysisStateAfter(node).getState();
		Pentagons pentagonsValueState = state.getValueState();
		Intervals intervalAbstractValue = pentagonsValueState.getIntervals().getState(id);
		UpperBounds upperboundsAbstractValue = pentagonsValueState.getUpperbounds().getState(id);

		// TODO: implement overflow/underflow check using intervals and bounds from NumericalSize
		// We can't check a bottom value
		if (intervalAbstractValue.isBottom())
			continue;

		// TODO: Get the bounds for the current NumericalSize
		Bounds bounds = getBoundsForSize(size);

		MathNumber low = intervalAbstractValue.interval.getLow();
		MathNumber high = intervalAbstractValue.interval.getHigh();

		// Skip completely unbounded intervals (no useful information)
		if (low.isMinusInfinity() && high.isPlusInfinity()) {
			continue;
		}

		// Create unique key for this location and variable to avoid duplicate warnings
		String locationKey = node.getLocation().toString() + ":" + varRef.getName();

		// TODO: Check if the interval exceeds the upper bound (overflow)
		if (high.compareTo(bounds.upper) > 0) {
			String overflowKey = locationKey + ":OVERFLOW";

			if (!warnedLocations.contains(overflowKey)) {
				warnedLocations.add(overflowKey);

				tool.warn(String.format(
						"Potential overflow detected for variable '%s' at %s. " +
								"Interval: %s, Max allowed: %s (type: %s), Upper bounds: %s",
						varRef.getName(),
						node.getLocation(),
						intervalAbstractValue.representation(),
						bounds.upper,
						size,
						upperboundsAbstractValue.representation()));
			}
		}

		// TODO: Check if the interval goes below the lower bound (underflow)
		if (low.compareTo(bounds.lower) < 0) {
			String underflowKey = locationKey + ":UNDERFLOW";

			if (!warnedLocations.contains(underflowKey)) {
				warnedLocations.add(underflowKey);

				tool.warn(String.format(
						"Potential underflow detected for variable '%s' at %s. " +
								"Interval: %s, Min allowed: %s (type: %s), Upper bounds: %s",
						varRef.getName(),
						node.getLocation(),
						intervalAbstractValue.representation(),
						bounds.lower,
						size,
						upperboundsAbstractValue.representation()));
			}
		}

		// TODO: Report warnings using tool.warn() or tool.warnOn()
		// (Already implemented above in the overflow/underflow checks)
	}
}

// Helper class to store bounds
private static class Bounds {
	MathNumber lower;
	MathNumber upper;

	Bounds(long lower, long upper) {
		this.lower = new MathNumber(lower);
		this.upper = new MathNumber(upper);
	}

	Bounds(MathNumber lower, MathNumber upper) {
		this.lower = lower;
		this.upper = upper;
	}
}

// Get bounds based on numerical size
private Bounds getBoundsForSize(NumericalSize size) {
	switch (size) {
		case INT8:
			return new Bounds(-128, 127);
		case INT16:
			return new Bounds(-32768, 32767);
		case INT32:
			return new Bounds(-2147483648L, 2147483647L);
		case UINT8:
			return new Bounds(0, 255);
		case UINT16:
			return new Bounds(0, 65535);
		case UINT32:
			return new Bounds(0, 4294967295L);
		case FLOAT8:
			// Simplified float bounds
			return new Bounds(-128, 127);
		case FLOAT16:
			// IEEE 754 half precision approximate range
			return new Bounds(-65504, 65504);
		case FLOAT32:
			// IEEE 754 single precision approximate range
			return new Bounds(new MathNumber(-3.4e38), new MathNumber(3.4e38));
		default:
			// Default to INT32 bounds
			return new Bounds(-2147483648L, 2147483647L);
	}
}

// compute possible dynamic types / runtime types
private Set<Type> getPossibleDynamicTypes(
		CheckToolWithAnalysisResults<SimpleAbstractState<PointBasedHeap, Pentagons, TypeEnvironment<InferredTypes>>> tool,
		CFG graph, Statement node, Variable id, VariableRef varRef) {

	Set<Type> possibleDynamicTypes = new HashSet<>();
	for (AnalyzedCFG<SimpleAbstractState<PointBasedHeap, Pentagons, TypeEnvironment<InferredTypes>>> result : tool
			.getResultOf(graph)) {
		SimpleAbstractState<PointBasedHeap, Pentagons, TypeEnvironment<InferredTypes>> state = result
				.getAnalysisStateAfter(varRef).getState();
		try {
			Type dynamicTypes = state.getDynamicTypeOf(id, varRef, state);
			if (dynamicTypes != null && !dynamicTypes.isUntyped()) {
				possibleDynamicTypes.add(dynamicTypes);
			} else if (dynamicTypes.isUntyped()) {
				Set<Type> runtimeTypes = state.getRuntimeTypesOf(id, varRef, state);
				if (runtimeTypes.stream().anyMatch(t -> t != Untyped.INSTANCE))
					for (Type t : runtimeTypes)
						possibleDynamicTypes.add(t);
			}
		} catch (SemanticException e) {
			System.err.println("Cannot check " + node);
			e.printStackTrace(System.err);
		}

	}
	return possibleDynamicTypes;
}
}