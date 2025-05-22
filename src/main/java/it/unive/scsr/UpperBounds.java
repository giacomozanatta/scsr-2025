package it.unive.scsr;

import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.nonrelational.value.BaseNonRelationalValueDomain;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.symbolic.value.operator.binary.BinaryOperator;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonEq;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonGe;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonGt;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonLe;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonLt;
import it.unive.lisa.util.representation.SetRepresentation;
import it.unive.lisa.util.representation.StringRepresentation;
import it.unive.lisa.util.representation.StructuredRepresentation;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;

public class UpperBounds
    implements BaseNonRelationalValueDomain<UpperBounds>, Iterable<Identifier> {

  private static final UpperBounds TOP = new UpperBounds(new HashSet<>(), true);
  private static final UpperBounds BOTTOM = new UpperBounds(new HashSet<>(), false);

  private final boolean isTop;
  public final Set<Identifier> bounds;

  public UpperBounds() {
    this.bounds = new HashSet<>();
    this.isTop = true;
  }

  public UpperBounds(Set<Identifier> bounds) {
    this.bounds = bounds;
    this.isTop = bounds.isEmpty();
  }

  // Make this UpperBounds constructor private to prevent the isTop flag from being writable by callers.
  private UpperBounds(Set<Identifier> bounds, boolean isTop) {
    this.bounds = bounds;
    this.isTop = isTop;
  }

  @Override
  public UpperBounds top() {
    return TOP;
  }

  @Override
  public UpperBounds bottom() {
    return BOTTOM;
  }

  @Override
  public boolean isBottom() {
    // This item is at the bottom when it is not marked as top but there is no information.
    return this == BOTTOM || (!isTop && bounds.isEmpty());
  }

  @Override
  public boolean isTop() {
    // The top element is represented by the empty set.
    return this == TOP || (isTop && bounds.isEmpty());
  }

  @Override
  public UpperBounds lubAux(UpperBounds other) {
    // At a join point, only relations that are valid on both (incoming) branches are maintained.
    var lub = new HashSet<>(bounds);
    lub.retainAll(other.bounds);
    return new UpperBounds(lub);
  }

  @Override
  public UpperBounds glbAux(UpperBounds other) {
    // The meet is a set union.
    var lub = new HashSet<>(bounds);
    lub.addAll(other.bounds);
    return new UpperBounds(lub);
  }

  @Override
  public boolean lessOrEqualAux(UpperBounds other) {
    // All upper bounds of the given element are contained within these upper bounds.
    return bounds.containsAll(other.bounds);
  }

  @Override
  public UpperBounds wideningAux(UpperBounds other) {
    // Keep only those constraints that are stable in subsequent iterations.
    return other.lessOrEqualAux(this) ? other : TOP;
  }

  @Override
  public ValueEnvironment<UpperBounds> assumeBinaryExpression(
      ValueEnvironment<UpperBounds> environment,
      BinaryOperator operator,
      ValueExpression left,
      ValueExpression right,
      ProgramPoint src,
      ProgramPoint dest,
      SemanticOracle oracle)
      throws SemanticException {
    if (!(left instanceof Identifier x && right instanceof Identifier y)) {
      return environment;
    }

    if (operator instanceof ComparisonEq) {
      // This block handles the case where the comparison operator is "equals" (x == y). When x
      // equals y, their abstract states must be consistent. This is achieved by taking the greatest
      // lower bound (glb) of their current states, which effectively computes their union. This new
      // state, is then applied to both x and y.
      var set = environment.getState(x).glb(environment.getState(y));
      return environment.putState(x, set).putState(y, set);
    }

    if (operator instanceof ComparisonLt) {
      // This block handles the case where the comparison operator is "less than" (x < y). If x is
      // less than y, the state of x is refined. The new state for x is the glb of its current
      // state, the glb of its current state with y's state, and a new upper bound created from y
      // (implying x cannot be greater than or equal to y).
      return environment.putState(
          x,
          environment
              .getState(x)
              .glb(environment.getState(y))
              .glb(new UpperBounds(Collections.singleton(y))));
    }

    if (operator instanceof ComparisonLe) {
      // This block handles the case where the comparison operator is "less than or equals" (x <=
      // y). If x is less than or equal to y, the state of x is refined. The new state for x is the
      // glb of its current state and y's state.
      return environment.putState(x, environment.getState(x).glb(environment.getState(y)));
    }

    if (operator instanceof ComparisonGt) {
      // This block handles the case where the comparison operator is "greater than" (x > y), which
      // is equivalent to y < x. The state of y is refined. The new state for y is the glb of its
      // current state, the glb of its current state with x's state, and a new upper bound created
      // from x (implying y cannot be greater than or equal to x).
      return environment.putState(
          y,
          environment
              .getState(x)
              .glb(environment.getState(y))
              .glb(new UpperBounds(Collections.singleton(x))));
    }

    if (operator instanceof ComparisonGe) {
      // This block handles the case where the comparison operator is "greater than or equals" (x >=
      // y), which is equivalent to y <= x. The state of y is refined. The new state for y is the
      // glb of its current state and x's state.
      return environment.putState(y, environment.getState(x).glb(environment.getState(y)));
    }

    return environment;
  }

  @Override
  public StructuredRepresentation representation() {
    if (isTop()) return new StringRepresentation("{}");
    if (isBottom()) return Lattice.bottomRepresentation();
    return new SetRepresentation(bounds, StringRepresentation::new);
  }

  @Override
  public Iterator<Identifier> iterator() {
    return bounds.iterator();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    UpperBounds other = (UpperBounds) obj;
    return Objects.equals(bounds, other.bounds) && isTop == other.isTop;
  }

  @Override
  public int hashCode() {
    return Objects.hash(bounds, isTop);
  }

  /**
   * Checks if this bounds contains a specified identifier of a program variable.
   *
   * @param id the identifier to check
   * @return {@code true} if this bounds contains the specified identifier; otherwise, {@code
   *     false}.
   */
  public boolean contains(Identifier id) {
    return bounds.contains(id);
  }

  /**
   * Adds the specified identifier of a program variable in the bounds.
   *
   * @param id the identifier to add in the bounds.
   * @return the updated bounds.
   */
  public UpperBounds add(Identifier id) {
    var res = new HashSet<>(bounds);
    res.add(id);
    return new UpperBounds(res);
  }
}
