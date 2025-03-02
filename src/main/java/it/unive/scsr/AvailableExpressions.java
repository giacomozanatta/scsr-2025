package it.unive.scsr;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
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

public class AvailableExpressions
        // instances of this class are dataflow elements such that:
        // - their state (fields) hold the information contained into a single
        // element
        // - they provide gen and kill functions that are specific to the
        // analysis that we are executing
        implements
        DataflowElement<
                // the type of dataflow domain that we want to use with this
                // analysis
                DefiniteDataflowDomain<
                        // java requires this type parameter to have this class
                        // as type in fields/methods
                        AvailableExpressions>,
                // java requires this type parameter to have this class
                // as type in fields/methods
                AvailableExpressions> {

    private ValueExpression expression;

    public AvailableExpressions(ValueExpression expression) {
        this.expression = expression;
    }

    public AvailableExpressions() {
        this(null);
    }

    @Override
    public StructuredRepresentation representation() {
        return new StringRepresentation(expression);
    }

    @Override
    public Collection<Identifier> getInvolvedIdentifiers() {
        return getVariablesIn(expression);
    }

    public static Collection<Identifier> getVariablesIn(ValueExpression expression) {
        Collection<Identifier> result = new HashSet<>();

        if (expression == null) return result;
        if (expression instanceof Identifier) {
            result.add((Identifier) expression);
        }
        if (expression instanceof UnaryExpression unary) {
            // An unary expression could be, for example, -x.
            // We are "extracting" the x from the unary expression.
            ValueExpression valueExpression = (ValueExpression) unary.getExpression();
            result.addAll(getVariablesIn(unary));
        }

        if (expression instanceof BinaryExpression binary) {
            result.addAll(getVariablesIn((ValueExpression) binary.getLeft()));
            result.addAll(getVariablesIn((ValueExpression) binary.getRight()));
        }

        if (expression instanceof TernaryExpression ternary) {
            result.addAll(getVariablesIn((ValueExpression) ternary.getLeft()));
            result.addAll(getVariablesIn((ValueExpression) ternary.getMiddle()));
            result.addAll(getVariablesIn((ValueExpression) ternary.getRight()));
        }

        return result;
    }

    @Override
    public Collection<AvailableExpressions> gen(Identifier id, ValueExpression expression, ProgramPoint pp, DefiniteDataflowDomain<AvailableExpressions> domain) throws SemanticException {
        Set<AvailableExpressions> result = new HashSet<>();
        AvailableExpressions ae = new AvailableExpressions(expression);
        // We need to filter out some expressions (https://lisa-analyzer.github.io/structure/analysis-infrastructure.html)
        // See filter()
        // We also need to check if an identifier is already being tracked
        if (!ae.getInvolvedIdentifiers().contains(id) && filter(expression)) {
            result.add(ae);
        }

        return result;
    }

    @Override
    public Collection<AvailableExpressions> gen(ValueExpression expression, ProgramPoint pp, DefiniteDataflowDomain<AvailableExpressions> domain) throws SemanticException {
        Set<AvailableExpressions> result = new HashSet<>();
        AvailableExpressions ae = new AvailableExpressions(expression);
        // We need to filter out some expressions (https://lisa-analyzer.github.io/structure/analysis-infrastructure.html)
        // See filter()
        if (filter(expression)) {
            result.add(ae);
        }

        return result;
    }

    private static boolean filter(ValueExpression expression) {
        // We don't want to keep track of Identifier, Constant, Skip, PushAny
        if (
                expression instanceof Identifier
                || expression instanceof Constant
                || expression instanceof Skip
                || expression instanceof PushAny
        ) {
            return false;
        }

        return true;
    }

    @Override
    public Collection<AvailableExpressions> kill(Identifier id, ValueExpression expression, ProgramPoint pp, DefiniteDataflowDomain<AvailableExpressions> domain) throws SemanticException {
        Collection<AvailableExpressions> result = new HashSet<>();
        for (AvailableExpressions ae : domain.getDataflowElements()) {
            // We search in the domain any expression that contains identifier id
            // id is being redefined, so we want to kill any expression that
            // is using id
            Collection<Identifier> ids = getVariablesIn(ae.expression);
            if (ids.contains(id)) {
                result.add(ae);
            }
        }

        return result;
    }

    @Override
    public Collection<AvailableExpressions> kill(ValueExpression expression, ProgramPoint pp, DefiniteDataflowDomain<AvailableExpressions> domain) throws SemanticException {
        // There is no identifier: this means that the expression
        // will not change any definitions, so we don't kill any expression
        return new HashSet<>();
    }


    /* Out of the scope of the course: these are needed to handle calls */

    @Override
    public AvailableExpressions pushScope(
            ScopeToken scope)
            throws SemanticException {
        return this;
    }

    @Override
    public AvailableExpressions popScope(
            ScopeToken scope)
            throws SemanticException {
        return this;
    }
}