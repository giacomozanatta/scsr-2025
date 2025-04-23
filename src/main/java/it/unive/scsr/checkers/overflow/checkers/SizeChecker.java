package it.unive.scsr.checkers.overflow.checkers;

import it.unive.scsr.Intervals;
import it.unive.scsr.checkers.OverflowChecker;
import java.util.Arrays;
import java.util.Optional;

import static it.unive.scsr.utils.Logging.defaultLogger;

public sealed abstract class SizeChecker permits UInt8 {

    /**
     * A record that represents whether a value has overflowed. If overflowed, it also specifies whether the value has
     * overflowed definitely or not.
     * @param isOverflowing <code>true</code> if the value is overflowing, <code>false</code> otherwise.
     * @param definitely <code>true</code> if the value is definitely overflowing, <code>false</code> otherwise.
     */
    public record OverflowingLevel(boolean isOverflowing, boolean definitely) {

        /**
         * @return The base record indicating that the analysis is unable to tell anything.
         */
        public static OverflowingLevel base() { return new OverflowingLevel(false, false); }
    }

    protected final OverflowChecker.NumericalSize size;

    protected SizeChecker(OverflowChecker.NumericalSize size) {
        this.size = size;
    }

    public abstract OverflowingLevel isOverflowing(Intervals intervals);

    /**
     * Try to find the most appropriate checker based on the numerical size.
     * @param size The numerical size used to find the most appropriate checker.
     * @return A subclass of {@link SizeChecker}.
     */
    public static Optional<SizeChecker> findBy(OverflowChecker.NumericalSize size) {
        record ClassNamePair(Class<?> checker, String className) {}

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
