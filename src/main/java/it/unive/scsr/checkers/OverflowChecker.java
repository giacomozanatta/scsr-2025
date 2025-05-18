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
import it.unive.lisa.util.numeric.IntInterval;
import it.unive.lisa.util.numeric.MathNumber;
import it.unive.scsr.Intervals;

public class OverflowChecker implements
		SemanticCheck<
				SimpleAbstractState<PointBasedHeap, ValueEnvironment<Intervals>, TypeEnvironment<InferredTypes>>> {

	public enum NumericalSize {
		INT8,  // signed integer 8-bit
		INT16, // signed integer 16-bit
		INT32, // signed integer 32-bit
		UINT8,  // unsigned integer 8-bit
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

		// Method to analyze only the CFGs that correspond to the numeric size bound to this instance.

		if (!isCfgRelevant(graph))
			return true; // skip this node – keep visiting to allow other checks

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

	private void checkVariableRef(CheckToolWithAnalysisResults<SimpleAbstractState<PointBasedHeap, ValueEnvironment<Intervals>, TypeEnvironment<InferredTypes>>> tool, VariableRef varRef, CFG graph, Statement node ) {
		Variable id = new Variable(((VariableRef) varRef).getStaticType(), ((VariableRef) varRef).getName(), ((VariableRef) varRef).getLocation());
// TYPE CHECKER
		Type staticType = id.getStaticType();
		Set<Type> dynamicTypes = getPossibleDynamicTypes(tool, graph, node, id, varRef);
		if (staticType.isUntyped()) {
			dynamicTypes.addAll(getPossibleDynamicTypes(tool, graph, node, id, varRef));
		} else {
			dynamicTypes.add(staticType);
		}
		boolean isNumeric = dynamicTypes.stream().anyMatch(this::isNumeric);
		if (!isNumeric) {
			System.err.printf("[WARNING] Variable '%s' in %s has the following non-numeric type: %s%n", id.getName(), id.getCodeLocation(), dynamicTypes);
			return;
		}
		double min = getMinValue();
		double max = getMaxValue();

		for (AnalyzedCFG<SimpleAbstractState<PointBasedHeap, ValueEnvironment<Intervals>,
				TypeEnvironment<InferredTypes>>> result : tool.getResultOf(graph)) {
			SimpleAbstractState<PointBasedHeap, ValueEnvironment<Intervals>, TypeEnvironment<InferredTypes>> state = result.getAnalysisStateAfter(node).getState();
			Intervals intervalAbstractValue = state.getValueState().getState(id);

// OVERFLOW & UNDERFLOW LOGIC
			if (intervalAbstractValue == null || intervalAbstractValue.isBottom())
				continue;

			IntInterval intv = intervalAbstractValue.interval;
			MathNumber lowMn = intv.getLow();
			MathNumber highMn = intv.getHigh();

			double low = toDouble(lowMn);
			double high = toDouble(highMn);

			// Skip warnings when an extremum is infinite or NaN
			boolean lowFinite = Double.isFinite(low);
			boolean highFinite = Double.isFinite(high);
			// Definite and possible underflow cases
			if (lowFinite && low < min && high <= min) {
				System.err.printf("[%s] Underflow in %s: %.3f < %.3f location %s%n",
						size, id.getName(), low, min, id.getCodeLocation());
			} else if (lowFinite && low < min) {
				System.err.printf("[%s] Possible underflow in %s: %.3f < %.3f location %s%n",
						size, id.getName(), low, min, id.getCodeLocation());
			}
			// Definite and possible overflow cases
			if (highFinite && high > max && low >= max) {
				System.err.printf("[%s] Overflow in %s: %.3f > %.3f location %s%n",
						size, id.getName(), high, max, id.getCodeLocation());
			} else if (highFinite && high > max) {
				System.err.printf("[%s] Possible overflow in %s: %.3f > %.3f location %s%n",
						size, id.getName(), high, max, id.getCodeLocation());
			}
		}

	}

	private static double toDouble(MathNumber n) {
		if (n == null)
			return Double.NaN;

		String s = n.toString();
		if ("Inf".equals(s) || "+Inf".equals(s))
			return Double.POSITIVE_INFINITY;
		if ("-Inf".equals(s))
			return Double.NEGATIVE_INFINITY;

		return Double.parseDouble(s);
	}

	// compute possible dynamic types / runtime types
	private Set<Type> getPossibleDynamicTypes(
			CheckToolWithAnalysisResults<SimpleAbstractState<PointBasedHeap, ValueEnvironment<Intervals>, TypeEnvironment<InferredTypes>>> tool,
			CFG graph, Statement node, Variable id, VariableRef varRef) {

		Set<Type> possibleDynamicTypes = new HashSet<>();
		for (AnalyzedCFG<
				SimpleAbstractState<PointBasedHeap, ValueEnvironment<Intervals>,
						TypeEnvironment<InferredTypes>>> result : tool.getResultOf(graph)) {
			SimpleAbstractState<PointBasedHeap, ValueEnvironment<Intervals>, TypeEnvironment<InferredTypes>> state = result.getAnalysisStateAfter(varRef).getState();
			try {
				Type dynamicTypes = state.getDynamicTypeOf(id, varRef, state);
				if(dynamicTypes != null && !dynamicTypes.isUntyped()) {
					possibleDynamicTypes.add(dynamicTypes);
				} else if(dynamicTypes.isUntyped()){
					Set<Type> runtimeTypes = state.getRuntimeTypesOf(id, varRef, state);
					if(runtimeTypes.stream().anyMatch(t -> t != Untyped.INSTANCE))
						for( Type t : runtimeTypes)
							possibleDynamicTypes.add(t);
				}
			} catch (SemanticException e) {
				System.err.println("Cannot check " + node);
				e.printStackTrace(System.err);
			}

		}
		return possibleDynamicTypes;
	}


	private boolean isNumeric(Type t) {
		return isIntegerType(t) || isFloatingPointType(t);
	}

	private boolean isIntegerType(Type t) {
		String name = t.toString().toLowerCase();
		return name.startsWith("int") || name.startsWith("uint");
	}

	private boolean isFloatingPointType(Type t) {
		String name = t.toString().toLowerCase();
		return name.startsWith("float");
	}

	private double getMinValue() {
		switch (size) {
			case INT8:   return Byte.MIN_VALUE;
			case INT16:  return Short.MIN_VALUE;
			case INT32:  return Integer.MIN_VALUE;
			case UINT8:  return 0;
			case UINT16: return 0;
			case UINT32: return 0;
			case FLOAT8: return -240.0;
			case FLOAT16: return -65504.0;
			case FLOAT32: return -Float.MAX_VALUE;
			default:      return Double.NEGATIVE_INFINITY;
		}
	}

	private double getMaxValue() {
		switch (size) {
			case INT8:   return Byte.MAX_VALUE;
			case INT16:  return Short.MAX_VALUE;
			case INT32:  return Integer.MAX_VALUE;
			case UINT8:  return 255;
			case UINT16: return 65535;
			case UINT32: return 4294967295L;
			case FLOAT8: return 240.0;
			case FLOAT16: return 65504.0;
			case FLOAT32: return Float.MAX_VALUE;
			default:      return Double.POSITIVE_INFINITY;
		}
	}
	/* The following method checks if the CFG name contains a type name, and if so
	   it performs the overflow/underflow check over that type, if not it performs
	   the test for all the types
	 */
	private boolean isCfgRelevant(CFG cfg) {
		String name = cfg.getDescriptor().getName().toUpperCase();
		if (name.contains(size.name()))
			return true;
		for (NumericalSize s : NumericalSize.values()) {
			if (name.contains(s.name()))
				return false;
		}
		return true;
	}
}