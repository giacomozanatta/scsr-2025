package it.unive.scsr;

import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.nonrelational.value.BaseNonRelationalValueDomain;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.symbolic.value.operator.*;
import it.unive.lisa.symbolic.value.operator.binary.BinaryOperator;
import it.unive.lisa.symbolic.value.operator.unary.NumericNegation;
import it.unive.lisa.symbolic.value.operator.unary.UnaryOperator;
import it.unive.lisa.util.numeric.IntInterval;
import it.unive.lisa.util.numeric.MathNumber;
import it.unive.lisa.util.numeric.MathNumberConversionException;
import it.unive.lisa.util.representation.StringRepresentation;
import it.unive.lisa.util.representation.StructuredRepresentation;

import java.math.BigDecimal;
import java.util.Objects;


public class HybridIntervals implements BaseNonRelationalValueDomain<HybridIntervals>, Comparable<HybridIntervals> {

    public final Object interval; // Either IntInterval or FloatInterval

    public static final HybridIntervals ZERO = new HybridIntervals(new IntInterval(0, 0));
    public static final HybridIntervals TOP = new HybridIntervals(IntInterval.INFINITY);
    public static final HybridIntervals BOTTOM = new HybridIntervals((IntInterval) null);


    public HybridIntervals(IntInterval interval) {
        this.interval = interval;
    }

    public HybridIntervals(FloatInterval interval) {
        this.interval = interval;
    }

    public HybridIntervals(MathNumber low, MathNumber high) {
        Object interval1;
        try {
            boolean isFloatLike = low.toDouble() % 1 != 0 || high.toDouble() % 1 != 0;
            interval1 = isFloatLike ? new FloatInterval(low, high) : new IntInterval(low, high);
        } catch (MathNumberConversionException e) {
            // fallback sicuro: considera l'intervallo come float
            interval1 = new FloatInterval(low, high);
        }
        this.interval = interval1;
    }


    public HybridIntervals() {
        this.interval = IntInterval.INFINITY;
    }

    private HybridIntervals(Object interval) {
        this.interval = interval;
    }

    private boolean isFloat() {
        return interval instanceof FloatInterval;
    }

    public MathNumber getLow() {
        return isBottom() ? null : isFloat() ? ((FloatInterval) interval).getLow() : ((IntInterval) interval).getLow();
    }

    public MathNumber getHigh() {
        return isBottom() ? null : isFloat() ? ((FloatInterval) interval).getHigh() : ((IntInterval) interval).getHigh();
    }

    @Override
    public HybridIntervals evalUnaryExpression(UnaryOperator operator, HybridIntervals arg, ProgramPoint pp, SemanticOracle oracle)
            throws SemanticException {

        if (arg.isBottom())
            return bottom();

        if (operator instanceof NegatableOperator || operator instanceof NumericNegation) {
            if (arg.interval instanceof IntInterval i) {
                IntInterval minusOne = new IntInterval(-1, -1);
                return new HybridIntervals(i.mul(minusOne));
            }

            if (arg.interval instanceof FloatInterval f) {
                FloatInterval minusOne = new FloatInterval(new MathNumber(new BigDecimal("-1.0")), new MathNumber(new BigDecimal("-1.0")));
                return new HybridIntervals(f.mul(minusOne));
            }
        }

        return top();
    }

    @Override
    public HybridIntervals evalNonNullConstant(Constant constant, ProgramPoint pp, SemanticOracle oracle) throws SemanticException {
        Object val = constant.getValue();
        if (val instanceof Integer i)
            return new HybridIntervals(new IntInterval(i, i));
        if (val instanceof Float f)
            return new HybridIntervals(new FloatInterval(f, f));
        return top();
    }

    @Override
    public HybridIntervals evalBinaryExpression(BinaryOperator operator, HybridIntervals left, HybridIntervals right, ProgramPoint pp, SemanticOracle oracle) throws SemanticException {
        if (left.isBottom() || right.isBottom())
            return bottom();

        MathNumber l1 = left.getLow(), u1 = left.getHigh();
        MathNumber l2 = right.getLow(), u2 = right.getHigh();

        boolean rightContainsZero = l2.leq(MathNumber.ZERO) &&
                u2.geq(MathNumber.ZERO);

        MathNumber[] res = switch (operator) {
            case AdditionOperator ignored -> new MathNumber[]{l1.add(l2), u1.add(u2)};
            case SubtractionOperator ignored -> new MathNumber[]{l1.subtract(u2), u1.subtract(l2)};
            case MultiplicationOperator ignored -> {
                MathNumber a = l1.multiply(l2), b = l1.multiply(u2), c = u1.multiply(l2), d = u1.multiply(u2);
                yield new MathNumber[]{a.min(b).min(c).min(d), a.max(b).max(c).max(d)};
            }
            case DivisionOperator ignored -> {
                // (1) caso pericoloso: il divisore include 0  -> top()
                if (rightContainsZero)
                    yield null;                        // verrà interpretato come top() più sotto

                // (2) calcolo sicuro: 0 non fa parte del divisore
                MathNumber[] cand = {
                        l1.divide(l2), l1.divide(u2),
                        u1.divide(l2), u1.divide(u2)
                };

                MathNumber min = null, max = null;
                for (MathNumber v : cand) {
                    if (v.isNaN())           // non dovrebbero comparire, ma per sicurezza
                        continue;
                    if (min == null || v.lt(min))
                        min = v;
                    if (max == null || v.gt(max))
                        max = v;
                }
                // se tutti NaN (caso molto raro qui) -> top()
                if (min == null || max == null)
                    yield null;

                yield new MathNumber[]{min, max};
            }

            default -> null;
        };
        if (res == null)
            return top();
        return new HybridIntervals(res[0], res[1]);
    }

    @Override public HybridIntervals glbAux(HybridIntervals other) throws SemanticException {
        MathNumber low = getLow().max(other.getLow());
        MathNumber high = getHigh().min(other.getHigh());
        return low.compareTo(high) > 0 ? bottom() : new HybridIntervals(low, high);
    }

    @Override public HybridIntervals lubAux(HybridIntervals other) throws SemanticException {
        MathNumber low = getLow().min(other.getLow());
        MathNumber high = getHigh().max(other.getHigh());
        return new HybridIntervals(low, high);
    }

    @Override public boolean lessOrEqualAux(HybridIntervals other) throws SemanticException {
        return getLow().compareTo(other.getLow()) >= 0 && getHigh().compareTo(other.getHigh()) <= 0;
    }

    @Override public HybridIntervals top() { return TOP; }
    @Override public boolean isTop() { return interval != null && getLow().isMinusInfinity() && getHigh().isPlusInfinity(); }
    @Override public HybridIntervals bottom() { return BOTTOM; }
    @Override public boolean isBottom() { return interval == null; }
    @Override public StructuredRepresentation representation() {
        return isBottom() ? Lattice.bottomRepresentation() : new StringRepresentation("[" + getLow() + "," + getHigh() + "]");
    }
    @Override public int compareTo(HybridIntervals o) {
        if (isBottom()) return o.isBottom() ? 0 : -1;
        if (isTop()) return o.isTop() ? 0 : 1;
        if (o.isBottom()) return 1;
        if (o.isTop()) return -1;
        int cmp = getLow().compareTo(o.getLow());
        return cmp != 0 ? cmp : getHigh().compareTo(o.getHigh());
    }
    @Override public int hashCode() { return Objects.hash(interval); }
    @Override public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        HybridIntervals other = (HybridIntervals) obj;
        return Objects.equals(interval, other.interval);
    }
    @Override public HybridIntervals wideningAux(HybridIntervals other) throws SemanticException {
        MathNumber low = other.getLow().compareTo(getLow()) < 0 ? MathNumber.MINUS_INFINITY : getLow();
        MathNumber high = other.getHigh().compareTo(getHigh()) > 0 ? MathNumber.PLUS_INFINITY : getHigh();
        return new HybridIntervals(low, high);
    }
    @Override public HybridIntervals narrowingAux(HybridIntervals other) throws SemanticException {
        MathNumber low = getLow().isInfinite() ? other.getLow() : getLow();
        MathNumber high = getHigh().isInfinite() ? other.getHigh() : getHigh();
        return new HybridIntervals(low, high);
    }
    @Override public ValueEnvironment<HybridIntervals> assumeBinaryExpression(ValueEnvironment<HybridIntervals> env, BinaryOperator op, ValueExpression l, ValueExpression r, ProgramPoint src, ProgramPoint dest, SemanticOracle oracle) throws SemanticException {
        return BaseNonRelationalValueDomain.super.assumeBinaryExpression(env, op, l, r, src, dest, oracle);
    }
}

