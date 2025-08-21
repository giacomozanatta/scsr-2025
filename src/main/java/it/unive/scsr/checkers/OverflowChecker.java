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
import it.unive.scsr.DoubleInterval;
import it.unive.scsr.Intervals;

public class OverflowChecker implements
SemanticCheck<
		SimpleAbstractState<PointBasedHeap, ValueEnvironment<Intervals>, TypeEnvironment<InferredTypes>>> {

    // Each size carries its bounds
	public enum NumericalSize {
		INT8(new DoubleInterval(Byte.MIN_VALUE, Byte.MAX_VALUE)),  // signed integer 8-bit
		INT16(new DoubleInterval(Short.MIN_VALUE, Short.MAX_VALUE)), // signed integer 16-bit
		INT32(new DoubleInterval(Integer.MIN_VALUE, Integer.MAX_VALUE)), // signed integer 32-bit
		UINT8(new DoubleInterval(0., Byte.MAX_VALUE * 2 + 1)),  // unsigned integer 8-bit
		UINT16(new DoubleInterval(0., Short.MAX_VALUE * 2 + 1)), // unsigned integer 16-bit
		UINT32(new DoubleInterval(0., ((long) Integer.MAX_VALUE) * 2L + 1L)), // unsigned integer 32-bit
        // Minifloat
        // https://en.wikipedia.org/wiki/Minifloat
		FLOAT8(new DoubleInterval(-240., 240.)), // signed float 8-bit
        // Half precision floating point
        // https://en.wikipedia.org/wiki/Half-precision_floating-point_format
		FLOAT16(new DoubleInterval(-65504., 65504.)), // signed float 16-bit
		FLOAT32(new DoubleInterval(-Float.MAX_VALUE, Float.MAX_VALUE)); // signed float 32-bit

        private final DoubleInterval bounds;

        NumericalSize(DoubleInterval bounds) {
            this.bounds = bounds;
        }

        public DoubleInterval getBounds() {
            return bounds;
        }
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
		Variable id = new Variable(varRef.getStaticType(), varRef.getName(), varRef.getLocation());
		
		Type staticType = id.getStaticType();
		Set<Type> dynamicTypes = getPossibleDynamicTypes(tool, graph, node, id, varRef);
				
        // It's a different type, ignore
        if (!staticType.isNumericType() && !staticType.isUntyped())
            return;

        // Handle dynamic types
        if (staticType.isUntyped()) {
            boolean found = false;
            for (Type type : dynamicTypes) {
                if (type.isNumericType()) {
                    found = true;
                    break;
                }
            }

            // Even the dynamic type is not numeric
            if  (!found)
                return;
        }

		for (var result : tool.getResultOf(graph)) {
				var state = result.getAnalysisStateAfter(node).getState();
				Intervals intervalAbstractValue = state.getValueState().getState(id);

                // We can't check a bottom value
                if (intervalAbstractValue.isBottom())
                    continue;

                if (!size.bounds.includes(intervalAbstractValue.interval)) {
                    if (intervalAbstractValue.interval.getLow().isInfinite()
                            || intervalAbstractValue.interval.getHigh().isInfinite()) {
                        // We're not sure whether the overflow occurred, it might be widening
                        tool.warn(String.format(
                                "Possible %s overflow detected at %s with interval %s",
                                size,
                                node.getLocation(),
                                intervalAbstractValue.interval
                        ));
                    } else {
                        // The interval has finite bounds, we're certain that an overflow occurred
                        tool.warn(String.format(
                                "%s overflow detected at %s with interval %s",
                                size,
                                node.getLocation(),
                                intervalAbstractValue.interval
                        ));
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