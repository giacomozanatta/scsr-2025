package it.unive.scsr.cp;

import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.scsr.CProp;
import java.util.Optional;

public class EmptyHandler extends ConstantsPropagationBase<ValueExpression> {

    public static final EmptyHandler INSTANCE = new EmptyHandler();

    private EmptyHandler() {
        super(null);
    }

    @Override
    public Optional<CProp> evaluate(ConstantPropagationData param) {
        return Optional.empty();
    }
}
