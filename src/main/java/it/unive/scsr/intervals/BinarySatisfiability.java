package it.unive.scsr.intervals;

import it.unive.lisa.analysis.lattices.Satisfiability;
import it.unive.scsr.Intervals;
import it.unive.scsr.utils.BiSatisfiabilityDispatcher;

import java.util.Optional;
import java.util.function.BiFunction;

public class BinarySatisfiability extends BiSatisfiabilityDispatcher<Intervals> {

  public static BinarySatisfiability INSTANCE = new BinarySatisfiability();

  @Override
  public BiFunction<Intervals, Intervals, Satisfiability> buildEqFunction() {
    return (left, right) ->
        intersection(left, right).isEmpty()
            ? Satisfiability.NOT_SATISFIED
            : numericEquality(left, right) ? Satisfiability.SATISFIED : Satisfiability.UNKNOWN;
  }

  @Override
  public BiFunction<Intervals, Intervals, Satisfiability> buildNeFunction() {
    return (left, right) ->
        intersection(left, right).isEmpty()
            ? Satisfiability.SATISFIED
            : numericEquality(left, right) ? Satisfiability.NOT_SATISFIED : Satisfiability.UNKNOWN;
  }

  @Override
  public BiFunction<Intervals, Intervals, Satisfiability> buildGtFunction() {
    return (left, right) ->
        numericEquality(left, right) || definitelyLessThan(left, right)
            ? Satisfiability.NOT_SATISFIED
            : definitelyGreaterThan(left, right)
                ? Satisfiability.SATISFIED
                : Satisfiability.UNKNOWN;
  }

  @Override
  public BiFunction<Intervals, Intervals, Satisfiability> buildLtFunction() {
    return (left, right) ->
        numericEquality(left, right) || definitelyGreaterThan(left, right)
            ? Satisfiability.NOT_SATISFIED
            : definitelyLessThan(left, right) ? Satisfiability.SATISFIED : Satisfiability.UNKNOWN;
  }

  @Override
  public BiFunction<Intervals, Intervals, Satisfiability> buildGeFunction() {
    return (left, right) -> {
      // ...
      var comparison = comparison(left, right);

      // ...
      if (singletonInCommon(left, right)) {
        return comparison == 0 || comparison > 0
            ? Satisfiability.SATISFIED
            : Satisfiability.NOT_SATISFIED;
      }

      // ...
      return definitelyGreaterThan(left, right)
          ? Satisfiability.SATISFIED
          : definitelyLessThan(left, right) ? Satisfiability.NOT_SATISFIED : Satisfiability.UNKNOWN;
    };
  }

  @Override
  public BiFunction<Intervals, Intervals, Satisfiability> buildLeFunction() {
    return (left, right) -> {
      // ...
      var comparison = comparison(left, right);

      // ...
      if (singletonInCommon(left, right)) {
        return comparison == 0 || comparison < 0
            ? Satisfiability.SATISFIED
            : Satisfiability.NOT_SATISFIED;
      }

      // ...
      return definitelyLessThan(left, right)
          ? Satisfiability.SATISFIED
          : definitelyGreaterThan(left, right)
              ? Satisfiability.NOT_SATISFIED
              : Satisfiability.UNKNOWN;
    };
  }

  // ...
  private boolean definitelyLessThan(Intervals left, Intervals right) {
    var leftInterval = left.interval;
    var rightInterval = right.interval;
    return leftInterval.definitelyLessThan(rightInterval);
  }

  // ...
  private boolean definitelyGreaterThan(Intervals left, Intervals right) {
    var leftInterval = left.interval;
    var rightInterval = right.interval;
    return leftInterval.definitelyGreaterThan(rightInterval);
  }

  // ...
  private Optional<NumericInterval> intersection(Intervals left, Intervals right) {
    var leftInterval = left.interval;
    var rightInterval = right.interval;
    return leftInterval.intersection(rightInterval);
  }

  // ...
  private int comparison(Intervals left, Intervals right) {
    var leftInterval = left.interval;
    var rightInterval = right.interval;
    return leftInterval.compareTo(rightInterval);
  }

  // ...
  private boolean singletonInCommon(Intervals left, Intervals right) {
    var intersection = intersection(left, right);
    return intersection.isPresent() && intersection.orElseThrow().isSingleton();
  }

  // ...
  private boolean numericEquality(Intervals left, Intervals right) {
    return singletonInCommon(left, right) && comparison(left, right) == 0;
  }
}
