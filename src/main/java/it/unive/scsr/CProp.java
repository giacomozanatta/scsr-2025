package it.unive.scsr;

import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.dataflow.DataflowElement;
import it.unive.lisa.analysis.dataflow.DefiniteDataflowDomain;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.*;
import it.unive.lisa.symbolic.value.operator.*;
import it.unive.lisa.symbolic.value.operator.binary.BinaryOperator;
import it.unive.lisa.symbolic.value.operator.unary.NumericNegation;
import it.unive.lisa.symbolic.value.operator.unary.UnaryOperator;
import it.unive.lisa.util.representation.ListRepresentation;
import it.unive.lisa.util.representation.StringRepresentation;
import it.unive.lisa.util.representation.StructuredRepresentation;

import java.util.*;

public class CProp implements DataflowElement<DefiniteDataflowDomain<CProp>, CProp> {
    private final Identifier id;
    private final Integer constant;

    public CProp(Identifier id, Integer constant) {
        this.id = id;
        this.constant = constant;
    }

    public CProp(){
        this(null,null);
    }

    @Override
	public StructuredRepresentation representation() {
		return new ListRepresentation(
				new StringRepresentation(id),
				new StringRepresentation(constant));
 	}

	@Override
	public CProp pushScope(ScopeToken scope) throws SemanticException {
		return this;
	}

	@Override
	public CProp popScope(ScopeToken scope) throws SemanticException {
		return this;
	}

    @Override
    public Collection<Identifier> getInvolvedIdentifiers() {
        return Collections.singleton(id);
    }

    @Override
    public Collection<CProp> gen(Identifier identifier, ValueExpression valueExpression, ProgramPoint programPoint, DefiniteDataflowDomain<CProp> cPropDefiniteDataflowDomain) throws SemanticException {
        Collection<CProp> newGen = new HashSet<>();

        Integer val = calcConstant(valueExpression, cPropDefiniteDataflowDomain);
        if(val != null) {
            newGen.add(new CProp(identifier, val));
        }

        return newGen;
    }

    @Override
    public Collection<CProp> gen(ValueExpression valueExpression, ProgramPoint programPoint, DefiniteDataflowDomain<CProp> cPropDefiniteDataflowDomain) throws SemanticException {
        return new HashSet<>();
    }

    @Override
    public Collection<CProp> kill(Identifier identifier, ValueExpression valueExpression, ProgramPoint programPoint, DefiniteDataflowDomain<CProp> cPropDefiniteDataflowDomain) throws SemanticException {
        Set<CProp> killed = new HashSet<>();

        for (CProp el : cPropDefiniteDataflowDomain.getDataflowElements()) {
            if (el.getInvolvedIdentifiers().contains(id)) {
                killed.add(el);
            }
        }

        return killed;
    }

    @Override
    public Collection<CProp> kill(ValueExpression valueExpression, ProgramPoint programPoint, DefiniteDataflowDomain<CProp> cPropDefiniteDataflowDomain) throws SemanticException {
        return Collections.emptySet();
    }

    @Override
    public boolean canProcess(SymbolicExpression expression, ProgramPoint pp, SemanticOracle oracle) {
        return DataflowElement.super.canProcess(expression, pp, oracle);
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
        return Objects.equals(constant, other.constant) && Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, constant);
    }

    private static Integer calcConstant(ValueExpression expression, DefiniteDataflowDomain<CProp> domain) {
        if(expression == null)
            return null;

        if(expression instanceof Constant constant) {
            if (constant.getValue() instanceof Integer)
                return (Integer) constant.getValue();
        }

        if(expression instanceof Identifier i){
            for (CProp element: domain.getDataflowElements()) {
                if (element.id.equals(i))
                    return element.constant;
            }
        }

        if (expression instanceof UnaryExpression unary) {
            UnaryOperator operator = unary.getOperator();
            ValueExpression ex = (ValueExpression) unary.getExpression();

            Integer value = calcConstant(ex, domain);
            if(value != null) {
                if(operator instanceof NumericNegation){
                    return -value;
                }
            }
        }

        if (expression instanceof BinaryExpression binary) {
            BinaryOperator operator = binary.getOperator();
            ValueExpression leftValue = (ValueExpression) binary.getLeft();
            ValueExpression rightValue = (ValueExpression) binary.getRight();

            Integer leftV = calcConstant(leftValue, domain);
            Integer rightV = calcConstant(rightValue, domain);
            if (leftV == null || rightV == null)
                return null;

            if (operator instanceof AdditionOperator)
                return leftV + rightV;

            if (operator instanceof SubtractionOperator)
                return leftV - rightV;

            if (operator instanceof MultiplicationOperator)
                return leftV * rightV;

            if (operator instanceof DivisionOperator) {
                if (rightV != 0)
                    return leftV / rightV;
            }

            if (operator instanceof ModuloOperator)
                return leftV % rightV;
        }

        return null;
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