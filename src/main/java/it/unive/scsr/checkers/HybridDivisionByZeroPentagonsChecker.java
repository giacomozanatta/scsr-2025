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
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;
import it.unive.scsr.HybridIntervals;
import it.unive.scsr.HybridPentagons;
import it.unive.scsr.CustomMathNumber;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class HybridDivisionByZeroPentagonsChecker implements
        SemanticCheck<SimpleAbstractState<PointBasedHeap, HybridPentagons, TypeEnvironment<InferredTypes>>> {

    private OverflowChecker.NumericalSize size;

    public HybridDivisionByZeroPentagonsChecker(OverflowChecker.NumericalSize size) {
        this.size = size;
    }

    @Override
    public boolean visit(
            CheckToolWithAnalysisResults<SimpleAbstractState<PointBasedHeap, HybridPentagons, TypeEnvironment<InferredTypes>>> tool,
            CFG graph, Statement node) {

        if (node instanceof Division)
            checkDivision(tool, graph, (Division) node);

        return true;
    }

    private void checkDivision(
            CheckToolWithAnalysisResults<SimpleAbstractState<PointBasedHeap, HybridPentagons, TypeEnvironment<InferredTypes>>> tool,
            CFG graph, Division div) {

        for (AnalyzedCFG<SimpleAbstractState<PointBasedHeap, HybridPentagons, TypeEnvironment<InferredTypes>>> result
                : tool.getResultOf(graph)) {

            AnalysisState<SimpleAbstractState<PointBasedHeap, HybridPentagons, TypeEnvironment<InferredTypes>>> state =
                    result.getAnalysisStateAfter(div.getRight());

            Set<SymbolicExpression> reachableIds = new HashSet<>();
            Iterator<SymbolicExpression> it = state.getComputedExpressions().iterator();
            if (it.hasNext()) {
                SymbolicExpression divisor = it.next();
                try {
                    reachableIds.addAll(state.getState().reachableFrom(divisor, div, state.getState()).elements);

                    for (SymbolicExpression s : reachableIds) {
                        Set<Type> types = getPossibleDynamicTypes(s, div, state.getState());
                        Type staticType = s.getStaticType();

                        if (!isNumerical(types, staticType))
                            continue;

                        HybridPentagons valueState = state.getState().getValueState();
                        //HybridPentagons pentagonValue = valueState.eval((ValueExpression) s, div, state.getState());

                        if (valueState != null && !valueState.isBottom()) {
                            if (!valueState.isTop() && s instanceof Identifier) {
                                Identifier id = (Identifier) s;
                                HybridIntervals interval = valueState.getIntervals().getState(id);
                                if (interval != null && !interval.isBottom() && !interval.isTop()) {
                                    CustomMathNumber lb = interval.getLow();
                                    CustomMathNumber ub = interval.getHigh();
                                    if (lb != null && ub != null && lb.isFinite() && ub.isFinite()
                                            && lb.getNumber().doubleValue() <= 0
                                            && ub.getNumber().doubleValue() >= 0) {
                                        tool.warnOn(div, "Possible division by zero: " + s + " ∈ " + interval);
                                    }
                                } else if (interval != null && interval.isTop()) {
                                    tool.warnOn(div, "Possible division by zero (Interval is TOP): " + s + " ∈ " + interval);
                                }
                            } else if (valueState.isTop()) {
                                tool.warnOn(div, "Possible division by zero (Pentagons is TOP): " + s + " ∈ " + valueState);
                            }
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

    private Set<Type> getPossibleDynamicTypes(SymbolicExpression s, Division div,
                                              SimpleAbstractState<PointBasedHeap, HybridPentagons, TypeEnvironment<InferredTypes>> state)
            throws SemanticException {

        Set<Type> possibleDynamicTypes = new HashSet<>();
        Type dynamicType = state.getDynamicTypeOf(s, div, state);
        if (dynamicType != null && !dynamicType.isUntyped()) {
            possibleDynamicTypes.add(dynamicType);
        } else if (dynamicType.isUntyped()) {
            Set<Type> runtimeTypes = state.getRuntimeTypesOf(s, div, state);
            for (Type t : runtimeTypes)
                if (!t.equals(Untyped.INSTANCE))
                    possibleDynamicTypes.add(t);
        }

        return possibleDynamicTypes;
    }
}
