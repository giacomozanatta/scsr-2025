package it.unive.scsr;

import it.unive.lisa.AnalysisException;
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
import it.unive.lisa.util.representation.ListRepresentation;
import it.unive.lisa.util.representation.StringRepresentation;
import it.unive.lisa.util.representation.StructuredRepresentation;
import org.apache.commons.lang3.NotImplementedException;

import java.util.*;

public class CProp implements DataflowElement<DefiniteDataflowDomain<CProp>, CProp> {

    private final Identifier id;
    private final Constant constant;

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

    public Integer getRawValueById(Set<CProp> elements, String id) {

        for (CProp element : elements) {
            if (element.id.getName().equals(id)) {
                return (Integer) element.constant.getValue();
            }
        }
        return null;
    }

    private boolean unSkippable(ValueExpression valueExpression) {
        return valueExpression.getStaticType() instanceof StringType;
    }

    private Integer doArithmetics(Operator operator, Integer value1, Integer value2) {

        if (operator instanceof NumericNonOverflowingAdd) {
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

    private Integer solveBinaryExpr(BinaryExpression binary, DefiniteDataflowDomain<CProp> domain) {

        Set<CProp> domainElements = domain.getDataflowElements();
        Integer leftPrevValue;
        Integer rightPrevValue;

        if (binary.getLeft() instanceof Constant constant) {
            leftPrevValue = (Integer) constant.getValue();

        } else {
            Variable variable = (Variable) binary.getLeft();
            leftPrevValue = getRawValueById(domainElements, variable.getName());
        }

        if (binary.getRight() instanceof Constant constant) {
            rightPrevValue = (Integer) constant.getValue();

        } else {
            Variable variable = (Variable) binary.getRight();
            rightPrevValue = getRawValueById(domainElements, variable.getName());
        }

        return doArithmetics(binary.getOperator(), leftPrevValue, rightPrevValue);
    }

    private Integer getRawValueFromVariable(Variable variable, DefiniteDataflowDomain<CProp> domain) {

        Optional<CProp> prev = domain.getDataflowElements().stream()
                .filter(element -> element.id.getName().equals(variable.getName()))
                .findFirst();

        if (prev.isPresent()) {
            return (Integer) prev.get().constant.getValue();

        } else {
            throw new AnalysisException("Value for variable " + variable.getName() + " was not found!");
        }
    }

    @Override
    public Collection<CProp> gen(Identifier identifier, ValueExpression expression, ProgramPoint programPoint, DefiniteDataflowDomain<CProp> domain) throws SemanticException {

        if (unSkippable(expression)) {
            return Set.of();
        }

        if (expression instanceof Constant constant) {
            return Set.of(new CProp(identifier, constant));
        }

        int value;
        if (expression instanceof UnaryExpression unaryExpr) {
            if (unaryExpr.getOperator() == NumericNegation.INSTANCE) {
                value = -getRawValueFromVariable((Variable) unaryExpr.getExpression(), domain);
            } else {
                throw new ArithmeticException("Only negations are supported in unary expressions!");
            }

        } else if (expression instanceof BinaryExpression binaryExpr) {
            value = solveBinaryExpr(binaryExpr, domain);

        } else if (expression instanceof Variable variable) {
            value = getRawValueFromVariable(variable, domain);

        } else {
            return Set.of();
        }

        CProp element = new CProp(identifier, new Constant(Int32Type.INSTANCE, value, programPoint.getLocation()));
        return Set.of(element);
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