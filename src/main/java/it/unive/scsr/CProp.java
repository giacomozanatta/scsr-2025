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
    private final Identifier id;
    private final Integer constant;

    public CProp() {
        this(null, null);
    }

    public CProp(Identifier id, Integer constant) {
        this.id = id;
        this.constant = constant;
    }

    @Override
    public Collection<Identifier> getInvolvedIdentifiers() {
        return id == null ? Collections.emptySet() : Collections.singleton(id);
    }

    @Override
    public Collection<CProp> gen(Identifier identifier, ValueExpression valueExpression, ProgramPoint programPoint, DefiniteDataflowDomain<CProp> domain) throws SemanticException {
        Set<CProp> generatedSet = new HashSet<>();
        Optional<Integer> value = evaluateExpression(valueExpression, domain);
        value.ifPresent(val -> generatedSet.add(new CProp(identifier, val)));
        return generatedSet;
    }

    @Override
    public Collection<CProp> gen(ValueExpression valueExpression, ProgramPoint programPoint, DefiniteDataflowDomain<CProp> domain) throws SemanticException {
        return Collections.emptyList();
    }

    @Override
    public Collection<CProp> kill(Identifier identifier, ValueExpression valueExpression, ProgramPoint programPoint, DefiniteDataflowDomain<CProp> domain) throws SemanticException {
        Set<CProp> removedSet = new HashSet<>();
        for (CProp cp : domain.getDataflowElements()) {
            if (cp.id.equals(identifier)) {
                removedSet.add(cp);
            }
        }
        return removedSet;
    }

    @Override
    public Collection<CProp> kill(ValueExpression valueExpression, ProgramPoint programPoint, DefiniteDataflowDomain<CProp> domain) throws SemanticException {
        return Collections.emptyList();
    }

    @Override
    public String toString() {
        return representation().toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CProp other = (CProp) o;
        return Objects.equals(id, other.id) && Objects.equals(constant, other.constant);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, constant);
    }

    private static Optional<Integer> evaluateExpression(SymbolicExpression expression, DefiniteDataflowDomain<CProp> domain) {
        if (expression instanceof Constant) {
            Object value = ((Constant) expression).getValue();
            if (value instanceof Integer) {
                return Optional.of((Integer) value);
            }
            return Optional.empty();
        }

        if (expression instanceof Identifier) {
            for (CProp cp : domain.getDataflowElements()) {
                if (cp.id.equals(expression)) {
                    return Optional.ofNullable(cp.constant);
                }
            }
            return Optional.empty();
        }

        if (expression instanceof UnaryExpression) {
            UnaryExpression unaryExpr = (UnaryExpression) expression;
            Optional<Integer> value = evaluateExpression(unaryExpr.getExpression(), domain);
            if (unaryExpr.getOperator() == NumericNegation.INSTANCE) {
                return value.map(val -> -val);
            }
            return value;
        }

        if (expression instanceof BinaryExpression) {
            BinaryExpression binaryExpr = (BinaryExpression) expression;
            Optional<Integer> left = evaluateExpression(binaryExpr.getLeft(), domain);
            Optional<Integer> right = evaluateExpression(binaryExpr.getRight(), domain);
            Operator operator = binaryExpr.getOperator();

            if (left.isEmpty() || right.isEmpty()) {
                return Optional.empty();
            }

            if (operator instanceof AdditionOperator) {
                return Optional.of(left.get() + right.get());
            }
            if (operator instanceof SubtractionOperator) {
                return Optional.of(left.get() - right.get());
            }
            if (operator instanceof MultiplicationOperator) {
                return Optional.of(left.get() * right.get());
            }
            if (operator instanceof DivisionOperator) {
                return right.get() == 0 ? Optional.empty() : Optional.of(left.get() / right.get());
            }
            if (operator instanceof ModuloOperator) {
                return right.get() == 0 ? Optional.empty() : Optional.of(left.get() % right.get());
            }
        }

        return Optional.empty();
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
}