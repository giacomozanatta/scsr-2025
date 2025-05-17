package it.unive.scsr.checkers.overflow.checkers;

import it.unive.scsr.Intervals;
import it.unive.scsr.checkers.OverflowChecker;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Stream;

public abstract sealed class SizeChecker permits DecimalChecker, IntegerChecker {

  /**
   * Represents the result of an overflow check for numeric types. It is a sealed interface, meaning
   * its implementations are limited to the specified permitted subclasses: {@link
   * DecimalChecker.DecimalOverflow} and {@link IntegerChecker.IntegerOverflow}.
   */
  public sealed interface OverflowResult
      permits DecimalChecker.DecimalOverflow, IntegerChecker.IntegerOverflow {

    /**
     * @return {@code true} if the overflow result represents a valuable overflow that should be
     *     reported, {@code false} otherwise.
     */
    boolean isValuable();

    /**
     * @return {@code true} if the overflow condition is definite (always occurs under the given
     *     circumstances), {@code false} if it's potential or uncertain.
     */
    boolean isDefinite();
  }

  /**
   * Determines whether the value can cause an overflow or not.
   *
   * @param intervals The range that represents the lower and upper numeric limit for a program
   *     element.
   * @return The {@link OverflowResult} indicating whether the value could overflow.
   */
  public abstract OverflowResult isOverflowing(Intervals intervals);

  /**
   * Try to find the most appropriate checker based on the numerical size.
   *
   * @param size The numerical size used to find the most appropriate checker.
   * @return A subclass of {@link SizeChecker}.
   */
  public static Optional<SizeChecker> findBy(OverflowChecker.NumericalSize size) {
    record ClassNamePair(Class<?> checker, String className) {}

    // Assuming that there is a direct mapping between the name representing the numeric size and
    // the class name of the subclasses of this sealed class. Because it is a sealed class, it is
    // known at compile time what the possible subclasses are, which allows for better reflection.
    return Stream.of(
            IntegerChecker.class.getPermittedSubclasses(),
            DecimalChecker.class.getPermittedSubclasses())
        .flatMap(Arrays::stream)
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
