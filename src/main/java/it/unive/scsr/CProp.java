package it.unive.scsr;

import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.dataflow.DataflowElement;
import it.unive.lisa.analysis.dataflow.DefiniteDataflowDomain;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.*;
import it.unive.lisa.symbolic.value.operator.*;
import it.unive.lisa.symbolic.value.operator.unary.NumericNegation;
import it.unive.lisa.util.representation.ListRepresentation;
import it.unive.lisa.util.representation.StringRepresentation;
import it.unive.lisa.util.representation.StructuredRepresentation;

import java.util.*;

public class CProp implements DataflowElement<DefiniteDataflowDomain<CProp>, CProp> {
    // IMPLEMENTATION NOTE:
    // the code below is outside the scope of the course. You can uncomment
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

    private final Identifier identifier;
    private final Integer constant;


    public CProp() {
        this(null, null);
    }

    public CProp(Identifier identifier, Integer constant) {
        this.identifier = identifier;
        this.constant = constant;
    }

    @Override
    public Collection<Identifier> getInvolvedIdentifiers() {
        return Collections.singleton(identifier);
    }

    @Override
    public Collection<CProp> gen(Identifier identifier, ValueExpression valueExpression, ProgramPoint programPoint, DefiniteDataflowDomain<CProp> cPropDefiniteDataflowDomain) throws SemanticException {
        Set<CProp> genSet = new HashSet<>();
        Integer value = evalExpression(valueExpression, cPropDefiniteDataflowDomain);
        if(value != null)
            genSet.add(new CProp(identifier, value));

        return genSet;
    }

    @Override
    public Collection<CProp> gen(ValueExpression valueExpression, ProgramPoint programPoint, DefiniteDataflowDomain<CProp> cPropDefiniteDataflowDomain) throws SemanticException {
        return Collections.emptyList();
    }

    @Override
    public Collection<CProp> kill(Identifier identifier, ValueExpression valueExpression, ProgramPoint programPoint, DefiniteDataflowDomain<CProp> cPropDefiniteDataflowDomain) throws SemanticException {
        Collection<CProp> res = new HashSet<>();

        for(CProp cp : cPropDefiniteDataflowDomain.getDataflowElements())
            if(cp.identifier.equals(identifier))
                res.add(cp);

        return res;
    }

    @Override
    public Collection<CProp> kill(ValueExpression valueExpression, ProgramPoint programPoint, DefiniteDataflowDomain<CProp> cPropDefiniteDataflowDomain) throws SemanticException {
        return Collections.emptyList();
    }

    @Override
    public String toString() {
        return representation().toString();
    }

    @Override
    public boolean equals(Object o) {
        if(this == o) return true;
        if((o == null) || (getClass() != o.getClass())) return false;

        CProp other = (CProp) o;
        if (identifier == null && other.identifier != null)
            return false;
        if (identifier != null && !identifier.equals(other.identifier))
            return false;
        if (constant == null && other.constant != null)
            return false;
        if (constant != null && !constant.equals(other.constant))
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((identifier == null) ? 0 : identifier.hashCode());
        result = prime * result + ((constant == null) ? 0 : constant.hashCode());
        return result;
    }

    private static Integer evalExpression(SymbolicExpression symbolicExpression, DefiniteDataflowDomain<CProp> cPropDefiniteDataflowDomain) {
        if(symbolicExpression instanceof Constant) {
            Constant c = (Constant) symbolicExpression;
            if(c.getValue() instanceof Integer)
                return (Integer) c.getValue();
            else
                return null;
        }

        if(symbolicExpression instanceof Identifier) {
            for(CProp cp : cPropDefiniteDataflowDomain.getDataflowElements())
                if(cp.identifier.equals(symbolicExpression))
                    return cp.constant;

            return null;
        }

        if(symbolicExpression instanceof UnaryExpression) {
            UnaryExpression unaryExpr = (UnaryExpression) symbolicExpression;
            Integer integer = evalExpression(unaryExpr.getExpression(), cPropDefiniteDataflowDomain);

            if(integer == null)
                return null;

            if(unaryExpr.getOperator() == NumericNegation.INSTANCE)
                return -integer;
        }

        if(symbolicExpression instanceof BinaryExpression) {
            BinaryExpression binaryExpr = (BinaryExpression) symbolicExpression;
            Integer left = evalExpression(binaryExpr.getLeft(), cPropDefiniteDataflowDomain);
            Integer right = evalExpression(binaryExpr.getRight(), cPropDefiniteDataflowDomain);
            Operator operator = binaryExpr.getOperator();

            if(left == null || right == null)
                return null;
            else {
                if(operator instanceof AdditionOperator) {
                    return left + right;
                }

                if(operator instanceof SubtractionOperator) {
                    return left - right;
                }

                if(operator instanceof MultiplicationOperator) {
                    return left * right;
                }

                if(operator instanceof DivisionOperator) {
                    if(left == 0)
                        return null;
                    else
                        return (int) left/right;
                }

                if(operator instanceof ModuloOperator) {
                    if(right == 0)
                        return null;
                    else
                        return left % right;
                }
            }
        }

        return null;
    }

    @Override
	public StructuredRepresentation representation() {
		return new ListRepresentation(
				new StringRepresentation(identifier),
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