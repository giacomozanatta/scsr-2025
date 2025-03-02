package it.unive.scsr.cp;

import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.operator.AdditionOperator;
import it.unive.lisa.symbolic.value.operator.DivisionOperator;
import it.unive.lisa.symbolic.value.operator.MultiplicationOperator;
import it.unive.lisa.symbolic.value.operator.SubtractionOperator;
import it.unive.scsr.CProp;
import java.text.MessageFormat;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;

import static it.unive.scsr.utils.Logging.defaultLogger;
import static java.util.Map.entry;
import static java.util.Map.ofEntries;

public class BinaryExpressionHandler extends ConstantsPropagationBase<BinaryExpression> {

    // Functional interface to quickly extract the integer value from a constant.
    private static final Function<CProp, Integer> toInt = (x) -> (Integer) x.constant.getValue();

    // Table that allows you to quickly identify the correct operation to perform. Only integer values are taken into
    // account.
    private static final Map<Class<?>, BiFunction<CProp, CProp, Integer>> binFunctions = ofEntries(
            entry(AdditionOperator.class,
                    (firstConst, secondCost) -> toInt.apply(firstConst) + toInt.apply(secondCost)),
            entry(SubtractionOperator.class,
                    (firstConst, secondCost) -> toInt.apply(firstConst) - toInt.apply(secondCost)),
            entry(MultiplicationOperator.class,
                    (firstConst, secondCost) -> toInt.apply(firstConst) * toInt.apply(secondCost)),
            entry(DivisionOperator.class,
                    (firstConst, secondCost) -> toInt.apply(firstConst) / toInt.apply(secondCost))
    );

    public BinaryExpressionHandler(BinaryExpression expression) {
        super(expression);
    }

    @Override
    public Optional<CProp> evaluate(ConstantPropagationData param) {
        // Extract the correct evaluator for the given expression, then calculate the constant associated with it when
        // possible.
        Function<SymbolicExpression, Optional<CProp>> f = (expression) ->
                registry()
                        .evaluatorFor(expression, EmptyHandler.INSTANCE)
                        .evaluate(param);

        final var oLeft = f.apply(expression.getLeft());
        final var oRight = f.apply(expression.getRight());

        if (oLeft.isEmpty() || oRight.isEmpty()) {
            defaultLogger.info(() -> MessageFormat.format("Stopping constant computation for {0}", expression));
            return Optional.empty();
        }

        // At this point both subexpressions are associated with the constant and can then be merged into a single
        // constant.
        final var leftConstant = oLeft.orElseThrow();
        final var rightConstant = oRight.orElseThrow();

        defaultLogger.info(() -> MessageFormat
                .format("Computing expression {0}; left operand is {1} and right operand is {2}",
                        expression,
                        leftConstant.constant.getValue(),
                        rightConstant.constant.getValue()));

        // Both constants were successfully calculated, so depending on the operator used, a specific integer
        // calculation will be performed. The final result will then be wrapped in a new data flow element to insert
        // into the domain.
        final var result = binFunctions
                .keySet()
                .stream()
                .filter(aClass -> aClass.isAssignableFrom(expression.getOperator().getClass()))
                .findFirst()
                .map(aClass -> binFunctions.get(aClass).apply(leftConstant, rightConstant))
                .map(integer -> new Constant(expression.getStaticType(), integer, expression.getCodeLocation()))
                .map(constant -> new CProp(param.id(), constant));

        defaultLogger.info(() -> MessageFormat
                .format("Expression {0} is equal to {1}",
                        expression, result.map(c -> c.constant).orElse(null)));

        return result;
    }
}
