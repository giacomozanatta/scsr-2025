package it.unive.scsr.cp;

import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.scsr.EvaluatorRegistry;

public abstract class ConstantsPropagationBase<V extends SymbolicExpression> implements ConstantPropagationEvaluator {

    public final V expression;

    public ConstantsPropagationBase(V expression) {
        this.expression = expression;
    }

    @Override
    public final EvaluatorRegistry<ConstantPropagationEvaluator> registry() {
        return ConstantPropagationEvaluator.defaultRegistry();
    }
}
