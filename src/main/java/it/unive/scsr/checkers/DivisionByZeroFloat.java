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
import it.unive.lisa.util.numeric.IntInterval;
import it.unive.lisa.util.numeric.MathNumber;
import it.unive.scsr.FloatInterval;
import it.unive.scsr.FloatIntervals;
import it.unive.scsr.Intervals;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class DivisionByZeroFloat implements
        SemanticCheck<
                SimpleAbstractState<PointBasedHeap, ValueEnvironment<FloatIntervals>, TypeEnvironment<InferredTypes>>> {


    private OverflowChecker.NumericalSize size;

    public DivisionByZeroFloat(OverflowChecker.NumericalSize size) {
        this.size = size;
    }

    @Override
    public boolean visit(
            CheckToolWithAnalysisResults<SimpleAbstractState<PointBasedHeap, ValueEnvironment<FloatIntervals>, TypeEnvironment<InferredTypes>>> tool,
            CFG graph, Statement node) {

        if( node instanceof Division)
            checkDivision(tool, graph, (Division) node);


        return true;

    }

    private void checkDivision(
            CheckToolWithAnalysisResults<SimpleAbstractState<PointBasedHeap, ValueEnvironment<FloatIntervals>, TypeEnvironment<InferredTypes>>> tool,
            CFG graph, Division div) {

        for (AnalyzedCFG<SimpleAbstractState<PointBasedHeap, ValueEnvironment<FloatIntervals>,
                TypeEnvironment<InferredTypes>>> result : tool.getResultOf(graph)) {
            AnalysisState<
                    SimpleAbstractState<PointBasedHeap, ValueEnvironment<FloatIntervals>,
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


                        // TODO: implement type checks, it is required a numerical type
                        // ------------ mio ------------------------------------------------------------------------------------
                        boolean isNumeric = types.stream().anyMatch(Type::isNumericType);
                        if (!isNumeric) {
                            tool.warn("This is not a numerical type! ");
                            return;
                        }
                        // ------------------------------------------------------------------------------------------------------


                        ValueEnvironment<FloatIntervals> valueState = state.getState().getValueState();

                        FloatIntervals intervalAbstractValue = valueState.eval((ValueExpression) s, div, state.getState());

                        // TODO: add checks for division by zero
                        // ------------ mio ------------------------------------------------------------------------------------
                        if (intervalAbstractValue == null){
                            tool.warnOn(div, "IntervalAbstractValue is null! ");
                            return;
                        }else if (intervalAbstractValue.isBottom()){
                            tool.warnOn(div, "IntervalAbstractValue is bottom! ");
                            return;
                        }
                        else if(intervalAbstractValue.isTop()){
                            tool.warnOn(div, "Interval abstract value is top! Possible division by zero!");
                        }
                        else {
                            FloatInterval a = intervalAbstractValue.interval;
                            double al = a.getLow();
                            double au = a.getHigh();

                            if (a.isFinite() && al <= 0.0 && au >= 0.0 ) {
                                if(al == 0.0 && au == 0.0){
                                    tool.warnOn(div, "Definite division by zero " + a);
                                    System.err.println("Division by zero ([0,0])! ");

                                }
                                tool.warnOn(div, "Possible division by zero: divisor may be zero in " + a);
                                System.err.println("Potential division by zero! ");
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
                                              SimpleAbstractState<PointBasedHeap, ValueEnvironment<FloatIntervals>, TypeEnvironment<InferredTypes>> state) throws SemanticException {

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