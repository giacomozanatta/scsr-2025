package it.unive.scsr.checkers;

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
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;
//import it.unive.scsr.Intervals;
import it.unive.scsr.CustomMathNumber;
import it.unive.scsr.HybridIntervals;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class HybridDivisionByZeroChecker implements
        SemanticCheck<
                SimpleAbstractState<PointBasedHeap, ValueEnvironment<HybridIntervals>, TypeEnvironment<InferredTypes>>> {


    private OverflowChecker.NumericalSize size;

    public HybridDivisionByZeroChecker(OverflowChecker.NumericalSize size) {
        this.size = size;
    }

    @Override
    public boolean visit(
            CheckToolWithAnalysisResults<SimpleAbstractState<PointBasedHeap, ValueEnvironment<HybridIntervals>, TypeEnvironment<InferredTypes>>> tool,
            CFG graph, Statement node) {

        if( node instanceof Division)
            checkDivision(tool, graph, (Division) node);


        return true;

    }



    private void checkDivision(
            CheckToolWithAnalysisResults<SimpleAbstractState<PointBasedHeap, ValueEnvironment<HybridIntervals>, TypeEnvironment<InferredTypes>>> tool,
            CFG graph, Division div) {

        for (AnalyzedCFG<SimpleAbstractState<PointBasedHeap, ValueEnvironment<HybridIntervals>,
                TypeEnvironment<InferredTypes>>> result : tool.getResultOf(graph)) {
            AnalysisState<
                    SimpleAbstractState<PointBasedHeap, ValueEnvironment<HybridIntervals>,
                            TypeEnvironment<InferredTypes>>> state = result.getAnalysisStateAfter(div.getRight());

            Set<SymbolicExpression> reachableIds = new HashSet<>();
            Iterator<SymbolicExpression> comExprIterator = state.getComputedExpressions().iterator();
            if(comExprIterator.hasNext()) {
                SymbolicExpression divisor = comExprIterator.next();
                try {
                    reachableIds
                            .addAll(state.getState().reachableFrom(divisor, div, state.getState()).elements);

                    for (SymbolicExpression s : reachableIds) {

                        Set<Type> types = getPossibleDynamicTypes(s, div, state.getState());

                        Type staticType = s.getStaticType();

                        // check if numerical type (static or dynamic)
                        if (!isNumerical(types, staticType))
                            continue;

                        // implement type checks, it is required a numerical type

                        ValueEnvironment<HybridIntervals> valueState = state.getState().getValueState();

                        HybridIntervals intervalAbstractValue = valueState.eval((ValueExpression) s, div, state.getState());


                        if (intervalAbstractValue != null && !intervalAbstractValue.isBottom() && !intervalAbstractValue.isTop()) {
                            if (intervalAbstractValue.getLow().isFinite() && intervalAbstractValue.getHigh().isFinite() && intervalAbstractValue.getLow().getNumber().longValue() <= 0 &&
                                    intervalAbstractValue.getHigh().getNumber().longValue() >= 0) {
                                tool.warnOn(
                                        div,
                                        "Possible division by zero: " + s.toString() + " ∈ " + intervalAbstractValue);
                            }
                        }
                        else if(intervalAbstractValue != null && intervalAbstractValue.isTop()){
                            tool.warnOn(
                                    div,
                                    "Possible division by zero (Interval undefined): " + s.toString() + " ∈ " + intervalAbstractValue);

                        }
                    }
                } catch (SemanticException e) {
                    e.printStackTrace();
                }


            }
        }

    }

    private boolean isNumerical(Set<Type> dynamicTypes, Type staticType) {
        if (!staticType.isUntyped() && staticType.isNumericType())
            return true;
        for (Type type : dynamicTypes)
            if (type.isNumericType())
                return true;
        return false;
    }

    // compute possible dynamic types / runtime types
    private Set<Type> getPossibleDynamicTypes(SymbolicExpression s, Division div,
                                              SimpleAbstractState<PointBasedHeap, ValueEnvironment<HybridIntervals>, TypeEnvironment<InferredTypes>> state) throws SemanticException {

        Set<Type> possibleDynamicTypes = new HashSet<>();
        Type dynamicTypes = state.getDynamicTypeOf(s, div, state);
        if(dynamicTypes != null && !dynamicTypes.isUntyped()) {
            possibleDynamicTypes.add(dynamicTypes);
        } else if(dynamicTypes.isUntyped()){
            Set<Type> runtimeTypes = state.getRuntimeTypesOf(s, div, state);
            if(runtimeTypes.stream().anyMatch(t -> t != Untyped.INSTANCE))
                for( Type t : runtimeTypes)
                    possibleDynamicTypes.add(t);
        }

        return possibleDynamicTypes;

    }


}
