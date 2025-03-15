package it.unive.scsr.parity;

import it.unive.lisa.symbolic.value.operator.AdditionOperator;
import it.unive.lisa.symbolic.value.operator.DivisionOperator;
import it.unive.lisa.symbolic.value.operator.MultiplicationOperator;
import it.unive.lisa.symbolic.value.operator.SubtractionOperator;
import it.unive.lisa.symbolic.value.operator.binary.BinaryOperator;
import it.unive.scsr.Parity;
import java.util.Map;
import java.util.function.BiFunction;

public class BinaryFunctions {

    public static final BinaryFunctions INSTANCE = new BinaryFunctions();

    private final Map<Class<?>, BiFunction<Parity, Parity, Parity>> functionByClass;

    private BinaryFunctions() {
        functionByClass = Map.ofEntries(
                Map.entry(AdditionOperator.class, (x, y) -> x == y ? Parity.EVEN : Parity.ODD),
                Map.entry(SubtractionOperator.class, (x, y) -> x == y ? Parity.EVEN : Parity.ODD),
                Map.entry(MultiplicationOperator.class, (x, y) -> x == y && x == Parity.ODD ? x : Parity.EVEN),
                Map.entry(DivisionOperator.class, (x, y) -> Parity.TOP));
    }

    public BiFunction<Parity, Parity, Parity> findBy(BinaryOperator operator) {
        return functionByClass
                .keySet()
                .stream()
                .filter(k -> k.isAssignableFrom(operator.getClass()))
                .findFirst()
                .map(functionByClass::get)
                .orElse(null);
    }
}
