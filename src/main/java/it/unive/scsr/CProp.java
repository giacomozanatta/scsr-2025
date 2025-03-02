package it.unive.scsr;

import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.dataflow.DataflowElement;
import it.unive.lisa.analysis.dataflow.DefiniteDataflowDomain;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.*;
import it.unive.lisa.symbolic.value.operator.*;
import it.unive.lisa.symbolic.value.operator.binary.BinaryOperator;
import it.unive.lisa.symbolic.value.operator.unary.NumericNegation;
import it.unive.lisa.symbolic.value.operator.unary.UnaryOperator;
import it.unive.lisa.util.representation.ListRepresentation;
import it.unive.lisa.util.representation.StringRepresentation;
import it.unive.lisa.util.representation.StructuredRepresentation;

import java.util.*;

public class CProp implements DataflowElement<DefiniteDataflowDomain<CProp>,CProp>{

    private final Identifier id;

    private final Integer constant;

    public CProp(Identifier id, Integer constant) {
        super();
        this.id = id;
        this.constant = constant;
    }

    public CProp(){
        this(null, null);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, constant);
    }

    @Override
    public boolean equals(Object obj) {
        if(obj == null)
            return false;
        if(obj == this)
            return true;
        if (obj.getClass() != this.getClass())
            return false;
        CProp other = (CProp) obj;
        return Objects.equals(id, other.id) && Objects.equals(constant, other.constant);
    }

    @Override
    public Collection<Identifier> getInvolvedIdentifiers() {
        Set<Identifier> result = new HashSet<>();
        result.add(id);
        return result;
    }

    private static Integer eval(ValueExpression expr, DefiniteDataflowDomain<CProp> domain ) {
        if(expr == null)
            return null;

        if(expr instanceof Constant)
            return (Integer) ((Constant) expr).getValue();

        if(expr instanceof Identifier){
            Identifier ide = (Identifier) expr;
            for ( CProp element : domain.getDataflowElements()) {
                if (element.id.equals(ide))
                    return element.constant;
            }
        }

        if (expr instanceof UnaryExpression) {
            UnaryExpression unary = (UnaryExpression) expr;
            UnaryOperator operator = unary.getOperator();
            ValueExpression ex = (ValueExpression) unary.getExpression();

            Integer value = eval(ex, domain);
            if (value == null)
                return null;
            if (operator instanceof NumericNegation)
                return -value;
        }

        if (expr instanceof BinaryExpression) {
            BinaryExpression binary = (BinaryExpression) expr;
            BinaryOperator operator = binary.getOperator();
            ValueExpression left = (ValueExpression) binary.getLeft();
            ValueExpression right = (ValueExpression) binary.getRight();

            // To integer
            Integer lvalue = eval(left, domain);
            Integer rvalue = eval(right, domain);
            if (lvalue == null || rvalue == null)
                return null;
            if (operator instanceof AdditionOperator)
                return lvalue + rvalue;
            if (operator instanceof SubtractionOperator)
                return lvalue - rvalue;
            if (operator instanceof MultiplicationOperator)
                return lvalue * rvalue;
            if (operator instanceof DivisionOperator) {
                if (rvalue == 0)
                    return null;
                return lvalue / rvalue;
            }
            if (operator instanceof ModuloOperator)
                return lvalue % rvalue;
        }


        return  null;
    }

    @Override
    public Collection<CProp> gen(Identifier identifier, ValueExpression valueExpression, ProgramPoint programPoint, DefiniteDataflowDomain<CProp> cPropDefiniteDataflowDomain) throws SemanticException {
        Integer constant = eval(valueExpression, cPropDefiniteDataflowDomain);
        CProp p = new CProp(identifier, constant);
        Set<CProp> result = new HashSet<>();

        if(constant != null){
            result.add(p);
        }

        return result;
    }

    @Override
    public Collection<CProp> gen(ValueExpression valueExpression, ProgramPoint programPoint, DefiniteDataflowDomain<CProp> cPropDefiniteDataflowDomain) throws SemanticException {
        return new HashSet<>();
    }

    @Override
    public Collection<CProp> kill(Identifier identifier, ValueExpression valueExpression, ProgramPoint programPoint, DefiniteDataflowDomain<CProp> cPropDefiniteDataflowDomain) throws SemanticException {
        Set<CProp> result = new HashSet<>();

        for ( CProp element : cPropDefiniteDataflowDomain.getDataflowElements()) {
            if (element.id.equals(identifier))
                result.add(element);
        }

        return result;
    }

    @Override
    public Collection<CProp> kill(ValueExpression valueExpression, ProgramPoint programPoint, DefiniteDataflowDomain<CProp> cPropDefiniteDataflowDomain) throws SemanticException {
        return List.of();
    }

    @Override
    public CProp pushScope(ScopeToken scopeToken) throws SemanticException {
        return this;
    }

    @Override
    public CProp popScope(ScopeToken scopeToken) throws SemanticException {
        return this;
    }

    @Override
    public StructuredRepresentation representation() {
        return new ListRepresentation(
                new StringRepresentation(id),
                new StringRepresentation(constant));

    }

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

}