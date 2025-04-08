package it.unive.scsr.taintthreelevels;

import it.unive.scsr.TaintThreeLevels;
import it.unive.scsr.utils.BiFunctionDispatcher;

import java.util.function.BiFunction;

public class BinaryFunctions extends BiFunctionDispatcher<TaintThreeLevels> {

    public static final BinaryFunctions INSTANCE = new BinaryFunctions();

    @Override
    protected BiFunction<TaintThreeLevels, TaintThreeLevels, TaintThreeLevels> buildAdditionFunction() {
        return (left, right) -> left == right ? left : TaintThreeLevels.TOP;
    }

    @Override
    protected BiFunction<TaintThreeLevels, TaintThreeLevels, TaintThreeLevels> buildSubtractionFunction() {
        return (left, right) -> left == right ? left : TaintThreeLevels.TOP;
    }

    @Override
    protected BiFunction<TaintThreeLevels, TaintThreeLevels, TaintThreeLevels> buildMultiplicationFunction() {
        return (left, right) -> left == right ? left : TaintThreeLevels.TOP;
    }

    @Override
    protected BiFunction<TaintThreeLevels, TaintThreeLevels, TaintThreeLevels> buildDivisionFunction() {
        return (left, right) -> left == right ? left : TaintThreeLevels.TOP;
    }
}
