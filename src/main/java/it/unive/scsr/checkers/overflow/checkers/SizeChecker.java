package it.unive.scsr.checkers.overflow.checkers;

import it.unive.scsr.Intervals;
import it.unive.scsr.checkers.OverflowChecker;
import it.unive.scsr.intervals.numbers.Numeric;

import java.util.Arrays;
import java.util.Optional;
import java.util.function.Supplier;

// TODO: implement float checker
public abstract sealed class SizeChecker permits Int16, Int32, Int8, UInt16, UInt32, UInt8 {

  /**
   * A record that represents whether a value has overflowed. If overflowed, it also specifies
   * whether the value has overflowed definitely or not.
   *
   * @param isOverflowing <code>true</code> if the value is overflowing, <code>false</code>
   *     otherwise.
   * @param definitely <code>true</code> if the value is definitely overflowing, <code>false</code>
   *     otherwise.
   */
  public record OverflowingLevel(boolean isOverflowing, boolean definitely) {

    /**
     * @return The base record indicating that the analysis is unable to tell anything.
     */
    public static OverflowingLevel base() {
      return new OverflowingLevel(false, false);
    }
  }

  /**
   * Determines whether the value can cause an overflow or not.
   *
   * @param intervals The range that represents the lower and upper numeric limit for a program
   *     element.
   * @return The {@link OverflowingLevel} indicating whether the value could overflow and whether it
   *     will definitely happen or not.
   */
  public OverflowingLevel isOverflowing(Intervals intervals) {

    Supplier<OverflowingLevel> possibleOverflow = () -> new OverflowingLevel(true, false);
    Supplier<OverflowingLevel> definiteOverflow = () -> new OverflowingLevel(true, true);

    if (intervals.isBottom()) {
      return OverflowingLevel.base();
    }

    if (intervals.isTop()) {
      // The value may exceed, but not definitively.
      return possibleOverflow.get();
    }

    var interval = intervals.interval;
    var min = interval.low;
    var max = interval.high;

    // From this point on the interval is finite, that is, the lower and upper bounds are
    // represented by finite numbers or by infinity.
    var isUnderflow = min.lessThan(minLimit());
    var isOverflow = max.greaterThan(maxLimit());

    if (isUnderflow || isOverflow) {
      // Whenever the value is still outside the limit, the overflow is definite.
      var alwaysDown = max.lessThan(minLimit());
      var alwaysUp = min.greaterThan(maxLimit());

      // If the direction is consistently down or up, return the definite overflow value. Otherwise,
      // return the possible
      // overflow value, accounting for uncertainty.
      if (alwaysDown || alwaysUp) return definiteOverflow.get();
      return possibleOverflow.get();
    }

    // If no further information can be deduced from the specified ranges, the verifier will not
    // assume anything and
    // will not report any errors.
    return OverflowingLevel.base();
  }

  public abstract Numeric<?> minLimit();

  public abstract Numeric<?> maxLimit();

  /**
   * Try to find the most appropriate checker based on the numerical size.
   *
   * @param size The numerical size used to find the most appropriate checker.
   * @return A subclass of {@link SizeChecker}.
   */
  public static Optional<SizeChecker> findBy(OverflowChecker.NumericalSize size) {
    record ClassNamePair(Class<?> checker, String className) {}

    // Assuming that there is a direct mapping between the name representing the numeric size and
    // the class name of
    // the subclasses of this sealed class. Because it is a sealed class, it is known at compile
    // time what the
    // possible subclasses are, which allows for better reflection.
    return Arrays.stream(SizeChecker.class.getPermittedSubclasses())
        .map(klass -> new ClassNamePair(klass, klass.getSimpleName()))
        .filter(pair -> pair.className.equalsIgnoreCase(size.name()))
        .findFirst()
        .map(
            pair -> {
              try {
                return (SizeChecker) pair.checker.getConstructor().newInstance();
              } catch (Exception e) {
                throw new IllegalArgumentException("Cannot build an instance of " + pair.className);
              }
            });
  }
}
