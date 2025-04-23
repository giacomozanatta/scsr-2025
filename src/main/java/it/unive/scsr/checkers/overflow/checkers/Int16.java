package it.unive.scsr.checkers.overflow.checkers;

import it.unive.scsr.checkers.OverflowChecker;

public final class Int16 extends SizeChecker {
    public Int16(OverflowChecker.NumericalSize size) {
        super(size);
    }

    @Override
    public Short minLimit() {
        return Short.MIN_VALUE;
    }

    @Override
    public Short maxLimit() {
        return Short.MAX_VALUE;
    }
}
