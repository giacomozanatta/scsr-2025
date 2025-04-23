package it.unive.scsr.checkers;

import java.text.MessageFormat;
import java.util.HashSet;
import java.util.Map;
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
import it.unive.lisa.program.type.*;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;
import it.unive.scsr.Intervals;
import it.unive.scsr.checkers.overflow.checkers.SizeChecker;

import static it.unive.scsr.utils.Logging.defaultLogger;
import static java.util.Map.entry;

public class OverflowChecker implements SemanticCheck<
		SimpleAbstractState<PointBasedHeap, ValueEnvironment<Intervals>, TypeEnvironment<InferredTypes>>> {
	
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
	
	private final NumericalSize size;
	private final Map<CodeLocation, Intervals> exitStates = new HashMap<>();

	public OverflowChecker(NumericalSize size) { this.size = size; }

	@Override
	public boolean visit(
			CheckToolWithAnalysisResults<SimpleAbstractState<PointBasedHeap, ValueEnvironment<Intervals>, TypeEnvironment<InferredTypes>>> tool,
			CFG graph,
			Statement node) {
		
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

	// A numerical type is required. Current support is for UInt8Type, UInt16Type, UInt32Type, Int8Type, Int16Type, and
	// Int32Type.
	private boolean isSupportedType(final Set<Type> possibleTypes) {
		// The only types available are integer representations. Checking for overflow and underflow of floats is more
		// complex than checking for overflow and underflow of integers.
		final Set<Class<? extends Type>> availableTypes =
				Set.of(UInt8Type.class, UInt16Type.class, UInt32Type.class,
						Int8Type.class, Int16Type.class, Int32Type.class);

		// The set of inferred types must intersect the available types with at least one element.
        return possibleTypes
                .stream()
                .map(Type::getClass)
                .anyMatch(availableTypes::contains);
	}
	
	private void checkVariableRef(
			CheckToolWithAnalysisResults<SimpleAbstractState<PointBasedHeap, ValueEnvironment<Intervals>, TypeEnvironment<InferredTypes>>> tool,
			VariableRef varRef,
			CFG graph,
			Statement node) {

		Variable id = new Variable(varRef.getStaticType(), varRef.getName(), varRef.getLocation());
		
		var staticType = Set.of(id.getStaticType());
		var dynamicTypes = getPossibleDynamicTypes(tool, graph, node, id, varRef);

		// Perform analysis only for some specific types, namely those checked in the "isSupportedType" method. If
		// staticType is untyped, then dynamic types are checked.
		if (!(isSupportedType(staticType) || isSupportedType(dynamicTypes))) {
			defaultLogger.info(() -> MessageFormat
					.format("Neither set {0} nor set {1} include types available for evaluations at node {2}",
							staticType,
							dynamicTypes,
							node));
			return;
		}

		for (AnalyzedCFG<SimpleAbstractState<PointBasedHeap, ValueEnvironment<Intervals>,
							TypeEnvironment<InferredTypes>>> result : tool.getResultOf(graph)) {

			SimpleAbstractState<PointBasedHeap, ValueEnvironment<Intervals>, TypeEnvironment<InferredTypes>> state =
					result.getAnalysisStateAfter(node).getState();

			Intervals intervalAbstractValue = state.getValueState().getState(id);

			// The overflow depends on the size of NumericalSize.
			var stickiness = SizeChecker
					.findBy(size)
					.map(sizeChecker -> sizeChecker.isOverflowing(intervalAbstractValue))
					.orElse(SizeChecker.OverflowingLevel.base());

			if (stickiness.isOverflowing()) {
				// Additional metadata to enable warning processing.
				var warningSet = new HashSet<>(Set.of(
						entry("location", node.getLocation().getCodeLocation()),
						entry("abstractData", intervalAbstractValue.representation().toString())));

				// To understand whether the overflow will definitely happen or not.
				warningSet.add(Map.entry("definitely", String.valueOf(stickiness.definitely())));

				// Finally, pointing out the warnings.
				tool.warn(new WarnMap(warningSet).toString());
			}
		}
	}

	// Compute possible dynamic types.
	@SuppressWarnings("DataFlowIssue")
    private Set<Type> getPossibleDynamicTypes(
			CheckToolWithAnalysisResults<SimpleAbstractState<PointBasedHeap, ValueEnvironment<Intervals>, TypeEnvironment<InferredTypes>>> tool,
			CFG graph,
			Statement node,
			Variable id,
			VariableRef varRef) {
		
			Set<Type> possibleDynamicTypes = new HashSet<>();
			for (AnalyzedCFG<
					SimpleAbstractState<PointBasedHeap, ValueEnvironment<Intervals>,
							TypeEnvironment<InferredTypes>>> result : tool.getResultOf(graph)) {
				SimpleAbstractState<PointBasedHeap, ValueEnvironment<Intervals>, TypeEnvironment<InferredTypes>> state = result.getAnalysisStateAfter(varRef).getState();
				try {
					Type dynamicTypes = state.getDynamicTypeOf(id, varRef, state);
					if(dynamicTypes != null && !dynamicTypes.isUntyped()) {
						possibleDynamicTypes.add(dynamicTypes);
					} else if(dynamicTypes.isUntyped()){
						Set<Type> runtimeTypes = state.getRuntimeTypesOf(id, varRef, state);
						if(runtimeTypes.stream().anyMatch(t -> t != Untyped.INSTANCE))
                            possibleDynamicTypes.addAll(runtimeTypes);
					}
				} catch (SemanticException e) {
					System.err.println("Cannot check " + node);
					e.printStackTrace(System.err);
				}
	
			}	
		return possibleDynamicTypes;
	}
}