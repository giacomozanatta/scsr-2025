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
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;
import it.unive.lisa.util.numeric.MathNumber;
import it.unive.scsr.DoubleInterval;
import it.unive.scsr.Intervals;
import it.unive.scsr.Pentagons;
import it.unive.scsr.UpperBounds;

public class OverflowChecker implements SemanticCheck<SimpleAbstractState<PointBasedHeap, Pentagons, TypeEnvironment<InferredTypes>>> {

	// Each size carries its bounds
	public enum NumericalSize {
		INT8(new DoubleInterval(Byte.MIN_VALUE, Byte.MAX_VALUE)),
		INT16(new DoubleInterval(Short.MIN_VALUE, Short.MAX_VALUE)),
		INT32(new DoubleInterval(Integer.MIN_VALUE, Integer.MAX_VALUE)),
		UINT8(new DoubleInterval(0., Byte.MAX_VALUE * 2 + 1)),
		UINT16(new DoubleInterval(0., Short.MAX_VALUE * 2 + 1)),
		UINT32(new DoubleInterval(0., ((long) Integer.MAX_VALUE) * 2L + 1L)),
		FLOAT8(new DoubleInterval(-240., 240.)),
		FLOAT16(new DoubleInterval(-65504., 65504.)),
		FLOAT32(new DoubleInterval(-Float.MAX_VALUE, Float.MAX_VALUE));

		private final DoubleInterval bounds;

		NumericalSize(DoubleInterval bounds) {
			this.bounds = bounds;
		}

		public DoubleInterval getBounds() {
			return bounds;
		}
	}

	private NumericalSize size;
	private Set<String> warnedLocations;

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

			if (leftExpression instanceof VariableRef) {
				checkVariableRef(tool, (VariableRef) leftExpression, graph, node);
			}

		} else {
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

		for (var result : tool.getResultOf(graph)) {
			var state = result.getAnalysisStateAfter(node).getState();
			Pentagons pentagonsValueState = state.getValueState();
			Intervals intervalAbstractValue = pentagonsValueState.getIntervals().getState(id);

			// TODO: implement logic for overflow/underflow checks


			if (intervalAbstractValue.isBottom())
				continue;

			MathNumber low = intervalAbstractValue.interval.getLow();
			MathNumber high = intervalAbstractValue.interval.getHigh();

			String locationKey = node.getLocation().toString() + ":" + varRef.getName();

			// TODO: Get the bounds for the current NumericalSize
			MathNumber boundsLow = size.bounds.getLow();
			MathNumber boundsHigh = size.bounds.getHigh();

			// TODO: Check if the interval exceeds the upper bound (overflow)
			// Check if both bounds are finite
			if (high.isFinite() && low.isFinite()) {
				if (low.compareTo(boundsLow) < 0 || high.compareTo(boundsHigh) > 0) {
					String certainKey = locationKey + ":CERTAIN";

					if (!warnedLocations.contains(certainKey)) {
						warnedLocations.add(certainKey);

						tool.warn(String.format(
								"%s overflow detected at %s with interval [%s, %s]",
								size,
								node.getLocation(),
								low,
								high));
					}
				}
				continue;
			}

			// At least one bound is infinite - check upper and lower bounds
			Identifier inBoundsUpperBound = getInBoundsUpperBound(pentagonsValueState, id);
			Identifier inBoundsLowerBound = getInBoundsLowerBound(pentagonsValueState, id);

			if (inBoundsUpperBound != null && inBoundsLowerBound != null) {
				// Found in-bounds upper and lower bounds - false positive from widening
				continue;
			} else {
				// Check for possible overflow/underflow
				boolean possibleOverflow = false;
				boolean possibleUnderflow = false;

				if (high.isPlusInfinity() || (!high.isPlusInfinity() && high.compareTo(boundsHigh) > 0)) {
					possibleOverflow = true;
				}

				// TODO: Check if the interval goes below the lower bound (underflow)
				if (low.isMinusInfinity() || (!low.isMinusInfinity() && low.compareTo(boundsLow) < 0)) {
					possibleUnderflow = true;
				}

				// TODO: Report warnings using tool.warn() or tool.warnOn()
				if (possibleOverflow) {
					String overflowKey = locationKey + ":OVERFLOW";

					if (!warnedLocations.contains(overflowKey)) {
						warnedLocations.add(overflowKey);

						tool.warn(String.format(
								"%s overflow detected at %s with interval [%s, %s]",
								size,
								node.getLocation(),
								low,
								high));
					}
				}

				if (possibleUnderflow) {
					String underflowKey = locationKey + ":UNDERFLOW";

					if (!warnedLocations.contains(underflowKey)) {
						warnedLocations.add(underflowKey);

						tool.warn(String.format(
								"%s underflow detected at %s with interval [%s, %s]",
								size,
								node.getLocation(),
								low,
								high));
					}
				}
			}
		}
	}

	// method to find an in-bounds upper bound in the bound chain
	private Identifier getInBoundsUpperBound(Pentagons valueState, Identifier id) {
		return getInBoundsUpperBoundAux(valueState, id, new HashSet<>());
	}

	private Identifier getInBoundsUpperBoundAux(Pentagons valueState, Identifier identifier, Set<Identifier> seen) {
		UpperBounds upperboundsAbstractValue = valueState.getUpperbounds().getState(identifier);

		for (var ubId : upperboundsAbstractValue) {
			if (seen.contains(ubId)) {
				continue;
			}
			seen.add(ubId);

			Intervals ubInterval = valueState.getIntervals().getState(ubId);
			var high = ubInterval.interval.getHigh();
			if (size.bounds.includes(new DoubleInterval(high, high)))
				return ubId;

			var boundId = getInBoundsUpperBoundAux(valueState, ubId, seen);
			if (boundId != null)
				return boundId;
		}
		return null;
	}

	// method to find an in-bounds lower bound in the bound chain
	private Identifier getInBoundsLowerBound(Pentagons valueState, Identifier id) {
		return getInBoundsLowerBoundAux(valueState, id, new HashSet<>());
	}

	private Identifier getInBoundsLowerBoundAux(Pentagons valueState, Identifier id, Set<Identifier> seen) {
		for (var lowerBound : valueState.getUpperbounds()) {
			if (seen.contains(lowerBound.getKey())) {
				continue;
			}

			seen.add(lowerBound.getKey());
			if (lowerBound.getValue().contains(id)) {
				Intervals lbInterval = valueState.getIntervals().getState(lowerBound.getKey());
				DoubleInterval low = new DoubleInterval(lbInterval.interval.getLow(), lbInterval.interval.getLow());
				if (size.bounds.includes(low)) {
					return lowerBound.getKey();
				} else {
					var boundId = getInBoundsLowerBoundAux(valueState, lowerBound.getKey(), seen);
					if (boundId != null)
						return boundId;
				}
			}
		}

		return null;
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