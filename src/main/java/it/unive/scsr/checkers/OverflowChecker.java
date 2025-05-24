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
import it.unive.lisa.util.numeric.IntInterval;
import it.unive.lisa.util.numeric.MathNumber;
import it.unive.scsr.Intervals;

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

	// -------------------------------------------------------- //

	private static MathNumber asNumber(int n) {
    	return new IntInterval(n, n).getLow();   
	}

	private static MathNumber boundMin(NumericalSize sz) {
		switch (sz) {
			case INT8:   return asNumber(-128);
			case INT16:  return asNumber(-32768);
			case INT32:  return asNumber(-2147483648);
			case UINT8:  return asNumber(0);     
			case UINT16: return asNumber(0);     
			case UINT32: return asNumber(0);         
			default:     return MathNumber.MINUS_INFINITY; 
		}
	}

	private static MathNumber boundMax(NumericalSize sz) {
		switch (sz) {
			case INT8:   return asNumber(127);
			case INT16:  return asNumber(32767);
			case INT32:  return asNumber(2147483647);
			case UINT8:  return asNumber(255);
			case UINT16: return asNumber(65535);
			case UINT32: return asNumber((int) 4294967295L);    
			default:     return MathNumber.PLUS_INFINITY;  
		}
	}
			
	// -------------------------------------------------------- //
	
	private NumericalSize size;
	
	public OverflowChecker(NumericalSize size) {
		this.size = size;
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
				
		// TODO: implement type checks, it is required a numerical type
		// hint: if staticType.isUntyped() == true, then should be checked possible dynamic types


		boolean numericStatic = staticType.isNumericType();
		boolean numericDynamic = dynamicTypes.stream().anyMatch(Type::isNumericType);
		if (!numericStatic && !numericDynamic) {
			return;
		}


		// ----------------------------------------------------------------------------------- //
		

		for (AnalyzedCFG<SimpleAbstractState<PointBasedHeap, ValueEnvironment<Intervals>,
							TypeEnvironment<InferredTypes>>> result : tool.getResultOf(graph)) {
				SimpleAbstractState<PointBasedHeap, ValueEnvironment<Intervals>, TypeEnvironment<InferredTypes>> state = result.getAnalysisStateAfter(node).getState();
				Intervals intervalAbstractValue = state.getValueState().getState(id);	
				
				// TODO: implement logic for overflow/underflow checks
				// hint: it depends to the NumericalSize size
                                                                      
                                                                      
				if (intervalAbstractValue == null || intervalAbstractValue.isTop() || intervalAbstractValue.isBottom())
					return;
				
				IntInterval iv = intervalAbstractValue.interval;
				MathNumber low  = iv.getLow();
				MathNumber high = iv.getHigh();

				switch (size) {
				case FLOAT8: case FLOAT16: case FLOAT32:
					return;
				default:
					MathNumber min = boundMin(size);
					MathNumber max = boundMax(size);
				
					boolean underflow = low.compareTo(min) < 0;
					boolean overflow  = high.compareTo(max) > 0;
				
					if (underflow || overflow) {
						String msg = (underflow && overflow)
							? "Possible underflow *and* overflow for variable '"+id.getName()+"'"
							: overflow ? "Possible overflow (> "+max+") for '"+id.getName()+"'"
								: "Possible underflow (< "+min+") for '"+id.getName()+"'";
						tool.warnOn(node, msg);
					}
				}
                                                                      
																      
				// ------------------------------------------------ //
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