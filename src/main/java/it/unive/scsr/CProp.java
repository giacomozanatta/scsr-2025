package it.unive.scsr;

import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.dataflow.DataflowElement;
import it.unive.lisa.analysis.dataflow.DefiniteDataflowDomain;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.*;
import it.unive.lisa.symbolic.value.operator.AdditionOperator;
import it.unive.lisa.symbolic.value.operator.DivisionOperator;
import it.unive.lisa.symbolic.value.operator.MultiplicationOperator;
import it.unive.lisa.symbolic.value.operator.SubtractionOperator;
import it.unive.lisa.symbolic.value.operator.unary.NumericNegation;
import it.unive.lisa.util.representation.ListRepresentation;
import it.unive.lisa.util.representation.StringRepresentation;
import it.unive.lisa.util.representation.StructuredRepresentation;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CProp implements DataflowElement<DefiniteDataflowDomain<CProp>, CProp> {

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

    // MY CODE BELOW
    private final Identifier id;
    private final Integer constant;

    public CProp(Identifier id, Integer constant) {
        this.id = id;
        this.constant = constant;
    }

    public CProp() {
        this(null, null);
    }

    @Override
    public Collection<Identifier> getInvolvedIdentifiers() {
        // A constant has only itself as identifier
        Set<Identifier> result = new HashSet<>();
        result.add(id);
        return result;
    }

    @Override
    public Collection<CProp> gen(Identifier id, ValueExpression expression, ProgramPoint pp, DefiniteDataflowDomain<CProp> domain) throws SemanticException {
        // Here we refer to assigning an expression to an identifier
        // What we add is the constant referred to the identifier, only if all the variables in the expression are propagated constant themselves
        Set<CProp> result = new HashSet<>();

        // We only consider a subset of the possible expressions
        if (!isExpressionSupported(expression)) {
            return result;
        }

        // Extract all identifiers in the provided expression
        Collection<Identifier> depIdentifiers = new AvailableExpressions(expression).getInvolvedIdentifiers();
        // Get all previous constants
        Stream<CProp> dataflowElements = domain.getDataflowElements().stream();
        // Filters all the previous constants that are referenced by the provided expression
        List<CProp> referencedConstants = dataflowElements.filter(el -> depIdentifiers.containsAll(el.getInvolvedIdentifiers())).toList();

        // If the expression references all of his constants, calculate its final value and assign to this constant
        if (referencedConstants.size() == depIdentifiers.size()) {
            result.add(new CProp(id, getExpressionValue(expression, referencedConstants)));
        }

        return result;
    }

    /**
     * Returns if an expression should be considered for its constant propagation
     */
    private boolean isExpressionSupported(SymbolicExpression expression) {
        return (expression instanceof Constant)
                || (expression instanceof Identifier)
                || (expression instanceof UnaryExpression && ((UnaryExpression) expression).getOperator() instanceof NumericNegation)
                || (expression instanceof BinaryExpression && ((BinaryExpression) expression).getOperator() instanceof AdditionOperator)
                || (expression instanceof BinaryExpression && ((BinaryExpression) expression).getOperator() instanceof SubtractionOperator)
                || (expression instanceof BinaryExpression && ((BinaryExpression) expression).getOperator() instanceof MultiplicationOperator)
                || (expression instanceof BinaryExpression && ((BinaryExpression) expression).getOperator() instanceof DivisionOperator);
    }

    /**
     * Returns the integer value of an expression, provided it is among one of the `isExpressionSupported` operations
     **/
    private Integer getExpressionValue(SymbolicExpression expression, List<CProp> referencedConstants) throws SemanticException {
        if (expression instanceof Constant) {
            // This is surely an integer since we're only handling integer constants
            return (Integer) ((Constant) expression).getValue();
        }
        if (expression instanceof Identifier) {
            // In this case it's a variable. We need to find the Constant value associated to the variable
            CProp referencedConstant = referencedConstants.stream().filter(cProp -> cProp.getInvolvedIdentifiers().contains(((Identifier) expression)))
                    .findFirst()
                    // Throws an error if not found, but we already checked it exists
                    .get();

            return referencedConstant.constant;
        }
        // We only support numeric negation among unary expressions
        if (expression instanceof UnaryExpression && ((UnaryExpression) expression).getOperator() instanceof NumericNegation) {
            SymbolicExpression innerExpression = ((UnaryExpression) expression).getExpression();

            // Explicit negation
            return -1 * getExpressionValue(innerExpression, referencedConstants);

        }
        // We only support +, -, * and / among binary expressions
        if (expression instanceof BinaryExpression) {
            if (((BinaryExpression) expression).getOperator() instanceof AdditionOperator) {
                return getExpressionValue(((BinaryExpression) expression).getLeft(), referencedConstants)
                        + getExpressionValue(((BinaryExpression) expression).getRight(), referencedConstants);
            }
            if (((BinaryExpression) expression).getOperator() instanceof SubtractionOperator) {
                return getExpressionValue(((BinaryExpression) expression).getLeft(), referencedConstants)
                        - getExpressionValue(((BinaryExpression) expression).getRight(), referencedConstants);
            }
            if (((BinaryExpression) expression).getOperator() instanceof MultiplicationOperator) {
                return getExpressionValue(((BinaryExpression) expression).getLeft(), referencedConstants)
                        * getExpressionValue(((BinaryExpression) expression).getRight(), referencedConstants);
            }
            if (((BinaryExpression) expression).getOperator() instanceof DivisionOperator) {
                return getExpressionValue(((BinaryExpression) expression).getLeft(), referencedConstants)
                        / getExpressionValue(((BinaryExpression) expression).getRight(), referencedConstants);
            }
        }

        // Otherwise, the expression is not supported
        throw new SemanticException("Unsupported SymbolicExpression" + expression.toString());
    }

    @Override
    public Collection<CProp> gen(ValueExpression expression, ProgramPoint pp, DefiniteDataflowDomain<CProp> domain) throws SemanticException {
        // Here we refer to just an expression
        // In this case, we don't assign to any variable so no constants are propagated
        return List.of();
    }

    @Override
    public Collection<CProp> kill(Identifier id, ValueExpression expression, ProgramPoint pp, DefiniteDataflowDomain<CProp> domain) throws SemanticException {
        // Here we assign a value to an identifier. Regardless of the expression value, the identifier should be removed from the propagated constants
        return domain.getDataflowElements()
                .stream().filter(el -> el.id.getName().equals(id.getName())).toList();
    }

    @Override
    public Collection<CProp> kill(ValueExpression expression, ProgramPoint pp, DefiniteDataflowDomain<CProp> domain) throws SemanticException {
        // Here we refer to just an expression
        // In this case, we don't assign to any variable so no constants are killed
        return List.of();
    }
}