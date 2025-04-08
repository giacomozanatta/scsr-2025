package it.unive.scsr.parity;

import it.unive.scsr.Parity;
import it.unive.scsr.utils.BiFunctionDispatcher;
import java.util.function.BiFunction;

public class BinaryFunctions extends BiFunctionDispatcher<Parity> {

    public static final BinaryFunctions INSTANCE = new BinaryFunctions();

    @Override
    protected BiFunction<Parity, Parity, Parity> buildAdditionFunction() {
        return (x, y) -> x == y ? Parity.EVEN : Parity.ODD;
    }

    @Override
    protected BiFunction<Parity, Parity, Parity> buildSubtractionFunction() {
        return (x, y) -> x == y ? Parity.EVEN : Parity.ODD;
    }

    @Override
    protected BiFunction<Parity, Parity, Parity> buildMultiplicationFunction() {
        return (x, y) -> x == y && x == Parity.ODD ? x : Parity.EVEN;
    }

    @Override
    protected BiFunction<Parity, Parity, Parity> buildDivisionFunction() {
        return (x, y) -> Parity.TOP;
    }
}
