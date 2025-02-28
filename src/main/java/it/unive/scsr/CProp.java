package it.unive.scsr;

import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.dataflow.DataflowElement;
import it.unive.lisa.analysis.dataflow.DefiniteDataflowDomain;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.*;
import it.unive.lisa.symbolic.value.operator.*;
import it.unive.lisa.symbolic.value.operator.binary.BinaryOperator;
import it.unive.lisa.symbolic.value.operator.unary.UnaryOperator;
import it.unive.lisa.util.representation.ListRepresentation;
import it.unive.lisa.util.representation.StringRepresentation;
import it.unive.lisa.util.representation.StructuredRepresentation;

import java.util.*;

public class CProp implements
        DataflowElement<DefiniteDataflowDomain<CProp>,CProp> {

    private final Identifier id;

    private final Integer constant;
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


    public CProp(Identifier id, Integer constant) {
        this.id = id;
        this.constant = constant;
    }

    public CProp(){
        this(null,null);
    }

    @Override
    public Collection<Identifier> getInvolvedIdentifiers() {
        // Returns a collection of one element since it tracks a constant
        return Collections.singleton(id);
    }

    @Override
    public Collection<CProp> kill(ValueExpression expression, ProgramPoint pp, DefiniteDataflowDomain<CProp> domain) throws SemanticException {
        return Collections.emptyList();
    }

    @Override
    public Collection<CProp> kill(Identifier id, ValueExpression expression, ProgramPoint pp, DefiniteDataflowDomain<CProp> domain) throws SemanticException {
        Collection<CProp> toKill = new HashSet<>();
        CProp elem = domain.getDataflowElements().stream().filter(x->x.id.equals(id)).findFirst().orElse(null);
        if(elem != null) {
            toKill.add(elem);
        }
        return toKill;
    }

    @Override
    public Collection<CProp> gen(ValueExpression expression, ProgramPoint pp, DefiniteDataflowDomain<CProp> domain) throws SemanticException {
        return Collections.emptyList();
    }

    @Override
    public Collection<CProp> gen(Identifier id, ValueExpression expression, ProgramPoint pp, DefiniteDataflowDomain<CProp> domain) throws SemanticException {
        Collection<CProp> generated = new HashSet<>();
        Integer elem = calculateConstant(expression,domain);
        if(elem != null) {
            generated.add(new CProp(id, elem));
        }
        return generated;
    }

    private static Integer calculateConstant( ValueExpression expression,DefiniteDataflowDomain<CProp> domain) {

        if(expression == null) {
            return null;
        }

        if(expression instanceof Identifier) {
            Identifier identifier = (Identifier) expression;
            //For getting the value I need to search it in the domain
            CProp elem = domain.getDataflowElements().stream().filter(x->x.id.equals(identifier)).findFirst().orElse(null);
            if(elem != null) {
                return elem.constant;
            }
        }

        if(expression instanceof Constant){
            Constant constant = (Constant) expression;
            //Check if the constant is inside our domain (Integer)
            if(constant.getValue() instanceof Integer) {
                return (Integer) constant.getValue();
            }
        }

        if(expression instanceof UnaryExpression){
            UnaryExpression unaryExpression = (UnaryExpression) expression;
            UnaryOperator operator = unaryExpression.getOperator();

            Integer value = calculateConstant((ValueExpression) unaryExpression.getExpression(),domain);
            if(value != null) {
                if(operator instanceof NegatableOperator){
                    return -value;
                }
            }
        }

        if(expression instanceof BinaryExpression){
            BinaryExpression binaryExpression = (BinaryExpression) expression;
            BinaryOperator operator = binaryExpression.getOperator();
            //Convert the two op into a Integer
            Integer leftOp = calculateConstant((ValueExpression) binaryExpression.getLeft(),domain);
            Integer rightOp = calculateConstant((ValueExpression) binaryExpression.getRight(),domain);

            if(leftOp != null && rightOp != null) {
                if(operator instanceof AdditionOperator){
                    return leftOp + rightOp;
                }
                if(operator instanceof SubtractionOperator){
                    return leftOp - rightOp;
                }
                if(operator instanceof MultiplicationOperator){
                    return leftOp * rightOp;
                }
                if(operator instanceof DivisionOperator){
                    //If rightOp is 0, exception
                    if(rightOp != 0){
                        return leftOp / rightOp;
                    }
                    //TODO: decide if I need to return a value rather then null
                }
                if(operator instanceof ModuloOperator){
                    return leftOp % rightOp;
                }
            }
        }

        if (expression instanceof TernaryExpression){

        }

        return null;
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
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        CProp cProp = (CProp) o;
        return Objects.equals(id, cProp.id) && Objects.equals(constant, cProp.constant);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, constant);
    }
}