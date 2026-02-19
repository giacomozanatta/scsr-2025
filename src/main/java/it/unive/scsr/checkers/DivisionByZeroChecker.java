package it.unive.scsr.checkers;

import java.util.HashSet;
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
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;
import it.unive.lisa.util.numeric.MathNumber;
import it.unive.scsr.Intervals;
import it.unive.scsr.Pentagons;
import it.unive.scsr.checkers.OverflowChecker.NumericalSize;
import it.unive.scsr.helper.NumericInterval;

public class DivisionByZeroChecker implements
		SemanticCheck<SimpleAbstractState<PointBasedHeap, ValueEnvironment<Intervals>, TypeEnvironment<InferredTypes>>> {

	private NumericalSize size;

	public DivisionByZeroChecker(NumericalSize size) {
		this.size = size;
	}

	private void log(String message) {
		try (java.io.PrintWriter out = new java.io.PrintWriter(new java.io.FileWriter("checker_output.log", true))) {
			out.println(message);
		} catch (java.io.IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public boolean visit(
			CheckToolWithAnalysisResults<SimpleAbstractState<PointBasedHeap, ValueEnvironment<Intervals>, TypeEnvironment<InferredTypes>>> tool,
			CFG graph, Statement node) {

		if (node instanceof Division division)
			checkDivision(tool, graph, division);

		return true;
	}

	private void checkDivision(
			CheckToolWithAnalysisResults<SimpleAbstractState<PointBasedHeap, ValueEnvironment<Intervals>, TypeEnvironment<InferredTypes>>> tool,
			CFG graph, Division div) {

		for (AnalyzedCFG<SimpleAbstractState<PointBasedHeap, ValueEnvironment<Intervals>, TypeEnvironment<InferredTypes>>> result : tool
				.getResultOf(graph)) {

			AnalysisState<SimpleAbstractState<PointBasedHeap, ValueEnvironment<Intervals>, TypeEnvironment<InferredTypes>>> state = result
					.getAnalysisStateAfter(div.getRight());

			for (SymbolicExpression divisor : state.getComputedExpressions()) {
				try {
					Set<SymbolicExpression> reachableIds = state.getState().reachableFrom(divisor, div,
							state.getState()).elements;

					for (SymbolicExpression s : reachableIds) {
						Set<Type> types = getPossibleDynamicTypes(s, div, state.getState());

						if (!isNumeric(types))
							continue;

						Intervals iv = resolveInterval(state.getState().getValueState(), (ValueExpression) s, div,
								state.getState());

						if (iv != null && !iv.isBottom() && iv.interval != null) {
							if (iv.interval.isSingleton() && iv.interval.is(0)) {
								tool.warnOn(div, "[DEFINITE] Division by zero detected: divisor " + s + " is exactly 0");
							} else if (iv.interval.intersects(NumericInterval.ZERO)) {
								tool.warnOn(div,
										"[POSSIBLE] Division by zero: divisor " + s + " may be 0 (in " + iv.interval.toString() + ")");
							}
						}
					}
				} catch (SemanticException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private Intervals resolveInterval(
			Object valueState,
			ValueExpression symbol,
			Division div,
			SimpleAbstractState<PointBasedHeap, ValueEnvironment<Intervals>, TypeEnvironment<InferredTypes>> state)
			throws SemanticException {

		// Case A: Pentagons
		if (valueState instanceof Pentagons pentagonDomain) {
			ValueEnvironment<Intervals> intervalEnv = pentagonDomain.getInterval();

			// 1. Get raw interval
			Intervals tmpInterval = intervalEnv.eval(symbol, div, state);

			if (tmpInterval == null || tmpInterval.isBottom())
				return null;

			// CRITICAL FIX: Base interval must be tmpInterval, NOT Intervals.TOP
			Intervals currentInterval = tmpInterval;

			// 2. Apply Pentagon Refinement (Upper Bounds) if it's an Identifier
			if (symbol instanceof Identifier id) {
				for (Identifier bound : pentagonDomain.getUpperBounds().getState(id)) {
					Intervals boundInterval = intervalEnv.getState(bound);

					NumericInterval constraintNI = new NumericInterval(MathNumber.MINUS_INFINITY,
							boundInterval.interval.getHigh());
					Intervals constraint = new Intervals(constraintNI);

					currentInterval = currentInterval.glb(constraint);
				}
			}
			return currentInterval;
		}

		// Case B: Standard ValueEnvironment
		else if (valueState instanceof ValueEnvironment<?>) {
			ValueEnvironment<Intervals> env = (ValueEnvironment<Intervals>) valueState;
			return env.eval(symbol, div, state);
		}

		return Intervals.TOP;
	}

	// OPTIMIZATION: Much faster and cleaner stream operation
	private boolean isNumeric(Set<Type> types) {
		if (types == null || types.isEmpty())
			return false;
		return types.stream().anyMatch(Type::isNumericType);
	}

	// compute possible dynamic types / runtime types
	private Set<Type> getPossibleDynamicTypes(SymbolicExpression s, Division div,
			SimpleAbstractState<PointBasedHeap, ValueEnvironment<Intervals>, TypeEnvironment<InferredTypes>> state)
			throws SemanticException {

		Set<Type> possibleDynamicTypes = new HashSet<>();
		Type dynamicTypes = state.getDynamicTypeOf(s, div, state);

		if (dynamicTypes != null && !dynamicTypes.isUntyped()) {
			possibleDynamicTypes.add(dynamicTypes);
		} else if (dynamicTypes.isUntyped()) {
			Set<Type> runtimeTypes = state.getRuntimeTypesOf(s, div, state);

			// OPTIMIZATION: Stream filter prevents manual loop checks
			runtimeTypes.stream()
					.filter(t -> t != Untyped.INSTANCE)
					.forEach(possibleDynamicTypes::add);
		}

		return possibleDynamicTypes;
	}
}