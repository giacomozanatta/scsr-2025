package it.unive.scsr.checkers.overflow.checkers;

import it.unive.scsr.checkers.OverflowChecker;

public final class UInt8 extends SizeChecker {
    public UInt8(OverflowChecker.NumericalSize size) {
        super(size);
    }

    @Override
    public Short minLimit() {
        return 0;
    }

    @Override
    public Short maxLimit() {
        return 255;
    }
}
