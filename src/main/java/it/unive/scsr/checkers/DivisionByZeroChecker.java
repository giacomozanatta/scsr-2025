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
import it.unive.scsr.Pentagons;
import it.unive.scsr.checkers.OverflowChecker.NumericalSize;

// Checks for potential division by zero.
// For every Division node it reads the abstract interval of the divisor
// and warns if 0 is within that interval: lo <= 0 <= hi.
@SuppressWarnings({"rawtypes", "unchecked"})
public class DivisionByZeroChecker implements SemanticCheck {

	private final NumericalSize size;

	public DivisionByZeroChecker(NumericalSize size) { this.size = size; }

	public boolean visit(CheckToolWithAnalysisResults tool, CFG graph, Statement node) {
		if (node instanceof Division)
			checkDivision(tool, graph, (Division) node);
		return true;
	}

	private void checkDivision(CheckToolWithAnalysisResults tool, CFG graph, Division div) {
		for (Object r : tool.getResultOf(graph)) {
			AnalyzedCFG result = (AnalyzedCFG) r;

			AnalysisState state = result.getAnalysisStateAfter(div.getRight());

			Set<SymbolicExpression> reachableIds = new HashSet<>();
			Iterator<SymbolicExpression> it = state.getComputedExpressions().iterator();
			if (!it.hasNext()) continue;
			SymbolicExpression divisorExpr = it.next();

			try {
				SimpleAbstractState rawState = (SimpleAbstractState) state.getState();

				reachableIds.addAll(rawState.reachableFrom(divisorExpr, div, rawState).elements);

				for (SymbolicExpression s : reachableIds) {
					// Only warn for numeric divisors (not strings, objects, etc.)
					Set<Type> dynamicTypes = getPossibleDynamicTypes(s, div, rawState);
					if (!isNumericSymbolicExpression(s, dynamicTypes)) continue;

					ValueEnvironment<Intervals> valueState = extractIntervals(rawState.getValueState());
					if (valueState == null) continue;

					Intervals divisorInterval;
					try {
						divisorInterval = valueState.eval((ValueExpression) s, div, state.getState());
					} catch (ClassCastException cce) {
						continue;
					}

					warnIfMayBeZero(tool, div, divisorInterval);
				}
			} catch (SemanticException e) {
				e.printStackTrace();
			}
		}
	}

	// 0 in [lo, hi] iff lo <= 0 <= hi.
	// TOP -> divisor unknown, warn. BOTTOM -> dead code, skip.
	private void warnIfMayBeZero(CheckToolWithAnalysisResults tool, Division div, Intervals iv) {
		if (iv == null || iv.isBottom()) return;

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

		// 0 is in the interval if lo <= 0 and hi >= 0
		boolean zeroPossible =
				lo.compareTo(MathNumber.ZERO) <= 0 &&
						hi.compareTo(MathNumber.ZERO) >= 0;

		if (zeroPossible) {
			tool.warnOn(div, prefix
					+ "Possible division by zero: divisor interval is ["
					+ lo + ", " + hi + "] which contains 0.");
		}
	}


	private ValueEnvironment<Intervals> extractIntervals(Object valueState) {
		if (valueState instanceof Pentagons) {
			try {
				java.lang.reflect.Field f = Pentagons.class.getDeclaredField("intervals");
				f.setAccessible(true);
				return (ValueEnvironment<Intervals>) f.get(valueState);
			} catch (Exception e) {
				return null;
			}
		}
		if (valueState instanceof ValueEnvironment)
			return (ValueEnvironment<Intervals>) valueState;
		return null;
	}

	private boolean isNumericSymbolicExpression(SymbolicExpression s, Set<Type> dynamicTypes) {
		Type staticType = s.getStaticType();
		if (staticType != null && !staticType.isUntyped())
			return isNumericType(staticType);
		return dynamicTypes.stream().anyMatch(this::isNumericType);
	}

	private boolean isNumericType(Type t) {
		if (t == null || t instanceof Untyped) return false;
		if (t instanceof NumericType) return true;
		String name = t.toString().toLowerCase();
		return name.contains("int")   || name.contains("uint")
				|| name.contains("float") || name.contains("double")
				|| name.contains("long")  || name.contains("short")
				|| name.contains("byte")  || name.contains("numeric");
	}

	private Set<Type> getPossibleDynamicTypes(SymbolicExpression s, Division div, SimpleAbstractState state)
			throws SemanticException {
		Set<Type> result = new HashSet<>();
		Type dynamic = state.getDynamicTypeOf(s, div, state);
		if (dynamic != null && !dynamic.isUntyped()) {
			result.add(dynamic);
		} else if (dynamic != null && dynamic.isUntyped()) {
			Set<Type> runtime = state.getRuntimeTypesOf(s, div, state);
			runtime.stream().filter(t -> t != Untyped.INSTANCE).forEach(result::add);
		}
		return result;
	}
}
