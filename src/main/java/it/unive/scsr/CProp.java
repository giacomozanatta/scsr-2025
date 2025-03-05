package it.unive.scsr;

import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.dataflow.DataflowElement;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.*;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.operator.*;

import it.unive.lisa.symbolic.value.operator.unary.NumericNegation;
import it.unive.lisa.util.representation.ListRepresentation;
import it.unive.lisa.util.representation.StringRepresentation;
import it.unive.lisa.util.representation.StructuredRepresentation;
import it.unive.lisa.analysis.dataflow.DefiniteDataflowDomain;


import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CProp implements
        DataflowElement<
                // the type of dataflow domain that we want to use with this
                // analysis
                DefiniteDataflowDomain<
                        // java requires this type parameter to have this class
                        // as type in fields/methods
                        CProp>,
                // java requires this type parameter to have this class
                // as type in fields/methods
                CProp> {
    private final Identifier id;
    private final Integer constant;
    public CProp() {this(null,null);}
    public CProp(Identifier id, Integer constant) {
        this.id = id;
        this.constant = constant;
    }

    public Integer getConstant() {
        return constant;
    }

    @Override
    public Collection<Identifier> getInvolvedIdentifiers() {
        return List.of(id);
    }

    private static Integer eval(SymbolicExpression ve, DefiniteDataflowDomain<CProp> domain){
        if(ve instanceof Constant c) return (c.getValue() instanceof Integer)?(Integer)c.getValue() :null;
        if(ve instanceof Identifier i){
            for (CProp cp : domain.getDataflowElements()){
                if(cp.id.equals(i)){
                    return cp.getConstant();
                }
            }
        }
        if(ve instanceof UnaryExpression u){
            Integer i= eval(u.getExpression(),domain);
            return u.getOperator() instanceof NumericNegation ? -i:i;
        }
        if(ve instanceof BinaryExpression b){
            Integer left= eval(b.getLeft(),domain);
            Integer right= eval(b.getRight(),domain);
            if(b.getOperator() instanceof AdditionOperator) return left+right;
            if(b.getOperator() instanceof SubtractionOperator) return left-right;
            if(b.getOperator() instanceof MultiplicationOperator) return left*right;
            if(b.getOperator() instanceof DivisionOperator) return left/right;
        }
        return null;
    }

    @Override
    public Collection<CProp> gen(Identifier identifier, ValueExpression valueExpression, ProgramPoint programPoint, DefiniteDataflowDomain<CProp> cPropDefiniteDataflowDomain) throws SemanticException {
        Set<CProp> result = new HashSet<>();
        result.add(new CProp(identifier, eval(valueExpression,cPropDefiniteDataflowDomain)));
        return result;
    }

    @Override
    public Collection<CProp> gen(ValueExpression valueExpression, ProgramPoint programPoint, DefiniteDataflowDomain<CProp> cPropDefiniteDataflowDomain) throws SemanticException {
        return List.of();
    }

    @Override
    public Collection<CProp> kill(Identifier identifier, ValueExpression valueExpression, ProgramPoint programPoint, DefiniteDataflowDomain<CProp> cPropDefiniteDataflowDomain) throws SemanticException {
        Set<CProp> kl = new HashSet<>();
        for(CProp i : cPropDefiniteDataflowDomain.getDataflowElements()){
            if(i.id.equals(identifier))kl.add(i);
        }
        return kl;
    }

    @Override
    public Collection<CProp> kill(ValueExpression valueExpression, ProgramPoint programPoint, DefiniteDataflowDomain<CProp> cPropDefiniteDataflowDomain) throws SemanticException {
        return List.of();
    }


    @Override
    public CProp pushScope(ScopeToken scopeToken) throws SemanticException {
        return null;
    }

    @Override
    public CProp popScope(ScopeToken scopeToken) throws SemanticException {
        return null;
    }

    @Override
    public StructuredRepresentation representation() {
        return new ListRepresentation(
				new StringRepresentation(id),
				new StringRepresentation(constant));
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

//	@Override
//	public StructuredRepresentation representation() {
//		return new ListRepresentation(
//				new StringRepresentation(id),
//				new StringRepresentation(constant));
//	}
//
//	@Override
//	public CProp pushScope(
//			ScopeToken scope)
//			throws SemanticException {
//		return this;
//	}
//
//	@Override
//	public CProp popScope(
//			ScopeToken scope)
//			throws SemanticException {
//		return this;
//	}
}