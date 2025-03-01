package it.unive.scsr;

import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.dataflow.DataflowElement;
import it.unive.lisa.analysis.dataflow.DefiniteDataflowDomain;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.util.representation.ListRepresentation;
import it.unive.lisa.util.representation.StructuredRepresentation;
import it.unive.lisa.util.representation.StringRepresentation;
import it.unive.lisa.symbolic.value.UnaryExpression;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.operator.unary.NumericNegation;
import it.unive.lisa.symbolic.value.operator.unary.UnaryOperator;
import it.unive.lisa.symbolic.value.operator.binary.BinaryOperator;
import it.unive.lisa.symbolic.value.operator.AdditionOperator;
import it.unive.lisa.symbolic.value.operator.DivisionOperator;
import it.unive.lisa.symbolic.value.operator.MultiplicationOperator;
import it.unive.lisa.symbolic.value.operator.SubtractionOperator;

import java.util.*;

public class CProp implements DataflowElement<DefiniteDataflowDomain<CProp>, CProp>
{

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

    public CProp(Identifier id, Integer constant) {
        this.id = id;
        this.constant = constant;
    }

    public CProp(){
        this(null, null);
    }

    @Override
    public Collection<Identifier> getInvolvedIdentifiers() {
        Set<Identifier> res = new HashSet<>();
        res.add(id);
        return res;
    }

    //this method evaluates an expression
    private Integer eval(ValueExpression expr, DefiniteDataflowDomain<CProp> domain){
        //empty expression returns null
        if(expr == null) return null;
        //if expression is a constant, extract the value
        //if value is Integer, return it
        if(expr instanceof Constant) {
            Object val = ((Constant) expr).getValue();
            if(val instanceof Integer)
                return (Integer) val;
        }
        //if expression is identifier, do the same
        if(expr instanceof Identifier) {
            for(CProp c: domain.getDataflowElements())
                if(c.id != null && c.id.equals(expr))
                    return c.constant;
        }

        //unary expression
        //only evaluate unary -, because we only evaluate operations on integers
        if(expr instanceof UnaryExpression e) {
            //extract operator and argument
            UnaryOperator o = e.getOperator();
            ValueExpression arg = (ValueExpression) e.getExpression();

            // evaluate the argument
            Integer v = eval(arg, domain);

            //if operator is -, negate the value.
            if(o instanceof NumericNegation)
                return -v;
            //if v has no value, return null
            if(v == null)
                return null;
        }

        //binary expression
        //only evaluate +, -, *, /, because we only evaluate operations on integers
        if(expr instanceof BinaryExpression e){
            BinaryOperator o = e.getOperator();
            ValueExpression e_l = (ValueExpression) e.getLeft(); //extract left argument
            ValueExpression e_r = (ValueExpression) e.getRight(); //extract right argument
            //extract values from arguments
            Integer v_l = eval(e_l, domain);
            Integer v_r = eval(e_r, domain);

            if(v_l == null || v_r == null)
                return null;
            if(o instanceof AdditionOperator)
                return v_l + v_r;
            if(o instanceof SubtractionOperator)
                return v_l - v_r;
            if(o instanceof MultiplicationOperator)
                return v_l * v_r;
            if(o instanceof DivisionOperator)
                return v_l / v_r;
            return null;
        }

        return null;
    }

    @Override
    public Collection<CProp> gen(Identifier identifier, ValueExpression expr, ProgramPoint programPoint, DefiniteDataflowDomain<CProp> domain) throws SemanticException {
        Integer value = eval(expr, domain);
        if(value != null)
            return Collections.singleton(new CProp(id, value));
        else
            return Collections.emptySet();
    }

    @Override
    public Collection<CProp> gen(ValueExpression valueExpression, ProgramPoint programPoint, DefiniteDataflowDomain<CProp> cPropDefiniteDataflowDomain) throws SemanticException {
        //Only the expression, no variable propagated, so need to assign anything
        return Collections.emptySet();
    }

    @Override
    public Collection<CProp> kill(Identifier identifier, ValueExpression valueExpression, ProgramPoint programPoint, DefiniteDataflowDomain<CProp> cPropDefiniteDataflowDomain) throws SemanticException {
        Collection<CProp> res = new HashSet<>();
        for(CProp c : cPropDefiniteDataflowDomain.getDataflowElements()){
            if(c.id != null && c.id.equals(id))
                res.add(c);
        }
        return res;
    }

    @Override
    public Collection<CProp> kill(ValueExpression valueExpression, ProgramPoint programPoint, DefiniteDataflowDomain<CProp> cPropDefiniteDataflowDomain) throws SemanticException {
        //Only the expression, no value lost, so need to assign anything
        return Collections.emptySet();
    }

    @Override
    public boolean equals(Object obj)
    {
        if(obj == this) return true;
        if(obj == null) return false;
        if(getClass() != obj.getClass()) return false;
        return Objects.equals(constant, ((CProp) obj).constant) &&
                Objects.equals(id, ((CProp) obj).id);
    }

    //pre-written methods
	@Override
	public StructuredRepresentation representation() {
		return new ListRepresentation(
				new StringRepresentation(id),
				new StringRepresentation(constant));
	}
//
	@Override
	public CProp pushScope(
			ScopeToken scope)
			throws SemanticException {
		return this;
	}
//
	@Override
	public CProp popScope(
			ScopeToken scope)
			throws SemanticException {
		return this;
	}
}