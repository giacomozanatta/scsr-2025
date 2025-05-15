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
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;
import it.unive.lisa.util.numeric.MathNumber;
import it.unive.scsr.Intervals;
import it.unive.scsr.Pentagons;

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
			
		} 
		return true;
		
	}
	
	private void checkVariableRef(CheckToolWithAnalysisResults<SimpleAbstractState<PointBasedHeap, ValueEnvironment<Intervals>, TypeEnvironment<InferredTypes>>> tool, VariableRef varRef, CFG graph, Statement node ) {
		Variable id = new Variable(((VariableRef) varRef).getStaticType(), ((VariableRef) varRef).getName(), ((VariableRef) varRef).getLocation());
		
		Type staticType = id.getStaticType();
		Set<Type> dynamicTypes = getPossibleDynamicTypes(tool, graph, node, id, varRef);

		Statement target = node;
				
		// ADDED: implement type checks, it is required a numerical type
		// hint: if staticType.isUntyped() == true, then should be checked possible dynamic types

		if (varRef.getParentStatement() instanceof Assignment && ((Assignment) varRef.getParentStatement()).getLeft() == varRef) {
			target = varRef.getParentStatement();
		}


		if (!staticType.isUntyped() && !staticType.isNumericType()) {
			tool.warnOn(varRef, "Variable " + id.getName() + " is not a numerical type, but has static type " + staticType.toString());
			return;
		}
		else if (staticType.isUntyped() && dynamicTypes.isEmpty()) {
			tool.warnOn(varRef, "Variable " + id.getName() + " is not a numerical type, but has no static or dynamic type");
			return;
		}

		else if (staticType.isUntyped() && !dynamicTypes.isEmpty()) {
			// check if any dynamic types are numeric types and return only if none are using map filter
			Set<Type> numericDynamicTypes = dynamicTypes.stream().filter(t -> t.isNumericType()).collect(java.util.stream.Collectors.toSet());
			if (numericDynamicTypes.isEmpty()) {
				tool.warnOn(varRef, "Variable " + id.getName() + " does not have any possible numeric types");
				return;
			}
		
		}
		
		for (AnalyzedCFG<SimpleAbstractState<PointBasedHeap, ValueEnvironment<Intervals>,
							TypeEnvironment<InferredTypes>>> result : tool.getResultOf(graph)) {

				SimpleAbstractState<PointBasedHeap, ValueEnvironment<Intervals>, TypeEnvironment<InferredTypes>> state = result.getAnalysisStateAfter(target).getState();
				
				Object vs = state.getValueState();

				Intervals intervalAbstractValue = Intervals.TOP;
				
				if (vs instanceof ValueEnvironment<?>) {
					intervalAbstractValue = state.getValueState().getState(id);
				}
				else if (vs instanceof Pentagons) {
					Pentagons p = (Pentagons) vs;
					ValueEnvironment<Intervals> ie = p.getInterval();
					Intervals raw = ie.getState(id);
					if (raw == null || raw.isBottom()) return;

					// reduce by relational bounds
					intervalAbstractValue = raw;
					for (Identifier b : p.getUpperBounds().getState(id)) {
					    Intervals ib = ie.getState(b);
					    if (ib != null && !ib.isBottom())
							try {
								intervalAbstractValue = intervalAbstractValue.glb(new Intervals(MathNumber.MINUS_INFINITY, ib.interval.getHigh()));
							} catch (SemanticException e) {
								tool.warnOn(varRef, "Error during interval computation: " + e.getMessage());
							}
					}
				}

				MathNumber low = intervalAbstractValue.interval.getLow();
				MathNumber high = intervalAbstractValue.interval.getHigh();
				
				// regardless of the numerical size, in case low is -Inf we have underflow and in case high is +Inf we have overflow
				if (low.isMinusInfinity() || high.isPlusInfinity()) {
					if (low.isMinusInfinity()) {
						tool.warnOn(varRef, "Possible negative overflow detected for variable " + id.getName() + " as it can be -Inf");
					}
					if (high.isPlusInfinity()) {
						tool.warnOn(varRef, "Possible positive overflow detected for variable " + id.getName() + " as it can be +Inf");
					}
					break;
				}
				boolean isZero = intervalAbstractValue.isNonBottomSingletonWithValue(0);
				String overflowMessage = "Overflow detected on size " + size.toString() + " for variable " + id.getName() + " with range [" + low.toString() + ", " + high.toString() + "]";
				String underflowMessage = "Underflow detected on size " + size.toString() + " for variable " + id.getName() + " with range [" + low.toString() + ", " + high.toString() + "]";
				
				// check for overflow/underflow based in the numerical size
				switch (size) {
					case INT8:
						if (low.compareTo(new MathNumber(Byte.MIN_VALUE)) < 0) {
							tool.warnOn(varRef, "Negative " + overflowMessage);
						}
						if (high.compareTo(new MathNumber(Byte.MAX_VALUE)) > 0) {
							tool.warnOn(varRef, "Positive " + overflowMessage);
						}
						break;
					case INT16:
						if (low.compareTo(new MathNumber(Short.MIN_VALUE)) < 0) {
							tool.warnOn(varRef, "Negative " + overflowMessage);
						}
						if (high.compareTo(new MathNumber(Short.MAX_VALUE)) > 0) {
							tool.warnOn(varRef, "Positive " + overflowMessage);
						}
						break;
					case UINT8:
						if (low.compareTo(new MathNumber(0)) < 0) {
							tool.warnOn(varRef, "Negative " + overflowMessage);
						}
						if (high.compareTo(new MathNumber(0xFF)) > 0) {
							tool.warnOn(varRef, "Positive " + overflowMessage);
						}
						break;
					case UINT16:
						if (low.compareTo(new MathNumber(0)) < 0) {
							tool.warnOn(varRef, "Negative " + overflowMessage);
						}
						if (high.compareTo(new MathNumber(65535)) > 0) {
							tool.warnOn(varRef, "Positive " + overflowMessage);
						}
						break;
					case INT32:
						if (low.compareTo(new MathNumber(Integer.MIN_VALUE)) < 0) {
							tool.warnOn(varRef, "Negative " + overflowMessage);
						}
						if (high.compareTo(new MathNumber(Integer.MAX_VALUE)) > 0) {
							tool.warnOn(varRef, "Positive " + overflowMessage);
						}
						break;
					case UINT32:
						if (low.compareTo(new MathNumber(0)) < 0) {
							tool.warnOn(varRef, "Negative " + overflowMessage);
						}
						if (high.compareTo(new MathNumber(4294967295L)) > 0) {
							tool.warnOn(varRef, "Positive " + overflowMessage);
						}
						break;
					case FLOAT8:
						// overflow: magnitude beyond ±240
						if (low.compareTo(new MathNumber(-240.0)) < 0 || high.compareTo(new MathNumber(240.0)) > 0) {
							tool.warnOn(varRef, overflowMessage
								+ " with range [" + low + ", " + high + "]");
						}
						// underflow-to-zero: entire interval within (−minSub, +minSub)
						// minSubnormal for FLOAT8 is 2^(-9) ≈ 0.001953125
						float minSub = 0.001953125f;
						MathNumber upperBound = new MathNumber(minSub);
						MathNumber lowerBound = new MathNumber(-minSub);
						if (high.compareTo(upperBound) < 0 && low.compareTo(lowerBound) > 0 && !isZero) {
							tool.warnOn(varRef, underflowMessage);
						}
						break;
					case FLOAT16:
						// overflow: magnitude beyond ±65504
						if (low.compareTo(new MathNumber(-65504.0)) < 0 || high.compareTo(new MathNumber(65504.0)) > 0) {
							tool.warnOn(varRef, overflowMessage);
						}
						// underflow-to-zero: entire interval within (−minSub, +minSub)
						// min subnormal for FLOAT16 is 2^(-24) ≈ 5.960464477539063E-8
						float minSub16 = 5.960464477539063E-8f;
						MathNumber upperBound16 = new MathNumber(minSub16);
						MathNumber lowerBound16 = new MathNumber(-minSub16);
						if (high.compareTo(upperBound16) < 0 && low.compareTo(lowerBound16) > 0 && !isZero) {
							tool.warnOn(varRef, underflowMessage);
						}
						break;
					case FLOAT32:
						// overflow: magnitude beyond ±Float.MAX_VALUE
						if (low.compareTo(new MathNumber(-Float.MAX_VALUE)) < 0 || high.compareTo(new MathNumber(Float.MAX_VALUE)) > 0) {
							tool.warnOn(varRef, overflowMessage);
						}
						// underflow-to-zero: entire interval within (−minSub, +minSub)
						// min subnormal for FLOAT32 is Float.MIN_VALUE
						MathNumber minSub32 = new MathNumber(Float.MIN_VALUE);
						if (high.compareTo(minSub32) < 0 && low.compareTo(new MathNumber(-Float.MIN_VALUE)) > 0 && !isZero) {
							tool.warnOn(varRef, underflowMessage);
						}
						break;
					default:
						tool.warnOn(varRef, "Case not implemented for numerical size " + size.toString());
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
