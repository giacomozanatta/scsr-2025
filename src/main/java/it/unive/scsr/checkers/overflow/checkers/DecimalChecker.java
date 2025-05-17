package it.unive.scsr.checkers.overflow.checkers;

import it.unive.scsr.Intervals;
import it.unive.scsr.intervals.numbers.Numeric;
import it.unive.scsr.intervals.numbers.SigNum;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

public abstract sealed class DecimalChecker extends SizeChecker permits Float16, Float32, Float8 {

  public static final class DecimalOverflow implements OverflowResult {

    // Represents the possible results of a decimal number check.
    private sealed interface DecimalResult {
      // Represents the case where no specific decimal characteristic is identified.
      record Empty() implements DecimalResult {
        private static final Empty INSTANCE = new Empty();
      }

      // Represents the case where any decimal characteristic might be present.
      record Any() implements DecimalResult {
        private static final Any INSTANCE = new Any();
      }

      // Represents a specific set of decimal characteristics identified during the check.
      final class Data implements DecimalResult {

        private final boolean roundToZero;
        private final boolean closeToZero;
        private final boolean roundPositiveLimit;
        private final boolean roundNegativeLimit;

        private Data(
            boolean roundToZero,
            boolean closeToZero,
            boolean roundPositiveLimit,
            boolean roundNegativeLimit) {
          this.roundToZero = roundToZero;
          this.closeToZero = closeToZero;
          this.roundPositiveLimit = roundPositiveLimit;
          this.roundNegativeLimit = roundNegativeLimit;
        }
      }

      // Determines the DecimalResult based on the Signum and the DecimalChecker.
      static DecimalResult result(SigNum sigNum, DecimalChecker checker) {
        // If the Signum represents infinity, any decimal characteristic is considered present.
        if (sigNum.isInfinity()) return Any.INSTANCE;

        var asNumeric = (Numeric<?>) sigNum;

        // Determine if the absolute value of the Signum's decimal representation is greater than
        // the largest representable decimal number.
        Supplier<Boolean> commonInfinityLogic =
            () -> asNumeric.toDecimal().abs().compareTo(checker.largestNumber().toDecimal()) > 0;
        var roundToPositiveLimit = asNumeric.isPositive() && commonInfinityLogic.get();
        var roundToNegativeLimit = asNumeric.isNegative() && commonInfinityLogic.get();

        // Defines a common logic to check if a Numeric value is non-zero, has an absolute value
        // less than one, and its fractional part's absolute value is less than a specified
        // comparison value.
        Function<Numeric<?>, Boolean> commonZeroLogic =
            (toCompare) ->
                !asNumeric.isZero()
                    && asNumeric.toDecimal().abs().compareTo(BigDecimal.ONE) < 0
                    && asNumeric
                            .toDecimal()
                            .remainder(BigDecimal.ONE)
                            .abs()
                            .compareTo(toCompare.toDecimal())
                        < 0;

        // The variable roundToZero holds the result of checking if the number is close to zero,
        // using the smallest positive subnormal number as the threshold, while closeToZero holds
        // the result of checking if the number is very close to zero, using the smallest positive
        // normal number as the threshold.
        var roundToZero = commonZeroLogic.apply(checker.smallestPositiveSubnormalNumber());
        var closeToZero = commonZeroLogic.apply(checker.smallestPositiveNormalNumber());

        // If any of the checked conditions (close to zero, round to zero, round to infinity) are
        // true, return a Data DecimalResult containing the boolean flags. Otherwise, return Empty.
        return Stream.of(closeToZero, roundToZero, roundToPositiveLimit, roundToNegativeLimit)
                .anyMatch(aBoolean -> aBoolean)
            ? new Data(roundToZero, closeToZero, roundToPositiveLimit, roundToNegativeLimit)
            : Empty.INSTANCE;
      }
    }

    private final DecimalResult left;
    private final DecimalResult right;

    private DecimalOverflow() {
      this(DecimalResult.Empty.INSTANCE, DecimalResult.Empty.INSTANCE);
    }

    private DecimalOverflow(DecimalResult left, DecimalResult right) {
      this.left = left;
      this.right = right;
    }

    @Override
    public boolean isValuable() {
      return !(left instanceof DecimalResult.Empty && right instanceof DecimalResult.Empty);
    }

    @Override
    public boolean isDefinite() {
      // Return true if both the left and right DecimalResults indicate a definite rounding to zero
      // or infinity; otherwise, return false.
      return definiteRoundToZero() || definiteReachPositiveLimit() || definiteReachNegativeLimit();
    }

    /**
     * Checks if both the left and right operands are only close to zero, meaning they are close to
     * zero but do not definitely round to zero and do not definitely reach positive or negative
     * limits.
     *
     * @return {@code true} if both operands are only close to zero, {@code false} otherwise.
     */
    public boolean onlyCloseToZero() {
      // Check if both the 'left' and 'right' DecimalResult instances are of type 'Data'. If either
      // is not 'Data', their specific boolean flags cannot be compared, so return false.
      if (!(left instanceof DecimalResult.Data lData
          && right instanceof DecimalResult.Data rData)) {
        return false;
      }

      // Define a Predicate to check if the roundNegativeLimit and roundPositiveLimit flags within a
      // DecimalResult.Data instance are both false.
      Predicate<DecimalResult.Data> areLimitFlagsFalse =
          data ->
              Stream.of(data.roundNegativeLimit, data.roundPositiveLimit)
                  .noneMatch(aBoolean -> aBoolean);

      // Return true if all the following conditions are met:
      // 1. The operands do not definitely round to zero (checked by calling definiteRoundToZero()).
      // 2. For both left and right operands, neither the roundNegativeLimit nor the
      //    roundPositiveLimit flag is true (checked using the areLimitFlagsFalse predicate).
      // 3. Both the left and right operands have their closeToZero flag set to true.
      return !definiteRoundToZero()
          && areLimitFlagsFalse.test(lData)
          && areLimitFlagsFalse.test(rData)
          && (lData.closeToZero && rData.closeToZero);
    }

    /**
     * Checks if both the left and right operands of this operation definitely round to zero. It
     * retrieves the rounding to zero status from the underlying {@link DecimalResult.Data} for both
     * operands. If either operand's {@link DecimalResult} is not {@link DecimalResult.Data} or its
     * {@code roundToZero} flag is {@code false}, this method returns {@code false}.
     *
     * @return {@code true} if both operands definitely round to zero, {@code false} otherwise.
     */
    public boolean definiteRoundToZero() {
      var leftRounding = toData(left).map(data -> data.roundToZero).orElse(false);
      var rightRounding = toData(right).map(data -> data.roundToZero).orElse(false);
      return leftRounding && rightRounding;
    }

    /**
     * Checks if both the left and right operands of this operation definitely reach their positive
     * limit. It retrieves the rounding to positive limit status from the underlying {@link
     * DecimalResult.Data} for both operands. If either operand's {@link DecimalResult} is not
     * {@link DecimalResult.Data} or its {@code roundPositiveLimit} flag is {@code false}, this
     * method returns {@code false}.
     *
     * @return {@code true} if both operands definitely reach their positive limit, {@code false}
     *     otherwise.
     */
    public boolean definiteReachPositiveLimit() {
      var leftRounding = toData(left).map(data -> data.roundPositiveLimit).orElse(false);
      var rightRounding = toData(right).map(data -> data.roundPositiveLimit).orElse(false);
      return leftRounding && rightRounding;
    }

    /**
     * Checks if both the left and right operands of this operation definitely reach their negative
     * limit. It retrieves the rounding to negative limit status from the underlying {@link
     * DecimalResult.Data} for both operands. If either operand's {@link DecimalResult} is not
     * {@link DecimalResult.Data} or its {@code roundNegativeLimit} flag is {@code false}, this
     * method returns {@code false}.
     *
     * @return {@code true} if both operands definitely reach their negative limit, {@code false}
     *     otherwise.
     */
    public boolean definiteReachNegativeLimit() {
      var leftRounding = toData(left).map(data -> data.roundNegativeLimit).orElse(false);
      var rightRounding = toData(right).map(data -> data.roundNegativeLimit).orElse(false);
      return leftRounding && rightRounding;
    }

    private Optional<DecimalResult.Data> toData(DecimalResult result) {
      return result instanceof DecimalResult.Data asData ? Optional.of(asData) : Optional.empty();
    }
  }

  @Override
  public DecimalOverflow isOverflowing(Intervals intervals) {
    // If the intervals represent the bottom value an empty overflow is returned.
    if (intervals.isBottom()) return new DecimalOverflow();

    // Extract the underlying Interval from the Intervals object by getting the lower and upper
    // bound.
    var interval = intervals.interval;
    var low = interval.low;
    var high = interval.high;

    // If both the lower and upper bounds of the interval are integer numbers, then an empty
    // overflow is returned. Otherwise, create a DecimalOverflow result with specific DecimalResult
    // information for both the lower and upper bounds.
    return low.isIntegerNumber() && high.isIntegerNumber()
        ? new DecimalOverflow()
        : new DecimalOverflow(
            DecimalOverflow.DecimalResult.result(interval.low, this),
            DecimalOverflow.DecimalResult.result(interval.high, this));
  }

  /**
   * Returns the smallest positive normal number representable by this checker. Normal numbers are
   * non-zero numbers with a normalized exponent.
   *
   * @return The smallest positive normal number.
   */
  public abstract Numeric<?> smallestPositiveNormalNumber();

  /**
   * Returns the smallest positive subnormal number representable by this checker. Subnormal numbers
   * (also known as denormalized numbers) are non-zero numbers with a minimal exponent and leading
   * zeros in their significand. They allow for a gradual underflow towards zero.
   *
   * @return The smallest positive subnormal number.
   */
  public abstract Numeric<?> smallestPositiveSubnormalNumber();

  /**
   * Returns the largest finite number representable by this checker. This is the upper bound for
   * finite values of numeric types.
   *
   * @return The largest finite number.
   */
  public abstract Numeric<?> largestNumber();
}
