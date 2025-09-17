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
import it.unive.lisa.analysis.value.ValueDomain;
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
import it.unive.scsr.Pentagons;

public class OverflowChecker<V extends ValueDomain<V>> implements SemanticCheck<
		SimpleAbstractState<PointBasedHeap, V, TypeEnvironment<InferredTypes>>> {

	public enum NumericalSize {
		INT8,   // signed 8-bit
		INT16,  // signed 16-bit
		INT32,  // signed 32-bit
		UINT8,  // unsigned 8-bit
		UINT16, // unsigned 16-bit
		UINT32, // unsigned 32-bit
		FLOAT8,   // float 8-bit (non gestito qui)
		FLOAT16,  // float 16-bit (non gestito qui)
		FLOAT32   // float 32-bit (non gestito qui)
	}

	private final NumericalSize size;

	public OverflowChecker(NumericalSize size) {
		this.size = size;
	}

	@Override
	public boolean visit(
			CheckToolWithAnalysisResults<SimpleAbstractState<PointBasedHeap, V, TypeEnvironment<InferredTypes>>> tool,
			CFG graph, Statement node) {

		if (node instanceof Assignment) {
			Assignment asg = (Assignment) node;
			Expression left = asg.getLeft();
			if (left instanceof VariableRef)
				checkVariableRef(tool, (VariableRef) left, graph, node);
		} else if (node instanceof VariableRef) {
			checkVariableRef(tool, (VariableRef) node, graph, node);
		}

		return true;
	}

	private void checkVariableRef(
			CheckToolWithAnalysisResults<SimpleAbstractState<PointBasedHeap, V, TypeEnvironment<InferredTypes>>> tool,
			VariableRef varRef, CFG graph, Statement node) {

		Variable id = new Variable(varRef.getStaticType(), varRef.getName(), varRef.getLocation());
		Type staticType = id.getStaticType();
		Set<Type> dynamicTypes = getPossibleDynamicTypes(tool, graph, node, id, varRef);

		boolean isNumerical = false;
		if (staticType != null && !staticType.isUntyped()) {
			if (staticType.isNumericType()) {
				isNumerical = true;
			} else {
				String name = String.valueOf(staticType);
				isNumerical = containsNumericKeyword(name);
			}
		} else {
			isNumerical = dynamicTypes.stream().anyMatch(t ->
					(t != null && !t.isUntyped() && t.isNumericType())
							|| containsNumericKeyword(String.valueOf(t)));
		}

		if (!isNumerical)
			return;

		Statement target = node;
		if (varRef.getParentStatement() instanceof Assignment
				&& ((Assignment) varRef.getParentStatement()).getLeft() == varRef) {
			target = varRef.getParentStatement();
		}

		switch (size) {
			case FLOAT8:
			case FLOAT16:
			case FLOAT32:
				tool.warnOn(target, "Overflow check not supported for floating types (" + size + ")");
				return;
			default:
		}

		for (AnalyzedCFG<SimpleAbstractState<PointBasedHeap, V, TypeEnvironment<InferredTypes>>> result
				: tool.getResultOf(graph)) {

			SimpleAbstractState<PointBasedHeap, V, TypeEnvironment<InferredTypes>> state =
					result.getAnalysisStateAfter(target).getState();

			Intervals iv = extractIntervals(state.getValueState(), id);
			if (iv == null || iv.isBottom() || iv.interval == null)
				continue;

			long min = Long.MIN_VALUE, max = Long.MAX_VALUE;
			switch (size) {
				case INT8:   min = -128;          max = 127;           break;
				case INT16:  min = -32768;        max = 32767;         break;
				case INT32:  min = -2147483648L;  max = 2147483647L;   break;
				case UINT8:  min = 0;             max = 255;           break;
				case UINT16: min = 0;             max = 65535;         break;
				case UINT32: min = 0;             max = 4294967295L;   break;
				default: 
					continue;
			}

			try {
				long low  = iv.interval.getLow().toLong();
				long high = iv.interval.getHigh().toLong();

				if (high < min) {
					tool.warnOn(target, "Underflow certo per " + size + " ([" + low + "," + high + "] < " + min + ")");
				} else if (low > max) {
					tool.warnOn(target, "Overflow certo per " + size + " ([" + low + "," + high + "] > " + max + ")");
				} else {
					boolean underPart = low < min;
					boolean overPart  = high > max;
					if (underPart || overPart) {
						String which = (underPart && overPart) ? "underflow/overflow"
								: (underPart ? "underflow" : "overflow");
						tool.warnOn(target, "Possibile " + which + " per " + size +
								" ([" + low + "," + high + "] ∉ [" + min + "," + max + "])");
					}
				}
			} catch (Exception ex) {
				boolean minusInf = false, plusInf = false;
				try {
					minusInf = iv.interval.getLow().isMinusInfinity();
					plusInf  = iv.interval.getHigh().isPlusInfinity();
				} catch (Exception ignored) {}

				if (minusInf || plusInf) {
					tool.warnOn(target, "Possibile overflow/underflow per " + size + " (intervallo non finito)");
				} else {
					tool.warnOn(target, "Possibile overflow/underflow per " + size + " (bound non interpretabili)");
				}
			}
		}
	}

	private static boolean containsNumericKeyword(String s) {
		if (s == null) return false;
		s = s.toLowerCase();
		return s.contains("int") || s.contains("float") || s.contains("double") || s.contains("number")
				|| s.contains("byte") || s.contains("short") || s.contains("long") || s.contains("bigint")
				|| s.matches(".*u?int(8|16|32|64)?.*");
	}

	private Set<Type> getPossibleDynamicTypes(
			CheckToolWithAnalysisResults<SimpleAbstractState<PointBasedHeap, V, TypeEnvironment<InferredTypes>>> tool,
			CFG graph, Statement node, Variable id, VariableRef varRef) {

		Set<Type> possibleDynamicTypes = new HashSet<>();

		for (AnalyzedCFG<SimpleAbstractState<PointBasedHeap, V, TypeEnvironment<InferredTypes>>> result
				: tool.getResultOf(graph)) {

			SimpleAbstractState<PointBasedHeap, V, TypeEnvironment<InferredTypes>> state =
					result.getAnalysisStateAfter(varRef).getState();

			try {
				Type dynamicTypes = state.getDynamicTypeOf(id, varRef, state);
				if (dynamicTypes != null && !dynamicTypes.isUntyped()) {
					possibleDynamicTypes.add(dynamicTypes);
				} else if (dynamicTypes != null && dynamicTypes.isUntyped()) {
					Set<Type> runtimeTypes = state.getRuntimeTypesOf(id, varRef, state);
					if (runtimeTypes != null && runtimeTypes.stream().anyMatch(t -> t != Untyped.INSTANCE)) {
						for (Type t : runtimeTypes)
							if (t != null && t != Untyped.INSTANCE)
								possibleDynamicTypes.add(t);
					}
				}
			} catch (SemanticException e) {
				System.err.println("Cannot check " + node);
				e.printStackTrace(System.err);
			}
		}

		return possibleDynamicTypes;
	}

	@SuppressWarnings("unchecked")
	private Intervals extractIntervals(V valueState, Variable id) {
		if (valueState instanceof ValueEnvironment) {
			ValueEnvironment<?> env = (ValueEnvironment<?>) valueState;
			Object state = env.getState(id);
			if (state instanceof Intervals) {
				return (Intervals) state;
			}
		} else if (valueState instanceof Pentagons) {
			Pentagons pentagons = (Pentagons) valueState;
			return pentagons.getIntervals().getState(id);
		}
		return new Intervals().bottom();
	}
}