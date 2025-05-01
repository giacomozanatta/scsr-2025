package it.unive.scsr.checkers.overflow.checkers;

import it.unive.scsr.checkers.OverflowChecker;
import it.unive.scsr.intervals.numbers.IntervalNumber;
import it.unive.scsr.intervals.numbers.Numeric;

public final class UInt16 extends SizeChecker {
    public UInt16(OverflowChecker.NumericalSize size) {
        super(size);
    }

    @Override
    public Numeric<?> minLimit() {
        return IntervalNumber.ofPrimitiveOrThrow(0);
    }

    @Override
    public Numeric<?> maxLimit() {
        return IntervalNumber.ofPrimitiveOrThrow(32767);
    }
}
