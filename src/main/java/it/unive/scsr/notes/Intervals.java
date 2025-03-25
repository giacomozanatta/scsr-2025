package it.unive.scsr.notes;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.nonrelational.value.BaseNonRelationalValueDomain;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.util.numeric.IntInterval;
import it.unive.lisa.util.numeric.MathNumber;
import it.unive.lisa.util.numeric.MathNumberConversionException;
import it.unive.lisa.util.representation.StringRepresentation;
import it.unive.lisa.util.representation.StructuredRepresentation;

public class Intervals implements BaseNonRelationalValueDomain<Intervals> {

    public static final Intervals BOTTOM = new Intervals(1, -1);
    public static final Intervals TOP = new Intervals(Integer.MIN_VALUE, Integer.MAX_VALUE); // Don't know if it's right though

    IntInterval interval;

    public Intervals(int lower, int upper) {
        interval = new IntInterval(lower, upper);
    }

    @Override
    public Intervals lubAux(Intervals other) throws SemanticException {
        IntInterval a = this.interval;
        IntInterval b = other.interval;

        MathNumber lA = a.getLow();
        MathNumber lB = b.getLow();

        MathNumber uA = a.getHigh();
        MathNumber uB = b.getHigh();

        if (lA.compareTo(uA) > 0) {
            // The lower bound is greater than the upper bound
            return BOTTOM;
        }
        if (lB.compareTo(uB) > 0) {
            // The lower bound is greater than the upper bound
            return BOTTOM;
        }

        MathNumber newLower = lA.min(lB);
        MathNumber newUpper = lA.max(lB);

        try {
            return new Intervals(newLower.toInt(), newUpper.toInt());
        } catch (MathNumberConversionException e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    public boolean lessOrEqualAux(Intervals other) throws SemanticException {
        // This interval is less than the other interval if it is contained
        // inside other. For example, [-1, 0] is included in [-5, 5], thus it is a lower value
        // in the partial order.
        IntInterval a = this.interval;
        IntInterval b = other.interval;

        return b.includes(a);
    }

    @Override
    public Intervals evalNonNullConstant(Constant constant, ProgramPoint pp, SemanticOracle oracle) throws SemanticException {
        if (constant.getValue() instanceof Integer value) {
            return new Intervals(value.intValue(), value.intValue());
        }

        // We return TOP because a different type than integer is not an error, it
        // is simply not controlled by the analyzer. We use BOTTOM only for errors.
        return TOP;
    }

    @Override
    public Intervals top() {
        return TOP;
    }

    @Override
    public Intervals bottom() {
        return BOTTOM;
    }

    @Override
    public StructuredRepresentation representation() {
        return new StringRepresentation("[" + interval.getLow() + ", " + interval.getHigh() + "]");
    }
}

