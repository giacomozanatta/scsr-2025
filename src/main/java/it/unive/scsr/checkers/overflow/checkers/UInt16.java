package it.unive.scsr.checkers.overflow.checkers;

import it.unive.scsr.checkers.OverflowChecker;

public final class UInt16 extends SizeChecker {
    public UInt16(OverflowChecker.NumericalSize size) {
        super(size);
    }

    @Override
    public Integer minLimit() {
        return 0;
    }

    @Override
    public Integer maxLimit() {
        return 32767;
    }
}
