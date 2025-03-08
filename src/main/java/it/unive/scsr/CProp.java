package it.unive.scsr;

import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.dataflow.DataflowElement;
import it.unive.lisa.analysis.dataflow.DefiniteDataflowDomain;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.program.type.Int32Type;
import it.unive.lisa.symbolic.value.*;
import it.unive.lisa.symbolic.value.operator.binary.NumericNonOverflowingAdd;
import it.unive.lisa.symbolic.value.operator.binary.NumericNonOverflowingDiv;
import it.unive.lisa.symbolic.value.operator.binary.NumericNonOverflowingMul;
import it.unive.lisa.symbolic.value.operator.binary.NumericNonOverflowingSub;
import it.unive.lisa.symbolic.value.operator.unary.NumericNegation;
import it.unive.lisa.type.StringType;
import it.unive.lisa.type.Untyped;
import it.unive.lisa.util.representation.ListRepresentation;
import it.unive.lisa.util.representation.StringRepresentation;
import it.unive.lisa.util.representation.StructuredRepresentation;
import org.apache.commons.lang3.NotImplementedException;

import java.util.*;

public class CProp implements DataflowElement<DefiniteDataflowDomain<CProp>, CProp> {

    private final Identifier id;
    private final Constant constant;
    private DefiniteDataflowDomain<CProp> domain;

    public CProp() {
        this(null, null);
    }

    public CProp(Identifier id, Constant constant) {
        this.id = id;
        this.constant = constant;
    }

    @Override
    public Collection<Identifier> getInvolvedIdentifiers() {
        throw new NotImplementedException();
    }

    public Integer getPrimitive(String id) {

        Integer value = null;

        for (CProp element : this.domain.getDataflowElements()) {
            if (element.id.getName().equals(id)) {
                value = (Integer) element.constant.getValue();
            }
        }
        return value;
    }

    private Integer getPrimitive(Variable variable) {

        Optional<CProp> prev = this.domain.getDataflowElements().stream()
                .filter(element -> element.id.getName().equals(variable.getName()))
                .findFirst();

        return prev.map(cProp -> (Integer) cProp.constant.getValue()).orElse(null);
    }

    private boolean isSkippable(ValueExpression expression) {

        if (expression.getStaticType() instanceof StringType) {
            return true;

        } else return expression.getStaticType() instanceof Untyped;
    }

    private Integer doArithmetics(Operator operator, Integer value1, Integer value2) {

        if (value1 == null) {
            return value2;

        } else if (value2 == null) {
            return value1;

        } else if (operator instanceof NumericNonOverflowingAdd) {
            return value1 + value2;

        } else if (operator instanceof NumericNonOverflowingSub) {
            return value1 - value2;

        } else if (operator instanceof NumericNonOverflowingMul) {
            return value1 * value2;

        } else if (operator instanceof NumericNonOverflowingDiv) {
            if (value2 != 0) {
                return value1 / value2;
            } else {
                throw new ArithmeticException("Division by zero! Inaccettabile!");
            }

        } else {
            throw new ArithmeticException("Unsupported operator: " + operator);
        }
    }

    private Integer solveExpression(ValueExpression expression) throws SemanticException {

        if (expression instanceof Constant) {
            return (Integer) ((Constant) expression).getValue();

        } else if (expression instanceof Variable) {
            return getPrimitive(((Variable) expression).getName());

        } else if (expression instanceof UnaryExpression unary) {

            if (unary.getOperator() == NumericNegation.INSTANCE) {

                if (unary.getExpression() instanceof BinaryExpression binary) {
                    return  -solveExpression(binary);
                }

                return -this.getPrimitive((Variable) unary.getExpression());

            } else {
                throw new ArithmeticException("Only negations are supported in unary expressions!");
            }

        } else if (expression instanceof BinaryExpression binary) {

            Integer leftSideConstant = solveExpression((ValueExpression) binary.getLeft());
            Integer rightSideConstant = solveExpression((ValueExpression) binary.getRight());

            return doArithmetics(binary.getOperator(), leftSideConstant, rightSideConstant);

        } else {
            throw new SemanticException("Unsupported expression: " + expression.getStaticType());
        }
    }

    @Override
    public Collection<CProp> gen(Identifier identifier, ValueExpression expression, ProgramPoint programPoint,
                                 DefiniteDataflowDomain<CProp> domain) throws SemanticException {

        if (isSkippable(expression)) {
            return Set.of();
        }

        this.domain = domain;

        if (expression instanceof Constant constant) {
            return Set.of(new CProp(identifier, constant));
        }

        Integer value;
        if (expression instanceof Variable variable) {
            value = getPrimitive(variable);
        } else {
            value = solveExpression(expression);
        }

        CProp toGenerate = new CProp(identifier, new Constant(Int32Type.INSTANCE, value, programPoint.getLocation()));
        return Set.of(toGenerate);
    }

    @Override
    public Collection<CProp> gen(ValueExpression valueExpression, ProgramPoint programPoint,
                                 DefiniteDataflowDomain<CProp> domain) throws SemanticException {
        return List.of();
    }

    @Override
    public Collection<CProp> kill(Identifier identifier, ValueExpression valueExpression, ProgramPoint programPoint,
                                  DefiniteDataflowDomain<CProp> domain) throws SemanticException {

        if (!domain.getDataflowElements().isEmpty()) {
            Optional<CProp> toRemove = domain.getDataflowElements()
                    .stream().filter(element -> element.id.getName().equals(identifier.getName()))
                    .findFirst();

            if (toRemove.isPresent()) {
                return List.of(toRemove.get());
            }
        }

        return List.of();
    }

    @Override
    public Collection<CProp> kill(ValueExpression valueExpression, ProgramPoint programPoint,
                                  DefiniteDataflowDomain<CProp> domain) throws SemanticException {
        return List.of();
    }

    @Override
    public StructuredRepresentation representation() {
        return new ListRepresentation(new StringRepresentation(id), new StringRepresentation(constant));
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