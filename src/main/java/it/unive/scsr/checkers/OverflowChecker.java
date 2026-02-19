package it.unive.scsr.checkers;

import java.util.HashSet;
import java.util.Map;
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
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;
import it.unive.lisa.util.numeric.MathNumber;
import it.unive.scsr.Intervals;
import it.unive.scsr.Pentagons;
import it.unive.scsr.helper.NumericInterval;

public class OverflowChecker implements
		SemanticCheck<SimpleAbstractState<PointBasedHeap, ValueEnvironment<Intervals>, TypeEnvironment<InferredTypes>>> {

	public enum NumericalSize {
		INT8, INT16, INT32, UINT8, UINT16, UINT32, FLOAT8, FLOAT16, FLOAT32
	}

	public record TypeLimitsRecord(
			MathNumber lowerOw,
			MathNumber upperOw,
			MathNumber uFlow,
			MathNumber negUFlow) {

		public TypeLimitsRecord(long lowerOw, long upperOw) {
			this(new MathNumber(lowerOw), new MathNumber(upperOw), MathNumber.ZERO, MathNumber.ZERO);
		}

		public TypeLimitsRecord(double lowerOw, double upperOw, double uFlow) {
			this(new MathNumber(lowerOw), new MathNumber(upperOw), new MathNumber(uFlow), new MathNumber(-uFlow));
		}

		public boolean isOverflow(MathNumber currentHigh) {
			return currentHigh.compareTo(this.upperOw) > 0;
		}

		public boolean isNegativeOverflow(MathNumber currentLow) {
			return currentLow.compareTo(this.lowerOw) < 0;
		}

		public boolean isPrecisionUnderflow(MathNumber currentLow, MathNumber currentHigh) {
			if (this.uFlow.equals(MathNumber.ZERO)) {
				return false;
			}

			// FIX: Safely use the pre-calculated limits without try-catch blocks!
			boolean overlapPos = currentHigh.compareTo(MathNumber.ZERO) > 0 && currentLow.compareTo(this.uFlow) < 0;
			boolean overlapNeg = currentLow.compareTo(MathNumber.ZERO) < 0 && currentHigh.compareTo(this.negUFlow) > 0;

			// Safe if exactly zero
			if (currentLow.equals(MathNumber.ZERO) && currentHigh.equals(MathNumber.ZERO)) {
				return false;
			}

			return overlapPos || overlapNeg;
		}
	}

	private static final Map<NumericalSize, TypeLimitsRecord> NUMERIC_LIMITS_MAP = Map.ofEntries(
			Map.entry(NumericalSize.INT8, new TypeLimitsRecord(-128L, 127L)),
			Map.entry(NumericalSize.INT16, new TypeLimitsRecord(-32768L, 32767L)),
			Map.entry(NumericalSize.INT32, new TypeLimitsRecord(-2147483648L, 2147483647L)),

			Map.entry(NumericalSize.UINT8, new TypeLimitsRecord(0L, 255L)),
			Map.entry(NumericalSize.UINT16, new TypeLimitsRecord(0L, 65535L)),
			Map.entry(NumericalSize.UINT32, new TypeLimitsRecord(0L, 4294967295L)),

			Map.entry(NumericalSize.FLOAT8, new TypeLimitsRecord(-240.0, 240.0, 0.001953125)),
			Map.entry(NumericalSize.FLOAT16, new TypeLimitsRecord(-65504.0, 65504.0, 6.1035e-5)),
			Map.entry(NumericalSize.FLOAT32,
					new TypeLimitsRecord((double) -Float.MAX_VALUE, (double) Float.MAX_VALUE, (double) Float.MIN_NORMAL)));

	private void checkLimits(
			CheckToolWithAnalysisResults<SimpleAbstractState<PointBasedHeap, ValueEnvironment<Intervals>, TypeEnvironment<InferredTypes>>> tool,
			Statement statement,
			String varName,
			MathNumber low,
			MathNumber high,
			NumericalSize size) {

		TypeLimitsRecord limits = NUMERIC_LIMITS_MAP.get(size);

		if (limits == null)
			return;

		if (limits.isOverflow(high)) {
			tool.warnOn(statement, "[OVERFLOW] Warning: " + varName + " overflowed!");
		}
		if (limits.isNegativeOverflow(low)) {
			tool.warnOn(statement, "[NEGATIVE OVERFLOW] Warning: " + varName + " underflowed (negative overflow)!");
		}
		if (limits.isPrecisionUnderflow(low, high)) {
			tool.warnOn(statement,
					"[PRECISION UNDERFLOW] Warning: " + varName + " suffered floating-point precision underflow!");
		}
	}

	private NumericalSize size;

	public OverflowChecker(NumericalSize size) {
		this.size = size;
	}

	@Override
	public boolean visit(
			CheckToolWithAnalysisResults<SimpleAbstractState<PointBasedHeap, ValueEnvironment<Intervals>, TypeEnvironment<InferredTypes>>> tool,
			CFG graph, Statement node) {

		if (node instanceof Assignment assignment) {
			Expression leftExpression = assignment.getLeft();

			if (leftExpression instanceof VariableRef variableRef) {
				checkVariableRef(tool, variableRef, graph, node);
			}

		} else {
			if (node instanceof VariableRef variableRef) {
				checkVariableRef(tool, variableRef, graph, node);
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

		if (varRef.getParentStatement() instanceof Assignment
				&& ((Assignment) varRef.getParentStatement()).getLeft() == varRef) {
			target = varRef.getParentStatement();
		}

		if (staticType.isUntyped()) {
			if (dynamicTypes.isEmpty()) {
				tool.warnOn(target, "Variable " + id.getName() + " is not a numerical type, but has no static or dynamic type");
				return;
			}

			if (!dynamicTypes.stream().anyMatch(Type::isNumericType)) {
				tool.warnOn(target, "Variable " + id.getName() + " does not have any possible numeric types");
				return;
			}

		} else {
			if (!staticType.isNumericType()) {
				tool.warnOn(target,
						"Variable " + id.getName() + " is not a numerical type, but has static type " + staticType.toString());
				return;
			}
		}

		for (AnalyzedCFG<SimpleAbstractState<PointBasedHeap, ValueEnvironment<Intervals>, TypeEnvironment<InferredTypes>>> result : tool
				.getResultOf(graph)) {

			SimpleAbstractState<PointBasedHeap, ValueEnvironment<Intervals>, TypeEnvironment<InferredTypes>> state = result
					.getAnalysisStateAfter(target).getState();

			Intervals intervalValue = resolveInterval(state.getValueState(), id, state);

			if (intervalValue.isBottom())
				continue;

			MathNumber low = intervalValue.interval.getLow();
			MathNumber high = intervalValue.interval.getHigh();

			if (low.isMinusInfinity() || high.isPlusInfinity()) {
				tool.warnOn(target,
						"[POSSIBLE NEG/POS OVERFLOW] Detected for variable "
								+ id.getName()
								+ " -> "
								+ intervalValue.interval.toString());

				continue;
			}

			checkLimits(tool, target, id.toString(), low, high, size);
		}
	}

	private Intervals resolveInterval(
			Object valueState,
			Variable id,
			SimpleAbstractState<PointBasedHeap, ValueEnvironment<Intervals>, TypeEnvironment<InferredTypes>> state) {

		if (valueState instanceof Pentagons pentagonDomain) {
			ValueEnvironment<Intervals> intervalEnv = pentagonDomain.getInterval();

			Intervals currentInterval = intervalEnv.getState(id);

			if (currentInterval == null || currentInterval.isBottom())
				return Intervals.BOTTOM;

			for (Identifier bound : pentagonDomain.getUpperBounds().getState(id)) {
				Intervals boundInterval = intervalEnv.getState(bound);

				NumericInterval constraintNI = new NumericInterval(MathNumber.MINUS_INFINITY,
						boundInterval.interval.getHigh());
				Intervals constraint = new Intervals(constraintNI);
				try {
					currentInterval = currentInterval.glb(constraint);
				} catch (SemanticException e) {
				}
			}

			return currentInterval;
		}

		else if (valueState instanceof ValueEnvironment<?>) {
			ValueEnvironment<Intervals> env = (ValueEnvironment<Intervals>) valueState;
			return env.getState(id);
		}

		return Intervals.TOP;
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