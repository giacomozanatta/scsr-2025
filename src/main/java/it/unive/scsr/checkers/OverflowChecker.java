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

    // Represents the exit state of an analysis, containing abstract data and the overflow level.
    public record ExitState(Intervals abstractData, SizeChecker.OverflowingLevel result) {
        @Override
        public String toString() {
            var abstractEntry = Map.entry("abstract", abstractData.representation().toString());
            var overflowingEntry = Map.entry("overflowing", String.valueOf(result.isOverflowing()));
            var definitelyEntry = Map.entry("definitely", String.valueOf(result.definitely()));
            return new WarnMap(Set.of(abstractEntry, overflowingEntry, definitelyEntry)).toString();
        }
    }

    // Represents the key for an exit state, composed of a code location, a variable, and its numerical size.
    public record ExitKey(CodeLocation codeLocation, Variable variable, NumericalSize size) {
        @Override
        public String toString() {
            var codeLocationEntry = Map.entry("location", codeLocation.getCodeLocation());
            var variableEntry = Map.entry("variable", variable.getName());
            var sizeEntry = Map.entry("size", size.name().toLowerCase());
            return new WarnMap(Set.of(codeLocationEntry, variableEntry, sizeEntry)).toString();
        }
    }

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
    private final Map<ExitKey, Set<ExitState>> exitStates = new HashMap<>();

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
        exitStates.forEach((key, value) -> {
            // Extracts information from the key and results from the input value.
            var info = key.toString();
            var data = Arrays.toString(value
                    .stream()
                    .map(ExitState::toString)
                    .toArray());

            // Log the warnings using the provided tool.
            tool.warn(new WarnMap(Set.of(entry("info", info), entry("data", data))).toString());
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
        var types = tool
                .getResultOf(graph)
                .stream()
                .flatMap(result -> new Analyzer<>(result).inferTypes(id, varRef, new Analyzer<>(result).getAnalysisStateAfter(varRef)).stream())
                .collect(Collectors.toSet());

        // Perform analysis only for some specific types, namely those checked in the "isSupportedType" method. If
        // staticType is untyped, then dynamic types are checked.
        if (!(isSupportedType(types))) {
            defaultLogger.info(() -> MessageFormat
                    .format("Set {0} does not include types available for evaluations at node {1}",
                            types,
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

                // The overflow depends on the size of NumericalSize.
                var stickiness = SizeChecker
                        .findBy(size)
                        .map(sizeChecker -> sizeChecker.isOverflowing(intervals))
                        .orElse(SizeChecker.OverflowingLevel.base());

                if (stickiness.definitely()) {
                    // Add the result only when a definite overflow has been detected.
                    var key = new ExitKey(id.getCodeLocation(), id, size);
                    var currentSet = exitStates.getOrDefault(key, new HashSet<>());
                    currentSet.add(new ExitState(intervals, stickiness));
                    exitStates.put(key, currentSet);
                }
            }
        });
    }
}