package it.unive.scsr.checkers;


import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import it.unive.lisa.analysis.AnalysisState;
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
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.numeric.Division;
import it.unive.lisa.program.type.*;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;
import it.unive.scsr.Intervals;
import it.unive.scsr.checkers.OverflowChecker.NumericalSize;
import it.unive.scsr.helpers.FloatInterval;

public class DivisionByZeroChecker implements
        SemanticCheck<
                SimpleAbstractState<PointBasedHeap, ValueEnvironment<Intervals>, TypeEnvironment<InferredTypes>>> {


    private NumericalSize size;

    public DivisionByZeroChecker(NumericalSize size) {
        this.size = size;
    }

    @Override
    public boolean visit(
            CheckToolWithAnalysisResults<SimpleAbstractState<PointBasedHeap, ValueEnvironment<Intervals>, TypeEnvironment<InferredTypes>>> tool,
            CFG graph, Statement node) {

        if (node instanceof Division)
            checkDivision(tool, graph, (Division) node);


        return true;

    }

    private void checkDivision(
            CheckToolWithAnalysisResults<SimpleAbstractState<PointBasedHeap, ValueEnvironment<Intervals>, TypeEnvironment<InferredTypes>>> tool,
            CFG graph, Division div) {

        for (AnalyzedCFG<SimpleAbstractState<PointBasedHeap, ValueEnvironment<Intervals>,
                TypeEnvironment<InferredTypes>>> result : tool.getResultOf(graph)) {
            AnalysisState<
                    SimpleAbstractState<PointBasedHeap, ValueEnvironment<Intervals>,
                            TypeEnvironment<InferredTypes>>> state = result.getAnalysisStateAfter(div.getRight());

            Set<SymbolicExpression> reachableIds = new HashSet<>();
            Iterator<SymbolicExpression> comExprIterator = state.getComputedExpressions().iterator();
            if (comExprIterator.hasNext()) {
                SymbolicExpression divisor = comExprIterator.next();
                try {
                    reachableIds
                            .addAll(state.getState().reachableFrom(divisor, div, state.getState()).elements);

                    for (SymbolicExpression s : reachableIds) {

                        Set<Type> types = getPossibleDynamicTypes(s, div, state.getState());

                        // implemented type checks, it is required a numerical type
                        boolean typeMatches = (size == NumericalSize.INT8 && types.contains(Int8Type.INSTANCE))
                                || (size == NumericalSize.INT16 && types.contains(Int16Type.INSTANCE))
                                || (size == NumericalSize.INT32 && types.contains(Int32Type.INSTANCE))
                                || (size == NumericalSize.UINT8 && types.contains(UInt8Type.INSTANCE))
                                || (size == NumericalSize.UINT16 && types.contains(UInt16Type.INSTANCE))
                                || (size == NumericalSize.UINT32 && types.contains(UInt32Type.INSTANCE))
                                || (size == NumericalSize.FLOAT32 && types.contains(Float32Type.INSTANCE));

                        // Only a specific type is considered on each analysis
                        if(typeMatches){
							ValueEnvironment<Intervals> valueState = state.getState().getValueState();

							Intervals intervalAbstractValue = valueState.eval((ValueExpression) s, div, state.getState());


							// TODO: add checks for division by zero
							System.out.println("Symbol: " + s + " has possible dynamic types " + types + " and value in [" + intervalAbstractValue.interval.getLow() + ", " + intervalAbstractValue.interval.getHigh() + "]");
							if (intervalAbstractValue.isZero()) {
								tool.warnOn(div, "Always zero");
							} else if (intervalAbstractValue.interval.includes(FloatInterval.ZERO)) {
                                tool.warnOn(div, "Possibly zero");
                            }
                        }

                    }
                } catch (SemanticException e) {
                    e.printStackTrace();
                }


            }
        }

    }

    // compute possible dynamic types / runtime types
    private Set<Type> getPossibleDynamicTypes(SymbolicExpression s, Division div,
                                              SimpleAbstractState<PointBasedHeap, ValueEnvironment<Intervals>, TypeEnvironment<InferredTypes>> state) throws SemanticException {

        Set<Type> possibleDynamicTypes = new HashSet<>();
        Type dynamicTypes = state.getDynamicTypeOf(s, div, state);
        if (dynamicTypes != null && !dynamicTypes.isUntyped()) {
            possibleDynamicTypes.add(dynamicTypes);
        } else if (dynamicTypes.isUntyped()) {
            Set<Type> runtimeTypes = state.getRuntimeTypesOf(s, div, state);
            if (runtimeTypes.stream().anyMatch(t -> t != Untyped.INSTANCE))
                for (Type t : runtimeTypes)
                    possibleDynamicTypes.add(t);
        }

        return possibleDynamicTypes;

    }


}