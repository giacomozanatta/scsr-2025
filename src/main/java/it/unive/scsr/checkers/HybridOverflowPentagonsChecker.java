package it.unive.scsr.checkers;

import it.unive.lisa.analysis.AnalyzedCFG;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SimpleAbstractState;
import it.unive.lisa.analysis.heap.pointbased.PointBasedHeap;
import it.unive.lisa.analysis.nonrelational.value.TypeEnvironment;
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
import it.unive.lisa.util.numeric.IntInterval;
import it.unive.lisa.util.numeric.MathNumber;
import it.unive.scsr.FloatInterval;
import it.unive.scsr.HybridIntervals;
import it.unive.scsr.HybridPentagons;
import it.unive.scsr.UpperBounds;

import java.util.HashSet;
import java.util.Set;

public class HybridOverflowPentagonsChecker implements SemanticCheck<SimpleAbstractState<PointBasedHeap, HybridPentagons, TypeEnvironment<InferredTypes>>>
{
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

    private final HybridOverflowChecker.NumericalSize size;

    public HybridOverflowPentagonsChecker(HybridOverflowChecker.NumericalSize size) {
        this.size = size;
    }

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

        Statement target = node;

        // ADDED: implement type checks, it is required a numerical type
        // hint: if staticType.isUntyped() == true, then should be checked possible dynamic types
        if(staticType.isUntyped()) {
            boolean flag = false;
            for (Type t : dynamicTypes)
                if (t.isNumericType())
                    flag = true;
            if(!flag)
                return;
        } else if(!staticType.isNumericType()) {
            //tool.warnOn(div, "NOT numeric Interval!");
            return;
        }

        double min = getTypeLowerBound();
        double max = getTypeUpperBound();

        if (varRef.getParentStatement() instanceof Assignment && ((Assignment) varRef.getParentStatement()).getLeft() == varRef) {
            node = varRef.getParentStatement();
        }

        for (AnalyzedCFG<SimpleAbstractState<PointBasedHeap, HybridPentagons, TypeEnvironment<InferredTypes>>> result : tool.getResultOf(graph)) {
            HybridPentagons pentagons = result.getAnalysisStateAfter(node).getState().getValueState();

            // Check Intervals
            HybridIntervals intervalAbstractValue = pentagons.getIntervals().getState(id);
            if (intervalAbstractValue == null || intervalAbstractValue.isBottom())
                if (intervalAbstractValue == null || intervalAbstractValue.interval == null || intervalAbstractValue.isBottom())
                    continue;

            if (intervalAbstractValue.isTop()) {
                tool.warnOn(node, String.format("[%s] Infinite value in the Interval of variable %s!", size, id.getName()));
                continue;
            }

            try {
                double lb, ub = 0.0;
                if (intervalAbstractValue.interval instanceof IntInterval) {
                    IntInterval interval = ((IntInterval) intervalAbstractValue.interval);
                    lb = interval.getLow().getNumber().doubleValue();
                    ub = interval.getHigh().getNumber().doubleValue();
                } else if (intervalAbstractValue.interval instanceof FloatInterval) {
                    FloatInterval interval = ((FloatInterval) intervalAbstractValue.interval);
                    lb = interval.getLow().getNumber().doubleValue();
                    ub = interval.getHigh().getNumber().doubleValue();
                } else {
                    tool.warnOn(node, String.format("[%s] ERROR in the input format!", size));
                    continue;
                }

                if (lb < min) {
                    if (ub < min)
                        tool.warnOn(node, String.format("[%s] Underflow DETECTED: %.3f < %.3f for variable %s",
                                size, ub, min, id.getName()));
                    else
                        tool.warnOn(node, String.format("[%s] POSSIBLE underflow DETECTED: %.3f < %.3f for variable %s",
                                size, lb, min, id.getName()));
                }
                if (ub > max) {
                    if (lb > max)
                        tool.warnOn(node, String.format("[%s] Overflow DETECTED: %.3f > %.3f for variable %s",
                                size, lb, max, id.getName()));
                    else
                        tool.warnOn(node, String.format("[%s] POSSIBLE overflow DETECTED: %.3f > %.3f for variable %s",
                                size, ub, max, id.getName()));
                }

                //upperbound check
                UpperBounds upper = pentagons.getUpperbounds().getState(id);
                if (upper != null && !upper.isTop()) {
                    for (Identifier u : upper) {
                        HybridIntervals ubInterval = pentagons.getIntervals().getState(u);
                        if (ubInterval != null && !ubInterval.isBottom()) {
                            MathNumber low = ubInterval.getLow();
                            if (low != null && low.isFinite() && low.getNumber().doubleValue() < min) {
                                tool.warnOn(node, "[Type " + size + "] Potential Underflow via upper-bound " + u.getName()
                                        + ": " + low + " < " + min);
                            }
                        }
                    }
                }
            } catch (Exception e) {
            }
        }
    }

    private double getTypeLowerBound() {
        switch (size) {
            case INT8:   return Byte.MIN_VALUE;
            case INT16:  return Short.MIN_VALUE;
            case INT32:  return Integer.MIN_VALUE;
            case UINT8:  return 0;
            case UINT16: return 0;
            case UINT32: return 0;
            case FLOAT8: return -240.0;
            case FLOAT16: return -65504.0;
            case FLOAT32: return -Float.MAX_VALUE;
            default:      return Double.NEGATIVE_INFINITY;
        }
    }
    private double getTypeUpperBound() {
        switch (size) {
            case INT8:   return Byte.MAX_VALUE;
            case INT16:  return Short.MAX_VALUE;
            case INT32:  return Integer.MAX_VALUE;
            case UINT8:  return 255;
            case UINT16: return 65535;
            case UINT32: return 4294967295L;
            case FLOAT8: return 240.0;
            case FLOAT16: return 65504.0;
            case FLOAT32: return Float.MAX_VALUE;
            default:      return Double.POSITIVE_INFINITY;
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
