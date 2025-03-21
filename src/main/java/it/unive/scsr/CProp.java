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

public class CProp implements DataflowElement<DefiniteDataflowDomain<CProp>, CProp> { // CProp follows the rules of DataflowElement
    private final Identifier id; // stores the name of the variable.
    private final Integer constant; // stores the value assigned to that variable.
    // The final keyword means their values cannot be changed after being assigned

    // This constructor sets id and constant to null (empty values). (Constructor)
    public CProp() {
        this(null, null);
    }

    // This constructor allows creating a CProp object with 2 parameters
    public CProp(Identifier id, Integer constant) {
        this.id = id;
        this.constant = constant;
    }

    // Tracking Variables Used
    @Override // this Override the method that is related to some super class definded previously
    public Collection<Identifier> getInvolvedIdentifiers() { // This function returns the variable that CProp is tracking.
        return id == null ? Collections.emptySet() : Collections.singleton(id); // If no variable is tracked (id == null), it returns an empty set.
    } // Otherwise, it returns a set containing id.

    // Generating New Constant Assignments
    // This function tracks new constants assigned to variables.
    @Override
    public Collection<CProp> gen(Identifier identifier, ValueExpression valueExpression, ProgramPoint programPoint, DefiniteDataflowDomain<CProp> domain) throws SemanticException {
        Set<CProp> generatedSet = new HashSet<>(); // Creating an empty set
        Optional<Integer> value = evaluateExpression(valueExpression, domain); //check if it's a constant
        value.ifPresent(val -> generatedSet.add(new CProp(identifier, val))); // If the expression resluts in a constant, then create a new CProp object and adds it to generatedSet
        return generatedSet; // and finaly retruns generatedSet
    }

    // Overloaded gen Method by using one similar to the one above it does not create a new assignment and returns empty list (like in tempo assignment)
    @Override
    public Collection<CProp> gen(ValueExpression valueExpression, ProgramPoint programPoint, DefiniteDataflowDomain<CProp> domain) throws SemanticException {
        return Collections.emptyList();
    }

    // Removing Constants When a Variable Changes
    // This function removes constants when a variable is re-assigned a non-constant value.
    @Override
    public Collection<CProp> kill(Identifier identifier, ValueExpression valueExpression, ProgramPoint programPoint, DefiniteDataflowDomain<CProp> domain) throws SemanticException {
        Set<CProp> removedSet = new HashSet<>(); // Create  an empty set
        for (CProp cp : domain.getDataflowElements()) { // goes throught all tracked variables
            if (cp.id.equals(identifier)) {
                removedSet.add(cp); // If a tracked variable is the same as identifier, it removes it from tracking.
            }
        }
        return removedSet; // Returns the removed constants.
    }

    // Overloaded kill Method
    // Similar to the gen method, this version of kill does nothing and returns an empty list.
    @Override
    public Collection<CProp> kill(ValueExpression valueExpression, ProgramPoint programPoint, DefiniteDataflowDomain<CProp> domain) throws SemanticException {
        return Collections.emptyList();
    }

    // Representing the Object as a String
    @Override
    public String toString() {
        return representation().toString();
    }

    // Equals function, It checks if both id and constant are the same.
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CProp other = (CProp) o;
        return Objects.equals(id, other.id) && Objects.equals(constant, other.constant);
    }

    // Generating a Unique Hash Code
    @Override
    public int hashCode() {
        return Objects.hash(id, constant);
    } // Hash codes help store and compare objects efficiently.

    // This function computes the value of an expressions, it can handle vaiables, Unary expressions, constants, and Binary expressions
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

    // Representing the Object in a Structured Way, It helps format the object into a structured output.
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

// This program tracks variables assigned to constant values and removes them if their values change
// It evaluates arithmetic expressions and ensures correctness in constant propagation analysis.