package it.unive.scsr.intervals;

import it.unive.lisa.analysis.lattices.Satisfiability;
import it.unive.scsr.Intervals;
import it.unive.scsr.utils.BiSatisfiabilityDispatcher;

import java.util.function.BiFunction;

public class BinarySatisfiability extends BiSatisfiabilityDispatcher<Intervals> {

  public static BinarySatisfiability INSTANCE = new BinarySatisfiability();

  @Override
  public BiFunction<Intervals, Intervals, Satisfiability> buildEqFunction() {
    return (left, right) ->
        left.interval.intersection(right.interval).isEmpty()
            ? Satisfiability.NOT_SATISFIED
            : numericEquality(left, right) ? Satisfiability.SATISFIED : Satisfiability.UNKNOWN;
  }

  @Override
  public BiFunction<Intervals, Intervals, Satisfiability> buildNeFunction() {
    return (left, right) ->
        left.interval.intersection(right.interval).isEmpty()
            ? Satisfiability.SATISFIED
            : numericEquality(left, right) ? Satisfiability.NOT_SATISFIED : Satisfiability.UNKNOWN;
  }

  @Override
  public BiFunction<Intervals, Intervals, Satisfiability> buildGtFunction() {
    return (left, right) ->
        numericEquality(left, right) || left.interval.definitelyLessThan(right.interval)
            ? Satisfiability.NOT_SATISFIED
            : left.interval.definitelyGreaterThan(right.interval)
                ? Satisfiability.SATISFIED
                : Satisfiability.UNKNOWN;
  }

  @Override
  public BiFunction<Intervals, Intervals, Satisfiability> buildLtFunction() {
    return (left, right) ->
        numericEquality(left, right) || left.interval.definitelyGreaterThan(right.interval)
            ? Satisfiability.NOT_SATISFIED
            : left.interval.definitelyLessThan(right.interval)
                ? Satisfiability.SATISFIED
                : Satisfiability.UNKNOWN;
  }

  @Override
  public BiFunction<Intervals, Intervals, Satisfiability> buildGeFunction() {
    return (left, right) -> {
      if (numericEquality(left, right)) return Satisfiability.SATISFIED;
      return left.interval.definitelyGreaterThan(right.interval)
          ? Satisfiability.SATISFIED
          : left.interval.definitelyLessThan(right.interval)
              ? Satisfiability.NOT_SATISFIED
              : Satisfiability.UNKNOWN;
    };
  }

  @Override
  public BiFunction<Intervals, Intervals, Satisfiability> buildLeFunction() {
    return (left, right) -> {
      if (numericEquality(left, right)) return Satisfiability.SATISFIED;
      return left.interval.definitelyLessThan(right.interval)
          ? Satisfiability.SATISFIED
          : left.interval.definitelyGreaterThan(right.interval)
              ? Satisfiability.NOT_SATISFIED
              : Satisfiability.UNKNOWN;
    };
  }

  // Checks if two Intervals objects represent numerically equal singleton intervals. Two intervals
  // are considered numerically equal singletons if both contain only a single value and that single
  // value is the same for both intervals.
  private boolean numericEquality(Intervals left, Intervals right) {
    return (left.interval.isSingleton() && right.interval.isSingleton())
        && (left.interval.compareTo(right.interval) == 0);
  }
}
