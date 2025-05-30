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
import it.unive.lisa.type.NumericType;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;
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

		if(!isNumericalType(staticType, dynamicTypes) ||
				(!isCompatibleWithSize(staticType, size) && !isAnyTypeCompatible(dynamicTypes, size)))
			return;

		for (AnalyzedCFG<SimpleAbstractState<PointBasedHeap, ValueEnvironment<Intervals>,
							TypeEnvironment<InferredTypes>>> result : tool.getResultOf(graph)) {
				SimpleAbstractState<PointBasedHeap, ValueEnvironment<Intervals>, TypeEnvironment<InferredTypes>> state = result.getAnalysisStateAfter(node).getState();
				Intervals intervalAbstractValue = state.getValueState().getState(id);	
				
				// TODO: implement logic for overflow/underflow checks
				// hint: it depends to the NumericalSize size

			if(intervalAbstractValue != null && !intervalAbstractValue.isBottom() && intervalAbstractValue.interval != null) {
				MathNumber min = intervalAbstractValue.interval.getLow();
				MathNumber max = intervalAbstractValue.interval.getHigh();

				if (min.isInfinite() || max.isInfinite())
					return;

				MathNumber[] bounds = getBoundsForSize(size);

				// Check for unsigned types
				if (isUnsigned(size) && min.lt(MathNumber.ZERO)) {
					String msg = "Overflow Checker for size: " + size + " generated: Underflow detected for variable '" + id.getName() + "' (negative value " + min + " in unsigned type)";
					tool.warnOn(node, msg);
					System.out.println(msg);
				}
				if (bounds[0].gt(min)) {
					String msg = "Overflow Checker for size: " + size + " generated: Underflow detected for variable '" + id.getName() + "' (" + min + " < " + bounds[0] + ")";
					tool.warnOn(node, msg);
					System.out.println(msg);
				}
				if (bounds[1].lt(max)) {
					String msg = "Overflow Checker for size: " + size + " generated: Overflow detected for variable '" + id.getName() + "' (" + max + " > " + bounds[1] + ")";
					tool.warnOn(node, msg);
					System.out.println(msg);
				}

				// Perform smallest subnormal number check for floating-point types
				if (size == NumericalSize.FLOAT8 || size == NumericalSize.FLOAT16 || size == NumericalSize.FLOAT32) {
					MathNumber[] subnormals = getSubnormalThresholds(size);

					// Check if the interval is entirely within subnormal range
					if (min.abs().lt(subnormals[1]) && !min.isZero()) {
						String msg = "Overflow Checker for size: " + size + " generated: Variable '" + id.getName() + "' might contain subnormal values (min = " + min + ")";
						tool.warnOn(node, msg);
						System.out.println(msg);
					}

					if (max.abs().lt(subnormals[1]) && !max.isZero()) {
						String msg = "Overflow Checker for size: " + size + " generated: Variable '" + id.getName() + "' might contain subnormal values (max = " + max + ")";
						tool.warnOn(node, msg);
						System.out.println(msg);
					}
				}
			}
		}
	}

	private boolean isNumericalType(Type staticType, Set<Type> dynamicTypes) {
		if (!staticType.isUntyped())
			return staticType.isNumericType();
		for (Type dynType : dynamicTypes)
			if (dynType.isNumericType())
				return true;
		return false;
	}

	private MathNumber[] getBoundsForSize(NumericalSize size) {
		MathNumber[] res = new MathNumber[2];
		switch (size) {
			case INT8:
				res[0] = new MathNumber(-128);
				res[1] = new MathNumber(127);
				break;
			case UINT8:
				res[0] = new MathNumber(0);
				res[1] = new MathNumber(255);
				break;
			case INT16:
				res[0] = new MathNumber(-32768);
				res[1] = new MathNumber(32767);
				break;
			case UINT16:
				res[0] = new MathNumber(0);
				res[1] = new MathNumber(65535);
				break;
			case INT32:
				res[0] = new MathNumber(-2147483648L);
				res[1] = new MathNumber(2147483647L);
				break;
			case UINT32:
				res[0] = new MathNumber(0);
				res[1] = new MathNumber(4294967295L);
				break;
			case FLOAT8:
				res[0] = new MathNumber(-240);
				res[1] = new MathNumber(240);
				break;
			case FLOAT16:
				res[0] = new MathNumber(-65504);
				res[1] = new MathNumber(65504);
				break;
			case FLOAT32:
				res[0] = new MathNumber(-Float.MAX_VALUE);
				res[1] = new MathNumber(Float.MAX_VALUE);
				break;
			default:
				throw new IllegalArgumentException("Unsupported NumericalSize: " + size);
		}

		return res;
	}

	private MathNumber[] getSubnormalThresholds(NumericalSize size) {
		MathNumber[] res = new MathNumber[2];
		switch (size) {
			case FLOAT8:
				res[0] = new MathNumber(-1.95e-3);
				res[1] = new MathNumber(1.95e-3);
				break;
			case FLOAT16:
				res[0] = new MathNumber(-5.96e-8);
				res[1] = new MathNumber(5.96e-8);
				break;
			case FLOAT32:
				res[0] = new MathNumber(-1.4e-45);
				res[1] = new MathNumber(1.4e-45);
				break;
			default:
				res[0] = new MathNumber(0);
				res[1] = new MathNumber(0);
				break;
		}
		return res;
	}

	private boolean isCompatibleWithSize(Type type, NumericalSize size) {
		if (type == null || type.isUntyped() || !(type instanceof NumericType))
			return false;

		NumericType numType = (NumericType) type;

		if (size == NumericalSize.FLOAT8 || size == NumericalSize.FLOAT16 || size == NumericalSize.FLOAT32)
			return !numType.isIntegral();

		if (size == NumericalSize.INT8 || size == NumericalSize.INT16 || size == NumericalSize.INT32
				|| size == NumericalSize.UINT8 || size == NumericalSize.UINT16 || size == NumericalSize.UINT32)
			return numType.isIntegral();

		return false;
	}

	private boolean isAnyTypeCompatible(Set<Type> types, NumericalSize size) {
		for (Type t : types)
			if (isCompatibleWithSize(t, size))
				return true;
		return false;
	}

	private boolean isUnsigned(NumericalSize size) {
		return size == NumericalSize.UINT8 || size == NumericalSize.UINT16 || size == NumericalSize.UINT32;
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