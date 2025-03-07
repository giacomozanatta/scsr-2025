package it.unive.scsr;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;


import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.dataflow.DataflowElement;
import it.unive.lisa.analysis.dataflow.DefiniteDataflowDomain;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.util.representation.ListRepresentation;
import it.unive.lisa.util.representation.StructuredRepresentation;
import it.unive.lisa.util.representation.StringRepresentation;
import it.unive.lisa.symbolic.value.UnaryExpression;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.operator.unary.NumericNegation;
import it.unive.lisa.symbolic.value.operator.unary.UnaryOperator;
import it.unive.lisa.symbolic.value.operator.binary.BinaryOperator;
import it.unive.lisa.symbolic.value.operator.AdditionOperator;
import it.unive.lisa.symbolic.value.operator.DivisionOperator;
import it.unive.lisa.symbolic.value.operator.MultiplicationOperator;
import it.unive.lisa.symbolic.value.operator.SubtractionOperator;

public class CProp 
implements
        DataflowElement<
                // the type of dataflow domain that we want to use with this
                // analysis
                DefiniteDataflowDomain<
                        // java requires this type parameter to have this class
                        // as type in fields/methods
                        CProp>,
                // java requires this type parameter to have this class
                // as type in fields/methods
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

    public CProp(Identifier id, Integer constant) {
        this.id = id;
        this.constant = constant;
    }

    public CProp() {
        this(null, null);
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

    @Override
    public Collection<Identifier> getInvolvedIdentifiers() {
        Set<Identifier> result = new HashSet<>();
        result.add(id);
        return result;
    }

    public Integer evaluateExpression(ValueExpression expression, 
                        DefiniteDataflowDomain<CProp> domain) throws SemanticException {

        if (expression == null) {
            return null;
        }

        if (expression instanceof Constant) {
            // for constants, we simply retrieve the value and return it
            Object val =  ((Constant) expression).getValue();
            if (val instanceof Integer) 
                return (Integer) val;
            return null;
        }

        if (expression instanceof Identifier) {
            // for identifiers, we look for the value in the domain and 
            // return it if found
            Collection<CProp> elementsInDomain = domain.getDataflowElements();
            for (CProp element : elementsInDomain) {
                if (element.id.equals((Identifier) expression)) {
                    return element.constant;
                }
            }
            return null;
        }
        
        if (expression instanceof UnaryExpression) {
            // we only consider unary expressions that are negations of integers
            UnaryExpression expr = (UnaryExpression) expression;
            UnaryOperator operator = expr.getOperator();
            if (operator instanceof NumericNegation) {
                Integer operand = evaluateExpression((ValueExpression) expr.getExpression(), domain);
                if (operand == null) {
                    return null;
                }
                return -operand;
            }
        }

        if (expression instanceof BinaryExpression) {
            // we only consider binary expressions that are operations on integers
            // such as addition, subtraction, multiplication and division
            BinaryExpression expr = (BinaryExpression) expression;
            BinaryOperator operator = expr.getOperator();
            
            Integer left = evaluateExpression((ValueExpression) expr.getLeft(), domain);
            Integer right = evaluateExpression((ValueExpression) expr.getRight(), domain);
            
            if (left == null || right == null) {
                return null;
            }
            if (operator instanceof AdditionOperator) {
                return left + right;
            }
            if (operator instanceof SubtractionOperator) {
                return left - right;
            }
            if (operator instanceof MultiplicationOperator) {
                return left * right;
            }
            if (operator instanceof DivisionOperator) {
                if (right == 0) {
                    return null;
                }
                return left / right;
            }
        
        }

        return null;
        }

    @Override
    public Collection<CProp> gen(
            Identifier id,
            ValueExpression expression,
            ProgramPoint pp,
            DefiniteDataflowDomain<CProp> domain)
            throws SemanticException {
        
        Set<CProp> result = new HashSet<>();
        // for an assignment, if the expression evaluate to a non-null integer value,
        // we add the pair (id, value) to the result
        Integer value = evaluateExpression(expression, domain);
        if (value != null) {
            result.add(new CProp(id, value));
        }
        return result;
    }

    @Override
    public Collection<CProp> gen(
            ValueExpression expression,
            ProgramPoint pp,
            DefiniteDataflowDomain<CProp> domain)
            throws SemanticException {
        // for a non-assignment, there is nothing to generate
        Set<CProp> result = new HashSet<>();
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
        // for an assignment, we need to kill all assignment to the same identifier
        // that are contained in the domain
        Collection<CProp> elementsInDomain = domain.getDataflowElements();
        for (CProp element : elementsInDomain) {
            if (element.id.equals(id)) {
                result.add(element);
            }
        }
        return result;
    }

    @Override
    public Collection<CProp> kill(
            ValueExpression expression,
            ProgramPoint pp,
            DefiniteDataflowDomain<CProp> domain)
            throws SemanticException {
        // for a non-assignment, there is nothing to kill
        Set<CProp> result = new HashSet<>();
        return result;
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