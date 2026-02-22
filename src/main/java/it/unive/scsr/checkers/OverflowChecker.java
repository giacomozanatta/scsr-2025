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
import it.unive.scsr.Pentagons;

// Checks whether variables can overflow or underflow for a given numeric type.
//
// It implements SemanticCheck (not SyntacticCheck) because I need to read the
// abstract interval of a variable from the analysis post-state. SyntacticCheck
// only sees the AST, not the analysis results.
//
// The raw types + @SuppressWarnings are needed because I want this checker to
// work with both a plain ValueEnvironment<Intervals> state and a Pentagons state,
// without fixing a single generic type parameter.
@SuppressWarnings({"rawtypes", "unchecked"})
public class OverflowChecker implements SemanticCheck {

	// Supported numeric types and their min/max bounds.
	// For floats I use the integer range of the mantissa as an approximation,
	// since the Intervals domain is integer-based.
	public enum NumericalSize {
		INT8   (true,  false, -128L,                    127L),
		INT16  (true,  false, -32768L,                  32767L),
		INT32  (true,  false, -2147483648L,              2147483647L),
		UINT8  (false, false, 0L,                        255L),
		UINT16 (false, false, 0L,                        65535L),
		UINT32 (false, false, 0L,                        4294967295L),
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

	private final NumericalSize size;

	public OverflowChecker(NumericalSize size) { this.size = size; }

	// Called by LiSA for every statement. I only care about assignments
	// (where a variable gets a new value). For other statements I just return true.
	public boolean visit(CheckToolWithAnalysisResults tool, CFG graph, Statement node) {
		if (node instanceof Assignment) {
			Expression left = ((Assignment) node).getLeft();
			if (left instanceof VariableRef)
				checkVariableRef(tool, (VariableRef) left, graph, node);
		} else if (node instanceof VariableRef) {
			checkVariableRef(tool, (VariableRef) node, graph, node);
		}
		return true;
	}

	private void checkVariableRef(CheckToolWithAnalysisResults tool, VariableRef varRef, CFG graph, Statement node) {
		Variable id = new Variable(varRef.getStaticType(), varRef.getName(), varRef.getLocation());

		// Skip non-numeric variables. If the static type is Untyped (IMP parameters
		// without explicit types), fall back to the types inferred by LiSA.
		Type staticType = id.getStaticType();
		Set<Type> dynamicTypes = getPossibleDynamicTypes(tool, graph, node, id, varRef);
		boolean isNumeric = isNumericType(staticType);
		if (!isNumeric) isNumeric = dynamicTypes.stream().anyMatch(this::isNumericType);
		if (!isNumeric) return;

		// I want the state AFTER the assignment, so I use the assignment node
		// as the target (not the variable reference itself).
		Statement target = node;
		if (varRef.getParentStatement() instanceof Assignment
				&& ((Assignment) varRef.getParentStatement()).getLeft() == varRef) {
			target = varRef.getParentStatement();
		}

		for (Object r : tool.getResultOf(graph)) {
			AnalyzedCFG result = (AnalyzedCFG) r;
			Object rawState = result.getAnalysisStateAfter(target).getState();
			ValueEnvironment<Intervals> valueEnv = extractIntervals(rawState);
			if (valueEnv == null) continue;
			Intervals iv = valueEnv.getState(id);
			checkOverflowUnderflow(tool, target, iv);
		}
	}

	// Compare the interval against the type bounds and emit warnings.
	//   upper > typeMax  -> overflow
	//   lower < typeMin  -> underflow
	//   TOP              -> warn because the value could be anything
	//   BOTTOM           -> dead code, skip
	private void checkOverflowUnderflow(CheckToolWithAnalysisResults tool, Statement target, Intervals iv) {
		if (iv == null || iv.isBottom()) return;

		String prefix = "[OverflowChecker-" + size + "] ";

		if (iv.isTop()) {
			tool.warnOn(target, prefix + "Possible overflow/underflow: value is unbounded (TOP).");
			return;
		}

		MathNumber lo = iv.getLowerBound();
		MathNumber hi = iv.getUpperBound();

		if (lo == null || hi == null) {
			tool.warnOn(target, prefix + "Possible overflow/underflow: cannot determine value bounds.");
			return;
		}

		MathNumber typeMin = size.minAsMathNumber();
		MathNumber typeMax = size.maxAsMathNumber();

		if (!hi.isInfinite() && hi.compareTo(typeMax) > 0) {
			tool.warnOn(target, prefix + "Possible overflow: upper bound " + hi
					+ " exceeds " + size + " maximum (" + size.max + ").");
		} else if (hi.isPlusInfinity()) {
			tool.warnOn(target, prefix + "Possible overflow: upper bound is +Inf, may exceed "
					+ size + " maximum (" + size.max + ").");
		}

		if (!lo.isInfinite() && lo.compareTo(typeMin) < 0) {
			tool.warnOn(target, prefix + "Possible underflow: lower bound " + lo
					+ " is below " + size + " minimum (" + size.min + ").");
		} else if (lo.isMinusInfinity()) {
			tool.warnOn(target, prefix + "Possible underflow: lower bound is -Inf, may go below "
					+ size + " minimum (" + size.min + ").");
		}
	}

	// Extract ValueEnvironment<Intervals> from the abstract state.
	// If the analysis ran with Pentagons, the state is a Pentagons object,
	// not a plain ValueEnvironment. Pentagons stores its interval component in
	// a package-private field called "intervals". I access it via reflection
	// (setAccessible(true)) to avoid touching professor-provided Pentagons.java.
	@SuppressWarnings("unchecked")
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

	// Check numeric type via LiSA's NumericType interface first,
	// then fall back to a name-based check for IMP types that don't implement it.
	private boolean isNumericType(Type t) {
		if (t == null || t instanceof Untyped) return false;
		if (t instanceof NumericType) return true;
		String name = t.toString().toLowerCase();
		return name.contains("int")   || name.contains("uint")
				|| name.contains("float") || name.contains("double")
				|| name.contains("long")  || name.contains("short")
				|| name.contains("byte")  || name.contains("numeric");
	}

	// If the static type is Untyped, collect runtime types from LiSA's type analysis.
	// getDynamicTypeOf() gives the most general type; if still Untyped,
	// getRuntimeTypesOf() gives all possible concrete types.
	private Set<Type> getPossibleDynamicTypes(
			CheckToolWithAnalysisResults tool, CFG graph, Statement node, Variable id, VariableRef varRef) {
		Set<Type> possibleDynamicTypes = new HashSet<>();
		for (Object r : tool.getResultOf(graph)) {
			AnalyzedCFG result = (AnalyzedCFG) r;
			SimpleAbstractState state =
					(SimpleAbstractState) result.getAnalysisStateAfter(varRef).getState();
			try {
				Type dynamic = state.getDynamicTypeOf(id, varRef, state);
				if (dynamic != null && !dynamic.isUntyped()) {
					possibleDynamicTypes.add(dynamic);
				} else if (dynamic != null && dynamic.isUntyped()) {
					Set<Type> runtime = state.getRuntimeTypesOf(id, varRef, state);
					runtime.stream().filter(t -> t != Untyped.INSTANCE).forEach(possibleDynamicTypes::add);
				}
			} catch (SemanticException e) {
				System.err.println("OverflowChecker: cannot check " + node);
				e.printStackTrace(System.err);
			}
		}
		return possibleDynamicTypes;
	}
}
