package it.unive.scsr.checkers;

import java.text.MessageFormat;
import java.util.*;
import java.util.stream.Collectors;

import it.unive.lisa.analysis.SimpleAbstractState;
import it.unive.lisa.analysis.heap.pointbased.PointBasedHeap;
import it.unive.lisa.analysis.nonrelational.value.TypeEnvironment;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.analysis.types.InferredTypes;
import it.unive.lisa.checks.semantic.CheckToolWithAnalysisResults;
import it.unive.lisa.checks.semantic.SemanticCheck;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.Assignment;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.VariableRef;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.type.NumericType;
import it.unive.lisa.type.Type;
import it.unive.scsr.Intervals;
import it.unive.scsr.checkers.overflow.checkers.SizeChecker;

import static it.unive.scsr.utils.Logging.defaultLogger;
import static java.util.Map.entry;

public class OverflowChecker implements SemanticCheck<SimpleAbstractState<PointBasedHeap, ValueEnvironment<Intervals>, TypeEnvironment<InferredTypes>>> {

    public enum NumericalSize {
        INT8,
        INT16,
        INT32,
        UINT8,
        UINT16,
        UINT32,
        FLOAT8,
        FLOAT16,
        FLOAT32,
    }

    private final NumericalSize size;
    private final Map<CodeLocation, Intervals> exitStates = new HashMap<>();

    public OverflowChecker(NumericalSize size) {
        this.size = size;
    }

    @Override
    public void beforeExecution(CheckToolWithAnalysisResults<SimpleAbstractState<PointBasedHeap, ValueEnvironment<Intervals>, TypeEnvironment<InferredTypes>>> tool) {
        exitStates.clear();
    }

    @Override
    public boolean visit(CheckToolWithAnalysisResults<SimpleAbstractState<PointBasedHeap, ValueEnvironment<Intervals>, TypeEnvironment<InferredTypes>>> tool, CFG graph, Statement node) {

        if (node instanceof Assignment assignment) {
            Expression leftExpression = assignment.getLeft();

            // Checking if each variable reference is over/under-flowing.
            if (leftExpression instanceof VariableRef) {
                checkVariableRef(tool, (VariableRef) leftExpression, graph, node);
            }

        } else {

            // Checking if each variable reference is over/under-flowing.
            if (node instanceof VariableRef) {
                checkVariableRef(tool, (VariableRef) node, graph, node);
            }
        }

        return true;
    }

    @Override
    public void afterExecution(CheckToolWithAnalysisResults<SimpleAbstractState<PointBasedHeap, ValueEnvironment<Intervals>, TypeEnvironment<InferredTypes>>> tool) {
        record LocationIntervals(CodeLocation location, Intervals intervals) {
        }

        // Extract the position for each detected interval domain. After collecting each pair consisting of a value
        // domain and its location, semantic checking is performed. If the value overflows, a warning is generated.
        exitStates.
                entrySet()
                .stream()
                .map(entry -> new LocationIntervals(entry.getKey(), entry.getValue()))
                .forEach(locationIntervals -> {
                    // The overflow depends on the size of NumericalSize.
                    var stickiness = SizeChecker
                            .findBy(size)
                            .map(sizeChecker -> sizeChecker.isOverflowing(locationIntervals.intervals))
                            .orElse(SizeChecker.OverflowingLevel.base());

                    if (stickiness.isOverflowing()) {
                        // Additional metadata to enable warning processing.
                        var warningSet = new HashSet<>(Set.of(
                                entry("location", locationIntervals.location.getCodeLocation()),
                                entry("numericalSize", size.name().toLowerCase()),
                                entry("abstractData", locationIntervals.intervals.representation().toString())));

                        // To understand whether the overflow will definitely happen or not.
                        warningSet.add(Map.entry("definitely", String.valueOf(stickiness.definitely())));

                        // Finally, pointing out the warnings.
                        tool.warn(new WarnMap(warningSet).toString());
                    }
                });
    }

    private boolean isSupportedType(final Set<Type> possibleTypes) {
        // The set of inferred types must contain at least one NumericType.
        return possibleTypes
                .stream()
                .map(Type::getClass)
                .anyMatch(NumericType.class::isAssignableFrom);
    }

    private void checkVariableRef(CheckToolWithAnalysisResults<SimpleAbstractState<PointBasedHeap, ValueEnvironment<Intervals>, TypeEnvironment<InferredTypes>>> tool, VariableRef varRef, CFG graph, Statement node) {

        var id = new Variable(varRef.getStaticType(), varRef.getName(), varRef.getLocation());
        var staticType = Set.of(id.getStaticType());
        var dynamicTypes = tool
                .getResultOf(graph)
                .stream()
                .flatMap(result -> new Analyzer<>(result).getDynamicTypes(id, varRef).stream())
                .collect(Collectors.toSet());

        // Perform analysis only for some specific types, namely those checked in the "isSupportedType" method. If
        // staticType is untyped, then dynamic types are checked.
        if (!(isSupportedType(staticType) || isSupportedType(dynamicTypes))) {
            defaultLogger.info(() -> MessageFormat
                    .format("Neither set {0} nor set {1} include types available for evaluations at node {2}",
                            staticType,
                            dynamicTypes,
                            node));

            // Returns without performing any parsing because the inferred type is not supported.
            return;
        }

        tool.getResultOf(graph).forEach(result -> {
            // Computes the exit state for the specified node.
            var env = new Analyzer<>(result)
                    .exitStateOrThrow();

            if (env.knowsIdentifier(id)) {
                // Since this checker deals with the interval domain, the environment state of the value must be an
                // interval.
                var intervals = env.getState(id);
                exitStates.put(id.getCodeLocation(), intervals);
            }
        });
    }
}