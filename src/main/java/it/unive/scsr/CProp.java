package it.unive.scsr;

import java.util.*;

import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.dataflow.DataflowElement;
import it.unive.lisa.analysis.dataflow.DefiniteDataflowDomain;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.UnaryExpression;
import it.unive.lisa.symbolic.value.ValueExpression;
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

public class CProp
        implements
        DataflowElement<
                DefiniteDataflowDomain<
                        CProp>,
                CProp> {

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

    private final Identifier id;
    private final Integer constant;

    public CProp(
            Identifier id,
            Integer constant) {
        super();
        this.id = id;
        this.constant = constant;
    }

    public CProp() {
        this(null, null);
    }

    @Override
    public StructuredRepresentation representation() {
        return new ListRepresentation(
                new StringRepresentation(id),
                new StringRepresentation(constant));
    }

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

    @Override
    public Collection<Identifier> getInvolvedIdentifiers() {
        Set<Identifier> result = new HashSet<>();
        result.add(id);
        return result;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + ((constant == null) ? 0 : constant.hashCode());
        return result;
    }

    @Override
    public boolean equals(
            Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        CProp other = (CProp) obj;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        if (constant == null) {
            if (other.constant != null)
                return false;
        } else if (!constant.equals(other.constant))
            return false;
        return true;
    }

    private static Integer valueOf(
            ValueExpression expression,
            DefiniteDataflowDomain<CProp> domain) {

        Integer result = null;

        if (expression != null) {

            // simplest case: direct constant value
            if (expression instanceof Constant) {
                Object value = ((Constant) expression).getValue();
                if (!(value instanceof Integer)) {
                    // ignore non integers
                } else {
                    result = (Integer) value;
                }

            // not a constant, if it is an identifier, retrieve its value
            } else if (expression instanceof Identifier) {
                for (CProp element : domain.getDataflowElements())
                    if (element.id.equals((Identifier) expression))
                        return element.constant;

            } else if (expression instanceof UnaryExpression) {
                UnaryExpression expr = (UnaryExpression) expression;

                result = valueOf( (ValueExpression) expr.getExpression(), domain); // recurse
                if (result != null && (expr.getOperator() instanceof NumericNegation)) {
                    result = -result;
                }

            } else if (expression instanceof BinaryExpression) {
                BinaryExpression expr = (BinaryExpression) expression;

                Integer l = valueOf((ValueExpression) expr.getLeft(), domain);
                Integer r = valueOf((ValueExpression) expr.getRight(), domain);

                if (l != null && r != null) {
                    BinaryOperator op = expr.getOperator();
                    if      (op instanceof AdditionOperator)           result = l + r;
                    else if (op instanceof SubtractionOperator)        result = l - r;
                    else if (op instanceof MultiplicationOperator)     result = l * r;
                    else if (op instanceof DivisionOperator && r != 0) result = l / r;
                }
            }
        }
        return result;
    }

    @Override
    public Collection<CProp> gen(
            Identifier id,
            ValueExpression expression,
            ProgramPoint pp,
            DefiniteDataflowDomain<CProp> domain)
            throws SemanticException {

        Set<CProp> result = new HashSet<>();
        Integer constant = valueOf(expression, domain);
        if (constant != null) {
            result.add(new CProp(id, constant));
        }
        return result;
    }

    @Override
    public Collection<CProp> gen(
            ValueExpression expression,
            ProgramPoint pp,
            DefiniteDataflowDomain<CProp> domain)
            throws SemanticException {

        Set<CProp> result = new HashSet<>();

        /*
        Integer constant = valueOf(expression, domain);
        if (constant != null) {
            result.add(new CProp(null, constant));
        }
        */

        return result;
    }

    @Override
    public Collection<CProp> kill(
            Identifier id,
            ValueExpression expression,
            ProgramPoint pp,
            DefiniteDataflowDomain<CProp> domain)
            throws SemanticException {

        Set<CProp> result = new HashSet<>();

        for (CProp prop : domain.getDataflowElements())
            if (prop.id.equals(id))
                result.add(prop);

        return result;
    }

    @Override
    public Collection<CProp> kill(
            ValueExpression expression,
            ProgramPoint pp,
            DefiniteDataflowDomain<CProp> domain)
            throws SemanticException {

        Set<CProp> result = new HashSet<>();
        return result;
    }

}