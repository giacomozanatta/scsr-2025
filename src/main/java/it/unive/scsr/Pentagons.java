package it.unive.scsr;

import java.util.*;
import java.util.Map.Entry;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

import it.unive.lisa.symbolic.value.operator.ComparisonOperator;
import it.unive.scsr.intervals.NumericInterval;
import it.unive.scsr.intervals.numbers.IntervalNumber;
import it.unive.scsr.intervals.numbers.PlusInfinity;
import it.unive.scsr.pentagons.BinarySatisfiability;
import org.apache.commons.collections4.CollectionUtils;

import it.unive.lisa.analysis.BaseLattice;
import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.lattices.Satisfiability;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.symbolic.value.operator.SubtractionOperator;
import it.unive.lisa.util.representation.MapRepresentation;
import it.unive.lisa.util.representation.StringRepresentation;
import it.unive.lisa.util.representation.StructuredRepresentation;

public class Pentagons implements ValueDomain<Pentagons>, BaseLattice<Pentagons> {
  private final ValueEnvironment<UpperBounds> upperbounds;
  private final ValueEnvironment<Intervals> intervals;

  public Pentagons() {
    this.upperbounds = new ValueEnvironment<>(new UpperBounds()).top();
    this.intervals = new ValueEnvironment<>(new Intervals()).top();
  }

  public Pentagons(
      ValueEnvironment<UpperBounds> upperbounds, ValueEnvironment<Intervals> intervals) {
    this.upperbounds = upperbounds;
    this.intervals = intervals;
  }

  @Override
  public Pentagons top() {
    return new Pentagons(upperbounds.top(), intervals.top());
  }

  @Override
  public boolean isTop() {
    // A pentagon is a top element if both the numerical and symbolic components are top elements.
    // Source: https://www.microsoft.com/en-us/research/wp-content/uploads/2009/01/pentagons.pdf.
    return upperbounds.isTop() && intervals.isTop();
  }

  @Override
  public Pentagons bottom() {
    return new Pentagons(upperbounds.bottom(), intervals.bottom());
  }

  @Override
  public boolean isBottom() {
    // A pentagon is a bottom element if either numerical or symbolic components are bottom
    // elements. Source:
    // https://www.microsoft.com/en-us/research/wp-content/uploads/2009/01/pentagons.pdf.
    return upperbounds.isBottom() || intervals.isBottom();
  }

  @Override
  // A pentagon <b1, s1> is smaller than a pentagon <b2, s2> if and only if the interval
  // environment b1 is included in the interval environment b2 and for all the symbolic
  // constraints of the form x < y in s2, either x < y is an explicit constraint in s1 or it is
  // implied by the interval environment b1, i.e., the numerical upper bound for x is strictly
  // smaller than the numerical lower bound for y. Source:
  // https://www.microsoft.com/en-us/research/wp-content/uploads/2009/01/pentagons.pdf.
  public boolean lessOrEqualAux(Pentagons other) throws SemanticException {
    // This interval environment must be included in the interval environment of the given pentagon.
    if (!this.intervals.lessOrEqual(other.intervals)) {
      return false;
    }

    for (Entry<Identifier, UpperBounds> entry : other.upperbounds) {
      // The other identifier is the one that is strictly less than each of the y contained in the
      // other limits.
      var otherIdentifier = entry.getKey();
      var otherBounds = entry.getValue();

      for (Identifier bound : otherBounds) {
        // The constraint of the form x < y in otherBounds is an explicit constraint this
        // upperbounds.
        var containsOtherBound = upperbounds.getState(otherIdentifier).contains(bound);

        // The "less than" constraint is implicit in the interval environment.
        var otherInterval = intervals.getState(otherIdentifier).interval;
        var boundInterval = intervals.getState(bound).interval;
        var definitelyLess = otherInterval.definitelyLessThan(boundInterval);

        if (!(containsOtherBound || definitelyLess)) {
          return false;
        }
      }
    }

    // All conditions are met.
    return true;
  }

  @Override
  public Pentagons glbAux(Pentagons other) throws SemanticException {
    // The meet operator simply delegates to the underlying abstract domains. Source:
    // https://www.microsoft.com/en-us/research/wp-content/uploads/2009/01/pentagons.pdf.
    return new Pentagons(upperbounds.glb(other.upperbounds), intervals.glb(other.intervals));
  }

  @Override
  public Pentagons lubAux(Pentagons other) throws SemanticException {
    // For the symbolic part, the join function keeps the constraints which are either explicit in
    // the two operators or which are explicit in one operator, and implied by the numerical domain
    // in the other component. Source:
    // https://www.microsoft.com/en-us/research/wp-content/uploads/2009/01/pentagons.pdf.
    final var newBounds = new HashSet<ValueEnvironment<UpperBounds>>();
    newBounds.add(upperbounds.lub(other.upperbounds));

    // Define a BiConsumer to apply the "definitely less than" closure logic. This helps to refine
    // the upper bounds by considering definite less-than relationships between identifiers based on
    // their intervals.
    BiConsumer<ValueEnvironment<UpperBounds>, ValueEnvironment<Intervals>> definitelyLess =
        (envBound, envInterval) -> {
          for (Entry<Identifier, UpperBounds> entry : envBound) {
            // Initialize a set to collect identifiers that are definitely greater than the current
            // entry's key.
            var closure = new HashSet<Identifier>();

            for (Identifier bound : entry.getValue()) {
              // Get the intervals for the current entry's key (shouldBeLess) and the current bound
              // (shouldBeGreater).
              var shouldBeLess = envInterval.getState(entry.getKey());
              var shouldBeGreater = envInterval.getState(bound);
              var bothValid = !(shouldBeLess.isBottom() || shouldBeGreater.isBottom());

              // If both intervals are valid and 'shouldBeLess' is definitely less than
              // 'shouldBeGreater', add 'bound' to the closure set.
              if (bothValid && shouldBeLess.interval.definitelyLessThan(shouldBeGreater.interval)) {
                closure.add(bound);
              }
            }

            // If the closure set is not empty, it means new upper bounds can be inferred.
            if (!closure.isEmpty()) {
              try {
                var oldBounds = newBounds.stream().findFirst().orElseThrow();
                newBounds.clear();
                newBounds.add(
                    oldBounds.putState(
                        entry.getKey(),
                        oldBounds.getState(entry.getKey()).glb(new UpperBounds(closure))));
              } catch (SemanticException e) {
                // Wrap and rethrow any SemanticException as a RuntimeException.
                throw new RuntimeException(e);
              }
            }
          }
        };

    // Apply the 'definitelyLess' logic in both directions:
    //  1. Using the current upper bounds and the other Pentagons' intervals.
    //  2. Using the other Pentagons' upper bounds and the current intervals.
    definitelyLess.accept(upperbounds, other.intervals);
    definitelyLess.accept(other.upperbounds, intervals);

    // For the numeric part, the join operator pushes the join into the underlying intervals
    // abstract domain. Return a new Pentagons object. Its upper bounds are the refined ones from
    // 'newBounds', and its intervals are the lub of the current and other Pentagons' intervals.
    return new Pentagons(
        newBounds.stream().findFirst().orElseThrow(), intervals.lub(other.intervals));
  }

  @Override
  public Pentagons wideningAux(Pentagons other) throws SemanticException {
    // The widening operator simply delegates to the underlying abstract domains. Source:
    // https://www.microsoft.com/en-us/research/wp-content/uploads/2009/01/pentagons.pdf.
    return new Pentagons(
        upperbounds.wideningAux(other.upperbounds), intervals.widening(other.intervals));
  }

  @Override
  public Pentagons assign(
      Identifier id, ValueExpression expression, ProgramPoint pp, SemanticOracle oracle)
      throws SemanticException {

    var newBounds = upperbounds.assign(id, expression, pp, oracle);
    var newIntervals = intervals.assign(id, expression, pp, oracle);

    if (expression instanceof BinaryExpression be) {

      var op = be.getOperator();

      if (op instanceof SubtractionOperator) {
        if (be.getLeft() instanceof Identifier x) {
          if (be.getRight() instanceof Identifier y) {
            if (newBounds.getState(y).contains(x)) {
              // This handles the case of the form var z = x - y. In this case it is known for sure
              // that x is an upper bound of y. The above code handles these scenarios:
              //  - The variable x is negative, so y is also negative, since x is an upper bound. By
              //    applying the "-" operator, y becomes greater than x, so the interval [1, +Inf]
              //    is a safe approximation
              //  - The variable x is positive and y is negative. By applying the "-" operator, y
              //    becomes positive and the interval [1, +Inf] is a safe approximation
              //  - The variable x is positive and y is positive but less than x. Again the interval
              //    [1, +Inf] is a safe approximation
              var state = newIntervals.getState(id);
              var intersection =
                  state.glb(
                      state.changeInterval(
                          new NumericInterval(
                              IntervalNumber.ofPrimitiveOrThrow(1), PlusInfinity.INSTANCE)));

              newIntervals = newIntervals.putState(id, intersection);
            }
          } else if (be.getRight() instanceof Constant) {
            // This handles the case of the form var z = x - c. Since this is a subtraction and the
            // right operand is a constant (i.e. a positive value, not being a unary expression),
            // the left variable to be assigned will have a value less than or equal to x.
            newBounds = newBounds.putState(id, upperbounds.getState(x).add(x));
          }
        }
      }
    }

    return new Pentagons(newBounds, newIntervals).closure();
  }

  @Override
  public Pentagons smallStepSemantics(
      ValueExpression expression, ProgramPoint pp, SemanticOracle oracle) throws SemanticException {
    return new Pentagons(
        upperbounds.smallStepSemantics(expression, pp, oracle),
        intervals.smallStepSemantics(expression, pp, oracle));
  }

  @Override
  public Pentagons assume(
      ValueExpression expression, ProgramPoint src, ProgramPoint dest, SemanticOracle oracle)
      throws SemanticException {
    return new Pentagons(
        upperbounds.assume(expression, src, dest, oracle),
        intervals.assume(expression, src, dest, oracle));
  }

  @Override
  public Pentagons forgetIdentifier(Identifier id) throws SemanticException {
    return new Pentagons(upperbounds.forgetIdentifier(id), intervals.forgetIdentifier(id));
  }

  @Override
  public Pentagons forgetIdentifiersIf(Predicate<Identifier> test) throws SemanticException {
    return new Pentagons(
        upperbounds.forgetIdentifiersIf(test), intervals.forgetIdentifiersIf(test));
  }

  @Override
  public Satisfiability satisfies(
      ValueExpression expression, ProgramPoint pp, SemanticOracle oracle) throws SemanticException {
    // First, check the satisfiability of the expression using both the interval analysis and the
    // upper bounds' analysis. The greatest lower bound (glb) combines the results, yielding the
    // most precise satisfiability known from these two domains.
    var satisfiability =
        intervals
            .satisfies(expression, pp, oracle)
            .glb(upperbounds.satisfies(expression, pp, oracle));

    // If the combined satisfiability is not UNKNOWN, we can directly return that result as it's a
    // definite conclusion from our existing analyses.
    if (satisfiability != Satisfiability.UNKNOWN) {
      return satisfiability;
    }

    if (expression instanceof BinaryExpression binaryExpression
        && binaryExpression.getOperator() instanceof ComparisonOperator comparisonOperator
        && binaryExpression.getLeft() instanceof Identifier left
        && binaryExpression.getRight() instanceof Identifier right) {

      return Optional.ofNullable(new BinarySatisfiability(upperbounds).findBy(comparisonOperator))
          .map(f -> f.apply(left, right))
          .orElse(Satisfiability.UNKNOWN);
    }

    // If none of the above checks yield a definite satisfiability, the result remains UNKNOWN.
    return Satisfiability.UNKNOWN;
  }

  @Override
  public Pentagons pushScope(ScopeToken token) throws SemanticException {
    return new Pentagons(upperbounds.pushScope(token), intervals.pushScope(token));
  }

  @Override
  public Pentagons popScope(ScopeToken token) throws SemanticException {
    return new Pentagons(upperbounds.popScope(token), intervals.popScope(token));
  }

  @Override
  public StructuredRepresentation representation() {
    if (isTop()) return Lattice.topRepresentation();
    if (isBottom()) return Lattice.bottomRepresentation();

    Map<StructuredRepresentation, StructuredRepresentation> mapping = new HashMap<>();
    for (Identifier id : CollectionUtils.union(intervals.getKeys(), upperbounds.getKeys()))
      mapping.put(
          new StringRepresentation(id),
          new StringRepresentation(
              intervals.getState(id).representation()
                  + ", "
                  + upperbounds.getState(id).representation()));

    return new MapRepresentation(mapping);
  }

  @Override
  public int hashCode() {
    return Objects.hash(intervals, upperbounds);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    Pentagons other = (Pentagons) obj;
    return Objects.equals(intervals, other.intervals)
        && Objects.equals(upperbounds, other.upperbounds);
  }

  @Override
  public String toString() {
    return representation().toString();
  }

  @Override
  public boolean knowsIdentifier(Identifier id) {
    return intervals.knowsIdentifier(id) || upperbounds.knowsIdentifier(id);
  }

  /**
   * Retrieves the Interval representation associated with a given identifier. This method provides
   * a safe way to access the interval information, returning an Optional to handle cases where no
   * interval is found for the ID.
   *
   * @param id The identifier for which to retrieve the intervals.
   * @return An {@link Optional} containing the {@link Intervals} object if found, or an empty
   *     {@link Optional} if no interval is associated with the given identifier.
   */
  public Optional<Intervals> getIntervals(Identifier id) {
    return Optional.ofNullable(intervals.getState(id));
  }

  // Computes the closure of the Pentagons abstract domain. This operation refines the upper bounds
  // for each identifier based on the definite less-than relationships observed between intervals.
  private Pentagons closure() throws SemanticException {
    // Initialize a new ValueEnvironment for upper bounds, starting with a copy of the current upper
    // bounds.
    var newBounds = new ValueEnvironment<>(upperbounds.lattice, upperbounds.getMap());

    for (Identifier outerIdentifier : intervals.getKeys()) {
      var closure = new HashSet<Identifier>();

      for (Identifier innerIdentifier : intervals.getKeys()) {
        if (!outerIdentifier.equals(innerIdentifier)) {
          var outerInterval = intervals.getState(outerIdentifier);
          var innerInterval = intervals.getState(innerIdentifier);
          var bothNotBottom = !(outerInterval.isBottom() || innerInterval.isBottom());

          if (bothNotBottom && outerInterval.interval.definitelyLessThan(innerInterval.interval)) {
            closure.add(innerIdentifier);
          }
        }
      }

      if (!closure.isEmpty()) {
        newBounds =
            newBounds.putState(
                outerIdentifier, newBounds.getState(outerIdentifier).glb(new UpperBounds(closure)));
      }
    }

    return new Pentagons(newBounds, intervals);
  }
}
