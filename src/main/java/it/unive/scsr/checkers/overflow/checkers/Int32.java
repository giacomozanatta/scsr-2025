package it.unive.scsr.checkers.overflow.checkers;

import it.unive.scsr.Intervals;
import it.unive.scsr.checkers.OverflowChecker;

public final class Int32 extends SizeChecker {
    public Int32(OverflowChecker.NumericalSize size) {
        super(size);
    }

    @Override
    public OverflowingLevel isOverflowing(Intervals intervals) {
        throw new RuntimeException();
    }

    @Override
    public Integer minLimit() {
        return Integer.MIN_VALUE;
    }

    @Override
    public Integer maxLimit() {
        return Integer.MAX_VALUE;
    }
}
