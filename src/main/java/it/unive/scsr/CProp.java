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

    private final Identifier id;
    private final Integer constant;

    public CProp(Identifier id, Integer constVal) {
        this.id = id;
        this.constant = constVal;
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
    public boolean equals(Object obj) {
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

    public Integer evaluateExpression(ValueExpression expression, DefiniteDataflowDomain<CProp> domain) throws SemanticException {
        if (expression == null) {
            return null;
        }
        if (expression instanceof Constant) {
            Object val = ((Constant) expression).getValue();
            if (val instanceof Integer)
                return (Integer) val;
            return null;
        }
        if (expression instanceof Identifier) {
            Collection<CProp> elementsInDomain = domain.getDataflowElements();
            for (CProp element : elementsInDomain) {
                if (element.id.equals((Identifier) expression)) {
                    return element.constant;
                }
            }
            return null;
        }
        if (expression instanceof UnaryExpression) {
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
                return left / right;
            }
        }
        return null;
    }

    @Override
    public Collection<CProp> gen(Identifier identifier, ValueExpression expression, ProgramPoint pp, DefiniteDataflowDomain<CProp> domain) throws SemanticException {
        Set<CProp> result = new HashSet<>();
        Integer value = evaluateExpression(expression, domain);
        if (value != null) {
            result.add(new CProp(identifier, value));
        }
        return result;
    }

    @Override
    public Collection<CProp> gen(ValueExpression expression, ProgramPoint pp, DefiniteDataflowDomain<CProp> domain) throws SemanticException {
        Set<CProp> result = new HashSet<>();
        return result;
    }

    @Override
    public Collection<CProp> kill(Identifier identifier, ValueExpression expression, ProgramPoint pp, DefiniteDataflowDomain<CProp> domain) throws SemanticException {
        Set<CProp> result = new HashSet<>();
        Collection<CProp> elementsInDomain = domain.getDataflowElements();
        for (CProp element : elementsInDomain) {
            if (element.id.equals(identifier)) {
                result.add(element);
            }
        }
        return result;
    }

    @Override
    public Collection<CProp> kill(ValueExpression expression, ProgramPoint pp, DefiniteDataflowDomain<CProp> domain) throws SemanticException {
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
