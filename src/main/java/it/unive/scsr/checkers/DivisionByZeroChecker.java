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

public class DivisionByZeroChecker implements
		SemanticCheck<
				SimpleAbstractState<PointBasedHeap, ValueEnvironment<Intervals>, TypeEnvironment<InferredTypes>>> {

	private int warningsFound = 0;

	// No-argument constructor
	public DivisionByZeroChecker() {
		// default constructor
	}

	// Constructor with NumericalSize (optional)
	public DivisionByZeroChecker(OverflowChecker.NumericalSize size) {
		// you can store size if needed
	}

	@Override
	public void afterExecution(CheckToolWithAnalysisResults<SimpleAbstractState<PointBasedHeap, ValueEnvironment<Intervals>, TypeEnvironment<InferredTypes>>> tool) {
		// This method is called after the analysis is complete
		if (warningsFound == 0) {
			// Print informational message when no division by zero detected
			System.out.println("[DivisionByZeroChecker] No division by zero issues detected");
		} else {
			System.out.println("[DivisionByZeroChecker] Found " + warningsFound + " potential division by zero issue(s)");
		}
	}

	@Override
	public boolean visit(
			CheckToolWithAnalysisResults<SimpleAbstractState<PointBasedHeap, ValueEnvironment<Intervals>, TypeEnvironment<InferredTypes>>> tool,
			CFG graph, Statement node) {

		if (node instanceof Assignment) {
			Assignment assignment = (Assignment) node;
			Expression leftExpression = assignment.getLeft();

			// Checking if each variable reference is dividing by zero
			if (leftExpression instanceof VariableRef) {
				checkVariableRef(tool, (VariableRef) leftExpression, graph, node);
			}

		} else {

			// Checking if each variable reference is dividing by zero
			if (node instanceof VariableRef) {
				checkVariableRef(tool, (VariableRef) node, graph, node);
			}
		}

		return true;

	}

	private void checkVariableRef(CheckToolWithAnalysisResults<SimpleAbstractState<PointBasedHeap, ValueEnvironment<Intervals>, TypeEnvironment<InferredTypes>>> tool, VariableRef varRef, CFG graph, Statement node ) {
		Variable id = new Variable(((VariableRef) varRef).getStaticType(), ((VariableRef) varRef).getName(), ((VariableRef) varRef).getLocation());

		Type staticType = id.getStaticType();
		Set<Type> dynamicTypes = getPossibleDynamicTypes(tool, graph, node, id, varRef);

		Statement target = node;

		// Check if the type is numeric
		boolean isNumeric = false;

		// First check static type
		if (staticType != null && !staticType.isUntyped()) {
			String typeName = staticType.toString().toLowerCase();
			if (typeName.contains("int") || typeName.contains("float") ||
					typeName.contains("uint") || typeName.contains("numeric")) {
				isNumeric = true;
			}
		}
		// If static type is untyped, check dynamic types
		else if (staticType == null || staticType.isUntyped()) {
			for (Type dynType : dynamicTypes) {
				String typeName = dynType.toString().toLowerCase();
				if (typeName.contains("int") || typeName.contains("float") ||
						typeName.contains("uint") || typeName.contains("numeric")) {
					isNumeric = true;
					break;
				}
			}
		}

		// If not numeric, no need to check for division by zero
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

			if (intervalAbstractValue != null) {
				// Check if interval contains zero

				Number lowNum = intervalAbstractValue.getLow();
				Number highNum = intervalAbstractValue.getHigh();

				if (lowNum != null && highNum != null) {
					double low = lowNum.doubleValue();
					double high = highNum.doubleValue();

					// Check if zero is within the interval [low, high]
					if (low <= 0 && high >= 0) {
						tool.warnOn(node, "Possible division by zero detected for variable: " +
								id.getName() + " with interval [" + low + ", " + high + "]");
						warningsFound++;
					}
				}
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