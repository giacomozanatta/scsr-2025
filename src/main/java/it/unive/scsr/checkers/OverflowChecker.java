package it.unive.scsr.checkers;


import java.util.HashSet;
import java.util.Set;

import it.unive.lisa.analysis.AnalyzedCFG;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SimpleAbstractState;
import it.unive.lisa.analysis.heap.pointbased.PointBasedHeap;
import it.unive.lisa.analysis.nonrelational.value.TypeEnvironment;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.analysis.types.InferredTypes;
import it.unive.lisa.checks.semantic.CheckToolWithAnalysisResults;
import it.unive.lisa.checks.semantic.SemanticCheck;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.statement.Assignment;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.VariableRef;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;
import it.unive.lisa.util.numeric.MathNumber;
import it.unive.scsr.Intervals;
import it.unive.scsr.overflowhelp.FloatInterval;

public class OverflowChecker implements
SemanticCheck<
		SimpleAbstractState<PointBasedHeap, ValueEnvironment<Intervals>, TypeEnvironment<InferredTypes>>> {
	
	public enum NumericalSize {
		INT8,  // signed integer 8-bit
		INT16, // signed integer 16-bit
		INT32, // signed integer 32-bit
		UINT8,  // unsigned integer 8-bit
		UINT16, // unsigned integer 16-bit
		UINT32, // unsigned integer 32-bit
		FLOAT8, // signed float 8-bit
		FLOAT16, // signed float 16-bit
		FLOAT32, // signed float 32-bit
	}
	
	private NumericalSize size;
	private FloatInterval boundary;
	private String typename;
	public OverflowChecker(NumericalSize size) {
		this.size = size;

		if (size == NumericalSize.INT8) {
			boundary=new FloatInterval(Byte.MIN_VALUE,Byte.MAX_VALUE);
			typename="int8";
		} else if (size == NumericalSize.INT16) {
			boundary=new FloatInterval(Short.MIN_VALUE,Short.MAX_VALUE);
			typename="int16";
		}else if (size == NumericalSize.INT32) {
			boundary=new FloatInterval(Integer.MIN_VALUE,Integer.MAX_VALUE);
			typename="int32";
		}else if (size == NumericalSize.UINT8) {
			boundary=new FloatInterval(MathNumber.ZERO,new MathNumber(255));
			typename="uint8";
		} else if (size == NumericalSize.UINT16) {
			boundary=new FloatInterval(MathNumber.ZERO,new MathNumber(65535));
			typename="uint16";
		} else if (size == NumericalSize.UINT32) {
			boundary=new FloatInterval(MathNumber.ZERO,new MathNumber(4294967295L));
			typename="uint32";
		} else if (size == NumericalSize.FLOAT8) {
			//15.5 (1.1111 x 2^3)
			//https://people.cs.umass.edu/~verts/cmpsci145/8-Bit_Floating_Point.pdf
			boundary=new FloatInterval(-15.5f,15.5f);
			typename="float8";
		} else if (size == NumericalSize.FLOAT16) {
			boundary=new FloatInterval(-65504.0f,65504.0f);
			typename="float16";
		} else if (size == NumericalSize.FLOAT32) {
			boundary=new FloatInterval(-Float.MAX_VALUE,Float.MAX_VALUE);
			typename="float32";
		}
	}

	@Override
	public boolean visit(
			CheckToolWithAnalysisResults<SimpleAbstractState<PointBasedHeap, ValueEnvironment<Intervals>, TypeEnvironment<InferredTypes>>> tool,
			CFG graph, Statement node) {
		
		if (node instanceof Assignment) {
			Assignment assignment = (Assignment) node;
			Expression leftExpression = assignment.getLeft();
			
			// Checking if each variable reference is over/under-flowing
			if (leftExpression instanceof VariableRef) {
				checkVariableRef(tool, (VariableRef) leftExpression, graph, node);
			}
			
		} else {

			// Checking if each variable reference is over/under-flowing
			if (node instanceof VariableRef) {
				checkVariableRef(tool, (VariableRef) node, graph, node);
			}
		}
		
		return true;
		
	}
	
	private void checkVariableRef(CheckToolWithAnalysisResults<SimpleAbstractState<PointBasedHeap, ValueEnvironment<Intervals>, TypeEnvironment<InferredTypes>>> tool, VariableRef varRef, CFG graph, Statement node ) {
		Variable id = new Variable(((VariableRef) varRef).getStaticType(), ((VariableRef) varRef).getName(), ((VariableRef) varRef).getLocation());
		
		Type staticType = id.getStaticType();
		Set<Type> dynamicTypes = getPossibleDynamicTypes(tool, graph, node, id, varRef);

		Statement target = node;
				
		// TODO: implement type checks, it is required a numerical type
		// hint: if staticType.isUntyped() == true, then should be checked possible dynamic types

		// System.out.println(staticType+" "+dynamicTypes+" "+node);

		if (varRef.getParentStatement() instanceof Assignment && ((Assignment) varRef.getParentStatement()).getLeft() == varRef) {
			target = varRef.getParentStatement();
		}


		for (AnalyzedCFG<SimpleAbstractState<PointBasedHeap, ValueEnvironment<Intervals>,
							TypeEnvironment<InferredTypes>>> result : tool.getResultOf(graph)) {
				SimpleAbstractState<PointBasedHeap, ValueEnvironment<Intervals>, TypeEnvironment<InferredTypes>> state = result.getAnalysisStateAfter(target).getState();
				Intervals intervalAbstractValue = state.getValueState().getState(id);
				// System.out.println(intervalAbstractValue.interval);
				// TODO: implement logic for overflow/underflow checks
				// hint: it depends to the NumericalSize size

				if(boundary!=null){
					if(intervalAbstractValue.interval.getHigh().gt(boundary.getHigh())) {
						if (intervalAbstractValue.interval.getLow().gt(boundary.getHigh()))
							tool.warnOn(node, "certain overflow, for type " + typename+" possible value:"+intervalAbstractValue.interval+" type boundary:"+boundary);
						else
							tool.warnOn(node, "possible overflow, for type " + typename+" possible value:"+intervalAbstractValue.interval+" type boundary:"+boundary);
					}if(intervalAbstractValue.interval.getLow().lt(boundary.getLow())){
						if (intervalAbstractValue.interval.getHigh().gt(boundary.getLow()))
							tool.warnOn(node, "certain underflow, for type " +typename+" possible value:"+intervalAbstractValue.interval+" type boundary:"+boundary);
						else
							tool.warnOn(node, "possible underflow, for type " + typename+" possible value:"+intervalAbstractValue.interval+" type boundary:"+boundary);
					}

				}






		}
		
		
	}

	// compute possible dynamic types / runtime types
	private Set<Type> getPossibleDynamicTypes(
			CheckToolWithAnalysisResults<SimpleAbstractState<PointBasedHeap, ValueEnvironment<Intervals>, TypeEnvironment<InferredTypes>>> tool,
			CFG graph, Statement node, Variable id, VariableRef varRef) {
		
			Set<Type> possibleDynamicTypes = new HashSet<>();
			for (AnalyzedCFG<
					SimpleAbstractState<PointBasedHeap, ValueEnvironment<Intervals>,
							TypeEnvironment<InferredTypes>>> result : tool.getResultOf(graph)) {
				SimpleAbstractState<PointBasedHeap, ValueEnvironment<Intervals>, TypeEnvironment<InferredTypes>> state = result.getAnalysisStateAfter(varRef).getState();
				try {
					Type dynamicTypes = state.getDynamicTypeOf(id, varRef, state);
					if(dynamicTypes != null && !dynamicTypes.isUntyped()) {
						possibleDynamicTypes.add(dynamicTypes);
					} else if(dynamicTypes.isUntyped()){
						Set<Type> runtimeTypes = state.getRuntimeTypesOf(id, varRef, state);
						if(runtimeTypes.stream().anyMatch(t -> t != Untyped.INSTANCE))
							for( Type t : runtimeTypes)
								possibleDynamicTypes.add(t);
					}
				} catch (SemanticException e) {
					System.err.println("Cannot check " + node);
					e.printStackTrace(System.err);
				}
	
			}	
		return possibleDynamicTypes;
	}

	
		
	

}
