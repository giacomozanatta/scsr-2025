package it.unive.scsr.utils;

import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.symbolic.value.operator.AdditionOperator;
import it.unive.lisa.symbolic.value.operator.DivisionOperator;
import it.unive.lisa.symbolic.value.operator.MultiplicationOperator;
import it.unive.lisa.symbolic.value.operator.SubtractionOperator;
import it.unive.lisa.symbolic.value.operator.binary.BinaryOperator;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;

public abstract class BiFunctionDispatcher<T extends Lattice<T>> {

    private final Map<Class<?>, BiFunction<T, T, T>> functionByClass;

    protected BiFunctionDispatcher() {
        this.functionByClass = Map.ofEntries(
                Map.entry(AdditionOperator.class, buildWrapper(buildAdditionFunction())),
                Map.entry(SubtractionOperator.class, buildWrapper(buildSubtractionFunction())),
                Map.entry(MultiplicationOperator.class, buildWrapper(buildMultiplicationFunction())),
                Map.entry(DivisionOperator.class, buildWrapper(buildDivisionFunction())));
    }

    /**
     * Find the most appropriate operation based on the given operator. Supported operations are
     * {@link AdditionOperator}, {@link SubtractionOperator}, {@link MultiplicationOperator} and
     * {@link DivisionOperator}.
     */
    public BiFunction<T, T, T> findBy(BinaryOperator operator) {
        return functionByClass
                .keySet()
                .stream()
                .filter(k -> k.isAssignableFrom(operator.getClass()))
                .findFirst()
                .map(functionByClass::get)
                .orElse(null);
    }

    // Protected functions that must be implemented by subclasses to create the correct function to apply when an
    // operator is provided.
    protected abstract BiFunction<T, T, T> buildAdditionFunction();
    protected abstract BiFunction<T, T, T> buildSubtractionFunction();
    protected abstract BiFunction<T, T, T> buildMultiplicationFunction();
    protected abstract BiFunction<T, T, T> buildDivisionFunction();

    /**
     * Refine the output after the calculation has been performed. By default, this function returns the output itself
     * without any modification.
     */
    protected T polishResult(T output) {
        return output;
    }

    /**
     * Returns an optional element whenever the specified operands cannot be combined to produce a new element. By
     * default, the following logic is applied:
     *  <ol>
     *      <li> When left or right is bottom, bottom is returned </li>
     *      <li> When left or right is top, top is returned </li>
     *  </ol>
     */
    protected Optional<T> inapplicable(T left, T right) {
        // As soon as one of the elements expresses a bottom element, the calculation is stopped and then bottom is
        // returned.
        if (left.isBottom() || right.isBottom()) return Optional.of(left.bottom());

        // Whenever a top element is found, the calculation is stopped and then the bottom is returned. This must be
        // done because the top element can also express something that is not an interval, so any calculation can
        // actually be performed.
        if (left.isTop() || right.isTop()) return Optional.of(left.top());

        // Whenever the above conditions are not met, an optional empty value is returned.
        return Optional.empty();
    }

    private BiFunction<T, T, T> buildWrapper(final BiFunction<T, T, T> biFunction) {
        return (left, right) -> {
            // If the computation cannot continue (e.g. the specified operand cannot be combined), the "inapplicable"
            // element is returned, otherwise the computation proceeds by calling the specified binary function and
            // refining the result.
            return inapplicable(left, right)
                    .orElse(polishResult(biFunction.apply(left, right)));
        };
    }
}
