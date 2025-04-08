package it.unive.scsr.intervals;

import it.unive.lisa.util.numeric.MathNumber;
import it.unive.scsr.Intervals;
import it.unive.scsr.utils.BiFunctionDispatcher;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;

public class BinaryFunctions extends BiFunctionDispatcher<Intervals> {

    // Record to group the extremes involved in the binary operation between intervals.
    private record Group(Intervals left, Intervals right) {
        MathNumber ll() { return left.interval.getLow(); }
        MathNumber hl() { return left.interval.getHigh(); }
        MathNumber lr() { return right.interval.getLow(); }
        MathNumber hr() { return right.interval.getHigh(); }
    }

    public static final BinaryFunctions INSTANCE = new BinaryFunctions();

    @Override
    protected BiFunction<Intervals, Intervals, Intervals> buildAdditionFunction() {
        return (left, right) -> {
            // Perform addition between two intervals. The order is somewhat preserved so that the smallest element on
            // the left is added to the smallest element on the right to get the smallest number. The same reasoning
            // applies to get the largest number.
            var group = new Group(left, right);
            return new Intervals(group.ll().add(group.lr()), group.hl().add(group.hr()));
        };
    }

    @Override
    protected BiFunction<Intervals, Intervals, Intervals> buildSubtractionFunction() {
        // Perform subtraction between two intervals. The order here is not "linearly" preserved in the sense that to
        // get the smallest element, the lower element of the left interval is related to the larger element of the
        // right interval. For the largest element the reasoning is similar, that is, the largest element of the left
        // interval is related to the smallest element of the right interval.
        return (left, right) -> {
            var group = new Group(left, right);
            return new Intervals(group.ll().subtract(group.hr()), group.hl().subtract(group.lr()));
        };
    }

    @Override
    protected BiFunction<Intervals, Intervals, Intervals> buildMultiplicationFunction() {
        return (left, right) -> {
            // When multiplying MathNumber, zero and infinity cannot be multiplied together. Here, instead of returning
            // the closest approximation, zero is returned.
            BiFunction<MathNumber, MathNumber, MathNumber> multiply = (x, y) ->
                    Optional.of(x.multiply(y))
                            .filter(number -> !number.isNaN())
                            .orElse(MathNumber.ZERO);

            // All possible combinations are calculated to obtain the minimum number for the smallest element and the
            // maximum number for the largest element.
            var group = new Group(left, right);
            var multiplications =
                    List.of(multiply.apply(group.ll(), group.lr()),
                            multiply.apply(group.ll(), group.hr()),
                            multiply.apply(group.hl(), group.lr()),
                            multiply.apply(group.hl(), group.hr()));

            // Returns the most accurate approximation for the multiplication operation between two ranges.
            return new Intervals(min(multiplications), max(multiplications));
        };
    }

    @Override
    protected BiFunction<Intervals, Intervals, Intervals> buildDivisionFunction() {
        return (left, right) -> {
            // When the correct interval is a singleton and contains zero, it is known for sure that the division is by
            // zero, that is the bottom element can be returned.
            if (right.interval.is(0)) return Intervals.BOTTOM;

            // When the division returns to the NaN element the following approach is adopted:
            //  - When the divisor can be zero, a NaN element is returned since the division cannot be calculated
            //  - When both operands are infinite, instead of returning a NaN element, the sign operation is applied
            BiFunction<MathNumber, MathNumber, MathNumber> canDivide = (x, y) ->
                    Optional.of(x.divide(y))
                            .filter(number -> !number.isNaN())
                            .orElseGet(() -> {
                                if (y.isZero()) return MathNumber.NaN;
                                if (x.isPositive() == y.isPositive()) return MathNumber.PLUS_INFINITY;
                                return MathNumber.MINUS_INFINITY;
                            });

            // All possible combinations are calculated to obtain the minimum number for the smallest element and the
            // maximum number for the largest element.
            var group = new Group(left, right);
            var divisions =
                    List.of(canDivide.apply(group.ll(), group.lr()),
                            canDivide.apply(group.ll(), group.hr()),
                            canDivide.apply(group.hl(), group.lr()),
                            canDivide.apply(group.hl(), group.hr()));

            // Returns the most accurate approximation for the division operation between two ranges.
            return new Intervals(min(divisions), max(divisions));
        };
    }

    @Override
    protected Intervals polishResult(Intervals output) {
        // ...
        if (output.isBottom() || output.isTop()) return output;

        // ...
        var low = output.interval.getLow();
        var high = output.interval.getHigh();
        if (low.isNaN() || high.isNaN()) return Intervals.BOTTOM;
        return output;
    }

    // Selects the minimum element from the given numbers. If the minimum element is NaN, instead of considering it a
    // computational error, the largest over-approximation for the minimum element is returned.
    private MathNumber min(Collection<MathNumber> numbers) {
        return numbers
                .stream()
                .min(MathNumber::compareTo)
                .filter(mathNumber -> !mathNumber.isNaN())
                .orElse(MathNumber.MINUS_INFINITY);
    }

    // Selects the maximum element from the given numbers. If the maximum element is NaN, instead of considering it a
    // computational error, the largest over-approximation for the maximum element is returned.
    private MathNumber max(Collection<MathNumber> numbers) {
        return numbers
                .stream()
                .max(MathNumber::compareTo)
                .filter(mathNumber -> !mathNumber.isNaN())
                .orElse(MathNumber.PLUS_INFINITY);
    }
}
