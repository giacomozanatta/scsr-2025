package it.unive.scsr.checkers;

import java.util.HashSet;
import java.util.Set;

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
import it.unive.lisa.program.cfg.statement.Assignment;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.VariableRef;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;
import it.unive.lisa.util.numeric.MathNumber;
import it.unive.scsr.Intervals;

public class OverflowChecker implements
		SemanticCheck<SimpleAbstractState<PointBasedHeap, ValueEnvironment<Intervals>, TypeEnvironment<InferredTypes>>> {

	public enum NumericalSize {
		INT8, // signed integer 8-bit
		INT16, // signed integer 16-bit
		INT32, // signed integer 32-bit
		UINT8, // unsigned integer 8-bit
		UINT16, // unsigned integer 16-bit
		UINT32, // unsigned integer 32-bit
		FLOAT8, // signed float 8-bit
		FLOAT16, // signed float 16-bit
		FLOAT32, // signed float 32-bit
	}

	private NumericalSize size;

	public OverflowChecker(NumericalSize size) {
		this.size = size;
	}

	@Override
	public boolean visit(
			CheckToolWithAnalysisResults<SimpleAbstractState<PointBasedHeap, ValueEnvironment<Intervals>, TypeEnvironment<InferredTypes>>> tool,
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
			CheckToolWithAnalysisResults<SimpleAbstractState<PointBasedHeap, ValueEnvironment<Intervals>, TypeEnvironment<InferredTypes>>> tool,
			VariableRef varRef, CFG graph, Statement node) {
		// The 'id' here is a symbolic representation used for querying the state and
		// for information in warnings.
		Variable id = new Variable(varRef.getStaticType(), varRef.getName(), varRef.getLocation());

		Type staticType = id.getStaticType();
		Set<Type> dynamicTypes = getPossibleDynamicTypes(tool, graph, node, id, varRef);

		Statement target = node; // Default target for state lookup

		// --- TODO: implement type checks, it is required a numerical type ---
		// hint: if staticType.isUntyped() == true, then should be checked possible
		// dynamic types
		boolean isPotentiallyNumeric = false;
		if (staticType.isNumericType()) {
			isPotentiallyNumeric = true;
		} else if (staticType.isUntyped() && dynamicTypes != null) { // Check dynamic types if static is untyped
			for (Type dt : dynamicTypes) {
				if (dt.isNumericType()) {
					isPotentiallyNumeric = true;
					break;
				}
			}
		}

		if (!isPotentiallyNumeric) {
			return; // Not a numeric type, skip overflow check for this variable ref
		}
		// --- End of type check ---

		// If varRef is the left-hand side of an assignment, we are interested in the
		// state *after* the assignment.
		if (varRef.getParentStatement() instanceof Assignment
				&& ((Assignment) varRef.getParentStatement()).getLeft() == varRef) {
			target = varRef.getParentStatement();
		}

		for (AnalyzedCFG<SimpleAbstractState<PointBasedHeap, ValueEnvironment<Intervals>, TypeEnvironment<InferredTypes>>> result : tool
				.getResultOf(graph)) {
			SimpleAbstractState<PointBasedHeap, ValueEnvironment<Intervals>, TypeEnvironment<InferredTypes>> state = result
					.getAnalysisStateAfter(target).getState();
			// Use the 'id' (symbolic variable) to get its abstract value from the state
			Intervals intervalAbstractValue = state.getValueState().getState(id);

			// --- TODO: implement logic for overflow/underflow checks ---
			// hint: it depends to the NumericalSize size
			if (intervalAbstractValue == null || intervalAbstractValue.isBottom()) {
				continue; // No value determined or state is bottom (unreachable), skip
			}

			long minBound = 0;
			long maxBound = 0;
			boolean isIntegerTypeForBoundCheck = false; // Flag to indicate if current 'this.size' is an integer type we
														// check bounds for

			switch (this.size) {
				case INT8:
					minBound = Byte.MIN_VALUE;
					maxBound = Byte.MAX_VALUE;
					isIntegerTypeForBoundCheck = true;
					break;
				case INT16:
					minBound = Short.MIN_VALUE;
					maxBound = Short.MAX_VALUE;
					isIntegerTypeForBoundCheck = true;
					break;
				case INT32:
					minBound = Integer.MIN_VALUE;
					maxBound = Integer.MAX_VALUE;
					isIntegerTypeForBoundCheck = true;
					break;
				case UINT8:
					minBound = 0;
					maxBound = 255;
					isIntegerTypeForBoundCheck = true;
					break;
				case UINT16:
					minBound = 0;
					maxBound = 65535;
					isIntegerTypeForBoundCheck = true;
					break;
				case UINT32:
					minBound = 0;
					maxBound = 4294967295L;
					isIntegerTypeForBoundCheck = true;
					break;
				case FLOAT8:
				case FLOAT16:
				case FLOAT32:
					isIntegerTypeForBoundCheck = false; // Floats handled differently
					break;
				default:
					// Unknown or unhandled NumericalSize, skip checks for this iteration
					continue;
			}

			// If the interval is TOP, it means [-inf, +inf], which is a potential
			// overflow/underflow for any fixed-size type.
			if (intervalAbstractValue.isTop()) {
				tool.warnOn(node, "Potential overflow/underflow for variable '" + id.getName() +
						"' (type " + this.size + "): value is TOP (unconstrained).");
				continue;
			}

			// Ensure interval field is accessible if not top or bottom.
			if (intervalAbstractValue.interval == null) {
				// This state is unexpected for non-top/non-bottom Intervals.
				System.err.println(
						"Internal Warning: intervalAbstractValue.interval is null for non-bottom/non-top state: "
								+ intervalAbstractValue.representation() + " for variable " + id.getName() + " at "
								+ node.getLocation());
				continue;
			}

			MathNumber low = intervalAbstractValue.interval.getLow();
			MathNumber high = intervalAbstractValue.interval.getHigh();

			if (isIntegerTypeForBoundCheck) {
				// Check for underflow against the integer type's minimum bound
				if (low.isMinusInfinity() || (low.isFinite() && low.compareTo(new MathNumber(minBound)) < 0)) {
					tool.warnOn(node, "Potential underflow for variable '" + id.getName() +
							"' (type " + this.size + "): value can be " + low +
							", which is less than min bound " + minBound + ".");
				}

				// Check for overflow against the integer type's maximum bound
				if (high.isPlusInfinity() || (high.isFinite() && high.compareTo(new MathNumber(maxBound)) > 0)) {
					tool.warnOn(node, "Potential overflow for variable '" + id.getName() +
							"' (type " + this.size + "): value can be " + high +
							", which is greater than max bound " + maxBound + ".");
				}
			} else { // Handling for FLOAT8, FLOAT16, FLOAT32
				// isTop() already handled. Interval is not [-inf, +inf].
				// Check if either bound is +/- Infinity.
				if (low.isMinusInfinity()) {
					tool.warnOn(node, "Potential underflow for float variable '" + id.getName() +
							"' (type " + this.size + "): value interval includes -Infinity.");
				} else if (high.isPlusInfinity()) {
					tool.warnOn(node, "Potential overflow for float variable '" + id.getName() +
							"' (type " + this.size + "): value interval includes +Infinity.");
				}
				// Note: No checks against specific finite min/max for FLOAT8/16/32 are
				// implemented here,
				// as those bounds are not standard Java types and would need explicit
				// definition.
			}
			// --- End of overflow/underflow logic ---
		}

	}

	// compute possible dynamic types / runtime types
	private Set<Type> getPossibleDynamicTypes(
			CheckToolWithAnalysisResults<SimpleAbstractState<PointBasedHeap, ValueEnvironment<Intervals>, TypeEnvironment<InferredTypes>>> tool,
			CFG graph, Statement node, Variable id, VariableRef varRef) {

		Set<Type> possibleDynamicTypes = new HashSet<>();
		for (AnalyzedCFG<SimpleAbstractState<PointBasedHeap, ValueEnvironment<Intervals>, TypeEnvironment<InferredTypes>>> result : tool
				.getResultOf(graph)) {
			// State should be queried at the point of the variable reference itself for
			// dynamic type info
			SimpleAbstractState<PointBasedHeap, ValueEnvironment<Intervals>, TypeEnvironment<InferredTypes>> state = result
					.getAnalysisStateAfter(varRef).getState();
			try {
				// The signature for getDynamicTypeOf is (Identifier, ProgramPoint,
				// SemanticOracle)
				// or (ValueExpression, ProgramPoint, SemanticOracle)
				// 'id' is a Variable (which is an Identifier). 'varRef' is the ProgramPoint.
				Type dynamicType = state.getDynamicTypeOf(id, varRef, state); // Assuming 'state' can serve as
																				// SemanticOracle here
				if (dynamicType != null && !dynamicType.isUntyped()) {
					possibleDynamicTypes.add(dynamicType);
				} else if (dynamicType != null && dynamicType.isUntyped()) { // Check if dynamicType is non-null before
																				// calling isUntyped
					// The signature for getRuntimeTypesOf is (Identifier, ProgramPoint,
					// SemanticOracle)
					Set<Type> runtimeTypes = state.getRuntimeTypesOf(id, varRef, state); // Assuming 'state' can serve
																							// as SemanticOracle
					if (runtimeTypes != null
							&& runtimeTypes.stream().anyMatch(t -> t != Untyped.INSTANCE && !t.isUntyped())) { // ensure
																												// not
																												// untyped
						for (Type t : runtimeTypes) {
							if (t != Untyped.INSTANCE && !t.isUntyped()) { // Double check, stream().anyMatch already
																			// filters
								possibleDynamicTypes.add(t);
							}
						}
					}
				}
			} catch (SemanticException e) {
				System.err.println("Cannot determine dynamic/runtime types for " + id.getName() + " at "
						+ node.getLocation() + ": " + e.getMessage());
				// e.printStackTrace(System.err); // Optionally print stack trace
			}

		}
		return possibleDynamicTypes;
	}

}