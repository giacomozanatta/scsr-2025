package it.unive.scsr;

import java.util.Collection;
import java.util.Collections;

import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.dataflow.DataflowElement;
import it.unive.lisa.analysis.dataflow.DefiniteDataflowDomain;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.UnaryExpression;
import it.unive.lisa.symbolic.value.operator.unary.NumericNegation;
import it.unive.lisa.symbolic.value.operator.unary.UnaryOperator;
import it.unive.lisa.symbolic.value.operator.binary.BinaryOperator;
import it.unive.lisa.symbolic.value.operator.AdditionOperator;
import it.unive.lisa.symbolic.value.operator.SubtractionOperator;
import it.unive.lisa.symbolic.value.operator.MultiplicationOperator;
import it.unive.lisa.symbolic.value.operator.DivisionOperator;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.util.representation.ListRepresentation;
import it.unive.lisa.util.representation.StringRepresentation;
import it.unive.lisa.util.representation.StructuredRepresentation;

public class CProp 
        implements
        DataflowElement<
                DefiniteDataflowDomain<
                        CProp>,
                CProp>


{

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

    // constructor
    public CProp(Identifier id, Integer constant) {
        this.id = id;
        this.constant = constant;
    }

    // constructor with null initialization
    public CProp() {
        this.id = null;
        this.constant = null;
    }

    // hashCode method
    public int hashCode() { 
        return id.hashCode() + constant.hashCode();
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        CProp other = (CProp) obj;
        return id.equals(other.id) && constant.equals(other.constant);
    }

    @Override
    public Collection<Identifier> getInvolvedIdentifiers() {
        return Collections.singleton(id);
    }

    public Integer eval(ValueExpression expression, DefiniteDataflowDomain<CProp> domain) throws SemanticException {
        if (expression == null)
			return null;

		if (expression instanceof Constant) {
			Object value = ((Constant) expression).getValue();
			if (value instanceof Integer)
                return (Integer) value;
        }

        if (expression instanceof Identifier) {
            Collection<CProp> elements = domain.getDataflowElements();
            for (CProp element : elements) {
                if (element.id.equals(expression)) {
                    return element.constant;
                }
            }
        }

        if (expression instanceof UnaryExpression) {
            UnaryExpression unary = (UnaryExpression) expression;
            UnaryOperator op = unary.getOperator(); 
            if (op instanceof NumericNegation) {
                return -eval((ValueExpression) unary.getExpression(), domain);
            }
        }

        if (expression instanceof BinaryExpression) {
            BinaryExpression binary = (BinaryExpression) expression;
            BinaryOperator op = binary.getOperator();

            ValueExpression left = (ValueExpression) binary.getLeft();
            ValueExpression right = (ValueExpression) binary.getRight();

            if (left == null || right == null) {
                return null;
            }

            if (op instanceof AdditionOperator) {
                return eval(left, domain) + eval(right, domain);
            }
            if (op instanceof SubtractionOperator) {
                return eval(left, domain) - eval(right, domain);
            }
            if (op instanceof MultiplicationOperator) {
                return eval(left, domain) * eval(right, domain);
            }
            if (op instanceof DivisionOperator) {
                return eval(left, domain) / eval(right, domain);
            }
        }

        return null;
    }


    @Override
    public Collection<CProp> gen(Identifier id, ValueExpression expression, ProgramPoint pp, DefiniteDataflowDomain<CProp> domain) throws SemanticException {
        
        Collection<CProp> result = Collections.emptySet();

        // compute value of expression
        Integer value = eval(expression, domain);
        if (value != null) {
            result.add(new CProp(id, value));
        }

        return result;
    }

    @Override
    public Collection<CProp> gen(ValueExpression expression, ProgramPoint pp, DefiniteDataflowDomain<CProp> domain) {
        // for a non-assignment statement we don't generate anything, so we return the empty set
        return Collections.emptySet();
    }

    @Override
    public Collection<CProp> kill(Identifier id, ValueExpression expression, ProgramPoint pp, DefiniteDataflowDomain<CProp> domain) {
        // if the id of the assignment is in the domain, we need to kill it 
        Collection<CProp> result = Collections.emptySet();
        for (CProp element : domain.getDataflowElements()) {
            if (element.id.equals(id)) {
                result.add(element);
            }
        }
        return result;
    }

    @Override
    public Collection<CProp> kill(ValueExpression expression, ProgramPoint pp, DefiniteDataflowDomain<CProp> domain) {
        // for a non-assignment statement we don't kill anything, so we return the empty set
        return Collections.emptySet();
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
}