package it.unive.scsr;

import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.dataflow.DataflowElement;
import it.unive.lisa.analysis.dataflow.DefiniteDataflowDomain;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.*;
import it.unive.lisa.symbolic.value.operator.AdditionOperator;
import it.unive.lisa.symbolic.value.operator.DivisionOperator;
import it.unive.lisa.symbolic.value.operator.MultiplicationOperator;
import it.unive.lisa.symbolic.value.operator.SubtractionOperator;
import it.unive.lisa.symbolic.value.operator.binary.BinaryOperator;
import it.unive.lisa.symbolic.value.operator.unary.NumericNegation;
import it.unive.lisa.symbolic.value.operator.unary.UnaryOperator;
import it.unive.lisa.util.representation.ListRepresentation;
import it.unive.lisa.util.representation.StringRepresentation;
import it.unive.lisa.util.representation.StructuredRepresentation;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class CProp implements DataflowElement<DefiniteDataflowDomain<CProp>, CProp> {
    // IMPLEMENTATION NOTE:
    // the code below is outside of the scope of the course. You can uncomment
    // it to get your code to compile. Be aware that the code is written
    // expecting that a field named "id" and a field named "constant" exist
    // in this class: if you name them differently, change also the code below
    // to make it work by just using the name of your choice instead of
    // "id"/"constant". If you don't have these fields in your
    // solution, then you should make sure that what you are doing is correct :)

    // - Implement your solution using the DefiniteDataFlowDomain.
    //   - What would happen if you used a PossibleDataFlowDomain instead? Think about it (or try it), but remember to deliver the Definite version.
    // - Keep it simple: track only integer values. Any non-integer values should be ignored.
    // - To test your implementation, you can use the inputs/cprop.imp file or define your own test cases.
    // - Refer to the Java test methods discussed in class and adjust them accordingly to work with your domain.
    // - How should integer constant values be propagated?
    //   - Consider the following code snippet:
    //       1. x = 1
    //       2. y = x + 2
    //     The expected output should be:
    //       1. [x,1]
    //       2. [x,1] [y,3]
    //   - How can you retrieve the constant value of `x` to use at program point 2?
    //   - When working with an object of type `Constant`, you can obtain its value by calling the `getValue()` method.

    @Override
    public CProp pushScope(
            ScopeToken scope)
            throws SemanticException {
        return this;
    }

    @Override
    public CProp popScope(
            ScopeToken scope)
            throws SemanticException {
        return this;
    }

    // ----------------------------------------------------------------------------------------

    private Identifier id;
    private int constant;

    public CProp() {}

    public CProp(Identifier id, int constant) {
        this.id = id;
        this.constant = constant;
    }

    @Override
    public StructuredRepresentation representation() {
        return new ListRepresentation(
                new StringRepresentation(id),
                new StringRepresentation(constant));
    }

    public static class ConstantException extends Exception {

        public ConstantException() {
        }

        public ConstantException(String message) {
            super(message);
        }

        public ConstantException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    @Override
    public Collection<Identifier> getInvolvedIdentifiers() {
        Set<Identifier> ids = new HashSet<>();
        ids.add(id);
        return ids;
    }

    private static int evaluateConstant(ValueExpression expression, DefiniteDataflowDomain<CProp> domain) throws ConstantException {
        // If the expression is a int constant, we simply return its value
        if (expression instanceof Constant constant) {
            if (constant.getValue() instanceof Integer)
                return (Integer) constant.getValue();
        }

        // If the expression is an identifier, we check if we are already tracking it.
        // If we're tracking it, it means it is constant, so we return its value
        if (expression instanceof Identifier identifier) {
            for (CProp cp : domain.getDataflowElements()) {
                if (cp.getInvolvedIdentifiers().contains(identifier))
                    return cp.constant;
            }
        }

        if (expression instanceof UnaryExpression unaryExpression) {
            UnaryOperator operator = unaryExpression.getOperator();
            // We only support the -x operator
            if (operator instanceof NumericNegation) {
                return - evaluateConstant((ValueExpression) unaryExpression.getExpression(), domain);
            }
        }

        if (expression instanceof BinaryExpression binaryExpression) {
            BinaryOperator operator = binaryExpression.getOperator();

            // We support x + y, x - y, x * y, x / y

            if (operator instanceof AdditionOperator) {
                return evaluateConstant((ValueExpression) binaryExpression.getLeft(), domain)
                        + evaluateConstant((ValueExpression) binaryExpression.getRight(), domain);
            }

            if (operator instanceof SubtractionOperator) {
                return evaluateConstant((ValueExpression) binaryExpression.getLeft(), domain)
                        - evaluateConstant((ValueExpression) binaryExpression.getRight(), domain);
            }

            if (operator instanceof MultiplicationOperator) {
                return evaluateConstant((ValueExpression) binaryExpression.getLeft(), domain)
                        * evaluateConstant((ValueExpression) binaryExpression.getRight(), domain);
            }

            if (operator instanceof DivisionOperator) {
                return evaluateConstant((ValueExpression) binaryExpression.getLeft(), domain)
                        / evaluateConstant((ValueExpression) binaryExpression.getRight(), domain);
            }
        }

        // All the ifs above return. If we reach this point, it means that the expression
        // is either not constant or not supported.
        throw new ConstantException("Expression " + expression + " is not constant");
    }

    @Override
    public Collection<CProp> gen(Identifier id, ValueExpression expression, ProgramPoint pp, DefiniteDataflowDomain<CProp> domain) throws SemanticException {
        Set<CProp> generated = new HashSet<>();
        try {
            int constant = evaluateConstant(expression, domain);
            generated.add(new CProp(id, constant));
        } catch (ConstantException e) {
            // We simply keep the "generated" set empty: no constant has been generated
        }

        return generated;
    }

    @Override
    public Collection<CProp> gen(ValueExpression expression, ProgramPoint pp, DefiniteDataflowDomain<CProp> domain) throws SemanticException {
        // The value is not stored in an identifier: we don't track it
        return new HashSet<>();
    }

    @Override
    public Collection<CProp> kill(Identifier id, ValueExpression expression, ProgramPoint pp, DefiniteDataflowDomain<CProp> domain) throws SemanticException {
        Set<CProp> killed = new HashSet<>();

        // If an identifier is rewritten, the previous value is killed
        for (CProp cp : domain.getDataflowElements()) {
            if (cp.getInvolvedIdentifiers().contains(id)) {
                killed.add(cp);
            }
        }

        return killed;
    }

    @Override
    public Collection<CProp> kill(ValueExpression expression, ProgramPoint pp, DefiniteDataflowDomain<CProp> domain) throws SemanticException {
        // The value is not stored in an identifier: no need to kill any variable
        return new HashSet<>();
    }
}