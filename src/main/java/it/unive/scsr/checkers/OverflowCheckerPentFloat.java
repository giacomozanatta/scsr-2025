package it.unive.scsr.checkers;

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
import it.unive.lisa.util.numeric.IntInterval;
import it.unive.lisa.util.numeric.MathNumber;
import it.unive.scsr.*;

import java.util.HashSet;
import java.util.Set;

public class OverflowCheckerPentFloat implements SemanticCheck<SimpleAbstractState<PointBasedHeap, FloatPentagons, TypeEnvironment<InferredTypes>>> {

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

    private OverflowChecker.NumericalSize size;

    public OverflowCheckerPentFloat(OverflowChecker.NumericalSize size) {
        this.size = size;
    }

    @Override
    public boolean visit(
            CheckToolWithAnalysisResults<SimpleAbstractState<PointBasedHeap, FloatPentagons, TypeEnvironment<InferredTypes>>> tool,
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

    private void checkVariableRef(CheckToolWithAnalysisResults<SimpleAbstractState<PointBasedHeap, FloatPentagons, TypeEnvironment<InferredTypes>>> tool, VariableRef varRef, CFG graph, Statement node ) {
        Variable id = new Variable(((VariableRef) varRef).getStaticType(), ((VariableRef) varRef).getName(), ((VariableRef) varRef).getLocation());

        Type staticType = id.getStaticType();
        Set<Type> dynamicTypes = getPossibleDynamicTypes(tool, graph, node, id, varRef);

        Statement target = node;

        // TODO: implement type checks, it is required a numerical type
        // hint: if staticType.isUntyped() == true, then should be checked possible dynamic types
        // --------------------------------------------------------- mio ------------------------------------------------------------------
        boolean isNumerical = staticType.isNumericType();
        boolean isUntyped = staticType.isUntyped();

        if (isUntyped || !isNumerical) {
            for (Type dynamic : dynamicTypes) {
                if (dynamic.isNumericType()) {
                    isNumerical = true;
                    break;
                }
            }
        }

        if (!isNumerical){
            tool.warn("This is not a numerical type! ");
            return;

        }


        // -------------------------------------------------------------------------------------------------------------------------------

        if (varRef.getParentStatement() instanceof Assignment && ((Assignment) varRef.getParentStatement()).getLeft() == varRef) {
            target = varRef.getParentStatement();
        }


        for (AnalyzedCFG<SimpleAbstractState<PointBasedHeap, FloatPentagons,
                TypeEnvironment<InferredTypes>>> result : tool.getResultOf(graph)) {
            /*SimpleAbstractState<PointBasedHeap, Pentagons, TypeEnvironment<InferredTypes>> state = result.getAnalysisStateAfter(target).getState();
            Intervals intervalAbstractValue = state.getValueState().getState(id);*/

            FloatPentagons valueState = result.getAnalysisStateAfter(node).getState().getValueState();
            ValueEnvironment<FloatIntervals> valueStateIntervals = valueState.getIntervals();
            FloatIntervals intervalAbstractValue = valueStateIntervals.getState(id);

            // TODO: implement logic for overflow/underflow checks
            // hint: it depends to the NumericalSize size
            // -------------------------------- mio ----------------------------------------------------------
            if(intervalAbstractValue.isBottom() || intervalAbstractValue.interval == null || intervalAbstractValue.isTop()){
                tool.warn("interval abstract value null of top! ");
                return;
            }else{
                FloatInterval a = intervalAbstractValue.interval;
                double la = a.getLow();
                double ua = a.getHigh();

                double lower = la;
                double upper = ua;
                double min = getMin(size);
                double max = getMax(size);

                //underflow
                if(lower != Double.NEGATIVE_INFINITY && lower != Double.NaN){ //if lower is finite
                    if(lower < min){
                        if(upper <= min) {
                            System.err.printf("Definite Underflow! Lower bound is < then minimum and upper bound is <= then minimum (for size: %s )", size);
                        }
                        //System.err.println("Underflow! Lower bound is < then minimum ");
                        else
                            System.err.printf("Possible Underflow! Lower bound is < then minimum (for size: %s )", size);

                    }
                }

                //overflow
                if(upper != Double.POSITIVE_INFINITY && upper != Double.NaN){
                    if(upper > max){
                        if(lower >= max){
                            System.err.printf("[%s] Underflow in %s: %.3f < %.3f location %s%n",
                                    size, id.getName(), lower, min, id.getCodeLocation());
                            //System.err.printf("Definite Overflow! Lower bound is >= then maximum and upper bound is > then maximum (for size: %s )", size);
                        }
                        else System.err.printf("Possible Overflow! Upper bound is > then maximum (for size: %s )", size);
                    }
                }
            }


            // --------------------------------------------------------------------------------------------------------
        }


    }

    // compute possible dynamic types / runtime types
    private Set<Type> getPossibleDynamicTypes(
            CheckToolWithAnalysisResults<SimpleAbstractState<PointBasedHeap, FloatPentagons, TypeEnvironment<InferredTypes>>> tool,
            CFG graph, Statement node, Variable id, VariableRef varRef) {

        Set<Type> possibleDynamicTypes = new HashSet<>();
        for (AnalyzedCFG<
                SimpleAbstractState<PointBasedHeap, FloatPentagons,
                        TypeEnvironment<InferredTypes>>> result : tool.getResultOf(graph)) {
            SimpleAbstractState<PointBasedHeap, FloatPentagons, TypeEnvironment<InferredTypes>> state = result.getAnalysisStateAfter(varRef).getState();
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

    private double getMin(OverflowChecker.NumericalSize size){
        return switch (size){
            case INT8 -> -128;
            case INT16 -> -32768;
            case INT32 -> Integer.MIN_VALUE;
            case UINT8 -> 0;
            case UINT16 -> 0;
            case UINT32 -> 0;
            case FLOAT8 -> -240.0;
            case FLOAT16 -> -65504.0;
            case FLOAT32 -> -Float.MAX_VALUE;
        };

    }

    private double getMax(OverflowChecker.NumericalSize size){
        return switch (size){
            case INT8 -> 127;
            case INT16 -> 32767;
            case INT32 -> Integer.MAX_VALUE;
            case UINT8 -> 255;
            case UINT16 -> 65535;
            case UINT32 -> 4294967295L;
            case FLOAT8 -> 240.0;
            case FLOAT16 -> 65504.0;
            case FLOAT32 -> Float.MAX_VALUE;
        };

    }

    private double parseToDouble(MathNumber number){
        if (number != null) {
            String str = number.toString();
            if (str.equals("Inf") || str.equals("+Inf"))
                return Double.POSITIVE_INFINITY;
            if (str.equals("-Inf"))
                return Double.NEGATIVE_INFINITY;
            return Double.parseDouble(str);
        }
        return Double.NaN;
    }



}

