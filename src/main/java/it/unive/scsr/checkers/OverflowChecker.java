package it.unive.scsr.checkers;


import java.math.BigDecimal;
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
import it.unive.lisa.util.numeric.MathNumberConversionException;
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

		boolean isNumeric = !staticType.isUntyped() && staticType.isNumericType();
		if (!isNumeric) {
			// check pointer, function, etc.
			for (Type type : dynamicTypes) {
				if (type.isNumericType()) {
					isNumeric = true;
					break;
				}
			}
		}

		// if a dynamic type, such a float applied to an Object is a numeric type
		if (!isNumeric) {
			return; // se non è numerico, niente overflow check
		}

		boolean isFloatingPoint = false;
		double min = 0;
		double max = 0;

		switch (size) {
			case INT8:
				min = Byte.MIN_VALUE;
				max = Byte.MAX_VALUE;
				break;
			case INT16:
				min = Short.MIN_VALUE;
				max = Short.MAX_VALUE;
				break;
			case INT32:
				min = Integer.MIN_VALUE;
				max = Integer.MAX_VALUE;
				break;
			case UINT8:
				min = 0;
				max = 255;
				break;
			case UINT16:
				min = 0;
				max = 65535;
				break;
			case UINT32:
				min = 0;
				max = 4294967295L;
				break;
			case FLOAT8:
				min = -240.0f;
				max = 240.0f;
				break;
			case FLOAT16:
				min = -65504.0f;
				max = 65504.0f;
				break;
			case FLOAT32:
				min = Float.MIN_VALUE;
				max = Float.MAX_VALUE;
				break;
		}


		// TODO: implement type checks, it is required a numerical type
		// hint: if staticType.isUntyped() == true, then should be checked possible dynamic types

		if (varRef.getParentStatement() instanceof Assignment && ((Assignment) varRef.getParentStatement()).getLeft() == varRef) {
			node = varRef.getParentStatement();
		}

		for (AnalyzedCFG<SimpleAbstractState<PointBasedHeap, ValueEnvironment<Intervals>,
							TypeEnvironment<InferredTypes>>> result : tool.getResultOf(graph)) {
				SimpleAbstractState<PointBasedHeap, ValueEnvironment<Intervals>, TypeEnvironment<InferredTypes>> state = result.getAnalysisStateAfter(node).getState();
				Intervals intervalAbstractValue = state.getValueState().getState(id);

				
				// TODO: implement logic for overflow/underflow checks
				// hint: it depends to the NumericalSize size
			if (intervalAbstractValue != null && intervalAbstractValue.interval != null && !intervalAbstractValue.isTop()) {
				MathNumber low = intervalAbstractValue.interval.getLow();
				MathNumber high = intervalAbstractValue.interval.getHigh();

				if (low != null && low.isFinite()) {
					double lower = low.getNumber().doubleValue();
					if (lower < min) {
						tool.warnOn(node, "[Type "+ size + "] " + "Underflow: value " + lower + " is below minimum allowed " + min + " for variable " + id.getName());
					}
				}
				//else if (low != null && !low.isFinite()) {
				//	tool.warnOn(node, "Underflow: value: infinite");
				//}

				if (high != null && high.isFinite()) {
					double upper = high.getNumber().doubleValue();
					if (upper > max) {
						tool.warnOn(node, "[Type "+ size + "] " + "Overflow: value " + upper + " is above maximum allowed " + max + " for variable " + id.getName());
					}
				}
				//else if (high != null && !high.isFinite()) {
				//	tool.warnOn(node, "Underflow: value: infinite");
				//}
			}
			else if(size != null && intervalAbstractValue != null && intervalAbstractValue.isTop())
				tool.warnOn(node, "Infinite value in the Interval of variable " + id.getName());
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