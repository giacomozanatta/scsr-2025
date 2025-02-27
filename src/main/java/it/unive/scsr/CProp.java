package it.unive.scsr;

import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.dataflow.DataflowDomain;
import it.unive.lisa.analysis.dataflow.DataflowElement;
import it.unive.lisa.analysis.dataflow.DefiniteDataflowDomain;
import it.unive.lisa.analysis.dataflow.PossibleDataflowDomain;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.*;
import it.unive.lisa.symbolic.value.operator.AdditionOperator;
import it.unive.lisa.symbolic.value.operator.DivisionOperator;
import it.unive.lisa.symbolic.value.operator.MultiplicationOperator;
import it.unive.lisa.symbolic.value.operator.SubtractionOperator;
import it.unive.lisa.symbolic.value.operator.binary.BinaryOperator;
import it.unive.lisa.symbolic.value.operator.unary.NumericNegation;
import it.unive.lisa.symbolic.value.operator.unary.UnaryOperator;
import it.unive.lisa.util.representation.ListRepresentation;
import it.unive.lisa.util.representation.StringRepresentation;
import it.unive.lisa.util.representation.StructuredRepresentation;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;

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

	private final Identifier id;
	private final Integer constant;

	public CProp(){
		this(null, null);
	}

	public CProp(Identifier id, Integer constant){
		this.id = id;
		this.constant = constant;
	}

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

	@Override
	public Collection<Identifier> getInvolvedIdentifiers() {
		return Collections.singleton(id);
	}

	@Override
	public Collection<CProp> gen(Identifier id, ValueExpression expression, ProgramPoint pp, DefiniteDataflowDomain<CProp> domain) throws SemanticException {
		Integer val = eval(expression, domain);
		if (val != null)
			return Collections.singleton(new CProp(id, val));
		return Collections.emptySet();
	}

	@Override
	public Collection<CProp> gen(ValueExpression expression, ProgramPoint pp, DefiniteDataflowDomain<CProp> domain) throws SemanticException {
		return Collections.emptySet();
	}

	@Override
	public Collection<CProp> kill(Identifier id, ValueExpression expression, ProgramPoint pp, DefiniteDataflowDomain<CProp> domain) throws SemanticException {
		Collection<CProp> killed = new HashSet<>();

		for (CProp cp : domain.getDataflowElements())
			if (cp.id.equals(id))
				killed.add(cp);

		return killed;
	}

	@Override
	public Collection<CProp> kill(ValueExpression expression, ProgramPoint pp, DefiniteDataflowDomain<CProp> domain) throws SemanticException {
		return Collections.emptySet();
	}

	private static Integer getValueOf(
			Identifier id,
			DefiniteDataflowDomain<CProp> domain) {
		for (CProp cp : domain.getDataflowElements())
			if (cp.id.equals(id))
				return cp.constant;
		return null;
	}

	@Override
	public int hashCode() {
		return Objects.hash(constant, id);
	}

	@Override
	public boolean equals(
			Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		CProp other = (CProp) obj;
		return Objects.equals(constant, other.constant) && Objects.equals(id, other.id);
	}

	private static Integer eval(
			SymbolicExpression expression, DefiniteDataflowDomain<CProp> domain) {
		Collection<Identifier> result = new HashSet<>();

		if (expression == null)
			return null;

		if (expression instanceof Constant) {
			Object value = ((Constant) expression).getValue();
			if (value instanceof Integer)
				return (Integer) value;
		}

		if (expression instanceof Identifier)
			return getValueOf((Identifier) expression, domain);


		if (expression instanceof UnaryExpression) {
			UnaryExpression unary = (UnaryExpression) expression;
			UnaryOperator operator = unary.getOperator();
			ValueExpression arg = (ValueExpression) unary.getExpression();

			Integer value = eval(arg, domain);
			if (value == null)
				return null;
			if (operator instanceof NumericNegation)
				return -value;
		}

		if (expression instanceof BinaryExpression) {
			BinaryExpression binary = (BinaryExpression) expression;
			BinaryOperator operator = binary.getOperator();
			Integer right = eval(binary.getRight(), domain);
			Integer left = eval(binary.getLeft(), domain);

			if (right == null || left == null)
				return null;

			if (operator instanceof AdditionOperator)
				return left + right;
			if (operator instanceof DivisionOperator)
				return left == 0 ? null : (int) left / right;
			if (operator instanceof MultiplicationOperator)
				return left * right;
			if (operator instanceof SubtractionOperator)
				return left - right;
		}

		return null;
	}

}