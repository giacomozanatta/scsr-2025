package it.unive.scsr.checkers.overflow.checkers;

import it.unive.scsr.Intervals;
import it.unive.scsr.checkers.OverflowChecker;

public final class Int8 extends SizeChecker {
    public Int8(OverflowChecker.NumericalSize size) {
        super(size);
    }

    @Override
    public OverflowingLevel isOverflowing(Intervals intervals) {
        throw new RuntimeException();
    }

    @Override
    public Byte minLimit() {
        return Byte.MIN_VALUE;
    }

    @Override
    public Byte maxLimit() {
        return Byte.MAX_VALUE;
    }
}
