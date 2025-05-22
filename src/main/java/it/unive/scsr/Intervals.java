package it.unive.scsr;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.lattices.Satisfiability;
import it.unive.lisa.analysis.nonrelational.value.BaseNonRelationalValueDomain;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.operator.ComparisonOperator;
import it.unive.lisa.symbolic.value.operator.binary.*;
import it.unive.lisa.symbolic.value.operator.unary.NumericNegation;
import it.unive.lisa.symbolic.value.operator.unary.UnaryOperator;
import it.unive.lisa.util.representation.StringRepresentation;
import it.unive.lisa.util.representation.StructuredRepresentation;
import it.unive.scsr.intervals.BinaryFunctions;
import it.unive.scsr.intervals.BinarySatisfiability;
import it.unive.scsr.intervals.NumericInterval;
import it.unive.scsr.intervals.numbers.*;

public class Intervals implements BaseNonRelationalValueDomain<Intervals>, Comparable<Intervals> {

  public final NumericInterval interval;
  public final Numeric<?> wideningThreshold;

  public Intervals(NumericInterval interval, Numeric<?> wideningThreshold) {
    this.interval = interval;
    this.wideningThreshold = wideningThreshold;
  }

  public Intervals(SigNum lower, SigNum upper, Numeric<?> wideningThreshold) {
    this(new NumericInterval(lower, upper), wideningThreshold);
  }

  public Intervals(Numeric<?> wideningThreshold) {
    this(NumericInterval.INFINITY, wideningThreshold);
  }

  public Intervals() {
    this(NumericInterval.INFINITY, null);
  }

  @Override
  public Intervals evalUnaryExpression(
      UnaryOperator operator, Intervals arg, ProgramPoint pp, SemanticOracle oracle) {
    return operator instanceof NumericNegation ? changeInterval(arg.interval.negate()) : top();
  }

  @Override
  public Intervals glbAux(Intervals other) {
    return this.interval.intersection(other.interval).map(this::changeInterval).orElse(bottom());
  }

  @Override
  public Intervals lubAux(Intervals other) {
    return changeInterval(this.interval.union(other.interval));
  }

  @Override
  public boolean lessOrEqualAux(Intervals other) {
    return other.interval.includes(this.interval);
  }

  @Override
  public Intervals top() {
    return changeInterval(NumericInterval.INFINITY);
  }

  @Override
  public boolean isTop() {
    return interval != null && interval.isInfinity();
  }

  @Override
  public Intervals bottom() {
    return changeInterval(null);
  }

  @Override
  public boolean isBottom() {
    return interval == null;
  }

  @Override
  public StructuredRepresentation representation() {
    if (this.isBottom()) return Lattice.bottomRepresentation();
    return new StringRepresentation("[" + this.interval.low + "," + this.interval.high + "]");
  }

  @Override
  public int compareTo(Intervals o) {
    if (isBottom()) return o.isBottom() ? 0 : -1;
    if (isTop()) return o.isTop() ? 0 : 1;

    // This element is neither TOP nor BOTTOM.
    if (o.isBottom()) return 1;
    if (o.isTop()) return -1;

    // This element and the given one are neither TOP nor BOTTOM.
    return interval.compareTo(o.interval);
  }

  @Override
  public Intervals evalNonNullConstant(Constant constant, ProgramPoint pp, SemanticOracle oracle) {
    // Cover all possible primitive numbers that can be deduced by analyzing the CFG.
    if (!(constant.getValue() instanceof Number number)) return top();

    // If there is a number that cannot be enclosed in a IntervalNumber, then top is returned.
    return IntervalNumber.ofPrimitive(number)
        .map(NumericInterval::new)
        .map(this::changeInterval)
        .orElse(top());
  }

  @Override
  public Intervals evalBinaryExpression(
      BinaryOperator operator,
      Intervals left,
      Intervals right,
      ProgramPoint pp,
      SemanticOracle oracle) {
    return Optional.ofNullable(BinaryFunctions.INSTANCE.findBy(operator))
        .map(f -> f.apply(left, right))
        .orElse(top());
  }

  @Override
  public int hashCode() {
    return Objects.hash(interval);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    Intervals other = (Intervals) obj;
    return Objects.equals(interval, other.interval);
  }

  @Override
  public Intervals wideningAux(Intervals other) {
    // Implementation for calculating unstable bounds. This approach typically expands the interval
    // to include the 'other' interval's range if it extends beyond the current interval.
    Supplier<NumericInterval> unstableBounds =
        () ->
            new NumericInterval(
                other.interval.low.compareTo(interval.low) < 0
                    ? MinusInfinity.INSTANCE
                    : interval.low,
                other.interval.high.compareTo(interval.high) > 0
                    ? PlusInfinity.INSTANCE
                    : interval.high);

    // Implementation for threshold-based widening. This approach expands the interval to infinity
    // if its bounds cross a predefined widening threshold.
    Supplier<NumericInterval> threshold =
        () -> {
          var min = interval.low.min(other.interval.high).asSigNum();
          var max = interval.high.max(other.interval.high).asSigNum();
          return new NumericInterval(
              min.compareTo(wideningThreshold.negate()) > 0 ? min : MinusInfinity.INSTANCE,
              max.compareTo(wideningThreshold) < 0 ? max : PlusInfinity.INSTANCE);
        };

    // Apply the chosen widening strategy: if 'wideningThreshold' is null, use unstableBounds;
    // otherwise, use the threshold-based widening. The resulting interval then replaces the current
    // one.
    return changeInterval(wideningThreshold == null ? unstableBounds.get() : threshold.get());
  }

  @Override
  public Intervals narrowingAux(Intervals other) {
    SigNum newHigh = interval.high instanceof PlusInfinity ? other.interval.high : interval.high;
    SigNum newLow = interval.low instanceof MinusInfinity ? other.interval.low : interval.low;
    return changeInterval(new NumericInterval(newLow, newHigh));
  }

  @Override
  public Satisfiability satisfiesBinaryExpression(
      BinaryOperator operator,
      Intervals left,
      Intervals right,
      ProgramPoint pp,
      SemanticOracle oracle) {
    // If it is a ComparisonOperator, attempt to find a corresponding BinarySatisfiability function
    // based on the operator type. If no corresponding BinarySatisfiability function is found for
    // the ComparisonOperator, return UNKNOWN.
    if (operator instanceof ComparisonOperator comparisonOperator) {
      return Optional.ofNullable(BinarySatisfiability.INSTANCE.findBy(comparisonOperator))
          .map(f -> f.apply(left, right))
          .orElse(Satisfiability.UNKNOWN);
    }

    // If the 'operator' is not a ComparisonOperator, the satisfiability cannot be determined by
    // this logic, so return UNKNOWN.
    return Satisfiability.UNKNOWN;
  }

  public Intervals changeInterval(NumericInterval interval) {
    return new Intervals(interval, wideningThreshold);
  }
}
