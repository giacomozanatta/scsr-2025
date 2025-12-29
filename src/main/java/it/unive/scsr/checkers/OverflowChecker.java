package it.unive.scsr.checkers;


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
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;
import it.unive.scsr.Intervals;
import it.unive.lisa.util.numeric.MathNumber;


public class OverflowChecker implements
SemanticCheck<
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
	
	private NumericalSize size;
	
	public OverflowChecker(NumericalSize size) {
		this.size = size;
	}

	@Override
	public boolean visit(
			CheckToolWithAnalysisResults<SimpleAbstractState<PointBasedHeap, ValueEnvironment<Intervals>, TypeEnvironment<InferredTypes>>> tool,
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
		private boolean isNumericType(Type t) {
		if (t == null)
			return false;

		// LiSA in genere ha metodi per classificare i tipi, ma nel caso non ci siano
		// facciamo un fallback robusto basato sul nome
		String name = t.toString().toLowerCase();
		return name.contains("int") || name.contains("float") || name.contains("double")
				|| name.contains("short") || name.contains("long") || name.contains("byte")
				|| name.contains("uint");
	}

	private double minFor(NumericalSize s) {
		switch (s) {
		case INT8:  return -128d;
		case INT16: return -32768d;
		case INT32: return -2147483648d;
		case UINT8:  return 0d;
		case UINT16: return 0d;
		case UINT32: return 0d;

		// valori pratici (float8/16 non sono IEEE “unici” in modo standard,
		// quindi uso bounds comunemente accettati per range rappresentabile)
		case FLOAT8:  return -240d;
		case FLOAT16: return -65504d;
		case FLOAT32: return -3.4028235e38d;
		default: throw new IllegalStateException("Unexpected size: " + s);
		}
	}

	private double maxFor(NumericalSize s) {
		switch (s) {
		case INT8:  return 127d;
		case INT16: return 32767d;
		case INT32: return 2147483647d;
		case UINT8:  return 255d;
		case UINT16: return 65535d;
		case UINT32: return 4294967295d;

		case FLOAT8:  return 240d;
		case FLOAT16: return 65504d;
		case FLOAT32: return 3.4028235e38d;
		default: throw new IllegalStateException("Unexpected size: " + s);
		}
	}

	


	private void checkVariableRef(
		CheckToolWithAnalysisResults<SimpleAbstractState<PointBasedHeap, ValueEnvironment<Intervals>, TypeEnvironment<InferredTypes>>> tool,
		VariableRef varRef, CFG graph, Statement node) {

	Variable id = new Variable(varRef.getStaticType(), varRef.getName(), varRef.getLocation());

	Type staticType = id.getStaticType();
	Set<Type> dynamicTypes = getPossibleDynamicTypes(tool, graph, node, id, varRef);

	// ---- 1) TYPE CHECK: deve essere numerico ----
	boolean numeric = false;

	if (staticType != null && !staticType.isUntyped())
		numeric = isNumericType(staticType);
	else
		numeric = dynamicTypes.stream().anyMatch(this::isNumericType);

	if (!numeric)
		return;

	// ---- 2) OVERFLOW/UNDERFLOW CHECK ----
	// bounds per la size scelta
	double minD = minFor(size);
	double maxD = maxFor(size);

	MathNumber min = MathNumber.fromDouble(minD);
	MathNumber max = MathNumber.fromDouble(maxD);

	for (AnalyzedCFG<SimpleAbstractState<PointBasedHeap, ValueEnvironment<Intervals>,
			TypeEnvironment<InferredTypes>>> result : tool.getResultOf(graph)) {

		SimpleAbstractState<PointBasedHeap, ValueEnvironment<Intervals>, TypeEnvironment<InferredTypes>> state =
				result.getAnalysisStateAfter(node).getState();

		Intervals v = state.getValueState().getState(id);
		if (v == null)
			continue;

		if (v.isBottom())
			continue;

		// TOP => valore sconosciuto => possibile overflow/underflow
		if (v.isTop()) {
			tool.warnOn(node, "[OverflowChecker-" + size + "] Value of '" + varRef.getName()
					+ "' is TOP/unknown: possible overflow/underflow.");
			continue;
		}
		
		
		MathNumber lo = v.interval.low();
		MathNumber hi = v.interval.high();

		// se non riesco a leggere bounds, meglio warning che silenzio
		if (lo == null || hi == null) {
			tool.warnOn(node, "[OverflowChecker-" + size + "] Cannot read bounds for '" + varRef.getName()
					+ "': possible overflow/underflow.");
			continue;
		}

		boolean under = lo.compareTo(min) < 0;
		boolean over = hi.compareTo(max) > 0;

		if (under || over) {
			String kind;
			if (under && over)
				kind = "overflow and underflow";
			else if (over)
				kind = "overflow";
			else
				kind = "underflow";

			tool.warnOn(node,
					"[OverflowChecker-" + size + "] Possible " + kind + " on '" + varRef.getName()
							+ "'. Interval=[" + lo + "," + hi + "], allowed=[" + min + "," + max + "].");
		}
	}
}


	// compute possible dynamic types / runtime types
	private Set<Type> getPossibleDynamicTypes(
			CheckToolWithAnalysisResults<SimpleAbstractState<PointBasedHeap, ValueEnvironment<Intervals>, TypeEnvironment<InferredTypes>>> tool,
			CFG graph, Statement node, Variable id, VariableRef varRef) {
		
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

	
		
	

}