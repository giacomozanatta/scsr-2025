package it.unive.scsr.checkers;

import java.math.BigDecimal;
import java.util.*;

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
import it.unive.scsr.Intervals;

public class OverflowChecker implements
		SemanticCheck<
				SimpleAbstractState<PointBasedHeap, ValueEnvironment<Intervals>, TypeEnvironment<InferredTypes>>> {

	public enum NumericalSize {
		INT8, INT16, INT32,
		UINT8, UINT16, UINT32,
		FLOAT8, FLOAT16, FLOAT32,
	}

	@Override
	public boolean visit(
			CheckToolWithAnalysisResults<SimpleAbstractState<PointBasedHeap, ValueEnvironment<Intervals>, TypeEnvironment<InferredTypes>>> tool,
			CFG graph, Statement node) {

		if (node instanceof Assignment) {
			Assignment assignment = (Assignment) node;
			Expression leftExpression = assignment.getLeft();
			if (leftExpression instanceof VariableRef) {
				checkVariableRef(tool, (VariableRef) leftExpression, graph, node);
			}
		} else if (node instanceof VariableRef) {
			checkVariableRef(tool, (VariableRef) node, graph, node);
		}

		return true;
	}

	private void checkVariableRef(
			CheckToolWithAnalysisResults<SimpleAbstractState<PointBasedHeap, ValueEnvironment<Intervals>, TypeEnvironment<InferredTypes>>> tool,
			VariableRef varRef, CFG graph, Statement node) {

		Variable id = new Variable(varRef.getStaticType(), varRef.getName(), varRef.getLocation());
		Statement target = (varRef.getParentStatement() instanceof Assignment
				&& ((Assignment) varRef.getParentStatement()).getLeft() == varRef)
				? varRef.getParentStatement() : node;

		Map<String, List<String>> groupedReports = new LinkedHashMap<>();

		for (AnalyzedCFG<SimpleAbstractState<PointBasedHeap, ValueEnvironment<Intervals>, TypeEnvironment<InferredTypes>>> result
				: tool.getResultOf(graph)) {

			SimpleAbstractState<PointBasedHeap, ValueEnvironment<Intervals>, TypeEnvironment<InferredTypes>> state =
					result.getAnalysisStateAfter(target).getState();

			Intervals intervalAbstractValue = state.getValueState().getState(id);
			if (intervalAbstractValue == null || intervalAbstractValue.isBottom())
				continue;

			try {
				Object lowObj = intervalAbstractValue.interval.getLow();
				Object highObj = intervalAbstractValue.interval.getHigh();

				boolean lowMinusInf = invokeBoolMethod(lowObj, "isMinusInfinity");
				boolean highPlusInf = invokeBoolMethod(highObj, "isPlusInfinity");

				BigDecimal minBd = null, maxBd = null;
				try {
					if (!lowMinusInf) minBd = new BigDecimal(lowObj.toString());
				} catch (Exception ignored) { lowMinusInf = true; }
				try {
					if (!highPlusInf) maxBd = new BigDecimal(highObj.toString());
				} catch (Exception ignored) { highPlusInf = true; }

				String intervalText = "[" +
						(lowMinusInf ? "-Inf" : minBd.stripTrailingZeros().toPlainString()) + "," +
						(highPlusInf ? "+Inf" : maxBd.stripTrailingZeros().toPlainString()) + "]";

				for (NumericalSize ns : NumericalSize.values()) {
					String category = classifyForSize(ns, minBd, maxBd, lowMinusInf, highPlusInf, varRef, intervalText);
					if (category != null && !category.equals("[SAFE]")) {
						groupedReports.computeIfAbsent(category, k -> new ArrayList<>()).add(ns.name());
					}
				}

			} catch (Exception e) {
				tool.warnOn(node, "[GENERIC] Exception while checking " + varRef.getName() + ": " + e.getMessage());
			}
		}

		for (Map.Entry<String, List<String>> entry : groupedReports.entrySet()) {
			String category = entry.getKey();
			String labels = String.join("/", entry.getValue());
			tool.warnOn(node, category + " for types " + labels);
		}
	}

	private boolean invokeBoolMethod(Object obj, String method) {
		try {
			return (boolean) obj.getClass().getMethod(method).invoke(obj);
		} catch (Exception e) {
			return false;
		}
	}

	private String classifyForSize(NumericalSize ns, BigDecimal minBd, BigDecimal maxBd,
								   boolean minIsNegInf, boolean maxIsPosInf,
								   VariableRef varRef, String intervalText) {
		switch (ns) {
			case INT8:
				return classifyInteger(minBd, maxBd, minIsNegInf, maxIsPosInf,
						new BigDecimal(Byte.MIN_VALUE), new BigDecimal(Byte.MAX_VALUE),
						varRef, intervalText);
			case INT16:
				return classifyInteger(minBd, maxBd, minIsNegInf, maxIsPosInf,
						new BigDecimal(Short.MIN_VALUE), new BigDecimal(Short.MAX_VALUE),
						varRef, intervalText);
			case INT32:
				return classifyInteger(minBd, maxBd, minIsNegInf, maxIsPosInf,
						new BigDecimal(Integer.MIN_VALUE), new BigDecimal(Integer.MAX_VALUE),
						varRef, intervalText);
			case UINT8:
				return classifyInteger(minBd, maxBd, minIsNegInf, maxIsPosInf,
						BigDecimal.ZERO, new BigDecimal(255), varRef, intervalText);
			case UINT16:
				return classifyInteger(minBd, maxBd, minIsNegInf, maxIsPosInf,
						BigDecimal.ZERO, new BigDecimal(65535), varRef, intervalText);
			case UINT32:
				return classifyInteger(minBd, maxBd, minIsNegInf, maxIsPosInf,
						BigDecimal.ZERO, new BigDecimal("4294967295"), varRef, intervalText);
			case FLOAT8:
				return classifyFloat(minBd, maxBd, minIsNegInf, maxIsPosInf, 8, varRef, intervalText);
			case FLOAT16:
				return classifyFloat(minBd, maxBd, minIsNegInf, maxIsPosInf, 16, varRef, intervalText);
			case FLOAT32:
				return classifyFloat(minBd, maxBd, minIsNegInf, maxIsPosInf, 32, varRef, intervalText);
			default:
				return null;
		}
	}

	private String classifyInteger(BigDecimal minBd, BigDecimal maxBd, boolean minIsNegInf, boolean maxIsPosInf,
								   BigDecimal low, BigDecimal high,
								   VariableRef varRef, String intervalText) {
		if (!minIsNegInf && minBd != null && minBd.compareTo(high) > 0)
			return "[OVERFLOW] definite overflow: variable " + varRef.getName() + " range " + intervalText;

		if (!maxIsPosInf && maxBd != null && maxBd.compareTo(low) < 0)
			return "[UNDERFLOW] definite underflow: variable " + varRef.getName() + " range " + intervalText;

		if (!minIsNegInf && minBd != null && minBd.compareTo(low) < 0 &&
				(maxIsPosInf || (maxBd != null && maxBd.compareTo(low) >= 0)))
			return "[POSSIBLE_UNDERFLOW] possible underflow: variable " + varRef.getName() + " range " + intervalText;

		if (!maxIsPosInf && maxBd != null && maxBd.compareTo(high) > 0 &&
				(minIsNegInf || (minBd != null && minBd.compareTo(high) <= 0)))
			return "[POSSIBLE_OVERFLOW] possible overflow: variable " + varRef.getName() + " range " + intervalText;

		return "[SAFE]";
	}

	// ✅ Option A: improved float handling without new imports
	private String classifyFloat(
			BigDecimal minBd, BigDecimal maxBd,
			boolean minIsNegInf, boolean maxIsPosInf,
			int floatBits,
			VariableRef var, String intervalText) {

		double min = minIsNegInf ? Double.NEGATIVE_INFINITY : minBd.doubleValue();
		double max = maxIsPosInf ? Double.POSITIVE_INFINITY : maxBd.doubleValue();

		double absMax = Math.max(Math.abs(min), Math.abs(max));

		double threshold;
		switch (floatBits) {
			case 8:
				threshold = 240.0; // approx range for 8-bit float
				break;
			case 16:
				threshold = 65504.0; // IEEE half
				break;
			case 32:
			default:
				threshold = Float.MAX_VALUE;
				break;
		}

		if (Double.isInfinite(min) || Double.isInfinite(max))
			return "[POSSIBLE_OVERFLOW] unbounded range " + intervalText + " for " + var.getName();

		if (absMax > threshold)
			return "[DEFINITE_OVERFLOW] range exceeds float precision (" + floatBits + " bits): "
					+ intervalText + " for " + var.getName();

		if (absMax > threshold * 0.9)
			return "[POSSIBLE_OVERFLOW] near float limit (" + floatBits + " bits): "
					+ intervalText + " for " + var.getName();

		return "[SAFE]";
	}
}
