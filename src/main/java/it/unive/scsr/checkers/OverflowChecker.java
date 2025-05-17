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

public class OverflowChecker implements
SemanticCheck<SimpleAbstractState<PointBasedHeap, ValueEnvironment<Intervals>, TypeEnvironment<InferredTypes>>> {

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
				
		// ADDED: implement type checks, it is required a numerical type
		// hint: if staticType.isUntyped() == true, then should be checked possible dynamic types
		if(staticType.isUntyped()) {
			boolean flag = false;
			for (Type t : dynamicTypes)
				if (t.isNumericType())
					flag = true;
			if(!flag)
				return;
		} else if(!staticType.isNumericType())
			return;

		double min = getTypeLowerBound();
		double max = getTypeUpperBound();

		if (varRef.getParentStatement() instanceof Assignment && ((Assignment) varRef.getParentStatement()).getLeft() == varRef) {
			node = varRef.getParentStatement();
		}

		for (AnalyzedCFG<SimpleAbstractState<PointBasedHeap, ValueEnvironment<Intervals>,
							TypeEnvironment<InferredTypes>>> result : tool.getResultOf(graph)) {
			SimpleAbstractState<PointBasedHeap, ValueEnvironment<Intervals>, TypeEnvironment<InferredTypes>> state = result.getAnalysisStateAfter(node).getState();
			Intervals intervalAbstractValue = state.getValueState().getState(id);

			// ADDED: implement logic for overflow/underflow checks
			// hint: it depends to the NumericalSize size
			if (intervalAbstractValue == null || intervalAbstractValue.interval == null || intervalAbstractValue.isBottom())
				continue;

			if(intervalAbstractValue.isTop()) {
				tool.warnOn(node, String.format("[%s] Infinite value in the Interval of variable %s!", size, id.getName()));
				continue;
			}

			//TO DO - controllare l'infinito, invece di usare il toDouble
			//TO DO - controllare i floating point
			try {
				double lb = intervalAbstractValue.interval.getLow().toDouble();
				double ub = intervalAbstractValue.interval.getHigh().toDouble();
				intervalAbstractValue.interval.getLow().getNumber().doubleValue();
				if(lb < min) {
					if(ub < min)
						tool.warnOn(node, String.format("[%s] Underflow DETECTED: %.3f < %.3f for variable %s",
								size, ub, min, id.getName()));
					else
						tool.warnOn(node, String.format("[%s] POSSIBLE underflow DETECTED: %.3f < %.3f for variable %s",
								size, lb, min, id.getName()));
				}

				if(ub > max) {
					if(lb > max)
						tool.warnOn(node, String.format("[%s] Overflow DETECTED: %.3f > %.3f for variable %s",
								size, lb, max, id.getName()));
					else
						tool.warnOn(node, String.format("[%s] POSSIBLE overflow DETECTED: %.3f > %.3f for variable %s",
								size, ub, max, id.getName()));
				}
			} catch (Exception e) {}
		}
	}

	//togliere l'infinito che viene controllato prima!
	private double toDouble(MathNumber n) {
		if (n == null)
			return Double.NaN;

		String s = n.toString().trim();
		if ("Inf".equals(s) || "+Inf".equals(s))
			return Double.POSITIVE_INFINITY;
		if ("-Inf".equals(s))
			return Double.NEGATIVE_INFINITY;
		if ("NaN".equalsIgnoreCase(s))
			return Double.NaN;

		return Double.parseDouble(s);
	}

	private double getTypeLowerBound() {
		switch (size) {
			case INT8:   return Byte.MIN_VALUE;
			case INT16:  return Short.MIN_VALUE;
			case INT32:  return Integer.MIN_VALUE;
			case UINT8:  return 0;
			case UINT16: return 0;
			case UINT32: return 0;
			case FLOAT8: return -240.0;
			case FLOAT16: return -65504.0;
			case FLOAT32: return -Float.MAX_VALUE;
			default:      return Double.NEGATIVE_INFINITY;
		}
	}

	private double getTypeUpperBound() {
		switch (size) {
			case INT8:   return Byte.MAX_VALUE;
			case INT16:  return Short.MAX_VALUE;
			case INT32:  return Integer.MAX_VALUE;
			case UINT8:  return 255;
			case UINT16: return 65535;
			case UINT32: return 4294967295L;
			case FLOAT8: return 240.0;
			case FLOAT16: return 65504.0;
			case FLOAT32: return Float.MAX_VALUE;
			default:      return Double.POSITIVE_INFINITY;
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