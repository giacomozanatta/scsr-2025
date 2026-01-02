package it.unive.scsr;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.lattices.Satisfiability;
import it.unive.lisa.analysis.nonrelational.value.BaseNonRelationalValueDomain;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.symbolic.value.operator.AdditionOperator;
import it.unive.lisa.symbolic.value.operator.ComparisonOperator;
import it.unive.lisa.symbolic.value.operator.MultiplicationOperator;
import it.unive.lisa.symbolic.value.operator.NegatableOperator;
import it.unive.lisa.symbolic.value.operator.SubtractionOperator;
import it.unive.lisa.symbolic.value.operator.binary.BinaryOperator;
import it.unive.lisa.symbolic.value.operator.unary.UnaryOperator;
import it.unive.lisa.util.representation.StringRepresentation;
import it.unive.lisa.util.representation.StructuredRepresentation;

public class Pentagons implements BaseNonRelationalValueDomain<Pentagons> {

	private final ValueEnvironment<Intervals> intervals;
	private final Map<VariablePair, Number> constraints;

	public static final Pentagons TOP = new Pentagons();
	public static final Pentagons BOTTOM = new Pentagons(null);

	public Pentagons() {
		this.intervals = new ValueEnvironment<>(new Intervals()).top();
		this.constraints = new HashMap<>();
	}

	public Pentagons(ValueEnvironment<Intervals> intervals, Map<VariablePair, Number> constraints) {
		this.intervals = intervals;
		this.constraints = constraints;
	}

	private Pentagons(Void dummy) {
		this.intervals = null;
		this.constraints = null;
	}

	public static class VariablePair {
		private final Variable x;
		private final Variable y;

		public VariablePair(Variable x, Variable y) {
			this.x = x;
			this.y = y;
		}

		@Override
		public int hashCode() {
			return x.hashCode() * 31 + y.hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) return true;
			if (obj == null) return false;
			if (getClass() != obj.getClass()) return false;
			VariablePair other = (VariablePair) obj;
			return x.equals(other.x) && y.equals(other.y);
		}
	}

	@Override
	public Pentagons top() {
		return TOP;
	}

	@Override
	public boolean isTop() {
		return intervals != null && intervals.isTop() && constraints.isEmpty();
	}

	@Override
	public Pentagons bottom() {
		return BOTTOM;
	}

	@Override
	public boolean isBottom() {
		return intervals == null;
	}

	@Override
	public StructuredRepresentation representation() {
		if (isBottom()) return new StringRepresentation("⊥");
		if (isTop()) return new StringRepresentation("⊤");

		StringBuilder sb = new StringBuilder();
		sb.append("Intervals: ").append(intervals.representation());
		if (!constraints.isEmpty()) {
			sb.append(", Constraints: {");
			boolean first = true;
			for (Map.Entry<VariablePair, Number> entry : constraints.entrySet()) {
				if (!first) sb.append(", ");
				first = false;
				VariablePair pair = entry.getKey();
				sb.append(pair.x.getName()).append(" <= ").append(pair.y.getName()).append(" + ").append(entry.getValue());
			}
			sb.append("}");
		}
		return new StringRepresentation(sb.toString());
	}

	@Override
	public Pentagons lubAux(Pentagons other) throws SemanticException {
		if (isBottom()) return other;
		if (other.isBottom()) return this;

		ValueEnvironment<Intervals> joinedIntervals = intervals.lub(other.intervals);
		Map<VariablePair, Number> joinedConstraints = new HashMap<>();

		for (Map.Entry<VariablePair, Number> entry : constraints.entrySet()) {
			VariablePair pair = entry.getKey();
			Number c1 = entry.getValue();
			Number c2 = other.constraints.get(pair);
			if (c2 != null) {
				joinedConstraints.put(pair, Math.max(c1.doubleValue(), c2.doubleValue()));
			}
		}

		return new Pentagons(joinedIntervals, joinedConstraints);
	}

	@Override
	public Pentagons glbAux(Pentagons other) throws SemanticException {
		if (isBottom() || other.isBottom()) return bottom();

		ValueEnvironment<Intervals> metIntervals = intervals.glb(other.intervals);
		if (metIntervals.isBottom()) return bottom();

		Map<VariablePair, Number> metConstraints = new HashMap<>(constraints);
		for (Map.Entry<VariablePair, Number> entry : other.constraints.entrySet()) {
			VariablePair pair = entry.getKey();
			Number c1 = metConstraints.get(pair);
			Number c2 = entry.getValue();
			if (c1 == null || c2.doubleValue() < c1.doubleValue()) {
				metConstraints.put(pair, c2);
			}
		}

		if (checkInconsistency(metIntervals, metConstraints)) return bottom();
		return new Pentagons(metIntervals, metConstraints);
	}

	private boolean checkInconsistency(ValueEnvironment<Intervals> intervals, Map<VariablePair, Number> constraints) {
		for (Map.Entry<VariablePair, Number> entry : constraints.entrySet()) {
			VariablePair pair = entry.getKey();
			Number constant = entry.getValue();

			Intervals xInterval = intervals.getState(pair.x);
			Intervals yInterval = intervals.getState(pair.y);

			if (xInterval != null && yInterval != null) {
				Number xLow = xInterval.getLow();
				Number yHigh = yInterval.getHigh();

				if (xLow != null && yHigh != null) {
					if (xLow.doubleValue() > yHigh.doubleValue() + constant.doubleValue()) {
						return true;
					}
				}
			}
		}
		return false;
	}

	@Override
	public boolean lessOrEqualAux(Pentagons other) throws SemanticException {
		if (isBottom()) return true;
		if (other.isBottom()) return false;

		if (!intervals.lessOrEqual(other.intervals)) return false;

		for (Map.Entry<VariablePair, Number> entry : constraints.entrySet()) {
			VariablePair pair = entry.getKey();
			Number c1 = entry.getValue();
			Number c2 = other.constraints.get(pair);

			if (c2 == null || c1.doubleValue() > c2.doubleValue()) {
				return false;
			}
		}

		return true;
	}

	@Override
	public Pentagons evalNonNullConstant(Constant constant, ProgramPoint pp, SemanticOracle oracle) throws SemanticException {
		if (isBottom()) return bottom();

		Intervals interval = new Intervals().evalNonNullConstant(constant, pp, oracle);
		ValueEnvironment<Intervals> newIntervals = new ValueEnvironment<>(interval).top();
		return new Pentagons(newIntervals, new HashMap<>());
	}

	@Override
	public Pentagons evalUnaryExpression(UnaryOperator operator, Pentagons arg, ProgramPoint pp, SemanticOracle oracle) throws SemanticException {
		if (arg.isBottom()) return bottom();

		ValueEnvironment<Intervals> newIntervals = new ValueEnvironment<>(new Intervals()).top();
		return new Pentagons(newIntervals, new HashMap<>());
	}

	@Override
	public Pentagons evalBinaryExpression(BinaryOperator operator, Pentagons left, Pentagons right, ProgramPoint pp, SemanticOracle oracle) throws SemanticException {
		if (left.isBottom() || right.isBottom()) return bottom();

		ValueEnvironment<Intervals> newIntervals = new ValueEnvironment<>(new Intervals()).top();
		return new Pentagons(newIntervals, new HashMap<>());
	}

	public Pentagons evalIdentifier(Identifier id, ProgramPoint pp, SemanticOracle oracle) throws SemanticException {
		if (isBottom()) return bottom();

		ValueEnvironment<Intervals> newIntervals = new ValueEnvironment<>(new Intervals()).top();
		if (id instanceof Variable) {
			Variable var = (Variable) id;
			Intervals varInterval = intervals.getState(var);
			if (varInterval != null) {
				newIntervals = newIntervals.putState(var, varInterval);
			}
		}

		Map<VariablePair, Number> newConstraints = new HashMap<>();
		if (id instanceof Variable) {
			Variable var = (Variable) id;
			for (Map.Entry<VariablePair, Number> entry : constraints.entrySet()) {
				VariablePair pair = entry.getKey();
				if (pair.x.equals(var) || pair.y.equals(var)) {
					newConstraints.put(pair, entry.getValue());
				}
			}
		}

		return new Pentagons(newIntervals, newConstraints);
	}

	// NO @Override annotation here - this is a custom method
	public Satisfiability satisfiesBinaryExpression(BinaryOperator operator, Pentagons left, Pentagons right, ProgramPoint pp, SemanticOracle oracle) throws SemanticException {
		if (left.isBottom() || right.isBottom()) return Satisfiability.BOTTOM;
		if (operator instanceof ComparisonOperator) return Satisfiability.UNKNOWN;
		return Satisfiability.UNKNOWN;
	}

	@Override
	public ValueEnvironment<Pentagons> assumeBinaryExpression(ValueEnvironment<Pentagons> environment, BinaryOperator operator, ValueExpression left, ValueExpression right, ProgramPoint src, ProgramPoint dest, SemanticOracle oracle) throws SemanticException {
		if (environment.isBottom()) return environment.bottom();
		return environment;
	}

	// NO @Override annotation here - this is a custom method
	public Pentagons variableCreation(Identifier id, ProgramPoint pp) throws SemanticException {
		if (isBottom()) return bottom();
		ValueEnvironment<Intervals> newIntervals = intervals.putState((Variable) id, new Intervals().top());
		return new Pentagons(newIntervals, constraints);
	}

	// NO @Override annotation here - this is a custom method
	public Pentagons variableAssignement(Identifier id, Pentagons value, ProgramPoint pp) throws SemanticException {
		if (isBottom() || value.isBottom()) return bottom();
		if (!(id instanceof Variable)) return this;

		Variable var = (Variable) id;
		ValueEnvironment<Intervals> newIntervals = intervals.putState(var, value.intervals.getState(var));
		Map<VariablePair, Number> newConstraints = new HashMap<>(constraints);
		newConstraints.entrySet().removeIf(entry -> entry.getKey().x.equals(var) || entry.getKey().y.equals(var));
		newConstraints.putAll(value.constraints);
		return new Pentagons(newIntervals, newConstraints);
	}

	@Override
	public Pentagons wideningAux(Pentagons other) throws SemanticException {
		if (isBottom()) return other;
		if (other.isBottom()) return this;

		ValueEnvironment<Intervals> widenedIntervals = intervals.widening(other.intervals);
		Map<VariablePair, Number> widenedConstraints = new HashMap<>();

		for (Map.Entry<VariablePair, Number> entry : constraints.entrySet()) {
			VariablePair pair = entry.getKey();
			Number c1 = entry.getValue();
			Number c2 = other.constraints.get(pair);
			if (c2 != null) {
				widenedConstraints.put(pair, Math.max(c1.doubleValue(), c2.doubleValue()));
			}
		}

		return new Pentagons(widenedIntervals, widenedConstraints);
	}

	@Override
	public Pentagons narrowingAux(Pentagons other) throws SemanticException {
		if (isBottom() || other.isBottom()) return bottom();

		ValueEnvironment<Intervals> narrowedIntervals = intervals.narrowing(other.intervals);
		Map<VariablePair, Number> narrowedConstraints = new HashMap<>();

		for (Map.Entry<VariablePair, Number> entry : constraints.entrySet()) {
			VariablePair pair = entry.getKey();
			Number c1 = entry.getValue();
			Number c2 = other.constraints.get(pair);
			if (c2 != null) {
				narrowedConstraints.put(pair, Math.min(c1.doubleValue(), c2.doubleValue()));
			}
		}

		return new Pentagons(narrowedIntervals, narrowedConstraints);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((constraints == null) ? 0 : constraints.hashCode());
		result = prime * result + ((intervals == null) ? 0 : intervals.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		Pentagons other = (Pentagons) obj;
		if (constraints == null) {
			if (other.constraints != null) return false;
		} else if (!constraints.equals(other.constraints)) return false;
		if (intervals == null) {
			if (other.intervals != null) return false;
		} else if (!intervals.equals(other.intervals)) return false;
		return true;
	}
}