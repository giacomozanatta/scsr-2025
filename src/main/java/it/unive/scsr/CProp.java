package it.unive.scsr;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.dataflow.DataflowElement;
import it.unive.lisa.analysis.dataflow.DefiniteDataflowDomain;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.PushAny;
import it.unive.lisa.symbolic.value.Skip;
import it.unive.lisa.symbolic.value.TernaryExpression;
import it.unive.lisa.symbolic.value.UnaryExpression;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.util.representation.StringRepresentation;
import it.unive.lisa.util.representation.StructuredRepresentation;


public class CProp
        implements DataflowElement<DefiniteDataflowDomain<CProp>, CProp> {

    private final String variableName;

    private final Integer constantValue;

    public CProp(String variableName, Integer constantValue) {
        this.variableName = variableName;
        this.constantValue = constantValue;
    }

    public CProp() {
        this(null, null);
    }

    @Override
    public StructuredRepresentation representation() {
        if (variableName == null)
            return new StringRepresentation("CProp()");
        if (constantValue == null)
            return new StringRepresentation(variableName + "=?");
        return new StringRepresentation(variableName + "=" + constantValue);
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
        return Collections.emptySet();
    }
    @Override
    public Collection<CProp> gen(
            Identifier id,
            ValueExpression expression,
            ProgramPoint pp,
            DefiniteDataflowDomain<CProp> domain)
            throws SemanticException {
        Integer value = evaluateExpression(expression, domain);

        if (value != null) {
            CProp generated = new CProp(id.getName(), value);
            return Collections.singleton(generated);
        }
        return Collections.emptySet();
    }

    @Override
    public Collection<CProp> gen(
            ValueExpression expression,
            ProgramPoint pp,
            DefiniteDataflowDomain<CProp> domain)
            throws SemanticException {
        return Collections.emptySet();
    }

    @Override
    public Collection<CProp> kill(
            Identifier id,
            ValueExpression expression,
            ProgramPoint pp,
            DefiniteDataflowDomain<CProp> domain)
            throws SemanticException {

        Set<CProp> toKill = new HashSet<>();
        for (CProp elem : domain.getDataflowElements()) {
            if (elem.variableName != null
                    && elem.variableName.equals(id.getName())) {
                toKill.add(elem);
            }
        }
        return toKill;
    }

    @Override
    public Collection<CProp> kill(
            ValueExpression expression,
            ProgramPoint pp,
            DefiniteDataflowDomain<CProp> domain)
            throws SemanticException {
        return Collections.emptySet();
    }

    private Integer evaluateExpression(
            ValueExpression expr,
            DefiniteDataflowDomain<CProp> domain) {

        if (expr == null)
            return null;

        if (expr instanceof Skip || expr instanceof PushAny)
            return null;

        if (expr instanceof Constant) {
            Object val = ((Constant) expr).getValue();
            if (val instanceof Integer)
                return (Integer) val;
            else
                return null;
        }

        if (expr instanceof Identifier) {
            String name = ((Identifier) expr).getName();

            for (CProp e : domain.getDataflowElements()) {
                if (name.equals(e.variableName))
                    return e.constantValue;
            }
            return null;
        }

        if (expr instanceof UnaryExpression) {
            UnaryExpression un = (UnaryExpression) expr;
            ValueExpression sub = (ValueExpression) un.getExpression();
            Integer subVal = evaluateExpression(sub, domain);
            return null;
        }

        if (expr instanceof BinaryExpression) {
            return null;
        }

        if (expr instanceof TernaryExpression) {
            return null;
        }

        return null;
    }

    @Override
    public String toString() {
        if (variableName == null)
            return "[CProp: empty]";
        if (constantValue == null)
            return "[CProp: " + variableName + "=?]";
        return "[CProp: " + variableName + "=" + constantValue + "]";
    }
}
