package it.unive.scsr.checkers.overflow.checkers;

import it.unive.lisa.util.numeric.MathNumber;
import it.unive.scsr.Intervals;
import it.unive.scsr.checkers.OverflowChecker;

import java.util.Arrays;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import static it.unive.scsr.utils.Logging.defaultLogger;

public sealed abstract class SizeChecker permits Int16, Int32, Int8, UInt16, UInt32, UInt8 {

    /**
     * A record that represents whether a value has overflowed. If overflowed, it also specifies whether the value has
     * overflowed definitely or not.
     *
     * @param isOverflowing <code>true</code> if the value is overflowing, <code>false</code> otherwise.
     * @param definitely    <code>true</code> if the value is definitely overflowing, <code>false</code> otherwise.
     */
    public record OverflowingLevel(boolean isOverflowing, boolean definitely) {

        /**
         * @return The base record indicating that the analysis is unable to tell anything.
         */
        public static OverflowingLevel base() {
            return new OverflowingLevel(false, false);
        }
    }

    protected final OverflowChecker.NumericalSize size;

    protected SizeChecker(OverflowChecker.NumericalSize size) {
        this.size = size;
    }

    /**
     * Determines whether the value can cause an overflow or not.
     *
     * @param intervals The range that represents the lower and upper numeric limit for a program element.
     * @return The {@link OverflowingLevel} indicating whether the value could overflow and whether it will definitely
     * happen or not.
     */
    public OverflowingLevel isOverflowing(Intervals intervals) {

        Supplier<OverflowingLevel> possibleOverflow = () -> new OverflowingLevel(true, false);
        Supplier<OverflowingLevel> definiteOverflow = () -> new OverflowingLevel(true, true);

        // Converts a Number to its double value for use in over-approximation scenarios. Useful when precision is not
        // critical and a uniform numeric type (double) is desired.
        Function<Number, Double> overApproximate = Number::doubleValue;

        if (intervals.isBottom()) {
            // TODO: handle bottom in a better way.
            return OverflowingLevel.base();
        }

        if (intervals.isTop()) {
            // The value may exceed, but not definitively.
            return possibleOverflow.get();
        }

        var interval = intervals.interval;
        var min = interval.low;
        var max = interval.high;

        if (min.isInfinite() || max.isInfinite()) {
            // Since it is not top, it is possible to know the "overflow direction". However, this requires additional
            // data to track.
            possibleOverflow.get();
        }

        // From this point on the interval is finite, that is, the lower and upper bounds are represented by finite
        // numbers.
        var minApproximation = overApproximate.apply(minLimit());
        var maxApproximation = overApproximate.apply(maxLimit());
        var isUnderflow = min.lt(new MathNumber(minApproximation));
        var isOverflow = max.gt(new MathNumber(maxApproximation));

        if (isUnderflow || isOverflow) {
            // Whenever the value is still outside the limit, the overflow is definite.
            var alwaysDown = max.lt(new MathNumber(minLimit().longValue()));
            var alwaysUp = min.gt(new MathNumber(maxLimit().longValue()));

            // If the direction is consistently down or up, return the definite overflow value. Otherwise, return the
            // possible overflow value, accounting for uncertainty.
            if (alwaysDown || alwaysUp) return definiteOverflow.get();
            return possibleOverflow.get();
        }

        // If no further information can be deduced from the specified ranges, the verifier will not assume anything and
        // will not report any errors.
        return OverflowingLevel.base();
    }

    public abstract Number minLimit();

    public abstract Number maxLimit();

    /**
     * Try to find the most appropriate checker based on the numerical size.
     *
     * @param size The numerical size used to find the most appropriate checker.
     * @return A subclass of {@link SizeChecker}.
     */
    public static Optional<SizeChecker> findBy(OverflowChecker.NumericalSize size) {
        record ClassNamePair(Class<?> checker, String className) {
        }

        // Assuming that there is a direct mapping between the name representing the numeric size and the class name of
        // the subclasses of this sealed class. Because it is a sealed class, it is known at compile time what the
        // possible subclasses are, which allows for better reflection.
        return Arrays
                .stream(SizeChecker.class.getPermittedSubclasses())
                .map(klass -> new ClassNamePair(klass, klass.getSimpleName()))
                .filter(pair -> pair.className.equalsIgnoreCase(size.name()))
                .findFirst()
                .map(pair -> {
                    try {
                        return (SizeChecker) pair
                                .checker
                                .getConstructor(size.getClass())
                                .newInstance(size);
                    } catch (Exception e) {
                        defaultLogger.warning(() -> "Cannot build an instance of " + pair.className);
                        return null;
                    }
                });
    }
}
