package it.unive.scsr;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
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
import it.unive.lisa.symbolic.value.operator.DivisionOperator;
import it.unive.lisa.symbolic.value.operator.MultiplicationOperator;
import it.unive.lisa.symbolic.value.operator.SubtractionOperator;
import it.unive.lisa.symbolic.value.operator.binary.BinaryOperator;
import it.unive.lisa.symbolic.value.operator.unary.NumericNegation;
import it.unive.lisa.symbolic.value.operator.unary.UnaryOperator;
import it.unive.lisa.util.numeric.MathNumber;
import it.unive.lisa.util.representation.StringRepresentation;
import it.unive.lisa.util.representation.StructuredRepresentation;

public class Intervals
		implements
		BaseNonRelationalValueDomain<Intervals>, Comparable<Intervals> {

	public final DoubleInterval interval;

	public static final Intervals ZERO   = new Intervals(DoubleInterval.ZERO);
	public static final Intervals TOP    = new Intervals(DoubleInterval.INFINITY);
	public static final Intervals BOTTOM = new Intervals(null);

	public Intervals(DoubleInterval interval) { this.interval = interval; }
	public Intervals(MathNumber lower, MathNumber upper) { this(new DoubleInterval(lower, upper)); }
	public Intervals(int low, int high) { this(new DoubleInterval(low, high)); }
	public Intervals() { this(DoubleInterval.INFINITY); }

	@Override
	public Intervals evalUnaryExpression(UnaryOperator operator, Intervals arg,
										 ProgramPoint pp, SemanticOracle oracle) throws SemanticException {
		if (operator instanceof NumericNegation) {
			if (arg.isBottom()) return bottom();
			MathNumber lo = arg.interval.getLow();
			MathNumber hi = arg.interval.getHigh();
			return new Intervals(MathNumber.ZERO.subtract(hi), MathNumber.ZERO.subtract(lo));
		}
		return top();
	}

	@Override
	public Intervals glbAux(Intervals other) throws SemanticException {
		MathNumber lA = interval.getLow(),       lB = other.interval.getLow();
		MathNumber uA = interval.getHigh(),      uB = other.interval.getHigh();
		if (lA.compareTo(uA) > 0 || lB.compareTo(uB) > 0) return BOTTOM;
		MathNumber newLo = lA.max(lB), newHi = uA.min(uB);
		if (newLo.compareTo(newHi) > 0) return BOTTOM;
		return newLo.isMinusInfinity() && newHi.isPlusInfinity() ? top() : new Intervals(newLo, newHi);
	}

	@Override
	public Intervals lubAux(Intervals other) throws SemanticException {
		MathNumber lA = interval.getLow(),       lB = other.interval.getLow();
		MathNumber uA = interval.getHigh(),      uB = other.interval.getHigh();
		if (lA.compareTo(uA) > 0 || lB.compareTo(uB) > 0) return BOTTOM;
		MathNumber newLo = lA.min(lB), newHi = uA.max(uB);
		return newLo.isMinusInfinity() && newHi.isPlusInfinity() ? top() : new Intervals(newLo, newHi);
	}

	@Override public boolean lessOrEqualAux(Intervals other) throws SemanticException { return other.interval.includes(this.interval); }
	@Override public Intervals top()    { return TOP; }
	@Override public boolean isTop()    { return interval != null && interval.isInfinity(); }
	@Override public Intervals bottom() { return BOTTOM; }
	@Override public boolean isBottom() { return interval == null; }

	@Override
	public StructuredRepresentation representation() {
		if (isBottom()) return Lattice.bottomRepresentation();
		return new StringRepresentation("[" + interval.getLow() + "," + interval.getHigh() + "]");
	}

	@Override
	public int compareTo(Intervals o) {
		if (isBottom())   return o.isBottom() ? 0 : -1;
		if (isTop())      return o.isTop()    ? 0 :  1;
		if (o.isBottom()) return  1;
		if (o.isTop())    return -1;
		return interval.compareTo(o.interval);
	}

	@Override
	public Intervals evalNonNullConstant(Constant constant, ProgramPoint pp, SemanticOracle oracle)
			throws SemanticException {
		if (constant.getValue() instanceof Number n) {
			MathNumber v = new MathNumber(new BigDecimal(n.toString()));
			return new Intervals(v, v);
		}
		return top();
	}

	@Override
	public Intervals evalBinaryExpression(BinaryOperator operator, Intervals left, Intervals right,
										  ProgramPoint pp, SemanticOracle oracle) throws SemanticException {
		if (left.isBottom() || right.isBottom()) return bottom();
		MathNumber lA = left.interval.getLow(),  uA = left.interval.getHigh();
		MathNumber lB = right.interval.getLow(), uB = right.interval.getHigh();

		if (operator instanceof AdditionOperator)
			return new Intervals(lA.add(lB), uA.add(uB));

		if (operator instanceof SubtractionOperator)
			return new Intervals(lA.subtract(uB), uA.subtract(lB));

		if (operator instanceof MultiplicationOperator)
			return evalMultiplication(left, right);

		if (operator instanceof DivisionOperator) {
			if (lB.leq(MathNumber.ZERO) && uB.geq(MathNumber.ZERO)) return bottom();
			return evalMultiplication(left, new Intervals(MathNumber.ONE.divide(uB), MathNumber.ONE.divide(lB)));
		}

		return top();
	}

	private Intervals evalMultiplication(Intervals left, Intervals right) {
		MathNumber lA = left.interval.getLow(),  uA = left.interval.getHigh();
		MathNumber lB = right.interval.getLow(), uB = right.interval.getHigh();
		List<MathNumber> p = new ArrayList<>(List.of(lA.multiply(lB), lA.multiply(uB), uA.multiply(lB), uA.multiply(uB)));
		p.sort(MathNumber::compareTo);
		return new Intervals(p.get(0), p.get(p.size() - 1));
	}

	@Override
	public Intervals wideningAux(Intervals other) throws SemanticException {
		MathNumber newLo = other.interval.getLow().compareTo(interval.getLow())   < 0 ? MathNumber.MINUS_INFINITY : interval.getLow();
		MathNumber newHi = other.interval.getHigh().compareTo(interval.getHigh()) > 0 ? MathNumber.PLUS_INFINITY  : interval.getHigh();
		return newLo.isMinusInfinity() && newHi.isPlusInfinity() ? top() : new Intervals(newLo, newHi);
	}

	@Override
	public Intervals narrowingAux(Intervals other) throws SemanticException {
		MathNumber newLo = interval.getLow().isInfinite()  ? other.interval.getLow()  : interval.getLow();
		MathNumber newHi = interval.getHigh().isInfinite() ? other.interval.getHigh() : interval.getHigh();
		return new Intervals(newLo, newHi);
	}

	@Override
	public ValueEnvironment<Intervals> assumeBinaryExpression(ValueEnvironment<Intervals> environment,
															  BinaryOperator operator, ValueExpression left, ValueExpression right,
															  ProgramPoint src, ProgramPoint dest, SemanticOracle oracle) throws SemanticException {
		return BaseNonRelationalValueDomain.super.assumeBinaryExpression(environment, operator, left, right, src, dest, oracle);
	}

	public MathNumber getLowerBound() { return interval == null ? null : interval.getLow(); }
	public MathNumber getUpperBound() { return interval == null ? null : interval.getHigh(); }

	@Override public int hashCode() { return Objects.hash(interval); }
	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null || getClass() != obj.getClass()) return false;
		return Objects.equals(interval, ((Intervals) obj).interval);
	}
}