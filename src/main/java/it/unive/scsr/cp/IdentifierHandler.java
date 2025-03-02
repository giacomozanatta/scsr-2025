package it.unive.scsr.cp;

import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.scsr.CProp;
import java.text.MessageFormat;
import java.util.Optional;

import static it.unive.scsr.utils.Logging.defaultLogger;

public class IdentifierHandler extends ConstantsPropagationBase<Identifier> {

    public IdentifierHandler(Identifier expression) {
        super(expression);
    }

    @Override
    public Optional<CProp> evaluate(ConstantPropagationData param) {
        defaultLogger.info(() -> MessageFormat
                .format("Dealing with identifier {0} and the with domain {1}",
                        param.id(),
                        param.domain()));

        // Searches for a variable in the domain that contains a constant and identified by the expression held by this
        // object.
        final var identifierInDomain = param
                .domain()
                .getDataflowElements()
                .stream()
                .filter(elem -> elem.id.equals(expression))
                .findFirst();

        if (identifierInDomain.isEmpty()) {
            defaultLogger.info(() -> MessageFormat.format("Constant for {0} cannot be computed", param.id()));
            return Optional.empty();
        }

        final var newConstant = new Constant(expression.getStaticType(),
                identifierInDomain.orElseThrow().constant.getValue(),
                expression.getCodeLocation());

        defaultLogger.info(() -> MessageFormat
                .format("Constant for {0} has value {1}",
                        param.id(),
                        newConstant.getValue()));

        // The returned value will keep the same constant found in the domain but with a potentially different
        // identifier.
        return Optional.of(new CProp(param.id(), newConstant));
    }
}
