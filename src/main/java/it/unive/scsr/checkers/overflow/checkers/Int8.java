package it.unive.scsr.checkers.overflow.checkers;

import it.unive.scsr.checkers.OverflowChecker;
import it.unive.scsr.intervals.numbers.IntervalNumber;
import it.unive.scsr.intervals.numbers.Numeric;

public final class Int8 extends SizeChecker {
    public Int8(OverflowChecker.NumericalSize size) {
        super(size);
    }

    @Override
    public Numeric<?> minLimit() {
        return IntervalNumber.ofPrimitiveOrThrow(Byte.MIN_VALUE);
    }

    @Override
    public Numeric<?> maxLimit() {
        return IntervalNumber.ofPrimitiveOrThrow(Byte.MAX_VALUE);
    }
}
