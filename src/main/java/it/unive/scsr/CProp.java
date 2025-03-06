package it.unive.scsr;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.dataflow.DataflowElement;
import it.unive.lisa.analysis.dataflow.DefiniteDataflowDomain;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.UnaryExpression;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.Operator;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.symbolic.value.operator.AdditionOperator;
import it.unive.lisa.symbolic.value.operator.DivisionOperator;
import it.unive.lisa.symbolic.value.operator.ModuloOperator;
import it.unive.lisa.symbolic.value.operator.MultiplicationOperator;
import it.unive.lisa.symbolic.value.operator.SubtractionOperator;
import it.unive.lisa.symbolic.value.operator.unary.NumericNegation;
import it.unive.lisa.util.representation.ListRepresentation;
import it.unive.lisa.util.representation.StringRepresentation;
import it.unive.lisa.util.representation.StructuredRepresentation;

public class CProp implements DataflowElement<DefiniteDataflowDomain<CProp>, CProp> {
    private final Identifier identifier;
    private final Integer value;

    public CProp(Identifier identifier, Integer value) {
        this.identifier = identifier;
        this.value = value;
    }

    public CProp() {
        this.identifier = null;
        this.value = null;
    }

    @Override
    public Collection<Identifier> getInvolvedIdentifiers() {
        Set<Identifier> identifiers = new HashSet<>();
        if (identifier != null) {
            identifiers.add(identifier);
        }
        return identifiers;
    }

    private static Integer computeExpression(SymbolicExpression expr, DefiniteDataflowDomain<CProp> domaincontext) {
        Integer result = null;

        if (expr instanceof Constant) {
            Constant constant = (Constant) expr;
            if (constant.getValue() instanceof Integer) {
                result = (Integer) constant.getValue();
            }
        } else if (expr instanceof Identifier) {
            for (CProp element : domaincontext.getDataflowElements()) {
                if (element.identifier.equals(expr)) {
                    result = element.value;
                    break;
                }
            }
        } else if (expr instanceof UnaryExpression) {
            UnaryExpression unaryExpression = (UnaryExpression) expr;
            Integer evaluated = computeExpression(unaryExpression.getExpression(), domaincontext);
            if (evaluated != null && unaryExpression.getOperator() instanceof NumericNegation) {
                result = -evaluated;
            } else {
                result = evaluated;
            }
        } else if (expr instanceof BinaryExpression) {
            BinaryExpression binaryExpression = (BinaryExpression) expr;
            Integer left = computeExpression(binaryExpression.getLeft(), domaincontext);
            Integer right = computeExpression(binaryExpression.getRight(), domaincontext);
            if (left != null && right != null) {
                Operator operator = binaryExpression.getOperator();
                if (operator instanceof AdditionOperator) {
                    result = left + right;
                } else if (operator instanceof SubtractionOperator) {
                    result = left - right;
                } else if (operator instanceof MultiplicationOperator) {
                    result = left * right;
                } else if (operator instanceof DivisionOperator && right != 0) {
                    result = left / right;
                } else if (operator instanceof ModuloOperator && right != 0) {
                    result = left % right;
                }
            }
        }

        return result;
    }

    @Override
    public Collection<CProp> gen(Identifier id, ValueExpression expr, ProgramPoint pp, DefiniteDataflowDomain<CProp> domain)
            throws SemanticException {
                Set<CProp> generatedElements = new HashSet<>();
                Integer result = computeExpression(expr, domain);
                if (result != null) {
                    generatedElements.add(new CProp(id, result));
                }
            return generatedElements;
    }

    @Override
    public Collection<CProp> gen(ValueExpression expr, ProgramPoint pp, DefiniteDataflowDomain<CProp> domain)
            throws SemanticException {
                return new HashSet<>();
    }

    @Override
    public Collection<CProp> kill(Identifier id, ValueExpression expr, ProgramPoint pp, DefiniteDataflowDomain<CProp> domain)
            throws SemanticException {
                Set<CProp> toRemove = new HashSet<>();
                for (CProp element : domain.getDataflowElements()) {
                    if (element.identifier.equals(id)) {
                        toRemove.add(element);
                    }
                }
                return toRemove;
    }

    @Override
    public Collection<CProp> kill(ValueExpression expr, ProgramPoint pp, DefiniteDataflowDomain<CProp> domain)
            throws SemanticException {
                return new HashSet<>();
    }

    @Override
    public int hashCode() {
        return Objects.hash(identifier, value);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        CProp other = (CProp) obj;
        return Objects.equals(identifier, other.identifier) && Objects.equals(value, other.value);
    }


// I've changed the field names, sorry

    @Override
    public StructuredRepresentation representation() {
        return new ListRepresentation(
                new StringRepresentation(identifier),
                new StringRepresentation(value));
    }

    @Override
    public CProp pushScope(ScopeToken scope)
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
