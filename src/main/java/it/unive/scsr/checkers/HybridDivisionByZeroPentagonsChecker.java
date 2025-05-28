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
import it.unive.lisa.util.numeric.IntInterval;
import it.unive.lisa.util.numeric.MathNumber;
import it.unive.scsr.FloatInterval;
import it.unive.scsr.HybridIntervals;
import it.unive.scsr.HybridPentagons;
import it.unive.scsr.UpperBounds;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class HybridDivisionByZeroPentagonsChecker implements
        SemanticCheck<SimpleAbstractState<PointBasedHeap, HybridPentagons, TypeEnvironment<InferredTypes>>> {
    private HybridOverflowPentagonsChecker.NumericalSize size;

    public HybridDivisionByZeroPentagonsChecker(HybridOverflowPentagonsChecker.NumericalSize size) {
        this.size = size;
    }

    @Override
    public boolean visit(
            CheckToolWithAnalysisResults<SimpleAbstractState<PointBasedHeap, HybridPentagons, TypeEnvironment<InferredTypes>>> tool,
            CFG graph, Statement node) {

        if( node instanceof Division)
            checkDivision(tool, graph, (Division) node);

        return true;
    }

    private void checkDivision(
            CheckToolWithAnalysisResults<SimpleAbstractState<PointBasedHeap, HybridPentagons, TypeEnvironment<InferredTypes>>> tool,
            CFG graph, Division div) {

        for (AnalyzedCFG<SimpleAbstractState<PointBasedHeap, HybridPentagons,
                TypeEnvironment<InferredTypes>>> result : tool.getResultOf(graph)) {
            AnalysisState<
                    SimpleAbstractState<PointBasedHeap, HybridPentagons,
                            TypeEnvironment<InferredTypes>>> state = result.getAnalysisStateAfter(div.getRight());

            Set<SymbolicExpression> reachableIds = new HashSet<>();
            Iterator<SymbolicExpression> comExprIterator = state.getComputedExpressions().iterator();

            if(comExprIterator.hasNext()) {
                SymbolicExpression divisor = comExprIterator.next();
                try {
                    reachableIds.addAll(state.getState().reachableFrom(divisor, div, state.getState()).elements);

                    for (SymbolicExpression s : reachableIds) {
                        Set<Type> types = getPossibleDynamicTypes(s, div, state.getState());

                        // ADDED: implement type checks, it is required a numerical type
                        boolean isNumeric = types.stream().anyMatch(Type::isNumericType);
                        if (!isNumeric) {
                            tool.warnOn(div, "NOT numeric Interval!");
                            continue;
                        }

                        HybridPentagons pentagons = state.getState().getValueState();
                        ValueEnvironment<HybridIntervals> intervals = pentagons.getIntervals();
                        HybridIntervals intervalAbstractValue = intervals.eval((ValueExpression) s, div, state.getState());

                        // ADDED: add checks for division by zero
                        if(intervalAbstractValue != null &&
                                intervalAbstractValue.isTop()) {
                            tool.warnOn(div, "TOP Interval!");
                        }

                        if (intervalAbstractValue != null &&
                                !intervalAbstractValue.isBottom()) {
                            MathNumber lb, ub;
                            if (intervalAbstractValue.interval instanceof IntInterval) {
                                IntInterval interval = ((IntInterval) intervalAbstractValue.interval);
                                lb = interval.getLow();
                                ub = interval.getHigh();
                            } else if (intervalAbstractValue.interval instanceof FloatInterval) {
                                FloatInterval interval = ((FloatInterval) intervalAbstractValue.interval);
                                lb = interval.getLow();
                                ub = interval.getHigh();
                            } else {
                                continue;
                            }

                            if(lb == null || ub == null)
                                continue;
                            if(lb.equals(MathNumber.ZERO) && ub.equals(MathNumber.ZERO))
                                tool.warnOn(div, "Division by zero DETECTED!");
                            else if(lb.compareTo(MathNumber.ZERO) <= 0 && ub.compareTo(MathNumber.ZERO) >= 0)
                                tool.warnOn(div, "POSSIBLE division by zero DETECTED!");
                        }

                        // ADDED: add checks for upperbounds
                        ValueEnvironment<UpperBounds> ubState = pentagons.getUpperbounds();  // già hai pentagons dallo step precedente

                        UpperBounds ub = ubState.getState((Identifier) s);
                        if (ub != null && !ub.isBottom()) {
                            for (Identifier u: ub) {
                                HybridIntervals upperInt = pentagons.getIntervals().getState(u);
                                if (upperInt != null && !upperInt.isBottom()) {
                                    MathNumber low = upperInt.getLow();
                                    MathNumber high = upperInt.getHigh();

                                    if (low != null && high != null) {
                                        if (low.equals(MathNumber.ZERO) && high.equals(MathNumber.ZERO)) {
                                            tool.warnOn(div, "POSSIBLE division by zero DETECTED (due to upper bound to variable known to be ZERO)");
                                        } else if (low.compareTo(MathNumber.ZERO) <= 0 && high.compareTo(MathNumber.ZERO) >= 0) {
                                            tool.warnOn(div, "POSSIBLE division by zero DETECTED (due to upper bound to variable possibly ZERO)");
                                        }
                                    }
                                }
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

    // compute possible dynamic types / runtime types
    private Set<Type> getPossibleDynamicTypes(SymbolicExpression s, Division div,
                                              SimpleAbstractState<PointBasedHeap, HybridPentagons, TypeEnvironment<InferredTypes>> state) throws SemanticException {

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
