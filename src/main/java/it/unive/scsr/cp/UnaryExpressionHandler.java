package it.unive.scsr.cp;

import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.UnaryExpression;
import it.unive.lisa.symbolic.value.operator.unary.NumericNegation;
import it.unive.scsr.CProp;
import java.text.MessageFormat;
import java.util.Optional;

import static it.unive.scsr.utils.Logging.defaultLogger;

public class UnaryExpressionHandler extends ConstantsPropagationBase<UnaryExpression> {

    public UnaryExpressionHandler(UnaryExpression expression) {
        super(expression);
    }

    @Override
    public Optional<CProp> evaluate(ConstantPropagationData param) {
        return registry()
                .evaluatorFor(expression.getExpression(), EmptyHandler.INSTANCE)
                .evaluate(param)
                .map(prop -> {
                    // When dealing with the unary expression, only numerical negation is taken into account for the
                    // purpose of the activity. When numerical negation is given, constant values are negated.
                    if (expression.getOperator() == NumericNegation.INSTANCE) {
                        final var newConstant =
                                new Constant(expression.getStaticType(),
                                        -((Integer) prop.constant.getValue()),
                                        expression.getCodeLocation());

                        return new CProp(param.id(), newConstant);
                    }

                    defaultLogger.info(() ->
                            MessageFormat
                                    .format("The only known operator to deal with is {0}",
                                            NumericNegation.INSTANCE));

                    // In all other cases there is now a mapping function that knows how to handle more specific
                    // operators.
                    return null;
                });
    }
}
