package it.unive.scsr.utils;

import it.unive.lisa.symbolic.value.operator.AdditionOperator;
import it.unive.lisa.symbolic.value.operator.DivisionOperator;
import it.unive.lisa.symbolic.value.operator.MultiplicationOperator;
import it.unive.lisa.symbolic.value.operator.SubtractionOperator;
import it.unive.lisa.symbolic.value.operator.binary.BinaryOperator;

import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;

public abstract class BiFunctionDispatcher<T, O> {

    private final Map<Class<?>, BiFunction<T, T, O>> functionByClass;

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
    public BiFunction<T, T, O> findBy(BinaryOperator operator) {
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
    protected abstract BiFunction<T, T, O> buildAdditionFunction();
    protected abstract BiFunction<T, T, O> buildSubtractionFunction();
    protected abstract BiFunction<T, T, O> buildMultiplicationFunction();
    protected abstract BiFunction<T, T, O> buildDivisionFunction();

    // Refine the output after the calculation has been performed. By default, this function returns the output itself
    // without any modification.
    protected O polishResult(O output) {
        return output;
    }

    // Returns an optional element whenever the specified operands cannot be combined to produce a new element.
    protected Optional<O> inapplicable(T left, T right) {
        return Optional.empty();
    }

    private BiFunction<T, T, O> buildWrapper(final BiFunction<T, T, O> biFunction) {
        return (left, right) -> {
            // If the computation cannot continue (e.g. the specified operand cannot be combined), the "inapplicable"
            // element is returned, otherwise the computation proceeds by calling the specified binary function and
            // refining the result.
            return inapplicable(left, right)
                    .orElse(polishResult(biFunction.apply(left, right)));
        };
    }
}
