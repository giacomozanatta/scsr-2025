package it.unive.scsr.checkers;

import java.math.BigDecimal;
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
//import it.unive.lisa.util.numeric.CustomMathNumber;
//import it.unive.lisa.util.numeric.CustomMathNumberConversionException;
import it.unive.scsr.*;

public class HybridOverflowPentagonsChecker implements SemanticCheck<SimpleAbstractState<PointBasedHeap, HybridPentagons, TypeEnvironment<InferredTypes>>>
{

    public HybridOverflowPentagonsChecker(HybridOverflowChecker.NumericalSize size) {
        this.size = size;
    }

    public enum NumericalSize {
        INT8, INT16, INT32, UINT8, UINT16, UINT32, FLOAT8, FLOAT16, FLOAT32
    }

    private final HybridOverflowChecker.NumericalSize size;
/*
    public HybridOverflowPentagonsChecker(NumericalSize size) {
        this.size = size;
    }*/

    @Override
    public boolean visit(
            CheckToolWithAnalysisResults<SimpleAbstractState<PointBasedHeap, HybridPentagons, TypeEnvironment<InferredTypes>>> tool,
            CFG graph, Statement node) {

        if (node instanceof Assignment) {
            Assignment assignment = (Assignment) node;
            Expression left = assignment.getLeft();
            if (left instanceof VariableRef)
                checkVariable(tool, (VariableRef) left, graph, node);
        } else if (node instanceof VariableRef) {
            checkVariable(tool, (VariableRef) node, graph, node);
        }

        return true;
    }

    private void checkVariable(
            CheckToolWithAnalysisResults<SimpleAbstractState<PointBasedHeap, HybridPentagons, TypeEnvironment<InferredTypes>>> tool,
            VariableRef varRef, CFG graph, Statement node) {

        Variable id = new Variable(varRef.getStaticType(), varRef.getName(), varRef.getLocation());
        Type staticType = id.getStaticType();
        Set<Type> dynamicTypes = getPossibleDynamicTypes(tool, graph, node, id, varRef);

        boolean isNumeric = staticType.isNumericType();
        if (!isNumeric)
            for (Type t : dynamicTypes)
                if (t.isNumericType())
                    isNumeric = true;

        if (!isNumeric)
            return;

        if (varRef.getParentStatement() instanceof Assignment
                && ((Assignment) varRef.getParentStatement()).getLeft() == varRef)
            node = varRef.getParentStatement();

        double min = 0, max = 0;
        boolean isFloat = false;

        switch (size) {
            case INT8 -> { min = Byte.MIN_VALUE; max = Byte.MAX_VALUE; }
            case INT16 -> { min = Short.MIN_VALUE; max = Short.MAX_VALUE; }
            case INT32 -> { min = Integer.MIN_VALUE; max = Integer.MAX_VALUE; }
            case UINT8 -> { min = 0; max = 255; }
            case UINT16 -> { min = 0; max = 65535; }
            case UINT32 -> { min = 0; max = 4294967295L; }
            case FLOAT8 -> { min = -240.0; max = 240.0; isFloat = true; }
            case FLOAT16 -> { min = -65504.0; max = 65504.0; isFloat = true; }
            case FLOAT32 -> { min = -Float.MAX_VALUE; max = Float.MAX_VALUE; isFloat = true; }
        }

        for (AnalyzedCFG<SimpleAbstractState<PointBasedHeap, HybridPentagons, TypeEnvironment<InferredTypes>>> result : tool.getResultOf(graph)) {
            HybridPentagons pentagons = result.getAnalysisStateAfter(node).getState().getValueState();

            // Check Intervals
            HybridIntervals interval = pentagons.getIntervals().getState(id);
            if (interval != null && !interval.isTop()) {
                CustomMathNumber low = interval.getLow(), high = interval.getHigh();

                if (low != null && low.isFinite() && low.getNumber().doubleValue() < min)
                    tool.warnOn(node, "[Type " + size + "] Underflow: " + low + " < " + min + " in variable " + id.getName());

                if (high != null && high.isFinite() && high.getNumber().doubleValue() > max)
                    tool.warnOn(node, "[Type " + size + "] Overflow: " + high + " > " + max + " in variable " + id.getName());
            } else if (interval != null && interval.isTop()) {
                tool.warnOn(node, "Unbounded value for variable " + id.getName());
            }

            // Check UpperBounds-based indirect underflow
            // Check UpperBounds-based indirect underflow
            UpperBounds upper = pentagons.getUpperbounds().getState(id);
            if (upper != null && !upper.isTop()) {
                for (Identifier ub : upper) {  // usa Identifier, non Variable
                    HybridIntervals ubInterval = pentagons.getIntervals().getState(ub);  // usa il getter
                    if (ubInterval != null && !ubInterval.isBottom()) {
                        CustomMathNumber low = ubInterval.getLow();
                        if (low != null && low.isFinite() && low.getNumber().doubleValue() < min) {
                            tool.warnOn(node, "[Type " + size + "] Potential Underflow via upper-bound " + ub.getName()
                                    + ": " + low + " < " + min);
                        }
                    }
                }
            }
        }
    }

    private Set<Type> getPossibleDynamicTypes(
            CheckToolWithAnalysisResults<SimpleAbstractState<PointBasedHeap, HybridPentagons, TypeEnvironment<InferredTypes>>> tool,
            CFG graph, Statement node, Variable id, VariableRef varRef) {

        Set<Type> possibleDynamicTypes = new HashSet<>();

        for (AnalyzedCFG<SimpleAbstractState<PointBasedHeap, HybridPentagons, TypeEnvironment<InferredTypes>>> result : tool.getResultOf(graph)) {
            SimpleAbstractState<PointBasedHeap, HybridPentagons, TypeEnvironment<InferredTypes>> state = result.getAnalysisStateAfter(varRef).getState();
            try {
                Type dynamicType = state.getDynamicTypeOf(id, varRef, state);
                if (dynamicType != null && !dynamicType.isUntyped()) {
                    possibleDynamicTypes.add(dynamicType);
                } else {
                    Set<Type> runtimeTypes = state.getRuntimeTypesOf(id, varRef, state);
                    for (Type t : runtimeTypes)
                        if (!t.isUntyped())
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
