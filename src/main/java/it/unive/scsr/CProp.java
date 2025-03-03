package it.unive.scsr;

import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.dataflow.DataflowElement;
import it.unive.lisa.analysis.dataflow.DefiniteDataflowDomain;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.program.type.Int64Type;
import it.unive.lisa.symbolic.value.*;
import it.unive.lisa.symbolic.value.operator.ArithmeticOperator;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
        Set<Identifier> result = new HashSet<>();
        result.add(id);
        return result;
    }

    @Override
    public Collection<CProp> gen(Identifier identifier, ValueExpression valueExpression, ProgramPoint programPoint, DefiniteDataflowDomain<CProp> cPropDefiniteDataflowDomain) throws SemanticException {
        Set<CProp> constants = new HashSet<>();
        try{
            Integer value = eval_constant(valueExpression, cPropDefiniteDataflowDomain);
            constants.add(new CProp(identifier, new Constant(Int64Type.INSTANCE, value, programPoint.getLocation())));
        } catch (UnhandledExpressionType ignored) {

        }
        return constants;
    }

    private static Integer eval_constant(ValueExpression valueExpression, DefiniteDataflowDomain<CProp> domain) throws UnhandledExpressionType{
        /* We handle just a few relevant expression Types to keep this lab simple */
        if(valueExpression instanceof Constant){
            if( ((Constant) valueExpression).getValue() instanceof Integer){
                return (Integer) ((Constant) valueExpression).getValue();
            }
        }

        /* e.g  y = z */
        if(valueExpression instanceof Identifier){
            for(CProp element : domain.getDataflowElements()){
                if(element.getInvolvedIdentifiers().contains((Identifier) valueExpression)){
                    return (Integer)element.constant.getValue();
                }
            }
        }

        /* e.g   -y */
        if(valueExpression instanceof UnaryExpression){
            UnaryOperator op = ((UnaryExpression) valueExpression).getOperator();
            if(op instanceof NumericNegation){
                return - eval_constant(valueExpression, domain);
            }
        }

        if(valueExpression instanceof BinaryExpression){
            BinaryOperator op = ((BinaryExpression) valueExpression).getOperator();
            Integer left = eval_constant(
                    (ValueExpression) ((BinaryExpression) valueExpression).getLeft()
                    , domain);
            Integer right = eval_constant(
                    (ValueExpression) ((BinaryExpression) valueExpression).getRight()
                    , domain);
            if(op instanceof ArithmeticOperator){
                return left.intValue() + right.intValue();
            }
            if(op instanceof SubtractionOperator){
                return left.intValue() - right.intValue();
            }
            if(op instanceof DivisionOperator){
                return left.intValue() / right.intValue();
            }
            if(op instanceof MultiplicationOperator){
                return left.intValue() * right.intValue();
            }
        }
        throw new UnhandledExpressionType("Expression " + valueExpression + " is not handled");
    }

    @Override
    public Collection<CProp> gen(ValueExpression valueExpression, ProgramPoint programPoint, DefiniteDataflowDomain<CProp> cPropDefiniteDataflowDomain) throws SemanticException {
        return new HashSet<>();
    }

    @Override
    public Collection<CProp> kill(Identifier identifier, ValueExpression valueExpression, ProgramPoint programPoint, DefiniteDataflowDomain<CProp> cPropDefiniteDataflowDomain) throws SemanticException {
        Set<CProp> result = new HashSet<>();

        /* We remove the previous value associated with the identifier*/
        for (CProp element : cPropDefiniteDataflowDomain.getDataflowElements()) {
            if (element.getInvolvedIdentifiers().contains(id)) {
                result.add(element);
            }
        }

        return result;
    }

    @Override
    public Collection<CProp> kill(ValueExpression valueExpression, ProgramPoint programPoint, DefiniteDataflowDomain<CProp> cPropDefiniteDataflowDomain) throws SemanticException {
        return new HashSet<>();
    }


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
}