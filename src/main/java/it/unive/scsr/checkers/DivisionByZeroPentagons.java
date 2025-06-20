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
import it.unive.scsr.Intervals;
import it.unive.scsr.Pentagons;
import it.unive.scsr.checkers.OverflowChecker.NumericalSize;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;


public class DivisionByZeroPentagons implements SemanticCheck<SimpleAbstractState<PointBasedHeap, Pentagons, TypeEnvironment<InferredTypes>>> {
    private OverflowChecker.NumericalSize size;

    public DivisionByZeroPentagons(OverflowChecker.NumericalSize size){this.size = size;}

    @Override
    public boolean visit(CheckToolWithAnalysisResults<SimpleAbstractState<PointBasedHeap, Pentagons, TypeEnvironment<InferredTypes>>> tool,
                         CFG graph,
                         Statement node) {

        if(node instanceof Division)
            checkDivisionPent(tool, graph, (Division) node);
        return true;
    }

    private void checkDivisionPent(CheckToolWithAnalysisResults<SimpleAbstractState<PointBasedHeap, Pentagons, TypeEnvironment<InferredTypes>>> tool,
                                   CFG graph, Division div){

        for (AnalyzedCFG<SimpleAbstractState<PointBasedHeap, Pentagons,
                TypeEnvironment<InferredTypes>>> result : tool.getResultOf(graph)) {
            AnalysisState<
                    SimpleAbstractState<PointBasedHeap, Pentagons,
                            TypeEnvironment<InferredTypes>>> state = result.getAnalysisStateAfter(div.getRight());

            Set<SymbolicExpression> reachableIds = new HashSet<>();
            Iterator<SymbolicExpression> comExprIterator = state.getComputedExpressions().iterator();
            if(comExprIterator.hasNext()) {
                SymbolicExpression divisor = comExprIterator.next();
                try {
                    reachableIds.addAll(state.getState().reachableFrom(divisor, div, state.getState()).elements);

                    for (SymbolicExpression s : reachableIds) {

                        Set<Type> types = getPossibleDynamicTypes(s, div, state.getState());


                        // TODO: implement type checks, it is required a numerical type
                        // ------------ mio ------------------------------------------------------------------------------------
                        boolean isNumeric = types.stream().anyMatch(Type::isNumericType);
                        if (!isNumeric) {
                            tool.warn("This is not a numerical type! ");
                            continue;
                        }
                        // ------------------------------------------------------------------------------------------------------


                        Pentagons valueState = state.getState().getValueState();
                        ValueEnvironment<Intervals> valueStateIntervals = valueState.getIntervals();
                        Intervals intervalAbstractValue = valueStateIntervals.eval((ValueExpression) s, div, state.getState());
                        //Identifier id = (Identifier) s;
                        //Intervals interval = valueState.getIntervals().getState(id);


                        // TODO: add checks for division by zero
                        // ------------ mio ------------------------------------------------------------------------------------
                        if (valueState == null){
                            tool.warnOn(div, "IntervalAbstractValue is null! ");
                            return;
                        }else if (valueState.isBottom()){
                            tool.warnOn(div, "IntervalAbstractValue is bottom! ");
                            return;
                        }
                        else if(valueState.isTop()){
                            tool.warnOn(div, "Pentagons is top! Possible division by zero!");
                        }
                        else {
                            IntInterval a = intervalAbstractValue.interval;
                           // IntInterval a = interval.interval;
                            MathNumber al = a.getLow();
                            MathNumber au = a.getHigh();

                            if(a != null && !intervalAbstractValue.isBottom() && !intervalAbstractValue.isTop()) {
                                if (a != null && al.isFinite() && au.isFinite() && (al.isNegative() || al.isZero()) && (au.isPositive() || au.isZero())) {
                                    if (al.isZero() && au.isZero()) {
                                        tool.warnOn(div, "Definite division by zero " + a);
                                        System.err.println("Division by zero ([0,0])! ");

                                    }
                                    tool.warnOn(div, "Possible division by zero: divisor may be zero in " + a);
                                    System.err.println("Potential division by zero! ");
                                }
                            }else if(a != null && intervalAbstractValue.isTop()){
                                tool.warnOn(div, "Possible division by zero: interval is top");
                            }
                            if(a == null) {
                                tool.warn("Interval null ");
                                return;
                            }
                        }


                        // ------------------------------------------------------------------------------------------------------
                    }
                } catch (SemanticException e) {
                    e.printStackTrace();
                }


            }
        }


    }


    // compute possible dynamic types / runtime types
    private Set<Type> getPossibleDynamicTypes(SymbolicExpression s, Division div,
                                              SimpleAbstractState<PointBasedHeap, Pentagons, TypeEnvironment<InferredTypes>> state) throws SemanticException {

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
