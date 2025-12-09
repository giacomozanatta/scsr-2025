package it.unive.scsr;

import java.util.Objects;

import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.nonrelational.value.BaseNonRelationalValueDomain;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.symbolic.value.operator.AdditionOperator;
import it.unive.lisa.symbolic.value.operator.DivisionOperator;
import it.unive.lisa.symbolic.value.operator.MultiplicationOperator;
import it.unive.lisa.symbolic.value.operator.NegatableOperator;
import it.unive.lisa.symbolic.value.operator.SubtractionOperator;
import it.unive.lisa.symbolic.value.operator.binary.BinaryOperator;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonEq;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonGe;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonGt;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonLe;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonLt;
import it.unive.lisa.symbolic.value.operator.unary.UnaryOperator;
import it.unive.lisa.util.numeric.IntInterval;
import it.unive.lisa.util.numeric.MathNumber;
import it.unive.lisa.util.representation.StringRepresentation;
import it.unive.lisa.util.representation.StructuredRepresentation;

public class Intervals
		implements
		BaseNonRelationalValueDomain<Intervals>, Comparable<Intervals> {

	public final IntInterval interval;

	public static final Intervals ZERO = new Intervals(IntInterval.ZERO);
	public static final Intervals TOP = new Intervals(IntInterval.INFINITY);
	public static final Intervals BOTTOM = new Intervals(null);

	public Intervals(IntInterval interval) {
		this.interval = interval;
	}

	public Intervals(MathNumber lower, MathNumber upper) {
		this(new IntInterval(lower, upper));
	}

	public Intervals(int low, int high) {
		this(new IntInterval(low, high));
	}

	public Intervals() {
		this(IntInterval.INFINITY);
	}

	@Override
	public Intervals evalUnaryExpression(UnaryOperator operator, Intervals arg, ProgramPoint pp, SemanticOracle oracle)
			throws SemanticException {
		if (arg.isBottom()) return bottom();

		if (operator instanceof NegatableOperator) {
			MathNumber negUpper = MathNumber.ZERO.subtract(arg.interval.getLow());
			MathNumber negLower = MathNumber.ZERO.subtract(arg.interval.getHigh());
			return new Intervals(negLower, negUpper);
		}
		return top();
	}

	@Override
	public Intervals glbAux(Intervals other) throws SemanticException {
		MathNumber lA = this.interval.getLow();
		MathNumber lB = other.interval.getLow();
		MathNumber uA = this.interval.getHigh();
		MathNumber uB = other.interval.getHigh();

		if (lA.compareTo(uA) > 0 || lB.compareTo(uB) > 0) return BOTTOM;

		MathNumber newLower = lA.max(lB);
		MathNumber newUpper = uA.min(uB);

		if (newLower.compareTo(newUpper) > 0) return BOTTOM;

		Intervals newInterval = new Intervals(newLower, newUpper);
		return newLower.isMinusInfinity() && newUpper.isPlusInfinity() ? top() : newInterval;
	}

	@Override
	public Intervals lubAux(Intervals other) throws SemanticException {
		MathNumber lA = this.interval.getLow();
		MathNumber lB = other.interval.getLow();
		MathNumber uA = this.interval.getHigh();
		MathNumber uB = other.interval.getHigh();

		MathNumber newLower = lA.min(lB);
		MathNumber newUpper = uA.max(uB);

		Intervals newInterval = new Intervals(newLower, newUpper);
		return newLower.isMinusInfinity() && newUpper.isPlusInfinity() ? top() : newInterval;
	}

	@Override
	public boolean lessOrEqualAux(Intervals other) throws SemanticException {
		return other.interval.includes(this.interval);
	}

	@Override
	public Intervals top() { return TOP; }

	@Override
	public boolean isTop() { return interval != null && interval.isInfinity(); }

	@Override
	public Intervals bottom() { return BOTTOM; }

	@Override
	public boolean isBottom() { return interval == null; }

	@Override
	public StructuredRepresentation representation() {
		if (this.isBottom()) return Lattice.bottomRepresentation();
		return new StringRepresentation("[" + this.interval.getLow() + ", " + this.interval.getHigh() + "]");
	}

	@Override
	public int compareTo(Intervals o) {
		if (isBottom()) return o.isBottom() ? 0 : -1;
		if (isTop()) return o.isTop() ? 0 : 1;
		if (o.isBottom()) return 1;
		if (isTop()) return -1;
		return interval.compareTo(o.interval);
	}

	@Override
	public Intervals evalNonNullConstant(Constant constant, ProgramPoint pp, SemanticOracle oracle)
			throws SemanticException {
		// INTEGER SUPPORT
		if (constant.getValue() instanceof Integer) {
			Integer i = (Integer) constant.getValue();
			return new Intervals(i, i);
		}
		// FLOAT SUPPORT
		if (constant.getValue() instanceof Float) {
			Float f = (Float) constant.getValue();
			return new Intervals(new MathNumber(f), new MathNumber(f));
		}
		// DOUBLE SUPPORT
		if (constant.getValue() instanceof Double) {
			Double d = (Double) constant.getValue();
			return new Intervals(new MathNumber(d), new MathNumber(d));
		}
		return top();
	}

	@Override
	public Intervals evalBinaryExpression(BinaryOperator operator, Intervals left, Intervals right, ProgramPoint pp,
										  SemanticOracle oracle) throws SemanticException {
		if (left.isBottom() || right.isBottom()) return bottom();

		IntInterval a = left.interval;
		IntInterval b = right.interval;

		if (operator instanceof AdditionOperator) {
			return new Intervals(a.getLow().add(b.getLow()), a.getHigh().add(b.getHigh()));
		} else if (operator instanceof SubtractionOperator) {
			return new Intervals(a.getLow().subtract(b.getHigh()), a.getHigh().subtract(b.getLow()));
		} else if (operator instanceof MultiplicationOperator) {
			MathNumber lA = a.getLow(), lB = b.getLow();
			MathNumber uA = a.getHigh(), uB = b.getHigh();
			MathNumber ll = lA.multiply(lB), lu = lA.multiply(uB);
			MathNumber ul = uA.multiply(lB), uu = uA.multiply(uB);
			MathNumber min = ll.min(lu).min(ul).min(uu);
			MathNumber max = ll.max(lu).max(ul).max(uu);
			return new Intervals(min, max);
		} else if (operator instanceof DivisionOperator) {
			if (b.getLow().compareTo(MathNumber.ZERO) <= 0 && b.getHigh().compareTo(MathNumber.ZERO) >= 0) {
				return top();
			}
			MathNumber lA = a.getLow(), lB = b.getLow();
			MathNumber uA = a.getHigh(), uB = b.getHigh();
			MathNumber d1 = lA.divide(lB), d2 = lA.divide(uB);
			MathNumber d3 = uA.divide(lB), d4 = uA.divide(uB);
			MathNumber min = d1.min(d2).min(d3).min(d4);
			MathNumber max = d1.max(d2).max(d3).max(d4);
			return new Intervals(min, max);
		}
		return top();
	}

	@Override
	public int hashCode() { return Objects.hash(interval); }

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null || getClass() != obj.getClass()) return false;
		Intervals other = (Intervals) obj;
		return Objects.equals(interval, other.interval);
	}

	@Override
	public Intervals wideningAux(Intervals other) throws SemanticException {
		MathNumber newLower = (other.interval.getLow().compareTo(interval.getLow()) < 0) ? MathNumber.MINUS_INFINITY : interval.getLow();
		MathNumber newUpper = (other.interval.getHigh().compareTo(interval.getHigh()) > 0) ? MathNumber.PLUS_INFINITY : interval.getHigh();
		return newLower.isMinusInfinity() && newUpper.isPlusInfinity() ? top() : new Intervals(newLower, newUpper);
	}

	@Override
	public Intervals narrowingAux(Intervals other) throws SemanticException {
		MathNumber newHigh = interval.getHigh().isInfinite() ? other.interval.getHigh() : interval.getHigh();
		MathNumber newLow = interval.getLow().isInfinite() ? other.interval.getLow() : interval.getLow();
		return new Intervals(newLow, newHigh);
	}

	@Override
	public ValueEnvironment<Intervals> assumeBinaryExpression(ValueEnvironment<Intervals> environment,
															  BinaryOperator operator, ValueExpression left, ValueExpression right, ProgramPoint src, ProgramPoint dest,
															  SemanticOracle oracle) throws SemanticException {

		if (left instanceof Variable && right instanceof Constant) {
			Variable var = (Variable) left;
			Intervals varInt = environment.getState(var);
			Intervals constInt = evalNonNullConstant((Constant) right, src, oracle);

			if (operator instanceof ComparisonLt || operator instanceof ComparisonLe) {
				MathNumber newHigh = varInt.interval.getHigh().min(constInt.interval.getHigh());
				return environment.putState(var, new Intervals(varInt.interval.getLow(), newHigh));
			} else if (operator instanceof ComparisonGt || operator instanceof ComparisonGe) {
				MathNumber newLow = varInt.interval.getLow().max(constInt.interval.getLow());
				return environment.putState(var, new Intervals(newLow, varInt.interval.getHigh()));
			}
		}
		return BaseNonRelationalValueDomain.super.assumeBinaryExpression(environment, operator, left, right, src, dest, oracle);
	}
}