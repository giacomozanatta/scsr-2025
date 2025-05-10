package it.unive.scsr.checkers;

import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.AnalyzedCFG;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.SimpleAbstractState;
import it.unive.lisa.analysis.heap.pointbased.PointBasedHeap;
import it.unive.lisa.analysis.nonrelational.value.NonRelationalValueDomain;
import it.unive.lisa.analysis.nonrelational.value.TypeEnvironment;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.analysis.types.InferredTypes;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.type.NumericType;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class Analyzer<V extends ValueDomain<V>> {

	private final AnalyzedCFG<SimpleAbstractState<PointBasedHeap, V, TypeEnvironment<InferredTypes>>> analyzer;

	public Analyzer(
		AnalyzedCFG<SimpleAbstractState<PointBasedHeap, V, TypeEnvironment<InferredTypes>>> analyzer) {
		this.analyzer = analyzer;
	}

	/**
	 * Compute the exit state or throw a {@link RuntimeException}.
	 *
	 * @return The {@link ValueDomain} contained in the exit state.
	 * @throws RuntimeException If the lub operator fails.
	 */
	public V exitStateOrThrow() {
		try {
			return analyzer.getExitState().getState().getValueState();
		} catch (SemanticException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * @param statement The instruction from which the state will be calculated.
	 * @return The {@link AbstractState} embedded into this analysis state, containing abstract
	 * values for program variables and memory locations after the given statement.
	 */
	public SimpleAbstractState<PointBasedHeap, V, TypeEnvironment<InferredTypes>> getStateAfter(
		Statement statement) {
		return analyzer.getAnalysisStateAfter(statement).getState();
	}

	/**
	 * @param statement The statement for which the last evaluated expression will be evaluated.
	 * @return The last computed expression for the given <code>Statement</code>.
	 */
	public List<SymbolicExpression> getComputedExpression(Statement statement) {
		var expressionsSpliterator = analyzer.getAnalysisStateAfter(statement)
			.getComputedExpressions().spliterator();
		return StreamSupport.stream(expressionsSpliterator, false).toList();
	}

	/**
	 * If the static type cannot be inferred from the specified expression, its dynamic types are
	 * retrieved. If the dynamic types cannot be inferred, the runtime types are retrieved.
	 *
	 * @param expression   The expression to type.
	 * @param programPoint The program point where the types are required.
	 * @param oracle       The oracle for inter-domain communication.
	 * @return <code>Set</code> of types that have been inferred. It may be empty.
	 */
	public static Set<Type> inferTypes(SymbolicExpression expression, ProgramPoint programPoint,
		SemanticOracle oracle) {
		try {
			// Try to deduce the static type.
			var staticType = expression.getStaticType();
			if (staticType != null && !staticType.isUntyped()) {
				return Set.of(staticType);
			}

			// Try to deduce dynamic types.
			var dynamicType = oracle.getDynamicTypeOf(expression, programPoint, oracle);
			if (dynamicType != null && !dynamicType.isUntyped()) {
				return Set.of(dynamicType);
			}

			// Try to infer runtime types.
			return oracle.getRuntimeTypesOf(expression, programPoint, oracle).stream()
				.filter(t -> t != null && t != Untyped.INSTANCE).collect(Collectors.toSet());
		} catch (SemanticException e) {
			// If a SemanticException occurs, the empty set is returned.
			return Collections.emptySet();
		}
	}

	/**
	 * Compute all the identifiers that are reachable starting from the given expression.
	 *
	 * @param expression   The expression corresponding to the starting point.
	 * @param programPoint The ProgramPoint where the computation happens.
	 * @param oracle       The oracle for inter-domain communication.
	 * @return The set of all reachable expressions or an empty set if something goes wrong during
	 * the computation.
	 */
	public Set<SymbolicExpression> reachableElements(SymbolicExpression expression,
		ProgramPoint programPoint, SemanticOracle oracle) {
		try {
			return oracle.reachableFrom(expression, programPoint, oracle).elements;
		} catch (SemanticException e) {
			return Collections.emptySet();
		}
	}

	/**
	 * Checks whether the set of inferred types contains at least one {@link NumericType}.
	 *
	 * @param expression   The expression to type.
	 * @param programPoint The program point where the types are required.
	 * @param oracle       The oracle for inter-domain communication.
	 * @return <code>true</code> when at least one of the inferred types is numeric,
	 * <code>false</code> otherwise.
	 */
	public static boolean anyNumericalType(SymbolicExpression expression, ProgramPoint programPoint,
		SemanticOracle oracle) {
		// The set of inferred types must contain at least one NumericType.
		return inferTypes(expression, programPoint, oracle).stream().map(Type::getClass)
			.anyMatch(NumericType.class::isAssignableFrom);
	}

	/**
	 * Calculates a non-relational value domain for the given expression.
	 *
	 * @param environment  The environment used to evaluate the expression.
	 * @param expression   The expression to evaluate
	 * @param programPoint The program point where the evaluation happens.
	 * @param oracle       The oracle for inter-domain communication.
	 * @param <T>          A {@link NonRelationalValueDomain}
	 * @return The domain calculated for the given expression.
	 */
	public static <T extends NonRelationalValueDomain<T>> T evalOrThrow(
		ValueEnvironment<T> environment, ValueExpression expression, ProgramPoint programPoint,
		SemanticOracle oracle) {
		try {
			return environment.eval(expression, programPoint, oracle);
		} catch (SemanticException e) {
			throw new IllegalArgumentException(e);
		}
	}
}
