package it.unive.scsr.checkers.overflow.checkers;

import it.unive.scsr.checkers.OverflowChecker;

public final class UInt32 extends SizeChecker {
    public UInt32(OverflowChecker.NumericalSize size) {
        super(size);
    }

    @Override
    public Long minLimit() {
        return 0L;
    }

    @Override
    public Long maxLimit() {
        return 2147483647L;
    }
}
