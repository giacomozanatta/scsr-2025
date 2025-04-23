package it.unive.scsr.checkers.overflow.checkers;

import it.unive.scsr.checkers.OverflowChecker;

public final class Int8 extends SizeChecker {
    public Int8(OverflowChecker.NumericalSize size) {
        super(size);
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
