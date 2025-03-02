package it.unive.scsr.cp;

import it.unive.lisa.symbolic.value.Constant;
import it.unive.scsr.CProp;
import java.util.Optional;

public class ConstantHandler extends ConstantsPropagationBase<Constant> {

    public ConstantHandler(Constant expression) {
        super(expression);
    }

    @Override
    public Optional<CProp> evaluate(ConstantPropagationData param) {
        if (!(expression.getStaticType().isNumericType() && expression.getValue() instanceof Integer)) {
            // Since only numeric constants are taken into account, when the type is not numeric DataFlowElement cannot
            // be calculated correctly. Also, only integer constants are handled correctly.
            return Optional.empty();
        }

        return Optional.of(new CProp(param.id(), expression));
    }
}
