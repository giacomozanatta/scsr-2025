package it.unive.scsr;
// Import needed classes
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.analysis.dataflow.DataflowElement;
import it.unive.lisa.analysis.dataflow.DefiniteDataflowDomain;
import it.unive.lisa.util.representation.ListRepresentation;
import it.unive.lisa.util.representation.StringRepresentation;
import it.unive.lisa.util.representation.StructuredRepresentation;
import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.UnaryExpression;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.operator.unary.NumericNegation;


import java.util.Collection;
import java.util.Collections;

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

    private final Identifier ID;
    private final Integer constant;

// Void object

public CProp() {
  this(null, null);
}
// Constructor

public CProp(Identifier ID, Integer constant) {
    this.ID = ID;
    this.constant = constant;
}
// CODE ALREADY ON CProp - IMPORTED NEEDED CLASSES TO HANDLE IT
	@Override
	public StructuredRepresentation representation() {
		return new ListRepresentation(
				new StringRepresentation(ID),
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
// END OF CODE ALREADY ON CProp
// This returns involved identifier
    @Override
public Collection<Identifier> getInvolvedIdentifiers() {
        return Collections.singleton(ID);
    }
// Method to evaluate symbolic expression and get constant value
    private static Integer evaluate(ValueExpression expr, DefiniteDataflowDomain<CProp> domain) {
        if (expr instanceof Constant) {
            Object value = ((Constant) expr).getValue();
            return (value instanceof Integer) ? (Integer) value : null;
        }

        if (expr instanceof Identifier) {
            for (CProp cp : domain.getDataflowElements())
                if (cp.ID.equals(expr))
                    return cp.constant;
            return null;
        }

        if (expr instanceof UnaryExpression) {
            UnaryExpression unary = (UnaryExpression) expr;
            if (unary.getExpression() instanceof ValueExpression) {
                Integer value = evaluate((ValueExpression) unary.getExpression(), domain);
                if (value == null)
                    return null;
                return (unary.getOperator() == NumericNegation.INSTANCE) ? -value : value;
            }
            return null;
        }

        if (expr instanceof BinaryExpression) {
            BinaryExpression binary = (BinaryExpression) expr;
            if (binary.getLeft() instanceof ValueExpression && binary.getRight() instanceof ValueExpression) {
                Integer left = evaluate((ValueExpression) binary.getLeft(), domain);
                Integer right = evaluate((ValueExpression) binary.getRight(), domain);
                if (left == null || right == null) return null;
// Switch between available operations
                return switch (binary.getOperator().getClass().getSimpleName()) {
                    case "AdditionOperator" -> left + right;
                    case "SubtractionOperator" -> left - right;
                    case "MultiplicationOperator" -> left * right;
                    case "DivisionOperator" -> (right == 0) ? null : left / right;
                    default -> null;
                };
            }
            return null;
        }

        return null;
    }
// The "generate" function if expression has constant value
    @Override
    public Collection<CProp> gen(Identifier ID, ValueExpression expr, ProgramPoint PP,
                                     DefiniteDataflowDomain<CProp> domain) {
        Integer value = evaluate(expr, domain);
        return (value != null) ? Collections.singleton(new CProp(ID, value)) : Collections.emptyList();
    }
// No propagation if there's no suitable expression
    @Override
    public Collection<CProp> gen(ValueExpression expression, ProgramPoint PP,
                                     DefiniteDataflowDomain<CProp> domain) {
        return Collections.emptyList();
    }
// The "kill" function that removes ID-value association if a new definition is encountered
    @Override
    public Collection<CProp> kill(Identifier ID, ValueExpression expression, ProgramPoint PP,
                                  DefiniteDataflowDomain<CProp> domain) {
        return domain.getDataflowElements().stream()
                .filter(cp -> cp.ID.equals(ID))
                .toList();
    }
// No kill over isolated constant expressions
    @Override
    public Collection<CProp> kill(ValueExpression expression, ProgramPoint PP,
                                  DefiniteDataflowDomain<CProp> domain) {
        return Collections.emptyList();
    }
    @Override
    public String toString() {
        return representation().toString();
    }
}