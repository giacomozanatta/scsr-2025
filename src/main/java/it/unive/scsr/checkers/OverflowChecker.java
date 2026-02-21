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
import it.unive.lisa.type.NumericType;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;
import it.unive.lisa.util.numeric.MathNumber;
import it.unive.scsr.Intervals;

public class OverflowChecker implements
		SemanticCheck<SimpleAbstractState<PointBasedHeap, ValueEnvironment<Intervals>, TypeEnvironment<InferredTypes>>> {

	// -------------------------------------------------------------------------
	// NumericalSize: min/max bounds for each supported type
	// -------------------------------------------------------------------------

	public enum NumericalSize {
		INT8   (true,  false, -128L,                    127L),
		INT16  (true,  false, -32768L,                  32767L),
		INT32  (true,  false, -2147483648L,              2147483647L),
		UINT8  (false, false, 0L,                        255L),
		UINT16 (false, false, 0L,                        65535L),
		UINT32 (false, false, 0L,                        4294967295L),
		// For floats we approximate with the integer range of their mantissa
		// so that the Intervals domain (which is integer-based) can still
		// flag obviously out-of-range values.
		FLOAT8 (true,  true,  -128L,                    127L),
		FLOAT16(true,  true,  -65504L,                  65504L),
		FLOAT32(true,  true,  -2147483648L,              2147483647L);

		public final boolean signed;
		public final boolean floatingPoint;
		public final long    min;
		public final long    max;

		NumericalSize(boolean signed, boolean floatingPoint, long min, long max) {
			this.signed        = signed;
			this.floatingPoint = floatingPoint;
			this.min           = min;
			this.max           = max;
		}

		public MathNumber minAsMathNumber() { return new MathNumber(min); }
		public MathNumber maxAsMathNumber() { return new MathNumber(max); }
	}

	// -------------------------------------------------------------------------

	private final NumericalSize size;

	public OverflowChecker(NumericalSize size) {
		this.size = size;
	}

	// -------------------------------------------------------------------------
	// SemanticCheck entry point
	// -------------------------------------------------------------------------

	@Override
	public boolean visit(
			CheckToolWithAnalysisResults<SimpleAbstractState<PointBasedHeap, ValueEnvironment<Intervals>, TypeEnvironment<InferredTypes>>> tool,
			CFG graph, Statement node) {

		if (node instanceof Assignment) {
			Expression left = ((Assignment) node).getLeft();
			if (left instanceof VariableRef)
				checkVariableRef(tool, (VariableRef) left, graph, node);

		} else if (node instanceof VariableRef) {
			checkVariableRef(tool, (VariableRef) node, graph, node);
		}

		return true;
	}

	// -------------------------------------------------------------------------
	// Core check logic
	// -------------------------------------------------------------------------

	private void checkVariableRef(
			CheckToolWithAnalysisResults<SimpleAbstractState<PointBasedHeap, ValueEnvironment<Intervals>, TypeEnvironment<InferredTypes>>> tool,
			VariableRef varRef, CFG graph, Statement node) {

		Variable id = new Variable(
				varRef.getStaticType(),
				varRef.getName(),
				varRef.getLocation());

		Type staticType = id.getStaticType();
		Set<Type> dynamicTypes = getPossibleDynamicTypes(tool, graph, node, id, varRef);

		// ---- TODO resolved: type check ----------------------------------------
		// We only proceed when the variable is of a numeric type.
		// If the static type is Untyped we fall back to dynamic types.
		boolean isNumeric = isNumericType(staticType);
		if (!isNumeric) {
			isNumeric = dynamicTypes.stream().anyMatch(this::isNumericType);
		}
		if (!isNumeric)
			return; // not a numerical variable – skip
		// -----------------------------------------------------------------------

		// The statement we want the post-state of is the assignment itself
		// (so we read the value *after* it has been assigned).
		Statement target = node;
		if (varRef.getParentStatement() instanceof Assignment
				&& ((Assignment) varRef.getParentStatement()).getLeft() == varRef) {
			target = varRef.getParentStatement();
		}

		for (AnalyzedCFG<SimpleAbstractState<PointBasedHeap, ValueEnvironment<Intervals>,
				TypeEnvironment<InferredTypes>>> result : tool.getResultOf(graph)) {

			SimpleAbstractState<PointBasedHeap, ValueEnvironment<Intervals>,
					TypeEnvironment<InferredTypes>> state =
					result.getAnalysisStateAfter(target).getState();

			Intervals iv = state.getValueState().getState(id);

			// ---- TODO resolved: overflow/underflow detection -------------------
			checkOverflowUnderflow(tool, target, iv);
			// -------------------------------------------------------------------
		}
	}

	// -------------------------------------------------------------------------
	// Overflow / underflow detection
	// -------------------------------------------------------------------------

	/**
	 * Compares the abstract interval {@code iv} against the valid range of
	 * {@link #size} and emits warnings when the bounds exceed the type limits
	 * (potential overflow) or go below the type minimum (potential underflow).
	 *
	 * <p>The check is sound but not complete: if the interval is TOP we emit a
	 * warning because the value <em>could</em> exceed the bounds; if the
	 * interval is entirely within the valid range we stay silent.</p>
	 */
	private void checkOverflowUnderflow(
			CheckToolWithAnalysisResults<SimpleAbstractState<PointBasedHeap, ValueEnvironment<Intervals>, TypeEnvironment<InferredTypes>>> tool,
			Statement target, Intervals iv) {

		if (iv == null || iv.isBottom())
			return; // unreachable code – nothing to warn about

		String prefix = "[OverflowChecker-" + size + "] ";

		// TOP means we have no information: the value may be anything,
		// so it might overflow or underflow.
		if (iv.isTop()) {
			tool.warnOn(target, prefix
					+ "Possible overflow/underflow: value is unbounded (TOP).");
			return;
		}

		MathNumber lo = iv.getLowerBound();
		MathNumber hi = iv.getUpperBound();

		if (lo == null || hi == null) {
			tool.warnOn(target, prefix
					+ "Possible overflow/underflow: cannot determine value bounds.");
			return;
		}

		MathNumber typeMin = size.minAsMathNumber();
		MathNumber typeMax = size.maxAsMathNumber();

		// Overflow:  upper bound exceeds the type maximum
		if (!hi.isInfinite() && hi.compareTo(typeMax) > 0) {
			tool.warnOn(target, prefix
					+ "Possible overflow: upper bound " + hi
					+ " exceeds " + size + " maximum (" + size.max + ").");
		} else if (hi.isPlusInfinity()) {
			// Infinite upper bound – could overflow
			tool.warnOn(target, prefix
					+ "Possible overflow: upper bound is +Inf, may exceed "
					+ size + " maximum (" + size.max + ").");
		}

		// Underflow: lower bound is below the type minimum
		if (!lo.isInfinite() && lo.compareTo(typeMin) < 0) {
			tool.warnOn(target, prefix
					+ "Possible underflow: lower bound " + lo
					+ " is below " + size + " minimum (" + size.min + ").");
		} else if (lo.isMinusInfinity()) {
			// Infinite lower bound – could underflow
			tool.warnOn(target, prefix
					+ "Possible underflow: lower bound is -Inf, may go below "
					+ size + " minimum (" + size.min + ").");
		}
	}

	// -------------------------------------------------------------------------
	// Type helpers
	// -------------------------------------------------------------------------

	/**
	 * Returns {@code true} if {@code t} represents a numeric type.
	 * We check both LiSA's {@link NumericType} marker interface and a
	 * name-based heuristic to cover all IMP/LiSA numeric type names.
	 */
	private boolean isNumericType(Type t) {
		if (t == null || t instanceof Untyped)
			return false;
		if (t instanceof NumericType)
			return true;
		String name = t.toString().toLowerCase();
		return name.contains("int")   || name.contains("uint")
				|| name.contains("float") || name.contains("double")
				|| name.contains("long")  || name.contains("short")
				|| name.contains("byte")  || name.contains("numeric");
	}

	/** Collects possible runtime types for a variable at a given program point. */
	private Set<Type> getPossibleDynamicTypes(
			CheckToolWithAnalysisResults<SimpleAbstractState<PointBasedHeap, ValueEnvironment<Intervals>, TypeEnvironment<InferredTypes>>> tool,
			CFG graph, Statement node, Variable id, VariableRef varRef) {

		Set<Type> possibleDynamicTypes = new HashSet<>();

		for (AnalyzedCFG<SimpleAbstractState<PointBasedHeap, ValueEnvironment<Intervals>,
				TypeEnvironment<InferredTypes>>> result : tool.getResultOf(graph)) {

			SimpleAbstractState<PointBasedHeap, ValueEnvironment<Intervals>,
					TypeEnvironment<InferredTypes>> state =
					result.getAnalysisStateAfter(varRef).getState();
			try {
				Type dynamic = state.getDynamicTypeOf(id, varRef, state);
				if (dynamic != null && !dynamic.isUntyped()) {
					possibleDynamicTypes.add(dynamic);
				} else if (dynamic != null && dynamic.isUntyped()) {
					Set<Type> runtime = state.getRuntimeTypesOf(id, varRef, state);
					runtime.stream()
							.filter(t -> t != Untyped.INSTANCE)
							.forEach(possibleDynamicTypes::add);
				}
			} catch (SemanticException e) {
				System.err.println("OverflowChecker: cannot check " + node);
				e.printStackTrace(System.err);
			}
		}

		return possibleDynamicTypes;
	}
}
