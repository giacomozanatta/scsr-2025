package it.unive.scsr;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.analysis.dataflow.DataflowElement;
import it.unive.lisa.analysis.dataflow.DefiniteDataflowDomain;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.*;
import it.unive.lisa.symbolic.value.operator.AdditionOperator;
import it.unive.lisa.symbolic.value.operator.DivisionOperator;
import it.unive.lisa.symbolic.value.operator.MultiplicationOperator;
import it.unive.lisa.symbolic.value.operator.SubtractionOperator;
import it.unive.lisa.symbolic.value.operator.binary.BinaryOperator;
import it.unive.lisa.symbolic.value.operator.unary.NumericNegation;
import it.unive.lisa.util.representation.ListRepresentation;
import it.unive.lisa.util.representation.StringRepresentation;
import it.unive.lisa.util.representation.StructuredRepresentation;

public class CProp implements DataflowElement<DefiniteDataflowDomain<CProp>, CProp> {

	// IMPLEMENTATION NOTE:
	// the code below is outside of the scope of the course. You can uncomment
	// it to get your code to compile. Be aware that the code is written
	// expecting that a field named "id" and a field named "constant" exist
	// in this class: if you name them differently, change also the code below
	// to make it work by just using the name of your choice instead of
	// "id"/"constant". If you don't have these fields in your
	// solution, then you should make sure that what you are doing is correct :)

	private final Identifier id;
	private final Integer constant;

	public CProp() {
		id = null;
		constant = null;
	}

	public CProp(Identifier id, int constant) {
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

	private static Integer evaluateExpression(SymbolicExpression expression, DefiniteDataflowDomain<CProp> domain) {
		Integer result = null;

		if (expression instanceof Constant constantExpr)
			result = constantExpr.getValue() instanceof Integer ? (Integer) constantExpr.getValue() : null;
		
		else if (expression instanceof Identifier) {
			for (CProp cProp : domain.getDataflowElements()) {
				
				if (cProp.id.equals(expression))
					result = cProp.constant;
			}

		} else if (expression instanceof UnaryExpression unaryExpr) {
			Integer value = evaluateExpression(unaryExpr.getExpression(), domain);
			
			if (value != null && unaryExpr.getOperator() == NumericNegation.INSTANCE)
				result = -value;
		
		} else if (expression instanceof BinaryExpression binaryExpr) {
			Integer rightValue = evaluateExpression(binaryExpr.getRight(), domain);
			Integer leftValue = evaluateExpression(binaryExpr.getLeft(), domain);
			
			BinaryOperator operator = binaryExpr.getOperator();

			if (rightValue == null || leftValue == null)
				return null;
			
			else if (operator instanceof AdditionOperator)
				result = leftValue + rightValue;
			
			else if (operator instanceof SubtractionOperator)
				result = leftValue - rightValue;
			
			else if (operator instanceof DivisionOperator)
				result = rightValue == 0 ? null : leftValue / rightValue;
			
			else if (operator instanceof MultiplicationOperator)
				result = leftValue * rightValue;
		}

		return result;
	}

	@Override
	public Collection<CProp> gen(Identifier id, ValueExpression expression, ProgramPoint programPoint,
			DefiniteDataflowDomain<CProp> domain) throws SemanticException {
		
		Set<CProp> generatedSet = new HashSet<>();
		Integer constant = evaluateExpression(expression, domain);
		
		if (constant != null)
			generatedSet.add(new CProp(id, constant));
		
			return generatedSet;
	}

	@Override
	public Collection<CProp> gen(ValueExpression expression, ProgramPoint programPoint, DefiniteDataflowDomain<CProp> domain)
			throws SemanticException {
		return new HashSet<>();
	}

	@Override
	public Collection<CProp> kill(Identifier id, ValueExpression expression, ProgramPoint programPoint,
			DefiniteDataflowDomain<CProp> domain) throws SemanticException {
		Set<CProp> killedSet = new HashSet<>();
		
		for (CProp cProp : domain.getDataflowElements()) {
		
			if (cProp.getInvolvedIdentifiers().contains(id))
				killedSet.add(cProp);
		}

		return killedSet;
	}

	@Override
	public Collection<CProp> kill(ValueExpression expression, ProgramPoint programPoint, DefiniteDataflowDomain<CProp> domain)
			throws SemanticException {
		return new HashSet<>();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;

		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((constant == null) ? 0 : constant.hashCode());
		
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		CProp other = (CProp) obj;
		if (constant == null) {
			if (other.constant != null)
				return false;
		} else if (!constant.equals(other.constant))
			return false;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		return true;
	}
}