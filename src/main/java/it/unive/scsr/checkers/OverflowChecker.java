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
	private int warningsFound = 0;

	public OverflowChecker(NumericalSize size) {
		this.size = size;
	}

	// Add constructor with boolean parameter for your tests
	public OverflowChecker(NumericalSize size, boolean inferTypes) {
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

	@Override
	public void afterExecution(CheckToolWithAnalysisResults<SimpleAbstractState<PointBasedHeap, ValueEnvironment<Intervals>, TypeEnvironment<InferredTypes>>> tool) {
		if (warningsFound == 0) {
			System.out.println("[OverflowChecker] No overflow/underflow issues detected for " + size);
		} else {
			System.out.println("[OverflowChecker] Found " + warningsFound + " potential overflow/underflow issue(s) for " + size);
		}
	}

	private void checkVariableRef(CheckToolWithAnalysisResults<SimpleAbstractState<PointBasedHeap, ValueEnvironment<Intervals>, TypeEnvironment<InferredTypes>>> tool, VariableRef varRef, CFG graph, Statement node ) {
		Variable id = new Variable(((VariableRef) varRef).getStaticType(), ((VariableRef) varRef).getName(), ((VariableRef) varRef).getLocation());

		Type staticType = id.getStaticType();
		Set<Type> dynamicTypes = getPossibleDynamicTypes(tool, graph, node, id, varRef);

		Statement target = node;

		// TODO: implement type checks, it is required a numerical type
		// hint: if staticType.isUntyped() == true, then should be checked possible dynamic types

		// Check if the type is numeric
		boolean isNumeric = false;

		// First check static type
		if (staticType != null && !staticType.isUntyped()) {
			// Check if it's a numeric type
			String typeName = staticType.toString().toLowerCase();
			if (typeName.contains("int") || typeName.contains("float") ||
					typeName.contains("uint") || typeName.contains("numeric") ||
					typeName.contains("number")) {
				isNumeric = true;
			}
		}
		// If static type is untyped, check possible dynamic types
		else if (staticType == null || staticType.isUntyped()) {
			for (Type dynType : dynamicTypes) {
				String typeName = dynType.toString().toLowerCase();
				if (typeName.contains("int") || typeName.contains("float") ||
						typeName.contains("uint") || typeName.contains("numeric") ||
						typeName.contains("number")) {
					isNumeric = true;
					break;
				}
			}
		}

		// If not numeric, no need to check for overflow/underflow
		if (!isNumeric) {
			return;
		}

		if (varRef.getParentStatement() instanceof Assignment && ((Assignment) varRef.getParentStatement()).getLeft() == varRef) {
			target = varRef.getParentStatement();
		}

		for (AnalyzedCFG<SimpleAbstractState<PointBasedHeap, ValueEnvironment<Intervals>,
				TypeEnvironment<InferredTypes>>> result : tool.getResultOf(graph)) {
			SimpleAbstractState<PointBasedHeap, ValueEnvironment<Intervals>, TypeEnvironment<InferredTypes>> state = result.getAnalysisStateAfter(target).getState();
			Intervals intervalAbstractValue = state.getValueState().getState(id);

			// TODO: implement logic for overflow/underflow checks
			// hint: it depends to the NumericalSize size
			if (intervalAbstractValue != null && !intervalAbstractValue.isBottom()) {
				Number low = intervalAbstractValue.getLow();
				Number high = intervalAbstractValue.getHigh();
				boolean isFloat = intervalAbstractValue.isFloat();

				if (low != null && high != null) {
					// Check for overflow/underflow based on the numerical size
					if (checkOverflowUnderflow(low, high, isFloat)) {
						tool.warnOn(node, "Possible overflow/underflow detected for variable: " +
								id.getName() + " with interval [" + low + ", " + high + "] " +
								"(size: " + size + ")");
						warningsFound++;
					}
				}
			}
		}
	}

	private boolean checkOverflowUnderflow(Number low, Number high, boolean isFloat) {
		double lowValue = low.doubleValue();
		double highValue = high.doubleValue();

		if (isFloat) {
			// Check float overflow/underflow
			switch (size) {
				case FLOAT8:
					// Approximate float8 bounds
					return lowValue < -3.4e38 || highValue > 3.4e38;
				case FLOAT16:
					return lowValue < -65504.0 || highValue > 65504.0;
				case FLOAT32:
					return lowValue < -Float.MAX_VALUE || highValue > Float.MAX_VALUE;
				default:
					// For float variables with integer size specifier, use float32
					return lowValue < -Float.MAX_VALUE || highValue > Float.MAX_VALUE;
			}
		} else {
			// Check integer overflow/underflow
			switch (size) {
				case INT8:
					return lowValue < -128 || highValue > 127;
				case UINT8:
					return lowValue < 0 || highValue > 255;
				case INT16:
					return lowValue < -32768 || highValue > 32767;
				case UINT16:
					return lowValue < 0 || highValue > 65535;
				case INT32:
					return lowValue < -2147483648 || highValue > 2147483647;
				case UINT32:
					return lowValue < 0 || highValue > 4294967295L;
				default:
					// Default to INT32
					return lowValue < -2147483648 || highValue > 2147483647;
			}
		}
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
}