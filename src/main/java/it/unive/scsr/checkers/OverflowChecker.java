package it.unive.scsr.checkers;

import java.math.BigDecimal;
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
		INT8, INT16, INT32,
		UINT8, UINT16, UINT32,
		FLOAT8, FLOAT16, FLOAT32
	}

	private final NumericalSize size;

	public OverflowChecker() {
		this(NumericalSize.INT32);
	}

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
			if (leftExpression instanceof VariableRef) {
				checkVariableRef(tool, (VariableRef) leftExpression, graph, node);
			}
		} else if (node instanceof VariableRef) {
			checkVariableRef(tool, (VariableRef) node, graph, node);
		}
		return true;
	}

	private void checkVariableRef(
			CheckToolWithAnalysisResults<SimpleAbstractState<PointBasedHeap, ValueEnvironment<Intervals>, TypeEnvironment<InferredTypes>>> tool,
			VariableRef varRef, CFG graph, Statement node) {

		Variable id = new Variable(varRef.getStaticType(), varRef.getName(), varRef.getLocation());
		Type staticType = id.getStaticType();
		Set<Type> dynamicTypes = getPossibleDynamicTypes(tool, graph, node, id, varRef);

		Statement target = node;
		if (varRef.getParentStatement() instanceof Assignment && ((Assignment) varRef.getParentStatement()).getLeft() == varRef) {
			target = varRef.getParentStatement();
		}

		boolean isNumeric = false;
		if (staticType.isNumericType()) {
			isNumeric = true;
		} else if (staticType.isUntyped()) {
			for (Type t : dynamicTypes) {
				if (t.isNumericType()) {
					isNumeric = true;
					break;
				}
			}
		}
		if (!isNumeric) return;

		BigDecimal limitMin = getMin(this.size);
		BigDecimal limitMax = getMax(this.size);

		for (AnalyzedCFG<SimpleAbstractState<PointBasedHeap, ValueEnvironment<Intervals>, TypeEnvironment<InferredTypes>>> result : tool.getResultOf(graph)) {

			SimpleAbstractState<PointBasedHeap, ValueEnvironment<Intervals>, TypeEnvironment<InferredTypes>> state =
					result.getAnalysisStateAfter(target).getState();
			Intervals intervalAbstractValue = state.getValueState().getState(id);

			if (intervalAbstractValue.isBottom() || intervalAbstractValue.isTop()) continue;

			try {
				MathNumber lowNum = intervalAbstractValue.interval.getLow();
				MathNumber highNum = intervalAbstractValue.interval.getHigh();

				boolean isInfinite = lowNum.isMinusInfinity() || highNum.isPlusInfinity();

				// --- SOFT NOISE FILTER ---
				// If interval is infinite, report a "Generic" warning so we know the tool worked.
				if (isInfinite) {
					tool.warnOn(node, "[" + this.size + "] Generic: Value is unconstrained (Infinite/Unknown).");
					continue;
				}

				BigDecimal intervalLow = new BigDecimal(lowNum.toString());
				BigDecimal intervalHigh = new BigDecimal(highNum.toString());

				if (intervalLow.compareTo(limitMin) < 0) {
					tool.warnOn(node, "[" + this.size + "] Definite Underflow: value " + intervalLow + " < " + limitMin);
				}
				if (intervalHigh.compareTo(limitMax) > 0) {
					tool.warnOn(node, "[" + this.size + "] Definite Overflow: value " + intervalHigh + " > " + limitMax);
				}

			} catch (Exception e) { }
		}
	}

	private Set<Type> getPossibleDynamicTypes(
			CheckToolWithAnalysisResults<SimpleAbstractState<PointBasedHeap, ValueEnvironment<Intervals>, TypeEnvironment<InferredTypes>>> tool,
			CFG graph, Statement node, Variable id, VariableRef varRef) {

		Set<Type> possibleDynamicTypes = new HashSet<>();
		for (AnalyzedCFG<SimpleAbstractState<PointBasedHeap, ValueEnvironment<Intervals>, TypeEnvironment<InferredTypes>>> result : tool.getResultOf(graph)) {
			SimpleAbstractState<PointBasedHeap, ValueEnvironment<Intervals>, TypeEnvironment<InferredTypes>> state = result.getAnalysisStateAfter(varRef).getState();
			try {
				Type dynamicTypes = state.getDynamicTypeOf(id, varRef, state);
				if (dynamicTypes != null && !dynamicTypes.isUntyped()) {
					possibleDynamicTypes.add(dynamicTypes);
				} else if (dynamicTypes.isUntyped()) {
					Set<Type> runtimeTypes = state.getRuntimeTypesOf(id, varRef, state);
					if (runtimeTypes.stream().anyMatch(t -> t != Untyped.INSTANCE))
						for (Type t : runtimeTypes) possibleDynamicTypes.add(t);
				}
			} catch (SemanticException e) {
				e.printStackTrace(System.err);
			}
		}
		return possibleDynamicTypes;
	}

	private BigDecimal getMin(NumericalSize size) {
		switch (size) {
			case INT8:    return new BigDecimal(Byte.MIN_VALUE);
			case INT16:   return new BigDecimal(Short.MIN_VALUE);
			case INT32:   return new BigDecimal(Integer.MIN_VALUE);
			case UINT8:   return BigDecimal.ZERO;
			case UINT16:  return BigDecimal.ZERO;
			case UINT32:  return BigDecimal.ZERO;
			case FLOAT8:  return new BigDecimal("-240.0");
			case FLOAT16: return new BigDecimal("-65504.0");
			case FLOAT32: return new BigDecimal(-Float.MAX_VALUE);
			default:      return null;
		}
	}

	private BigDecimal getMax(NumericalSize size) {
		switch (size) {
			case INT8:    return new BigDecimal(Byte.MAX_VALUE);
			case INT16:   return new BigDecimal(Short.MAX_VALUE);
			case INT32:   return new BigDecimal(Integer.MAX_VALUE);
			case UINT8:   return new BigDecimal(255);
			case UINT16:  return new BigDecimal(65535);
			case UINT32:  return new BigDecimal("4294967295");
			case FLOAT8:  return new BigDecimal("240.0");
			case FLOAT16: return new BigDecimal("65504.0");
			case FLOAT32: return new BigDecimal(Float.MAX_VALUE);
			default:      return null;
		}
	}
}