package it.unive.scsr;

import java.util.Optional;

// Interface representing an object capable of performing a meaningful calculation while having a collection of
// evaluators to manage.
public interface ExpressionEvaluator<T, R, E extends ExpressionEvaluator<T, R, E>> {

    EvaluatorRegistry<E> registry();

    Optional<R> evaluate(T param);
}
