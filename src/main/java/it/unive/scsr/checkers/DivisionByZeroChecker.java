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

/**
 * A semantic checker that warns whenever a division may have a zero divisor.
 *
 * <p>Strategy:
 * <ol>
 *   <li>Intercept every {@link Division} node in the CFG.</li>
 *   <li>Look up the abstract state <em>after</em> evaluating the right-hand
 *       operand (the divisor).</li>
 *   <li>Collect all symbolic expressions that could represent the divisor value
 *       (via {@code reachableFrom}).</li>
 *   <li>For each such expression that has a numeric type, evaluate it against
 *       the {@link Intervals} domain.</li>
 *   <li>Warn if 0 is contained in the resulting interval.</li>
 * </ol>
 */
public class DivisionByZeroChecker implements
		SemanticCheck<SimpleAbstractState<PointBasedHeap, ValueEnvironment<Intervals>, TypeEnvironment<InferredTypes>>> {

	private final NumericalSize size;

	public DivisionByZeroChecker(NumericalSize size) {
		this.size = size;
	}

	// -------------------------------------------------------------------------
	// SemanticCheck entry point
	// -------------------------------------------------------------------------

	@Override
	public boolean visit(
			CheckToolWithAnalysisResults<SimpleAbstractState<PointBasedHeap, ValueEnvironment<Intervals>, TypeEnvironment<InferredTypes>>> tool,
			CFG graph, Statement node) {

		if (node instanceof Division)
			checkDivision(tool, graph, (Division) node);

		return true;
	}

	// -------------------------------------------------------------------------
	// Division check
	// -------------------------------------------------------------------------

	private void checkDivision(
			CheckToolWithAnalysisResults<SimpleAbstractState<PointBasedHeap, ValueEnvironment<Intervals>, TypeEnvironment<InferredTypes>>> tool,
			CFG graph, Division div) {

		for (AnalyzedCFG<SimpleAbstractState<PointBasedHeap, ValueEnvironment<Intervals>,
				TypeEnvironment<InferredTypes>>> result : tool.getResultOf(graph)) {

			// We want the state *after* the right operand (the divisor) has been
			// computed, so we read getAnalysisStateAfter(div.getRight()).
			AnalysisState<SimpleAbstractState<PointBasedHeap, ValueEnvironment<Intervals>,
					TypeEnvironment<InferredTypes>>> state =
					result.getAnalysisStateAfter(div.getRight());

			// getComputedExpressions() holds the symbolic expressions produced
			// by evaluating the right operand.
			Set<SymbolicExpression> reachableIds = new HashSet<>();
			Iterator<SymbolicExpression> it = state.getComputedExpressions().iterator();

			if (!it.hasNext())
				continue;

			SymbolicExpression divisorExpr = it.next();

			try {
				// Expand to all memory locations reachable from the divisor
				// expression (important when the divisor is a pointer/reference).
				reachableIds.addAll(
						state.getState().reachableFrom(divisorExpr, div, state.getState()).elements);

				for (SymbolicExpression s : reachableIds) {
					// ---- TODO resolved: type check ----------------------------------
					// We only care about numeric divisors.
					Set<Type> dynamicTypes = getPossibleDynamicTypes(s, div, state.getState());
					if (!isNumericSymbolicExpression(s, dynamicTypes))
						continue;
					// -----------------------------------------------------------------

					// Evaluate the symbolic expression in the Intervals domain
					// to get an abstract value for the divisor.
					ValueEnvironment<Intervals> valueState = state.getState().getValueState();
					Intervals divisorInterval;
					try {
						divisorInterval = valueState.eval((ValueExpression) s, div, state.getState());
					} catch (ClassCastException cce) {
						// s is not a ValueExpression – skip
						continue;
					}

					// ---- TODO resolved: division-by-zero check ----------------------
					warnIfMayBeZero(tool, div, divisorInterval);
					// -----------------------------------------------------------------
				}
			} catch (SemanticException e) {
				e.printStackTrace();
			}
		}
	}

	// -------------------------------------------------------------------------
	// Division-by-zero detection
	// -------------------------------------------------------------------------

	/**
	 * Emits a warning on {@code div} if the abstract value {@code iv} of the
	 * divisor contains 0.
	 *
	 * <p>Cases:
	 * <ul>
	 *   <li><b>null / bottom</b> – unreachable, nothing to warn.</li>
	 *   <li><b>TOP</b> – the divisor is completely unknown; 0 is possible.</li>
	 *   <li><b>[lo, hi]</b> – 0 is possible iff {@code lo <= 0 <= hi}.</li>
	 * </ul>
	 */
	private void warnIfMayBeZero(
			CheckToolWithAnalysisResults<SimpleAbstractState<PointBasedHeap, ValueEnvironment<Intervals>, TypeEnvironment<InferredTypes>>> tool,
			Division div, Intervals iv) {

		if (iv == null || iv.isBottom())
			return; // unreachable – no warning needed

		String prefix = "[DivisionByZeroChecker-" + size + "] ";

		if (iv.isTop()) {
			tool.warnOn(div, prefix + "Possible division by zero: divisor is TOP (unknown value).");
			return;
		}

		MathNumber lo = iv.getLowerBound();
		MathNumber hi = iv.getUpperBound();

		if (lo == null || hi == null) {
			tool.warnOn(div, prefix + "Possible division by zero: cannot determine divisor bounds.");
			return;
		}

		// 0 ∈ [lo, hi]  ⟺  lo <= 0 <= hi
		boolean zeroPossible =
				lo.compareTo(MathNumber.ZERO) <= 0 &&
						hi.compareTo(MathNumber.ZERO) >= 0;

		if (zeroPossible) {
			tool.warnOn(div, prefix
					+ "Possible division by zero: divisor interval is ["
					+ lo + ", " + hi + "] which contains 0.");
		}
	}

	// -------------------------------------------------------------------------
	// Type helpers
	// -------------------------------------------------------------------------

	/**
	 * Returns {@code true} if the symbolic expression {@code s} (or one of its
	 * possible dynamic types) is numeric.
	 */
	private boolean isNumericSymbolicExpression(SymbolicExpression s, Set<Type> dynamicTypes) {
		// 1. Try the static type of the expression first.
		Type staticType = s.getStaticType();
		if (staticType != null && !staticType.isUntyped())
			return isNumericType(staticType);

		// 2. Fall back to dynamic / runtime types.
		return dynamicTypes.stream().anyMatch(this::isNumericType);
	}

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

	/** Collects possible runtime types for a symbolic expression. */
	private Set<Type> getPossibleDynamicTypes(SymbolicExpression s, Division div,
											  SimpleAbstractState<PointBasedHeap, ValueEnvironment<Intervals>, TypeEnvironment<InferredTypes>> state)
			throws SemanticException {

		Set<Type> result = new HashSet<>();
		Type dynamic = state.getDynamicTypeOf(s, div, state);

		if (dynamic != null && !dynamic.isUntyped()) {
			result.add(dynamic);
		} else if (dynamic != null && dynamic.isUntyped()) {
			Set<Type> runtime = state.getRuntimeTypesOf(s, div, state);
			runtime.stream()
					.filter(t -> t != Untyped.INSTANCE)
					.forEach(result::add);
		}

		return result;
	}
}
