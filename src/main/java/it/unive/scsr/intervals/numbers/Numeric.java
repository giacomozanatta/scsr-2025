package it.unive.scsr.intervals.numbers;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.Objects;
import java.util.function.BiFunction;

public sealed abstract class Numeric<T> implements SigNum permits IntegerNumber, DecimalNumber {

    private enum Operation {
        ADD, SUBTRACT, MULTIPLY, DIVIDE
    }

    private final static int SCALE = 32;
    private final static RoundingMode ROUNDING_MODE = RoundingMode.HALF_DOWN;

    protected final Number number;

    public static final BigDecimal ZERO = new BigDecimal(0).setScale(SCALE, ROUNDING_MODE);

    protected Numeric(Number number) {
        this.number = number;
    }

    @SuppressWarnings("unchecked")
    public T value() {
        return (T) number;
    }

    public BigDecimal toDecimal() {
        return new BigDecimal(String.valueOf(number)).setScale(SCALE, ROUNDING_MODE);
    }

    @Override
    public IntervalNumber add(IntervalNumber other, Computation orElse) {
        return finalize(switch (other) {
            case NaN nan -> nan;
            case Infinity infinity -> infinity;
            case Numeric<?> numeric -> bestApproximation(numeric, Operation.ADD);
        }, other, orElse);
    }

    @Override
    public IntervalNumber subtract(IntervalNumber other, Computation orElse) {
        return finalize(switch (other) {
            case NaN nan -> nan;
            case Infinity infinity -> infinity.negate();
            case Numeric<?> numeric -> bestApproximation(numeric, Operation.SUBTRACT);
        }, other, orElse);
    }

    @Override
    public IntervalNumber multiply(IntervalNumber other, Computation orElse) {
        return finalize(switch (other) {
            case NaN nan -> nan;
            case Infinity infinity -> infinity.multiply(this);
            case Numeric<?> numeric -> bestApproximation(numeric, Operation.MULTIPLY);
        }, other, orElse);
    }

    // TODO: handle by respecting limit definitions.
    @Override
    public IntervalNumber divide(IntervalNumber other, Computation orElse) {
        return finalize(switch (other) {
            case NaN nan -> nan;
            case Infinity ignored -> new IntegerNumber(BigInteger.ZERO);
            case Numeric<?> numeric -> numeric.isZero() ? NaN.INSTANCE : bestApproximation(numeric, Operation.DIVIDE);
        }, other, orElse);
    }

    @Override
    public SigNum negate() {
        return bestApproximation(new IntegerNumber(BigInteger.ONE.negate()), Operation.MULTIPLY);
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Numeric<?> that && Objects.equals(toDecimal(), that.toDecimal());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(toDecimal());
    }

    @Override
    public String toString() {
        return "Numeric{" + "number=" + number + '}';
    }

    private IntervalNumber finalize(IntervalNumber result, IntervalNumber other, Computation orElse) {
        return (result.isNaN() && orElse != null) ? orElse.perform(this, other) : result;
    }

    private Numeric<?> bestApproximation(Numeric<?> otherNumeric, Operation operation) {
        // Define a BiFunction to perform the given operation on two Numeric values, converting them to DecimalNumber
        // first to ensure precision.
        BiFunction<Numeric<?>, Numeric<?>, DecimalNumber> decimalApproximation = (first, second) -> {
            var decimalOperation = numericMethod(BigDecimal.class, operation);
            return new DecimalNumber(decimalOperation.apply(first.toDecimal(), second.toDecimal()));
        };

        // Use a switch expression based on the type of 'otherNumeric' to determine the best approximation.
        return switch (otherNumeric) {
            case DecimalNumber ignored -> decimalApproximation.apply(this, otherNumeric);
            case IntegerNumber otherInteger -> switch (this) {
                case DecimalNumber ignored -> decimalApproximation.apply(this, otherNumeric);
                case IntegerNumber thisInteger ->
                        new IntegerNumber(numericMethod(BigInteger.class, operation).apply(thisInteger.value(), otherInteger.value()));

            };
        };
    }

    @SuppressWarnings("unchecked")
    private static <T> BiFunction<T, T, T> numericMethod(Class<? extends T> type, Operation operation) {
        try {
            // Convert the operation enum name to lowercase to match the method naming convention. Then, get the
            // specific arithmetic method from the 'type' class that corresponds to the given operation.
            var operationName = operation.name().toLowerCase();
            var method = type.getMethod(operationName, type);

            return (first, second) -> {
                try {
                    // Invoke the retrieved method on the 'first' object, passing 'second' as the argument.
                    return (T) method.invoke(first, second);
                } catch (Exception e) {
                    throw new IllegalArgumentException(e);
                }
            };
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }
}
