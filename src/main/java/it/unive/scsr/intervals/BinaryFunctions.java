package it.unive.scsr.intervals;

import it.unive.scsr.Intervals;
import it.unive.scsr.intervals.numbers.IntervalNumber;
import it.unive.scsr.utils.BiFunctionDispatcher;

import java.util.Optional;
import java.util.function.BiFunction;

public class BinaryFunctions extends BiFunctionDispatcher<Intervals> {

  public static final BinaryFunctions INSTANCE = new BinaryFunctions();

  @Override
  protected BiFunction<Intervals, Intervals, Intervals> buildAdditionFunction() {
    return (left, right) ->
        left.interval.add(right.interval, null).map(Intervals::new).orElse(Intervals.TOP);
  }

  @Override
  protected BiFunction<Intervals, Intervals, Intervals> buildSubtractionFunction() {
    return (left, right) ->
        left.interval.subtract(right.interval, null).map(Intervals::new).orElse(Intervals.TOP);
  }

  @Override
  protected BiFunction<Intervals, Intervals, Intervals> buildMultiplicationFunction() {
    // When multiplying MathNumber, zero and infinity cannot be multiplied together. Here, instead
    // of returning the
    // closest approximation, zero is returned.
    IntervalNumber.Computation computation =
        (first, second) -> {
          if (first.isZero() && second.isInfinity()) return Optional.of(first);
          if (second.isZero() && first.isInfinity()) return Optional.of(second);

          // It is impossible to be more specific than returning zero when a multiplication is
          // between zero and any
          // other IntervalNumber, including Infinity.
          return Optional.empty();
        };

    return (left, right) ->
        left.interval
            .multiply(right.interval, computation)
            .map(Intervals::new)
            .orElse(Intervals.TOP);
  }

  @Override
  protected BiFunction<Intervals, Intervals, Intervals> buildDivisionFunction() {
    return (left, right) -> {
      // When the correct interval is a singleton and contains zero, it is known for sure that the
      // division is by
      // zero, that is the bottom element can be returned.
      if (right.interval.is(IntervalNumber.ofPrimitiveOrThrow(0))) return Intervals.BOTTOM;

      return left.interval.divide(right.interval, null).map(Intervals::new).orElse(Intervals.TOP);
    };
  }
}
