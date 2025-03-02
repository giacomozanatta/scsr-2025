package it.unive.scsr;

import it.unive.lisa.symbolic.SymbolicExpression;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

public class EvaluatorRegistry<T extends ExpressionEvaluator<?, ?, ?>> {

    private final Map<Class<?>, Function<? super SymbolicExpression, T>> registry;

    public EvaluatorRegistry(Map<Class<?>, Function<? super SymbolicExpression, T>> registry) {
        this.registry = registry;
    }

    public static <T extends ExpressionEvaluator<?, ?, ?>> EvaluatorRegistry<T> compose(
            Consumer<Map<Class<?>, Function<? super SymbolicExpression, T>>> consumer) {
        // Making the callee modify the register field via a function and return an instance of this class with the
        // generated composition.
        final Map<Class<?>, Function<? super SymbolicExpression, T>> evaluators = new HashMap<>();
        consumer.accept(evaluators);
        return new EvaluatorRegistry<>(evaluators);
    }

    public T evaluatorFor(SymbolicExpression valueExpression, T otherwise) {
        // Retrieves the correct evaluator for the given expression. If none are found, the evaluator handled by the
        // variable named otherwise is returned.
        return registry
                .keySet()
                .stream()
                .filter(aClass -> aClass.isAssignableFrom(valueExpression.getClass()))
                .findFirst()
                .map(aClass -> registry.get(aClass).apply(valueExpression))
                .orElse(otherwise);
    }

    @Override
    public String toString() {
        return "ExpressionRegistry{" +
                "registry=" + registry +
                '}';
    }
}
