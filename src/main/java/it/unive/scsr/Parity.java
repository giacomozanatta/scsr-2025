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
import it.unive.lisa.symbolic.SymbolicExpression;
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

public class Parity implements DataflowElement<DefiniteDataflowDomain<Parity>, Parity> {

    public enum ParityValue {
        EVEN, ODD, TOP
    }

    private final String variableName;
    private final ParityValue parity;

    public Parity(String variableName, ParityValue parity) {
        this.variableName = variableName;
        this.parity = parity;
    }

    public Parity() {
        this.variableName = null;
        this.parity = null;
    }

    @Override
    public StructuredRepresentation representation() {
        if (variableName == null || parity == null) {
            return new StringRepresentation("Parity()");
        }
        return new StringRepresentation(variableName + "=" + parity);
    }

    @Override
    public Parity pushScope(ScopeToken scope) throws SemanticException {
        return this;
    }

    @Override
    public Parity popScope(ScopeToken scope) throws SemanticException {
        return this;
    }

    @Override
    public Collection<Identifier> getInvolvedIdentifiers() {
        return Collections.emptySet();
    }

    /**
     * Matches DataflowElement's signature:
     * gen(Identifier id, ValueExpression expression, ProgramPoint pp, D domain).
     */
    @Override
    public Collection<Parity> gen(Identifier id, ValueExpression expression, ProgramPoint pp,
                                  DefiniteDataflowDomain<Parity> domain) throws SemanticException {

        SymbolicExpression sym = (SymbolicExpression) expression; // cast
        ParityValue val = evaluateExpression(sym, domain);
        if (val != null) {
            Parity created = new Parity(id.getName(), val);
            return Collections.singleton(created);
        }
        return Collections.emptySet();
    }

    /**
     * Matches DataflowElement's signature:
     * gen(ValueExpression expression, ProgramPoint pp, D domain).
     */
    @Override
    public Collection<Parity> gen(ValueExpression expression, ProgramPoint pp, DefiniteDataflowDomain<Parity> domain)
            throws SemanticException {
        return Collections.emptySet();
    }

    /**
     * Matches DataflowElement's signature:
     * kill(Identifier id, ValueExpression expression, ProgramPoint pp, D domain).
     */
    @Override
    public Collection<Parity> kill(Identifier id, ValueExpression expression, ProgramPoint pp,
                                   DefiniteDataflowDomain<Parity> domain) throws SemanticException {

        Set<Parity> toKill = new HashSet<>();
        for (Parity e : domain.getDataflowElements()) {
            if (e.variableName != null && e.variableName.equals(id.getName())) {
                toKill.add(e);
            }
        }
        return toKill;
    }

    /**
     * Matches DataflowElement's signature:
     * kill(ValueExpression expression, ProgramPoint pp, D domain).
     */
    @Override
    public Collection<Parity> kill(ValueExpression expression, ProgramPoint pp, DefiniteDataflowDomain<Parity> domain)
            throws SemanticException {
        return Collections.emptySet();
    }

    private ParityValue evaluateExpression(SymbolicExpression expr, DefiniteDataflowDomain<Parity> domain) {
        if (expr == null) {
            return null;
        }
        if (expr instanceof Skip || expr instanceof PushAny) {
            return ParityValue.TOP;
        }
        if (expr instanceof Constant) {
            Object c = ((Constant) expr).getValue();
            if (c instanceof Integer) {
                int n = (Integer) c;
                return (n % 2 == 0) ? ParityValue.EVEN : ParityValue.ODD;
            }
            return ParityValue.TOP;
        }
        if (expr instanceof Identifier) {
            String name = ((Identifier) expr).getName();
            for (Parity p : domain.getDataflowElements()) {
                if (name.equals(p.variableName)) {
                    return p.parity;
                }
            }
            return ParityValue.TOP;
        }
        if (expr instanceof UnaryExpression) {
            UnaryExpression un = (UnaryExpression) expr;
            SymbolicExpression sub = un.getExpression();
            ParityValue subVal = evaluateExpression(sub, domain);
            if (subVal == null) {
                subVal = ParityValue.TOP;
            }
            String op = un.getOperator().toString().toLowerCase();
            if (op.contains("neg") || op.contains("-")) {
                if (subVal == ParityValue.EVEN) {
                    return ParityValue.EVEN;
                }
                if (subVal == ParityValue.ODD) {
                    return ParityValue.ODD;
                }
                return ParityValue.TOP;
            }
            return ParityValue.TOP;
        }
        if (expr instanceof BinaryExpression) {
            BinaryExpression bin = (BinaryExpression) expr;
            SymbolicExpression leftExpr = bin.getLeft();
            SymbolicExpression rightExpr = bin.getRight();
            ParityValue leftVal = evaluateExpression(leftExpr, domain);
            ParityValue rightVal = evaluateExpression(rightExpr, domain);
            if (leftVal == null) {
                leftVal = ParityValue.TOP;
            }
            if (rightVal == null) {
                rightVal = ParityValue.TOP;
            }
            String op = bin.getOperator().toString().toLowerCase();
            if (op.contains("add") || op.contains("+") || op.contains("sub") || op.contains("-")) {
                if (leftVal == ParityValue.TOP || rightVal == ParityValue.TOP) {
                    return ParityValue.TOP;
                }
                if (leftVal == ParityValue.EVEN && rightVal == ParityValue.EVEN) {
                    return ParityValue.EVEN;
                }
                if (leftVal == ParityValue.ODD && rightVal == ParityValue.ODD) {
                    return ParityValue.EVEN;
                }
                return ParityValue.ODD;
            }
            if (op.contains("mul") || op.contains("*")) {
                if (leftVal == ParityValue.TOP || rightVal == ParityValue.TOP) {
                    return ParityValue.TOP;
                }
                if (leftVal == ParityValue.EVEN || rightVal == ParityValue.EVEN) {
                    return ParityValue.EVEN;
                }
                return ParityValue.ODD;
            }
            if (op.contains("div") || op.contains("/")) {
                return ParityValue.TOP;
            }
            return ParityValue.TOP;
        }
        if (expr instanceof TernaryExpression) {
            return ParityValue.TOP;
        }
        return ParityValue.TOP;
    }

    @Override
    public String toString() {
        if (variableName == null || parity == null) {
            return "[Parity(): empty]";
        }
        return "[" + variableName + "=" + parity + "]";
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Parity)) {
            return false;
        }
        Parity other = (Parity) obj;
        if (variableName == null && other.variableName != null) {
            return false;
        }
        if (variableName != null && !variableName.equals(other.variableName)) {
            return false;
        }
        return parity == other.parity;
    }

    @Override
    public int hashCode() {
        int h = 1;
        h = 31 * h + (variableName == null ? 0 : variableName.hashCode());
        h = 31 * h + (parity == null ? 0 : parity.hashCode());
        return h;
    }
}
