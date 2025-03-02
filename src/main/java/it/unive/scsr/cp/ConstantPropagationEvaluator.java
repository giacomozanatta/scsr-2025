package it.unive.scsr.cp;

import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.UnaryExpression;
import it.unive.scsr.CProp;
import it.unive.scsr.ExpressionEvaluator;
import it.unive.scsr.EvaluatorRegistry;

public interface ConstantPropagationEvaluator extends
        ExpressionEvaluator<ConstantPropagationData, CProp, ConstantPropagationEvaluator> {
    // The default dispatch register to avoid checking what type of expression the dataflow function should handle.
    static EvaluatorRegistry<ConstantPropagationEvaluator> defaultRegistry() {
        return EvaluatorRegistry
                .compose(map -> {
                    map.put(BinaryExpression.class, x -> new BinaryExpressionHandler((BinaryExpression) x));
                    map.put(Constant.class, x -> new ConstantHandler((Constant) x));
                    map.put(Identifier.class, x -> new IdentifierHandler((Identifier) x));
                    map.put(UnaryExpression.class, x -> new UnaryExpressionHandler((UnaryExpression) x));
                });
    }
}