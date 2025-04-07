package it.unive.scsr.parity;

import it.unive.scsr.Parity;
import it.unive.scsr.utils.BiFunctionDispatcher;

import java.util.Optional;
import java.util.function.BiFunction;

public class BinaryFunctions extends BiFunctionDispatcher<Parity, Parity> {

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

    @Override
    protected Optional<Parity> inapplicable(Parity left, Parity right) {
        if (left.isTop() || right.isTop()) {
            // Whenever a top element is found, the calculation is stopped and then the bottom is returned. This must be
            // done because the top element can also express something that is not a Parity element, so any calculation
            // can actually be performed.
            return Optional.of(Parity.TOP);
        }

        // No condition was found to stop the binary calculation, so the "inapplicable" object cannot be returned.
        return Optional.empty();
    }
}
