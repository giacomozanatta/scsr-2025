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
import it.unive.lisa.symbolic.value.operator.AdditionOperator;
import it.unive.lisa.symbolic.value.operator.MultiplicationOperator;
import it.unive.lisa.symbolic.value.operator.NegatableOperator;
import it.unive.lisa.symbolic.value.operator.SubtractionOperator;
import it.unive.lisa.symbolic.value.operator.binary.BinaryOperator;
import it.unive.lisa.symbolic.value.operator.unary.UnaryOperator;
import it.unive.lisa.util.numeric.IntInterval;
import it.unive.lisa.util.numeric.MathNumber;
import it.unive.lisa.util.representation.StringRepresentation;
import it.unive.lisa.util.representation.StructuredRepresentation;

public class Intervals
		implements BaseNonRelationalValueDomain<Intervals>, Comparable<Intervals> {

	public final IntInterval interval;
	private final boolean isFloatInterval;

	public static final Intervals ZERO = new Intervals(IntInterval.ZERO);
	public static final Intervals FLOAT_ZERO = new Intervals(new IntInterval(MathNumber.ZERO, MathNumber.ZERO), true);
	public static final Intervals TOP = new Intervals(IntInterval.INFINITY);
	public static final Intervals FLOAT_TOP = new Intervals(IntInterval.INFINITY, true);
	public static final Intervals BOTTOM = new Intervals(null, false);

	public Intervals(IntInterval interval) {
		this(interval, false);
	}

	public Intervals(IntInterval interval, boolean isFloat) {
		this.interval = interval;
		this.isFloatInterval = isFloat;
	}

	public Intervals(MathNumber lower, MathNumber upper) {
		this(new IntInterval(lower, upper), false);
	}

	public Intervals(MathNumber lower, MathNumber upper, boolean isFloat) {
		this(new IntInterval(lower, upper), isFloat);
	}

	public Intervals(int low, int high) {
		this(new IntInterval(low, high), false);
	}

	public Intervals(double low, double high) {
		this(new IntInterval(new MathNumber(low), new MathNumber(high)), true);
	}

	public Intervals() {
		this(IntInterval.INFINITY, false);
	}

	public Number getLow() {
		if (interval == null) return null;
		try {
			// Always use toDouble() which doesn't throw for infinite values
			return interval.getLow().toDouble();
		} catch (Exception e) {
			// If toDouble() fails, return null or handle appropriately
			return null;
		}
	}

	public Number getHigh() {
		if (interval == null) return null;
		try {
			// Always use toDouble() which doesn't throw for infinite values
			return interval.getHigh().toDouble();
		} catch (Exception e) {
			// If toDouble() fails, return null or handle appropriately
			return null;
		}
	}

	public boolean isFloat() {
		return isFloatInterval;
	}

	@Override
	public Intervals evalUnaryExpression(UnaryOperator operator, Intervals arg, ProgramPoint pp, SemanticOracle oracle)
			throws SemanticException {

		if (arg.isBottom()) {
			return bottom();
		}

		if (operator instanceof NegatableOperator) {
			MathNumber low = arg.interval.getLow();
			MathNumber high = arg.interval.getHigh();

			MathNumber newLow = high.multiply(new MathNumber(-1));
			MathNumber newHigh = low.multiply(new MathNumber(-1));

			return new Intervals(newLow, newHigh, arg.isFloatInterval);
		}

		return arg.isFloatInterval ? FLOAT_TOP : TOP;
	}

	@Override
	public Intervals glbAux(Intervals other) throws SemanticException {

		if (this.isBottom() || other.isBottom()) {
			return bottom();
		}

		boolean resultIsFloat = this.isFloatInterval || other.isFloatInterval;

		IntInterval a = this.interval;
		IntInterval b = other.interval;

		MathNumber lA = a.getLow();
		MathNumber lB = b.getLow();

		MathNumber uA = a.getHigh();
		MathNumber uB = b.getHigh();

		if (lA.compareTo(uA) > 0 || lB.compareTo(uB) > 0)
			return BOTTOM;

		MathNumber newLower = lA.max(lB);
		MathNumber newUpper = uA.min(uB);

		Intervals newInterval = new Intervals(newLower, newUpper, resultIsFloat);

		return newLower.isMinusInfinity() && newUpper.isPlusInfinity() ?
				(resultIsFloat ? FLOAT_TOP : TOP) : newInterval;
	}

	@Override
	public Intervals lubAux(Intervals other) throws SemanticException {

		if (this.isBottom()) {
			return other;
		}
		if (other.isBottom()) {
			return this;
		}

		boolean resultIsFloat = this.isFloatInterval || other.isFloatInterval;

		IntInterval a = this.interval;
		IntInterval b = other.interval;

		MathNumber lA = a.getLow();
		MathNumber lB = b.getLow();

		MathNumber uA = a.getHigh();
		MathNumber uB = b.getHigh();

		MathNumber newLower = lA.min(lB);
		MathNumber newUpper = uA.max(uB);

		if (lA.compareTo(uA) > 0 || lB.compareTo(uB) > 0)
			return BOTTOM;

		Intervals newInterval = new Intervals(newLower, newUpper, resultIsFloat);
		return newLower.isMinusInfinity() && newUpper.isPlusInfinity() ?
				(resultIsFloat ? FLOAT_TOP : TOP) : newInterval;
	}

	@Override
	public boolean lessOrEqualAux(Intervals other) throws SemanticException {

		return other.interval.includes(this.interval);
	}

	@Override
	public Intervals top() {
		return isFloatInterval ? FLOAT_TOP : TOP;
	}

	@Override
	public boolean isTop() {
		return interval != null && interval.isInfinity();
	}

	@Override
	public Intervals bottom() {
		return BOTTOM;
	}

	@Override
	public boolean isBottom() {
		return interval == null;
	}

	@Override
	public StructuredRepresentation representation() {
		if (this.isBottom())
			return Lattice.bottomRepresentation();

		String type = isFloatInterval ? "(float)" : "(int)";
		return new StringRepresentation(type + "["+this.interval.getLow()+","+this.interval.getHigh()+"]");
	}

	@Override
	public int compareTo(Intervals o) {
		if (isBottom())
			return o.isBottom() ? 0 : -1;
		if (isTop())
			return o.isTop() ? 0 : 1;

		if (o.isBottom())
			return 1;

		if (o.isTop())
			return -1;

		return interval.compareTo(o.interval);
	}

	@Override
	public Intervals evalNonNullConstant(Constant constant, ProgramPoint pp, SemanticOracle oracle)
			throws SemanticException {
		Object value = constant.getValue();

		if (value instanceof Integer) {
			Integer i = (Integer) value;
			return new Intervals(i, i);
		} else if (value instanceof Float) {
			Float f = (Float) value;
			MathNumber num = new MathNumber(f.doubleValue());
			return new Intervals(num, num, true);
		} else if (value instanceof Double) {
			Double d = (Double) value;
			MathNumber num = new MathNumber(d);
			return new Intervals(num, num, true);
		} else if (value instanceof Long) {
			Long l = (Long) value;
			return new Intervals(l.intValue(), l.intValue());
		} else if (value instanceof Short) {
			Short s = (Short) value;
			return new Intervals(s.intValue(), s.intValue());
		} else if (value instanceof Byte) {
			Byte b = (Byte) value;
			return new Intervals(b.intValue(), b.intValue());
		}

		return top();
	}

	@Override
	public Intervals evalBinaryExpression(BinaryOperator operator, Intervals left, Intervals right, ProgramPoint pp,
										  SemanticOracle oracle) throws SemanticException {

		if (left.isBottom() || right.isBottom())
			return bottom();

		boolean resultIsFloat = left.isFloatInterval || right.isFloatInterval;

		IntInterval a = left.interval;
		IntInterval b = right.interval;

		if (operator instanceof AdditionOperator) {

			MathNumber lA = a.getLow();
			MathNumber lB = b.getLow();

			MathNumber uA = a.getHigh();
			MathNumber uB = b.getHigh();

			return new Intervals(lA.add(lB), uA.add(uB), resultIsFloat);

		} else if (operator instanceof SubtractionOperator) {

			MathNumber lA = a.getLow();
			MathNumber lB = b.getLow();

			MathNumber uA = a.getHigh();
			MathNumber uB = b.getHigh();

			return new Intervals(lA.subtract(uB), uA.subtract(lB), resultIsFloat);

		} else if (operator instanceof MultiplicationOperator) {

			MathNumber lA = a.getLow();
			MathNumber lB = b.getLow();

			MathNumber uA = a.getHigh();
			MathNumber uB = b.getHigh();

			MathNumber p1 = lA.multiply(lB);
			MathNumber p2 = lA.multiply(uB);
			MathNumber p3 = uA.multiply(lB);
			MathNumber p4 = uA.multiply(uB);

			MathNumber min = p1;
			if (p2.compareTo(min) < 0) min = p2;
			if (p3.compareTo(min) < 0) min = p3;
			if (p4.compareTo(min) < 0) min = p4;

			MathNumber max = p1;
			if (p2.compareTo(max) > 0) max = p2;
			if (p3.compareTo(max) > 0) max = p3;
			if (p4.compareTo(max) > 0) max = p4;

			return new Intervals(min, max, resultIsFloat);
		}

		return resultIsFloat ? FLOAT_TOP : TOP;
	}

	@Override
	public int hashCode() {
		return Objects.hash(interval, isFloatInterval);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Intervals other = (Intervals) obj;
		return Objects.equals(interval, other.interval) &&
				isFloatInterval == other.isFloatInterval;
	}

	@Override
	public Intervals wideningAux(Intervals other) throws SemanticException {
		if (this.isBottom()) {
			return other;
		}
		if (other.isBottom()) {
			return this;
		}

		boolean resultIsFloat = this.isFloatInterval || other.isFloatInterval;

		MathNumber newLower, newUpper;
		if (other.interval.getHigh().compareTo(interval.getHigh()) > 0)
			newUpper = MathNumber.PLUS_INFINITY;
		else
			newUpper = interval.getHigh();

		if (other.interval.getLow().compareTo(interval.getLow()) < 0)
			newLower = MathNumber.MINUS_INFINITY;
		else
			newLower = interval.getLow();

		Intervals result = new Intervals(newLower, newUpper, resultIsFloat);
		return newLower.isMinusInfinity() && newUpper.isPlusInfinity() ?
				(resultIsFloat ? FLOAT_TOP : TOP) : result;
	}

	@Override
	public Intervals narrowingAux(Intervals other) throws SemanticException {
		if (this.isBottom() || other.isBottom()) {
			return bottom();
		}

		boolean resultIsFloat = this.isFloatInterval || other.isFloatInterval;

		MathNumber newLow = interval.getLow().isInfinite() ? other.interval.getLow() : interval.getLow();
		MathNumber newHigh = interval.getHigh().isInfinite() ? other.interval.getHigh() : interval.getHigh();
		return new Intervals(newLow, newHigh, resultIsFloat);
	}

	@Override
	public ValueEnvironment<Intervals> assumeBinaryExpression(ValueEnvironment<Intervals> environment,
															  BinaryOperator operator, ValueExpression left, ValueExpression right, ProgramPoint src, ProgramPoint dest,
															  SemanticOracle oracle) throws SemanticException {

		return BaseNonRelationalValueDomain.super.assumeBinaryExpression(environment, operator, left, right, src, dest, oracle);
	}
}