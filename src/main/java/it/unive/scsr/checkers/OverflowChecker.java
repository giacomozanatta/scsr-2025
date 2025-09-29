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
import it.unive.scsr.Pentagons;

public class OverflowChecker implements
SemanticCheck<
		SimpleAbstractState<PointBasedHeap, Pentagons, TypeEnvironment<InferredTypes>>> {
	
	// Supported numerical sizes
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
	
	// Will be used to warn if the current stmnt is inside a conditional
	private boolean isInConditional = false; 
	
	public OverflowChecker(NumericalSize size) {
		this.size = size;
	}
	// Context learning
	@Override
	public boolean visit(
			CheckToolWithAnalysisResults<SimpleAbstractState<PointBasedHeap, Pentagons, TypeEnvironment<InferredTypes>>> tool,
			CFG graph, Statement node) {

		// Tracker for conditional warning
		if (node.getClass().getSimpleName().equals("If") || node.getClass().getSimpleName().equals("While")) {
			isInConditional = true;
		}
		
		if (node instanceof Assignment) {
			Assignment assignment = (Assignment) node;
			Expression leftExpression = assignment.getLeft();
			
			if (leftExpression instanceof VariableRef) {
				checkVariableRef(tool, (VariableRef) leftExpression, graph, node);
			}
			
		} else {

			// Checking if each variable reference is over/under-flowing
			if (node instanceof VariableRef) {
				checkVariableRef(tool, (VariableRef) node, graph, node);
			}
		}
		
		if (node.getClass().getSimpleName().equals("If") || node.getClass().getSimpleName().equals("While")) {
			isInConditional = false;
		}
		
		return true;
		
	}

	// CORE LOGIC 
	private void checkVariableRef(CheckToolWithAnalysisResults<SimpleAbstractState<PointBasedHeap, Pentagons, TypeEnvironment<InferredTypes>>> tool, VariableRef varRef, CFG graph, Statement node ) {
		Variable id = new Variable(((VariableRef) varRef).getStaticType(), ((VariableRef) varRef).getName(), ((VariableRef) varRef).getLocation());
		
		Type staticType = id.getStaticType();
		Set<Type> dynamicTypes = getPossibleDynamicTypes(tool, graph, node, id, varRef);

		Statement target = node;
				
		// Verify if the variable is numerical (and avoid an headache...)
		boolean isNumerical = false;
		if (!staticType.isUntyped()) {
			isNumerical = staticType.toString().contains("int") || staticType.toString().contains("float") || staticType.toString().contains("double");
		} else {
			for (Type t : dynamicTypes) {
				if (t.toString().contains("int") || t.toString().contains("float") || t.toString().contains("double")) {
					isNumerical = true;
					break;
				}
			}
		}
		if (!isNumerical) return;

		// If the var is on left hand side
		if (varRef.getParentStatement() instanceof Assignment && ((Assignment) varRef.getParentStatement()).getLeft() == varRef) {
			target = varRef.getParentStatement();
		}

		// Analysis abstract state AFTER the statement
		for (AnalyzedCFG<SimpleAbstractState<PointBasedHeap, Pentagons,
							TypeEnvironment<InferredTypes>>> result : tool.getResultOf(graph)) {
				SimpleAbstractState<PointBasedHeap, Pentagons, TypeEnvironment<InferredTypes>> state = result.getAnalysisStateAfter(target).getState();
				Object valueState = state.getValueState();
				Intervals intervalAbstractValue = null;
				
				if (valueState instanceof Pentagons) {
					intervalAbstractValue = ((Pentagons) valueState).getIntervals().getState(id);
				} else if (valueState instanceof ValueEnvironment) {
					@SuppressWarnings("unchecked")
					ValueEnvironment<Intervals> intervalsEnv = (ValueEnvironment<Intervals>) valueState;
					intervalAbstractValue = intervalsEnv.getState(id);
				} else {
					// Skip if not a supported domain
					continue;
				}	
				
				// Define bounds for each numsize
				MathNumber min = null, max = null;
				switch (size) {
					case INT8:
						min = new MathNumber(-128);
						max = new MathNumber(127);
						break;
					case INT16:
						min = new MathNumber(-32768);
						max = new MathNumber(32767);
						break;
					case INT32:
						min = new MathNumber(-2147483648L);
						max = new MathNumber(2147483647L);
						break;
					case UINT8:
						min = new MathNumber(0);
						max = new MathNumber(255);
						break;
					case UINT16:
						min = new MathNumber(0);
						max = new MathNumber(65535);
						break;
					case UINT32:
						min = new MathNumber(0);
						max = new MathNumber(4294967295L);
						break;
					case FLOAT8:
						// Approximate large range for 8-bit float
						min = new MathNumber(-1e10);
						max = new MathNumber(1e10);
						break;
					case FLOAT16:
						min = new MathNumber(-65504);
						max = new MathNumber(65504);
						break;
					case FLOAT32:
						min = new MathNumber(-3.4028235e38);
						max = new MathNumber(3.4028235e38);
						break;
				}

				// Actual OF/UF checker
				if (min != null && max != null) {
					String prefix = isInConditional ? "Conditional " : "";
					if (intervalAbstractValue.interval.getLow().compareTo(min) < 0) {
						tool.warnOn(node, prefix + "Underflow detected for " + size + ": value " + intervalAbstractValue.interval.getLow() + " < " + min);
					}
					if (intervalAbstractValue.interval.getHigh().compareTo(max) > 0) {
						tool.warnOn(node, prefix + "Overflow detected for " + size + ": value " + intervalAbstractValue.interval.getHigh() + " > " + max);
					}
				}
				
				// Warn for close to limit values
				if (min != null && max != null && intervalAbstractValue.interval.getLow().compareTo(min) >= 0 && intervalAbstractValue.interval.getHigh().compareTo(max) <= 0) {
					MathNumber threshold = new MathNumber(100); // Custom threshold (report results use 100)
					if (intervalAbstractValue.interval.getHigh().compareTo(max.subtract(threshold)) > 0) {
						tool.warnOn(node, "Conditional overflow possible for " + size + ": value close to upper limit, may exceed with additional operations");
					}
					if (intervalAbstractValue.interval.getLow().compareTo(min.add(threshold)) < 0) {
						tool.warnOn(node, "Conditional underflow possible for " + size + ": value close to lower limit, may exceed with additional operations");
					}
				}
				
				// Arithmetic operations
				if (target instanceof Assignment && min != null && max != null) {
					Assignment assign = (Assignment) target;
					Expression right = assign.getRight();
					if (right instanceof it.unive.lisa.program.cfg.statement.BinaryExpression) {
						it.unive.lisa.program.cfg.statement.BinaryExpression binExpr = (it.unive.lisa.program.cfg.statement.BinaryExpression) right;
						String op = right.getClass().getSimpleName();

						// Support for +, - and *
						if (op.equals("Addition") || op.equals("Subtraction") || op.equals("Multiplication")) {
							Expression leftOp = binExpr.getLeft();
							Expression rightOp = binExpr.getRight();
							if (leftOp instanceof VariableRef && ((VariableRef) leftOp).getName().equals(id.getName())) {
								if (rightOp instanceof VariableRef) {
									Variable paramId = new Variable(((VariableRef) rightOp).getStaticType(), ((VariableRef) rightOp).getName(), ((VariableRef) rightOp).getLocation());
									Intervals paramInterval = null;
									if (valueState instanceof Pentagons) {
										paramInterval = ((Pentagons) valueState).getIntervals().getState(paramId);
									} else if (valueState instanceof ValueEnvironment) {
										@SuppressWarnings("unchecked")
										ValueEnvironment<Intervals> intervalsEnv = (ValueEnvironment<Intervals>) valueState;
										paramInterval = intervalsEnv.getState(paramId);
									}
									if (paramInterval != null) {
										MathNumber possibleHigh = null;
										MathNumber possibleLow = null;
										
										// Calc simulation for estimated result range
										if (op.equals("Addition")) {
											possibleHigh = intervalAbstractValue.interval.getHigh().add(paramInterval.interval.getHigh());
											possibleLow = intervalAbstractValue.interval.getLow().add(paramInterval.interval.getLow());
										} else if (op.equals("Subtraction")) {
											possibleHigh = intervalAbstractValue.interval.getHigh().subtract(paramInterval.interval.getLow());
											possibleLow = intervalAbstractValue.interval.getLow().subtract(paramInterval.interval.getHigh());
										} else if (op.equals("Multiplication")) {
											// Approxim.
											MathNumber h1 = intervalAbstractValue.interval.getHigh().multiply(paramInterval.interval.getHigh());
											MathNumber h2 = intervalAbstractValue.interval.getHigh().multiply(paramInterval.interval.getLow());
											MathNumber h3 = intervalAbstractValue.interval.getLow().multiply(paramInterval.interval.getHigh());
											MathNumber h4 = intervalAbstractValue.interval.getLow().multiply(paramInterval.interval.getLow());
											possibleHigh = h1.max(h2).max(h3).max(h4);

											MathNumber l1 = intervalAbstractValue.interval.getHigh().multiply(paramInterval.interval.getHigh());
											MathNumber l2 = intervalAbstractValue.interval.getHigh().multiply(paramInterval.interval.getLow());
											MathNumber l3 = intervalAbstractValue.interval.getLow().multiply(paramInterval.interval.getHigh());
											MathNumber l4 = intervalAbstractValue.interval.getLow().multiply(paramInterval.interval.getLow());
											possibleLow = l1.min(l2).min(l3).min(l4);
										}
										if (possibleHigh != null && (possibleHigh.compareTo(max) > 0 || possibleLow.compareTo(min) < 0)) {
											tool.warnOn(node, "Conditional overflow/underflow possible for " + size + ": operation with parameter may exceed limits");
										}
									}
								}
							}
						}
					}
				}
		}
		
		
	}

	// compute possible dynamic types / runtime types
	private Set<Type> getPossibleDynamicTypes(
			CheckToolWithAnalysisResults<SimpleAbstractState<PointBasedHeap, Pentagons, TypeEnvironment<InferredTypes>>> tool,
			CFG graph, Statement node, Variable id, VariableRef varRef) {
		
			Set<Type> possibleDynamicTypes = new HashSet<>();
			for (AnalyzedCFG<
					SimpleAbstractState<PointBasedHeap, Pentagons,
							TypeEnvironment<InferredTypes>>> result : tool.getResultOf(graph)) {
				SimpleAbstractState<PointBasedHeap, Pentagons, TypeEnvironment<InferredTypes>> state = result.getAnalysisStateAfter(varRef).getState();
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