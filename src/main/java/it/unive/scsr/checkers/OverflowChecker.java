package it.unive.scsr.checkers;

import java.util.Collections;
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
import it.unive.scsr.helpers.FloatInterval;

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
		Variable id = new Variable(((VariableRef) varRef).getStaticType(), ((VariableRef) varRef).getName(),
				((VariableRef) varRef).getLocation());

		Type staticType = id.getStaticType();
		Set<Type> dynamicTypes = getPossibleDynamicTypes(tool, graph, node, id, varRef);

		Statement target = node;

		Set<Type> typesToCheck = !staticType.isUntyped() ? Collections.singleton(staticType) : dynamicTypes;

		if (varRef.getParentStatement() instanceof Assignment
				&& ((Assignment) varRef.getParentStatement()).getLeft() == varRef) {
			target = varRef.getParentStatement();
		}

		for (AnalyzedCFG<SimpleAbstractState<PointBasedHeap, ValueEnvironment<Intervals>, TypeEnvironment<InferredTypes>>> result : tool
				.getResultOf(graph)) {
			SimpleAbstractState<PointBasedHeap, ValueEnvironment<Intervals>, TypeEnvironment<InferredTypes>> state = result
					.getAnalysisStateAfter(target).getState();
			Intervals intervalAbstractValue = state.getValueState().getState(id);

			FloatInterval bounds = null;
			if (this.size == NumericalSize.INT8) {
				bounds = new FloatInterval(Byte.MIN_VALUE, Byte.MAX_VALUE);
			} else if (this.size == NumericalSize.INT16) {
				bounds = new FloatInterval(Short.MIN_VALUE, Short.MAX_VALUE);
			} else if (this.size == NumericalSize.INT32) {
				bounds = new FloatInterval(Integer.MIN_VALUE, Integer.MAX_VALUE);
			} else if (this.size == NumericalSize.UINT8) {
				bounds = new FloatInterval(0, 255);
			} else if (this.size == NumericalSize.UINT16) {
				bounds = new FloatInterval(0, 65535);
			} else if (this.size == NumericalSize.UINT32) {
				bounds = new FloatInterval(0, 4294967295L);
			} else if (this.size == NumericalSize.FLOAT8) {
				// 8-bit floats are not standard; using plausible range for demonstration
				bounds = new FloatInterval(-127.0f, 127.0f);
			} else if (this.size == NumericalSize.FLOAT16) {
				// IEEE 754 half-precision float: approx -65504 to 65504
				bounds = new FloatInterval(-65504.0f, 65504.0f);
			} else if (this.size == NumericalSize.FLOAT32) {
				// Use Float.MIN_VALUE for smallest positive, -Float.MAX_VALUE for lowest
				// negative
				bounds = new FloatInterval(-Float.MAX_VALUE, Float.MAX_VALUE);
			}
			if (bounds != null) {
				if (intervalAbstractValue.interval.getHigh().gt(bounds.getHigh())) {
					tool.warnOn(node, "Overflow");
				} else if (intervalAbstractValue.interval.getLow().lt(bounds.getLow())) {
					tool.warnOn(node, "Underflow");
				}
			}
		}
	}

	// compute possible dynamic types / runtime types
	private Set<Type> getPossibleDynamicTypes(
			CheckToolWithAnalysisResults<SimpleAbstractState<PointBasedHeap, ValueEnvironment<Intervals>, TypeEnvironment<InferredTypes>>> tool,
			CFG graph, Statement node, Variable id, VariableRef varRef) {

		Set<Type> possibleDynamicTypes = new HashSet<>();
		for (AnalyzedCFG<SimpleAbstractState<PointBasedHeap, ValueEnvironment<Intervals>, TypeEnvironment<InferredTypes>>> result : tool
				.getResultOf(graph)) {
			SimpleAbstractState<PointBasedHeap, ValueEnvironment<Intervals>, TypeEnvironment<InferredTypes>> state = result
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