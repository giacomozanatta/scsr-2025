package it.unive.scsr.pentagons;

import it.unive.lisa.analysis.lattices.Satisfiability;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.scsr.UpperBounds;
import it.unive.scsr.utils.BiSatisfiabilityDispatcher;

import java.util.function.BiFunction;

public class BinarySatisfiability extends BiSatisfiabilityDispatcher<Identifier> {

  private final ValueEnvironment<UpperBounds> upperbounds;

  public BinarySatisfiability(ValueEnvironment<UpperBounds> upperbounds) {
    this.upperbounds = upperbounds;
  }

  @Override
  public BiFunction<Identifier, Identifier, Satisfiability> buildEqFunction() {
    return (first, second) ->
        strictlyLessThan(first, second) || strictlyLessThan(second, first)
            ? Satisfiability.NOT_SATISFIED
            : Satisfiability.UNKNOWN;
  }

  @Override
  public BiFunction<Identifier, Identifier, Satisfiability> buildNeFunction() {
    return (first, second) ->
        strictlyLessThan(first, second) || strictlyLessThan(second, first)
            ? Satisfiability.SATISFIED
            : Satisfiability.UNKNOWN;
  }

  @Override
  public BiFunction<Identifier, Identifier, Satisfiability> buildGtFunction() {
    return this::greaterThan;
  }

  @Override
  public BiFunction<Identifier, Identifier, Satisfiability> buildLtFunction() {
    return this::lessThan;
  }

  @Override
  public BiFunction<Identifier, Identifier, Satisfiability> buildGeFunction() {
    return this::greaterThan;
  }

  @Override
  public BiFunction<Identifier, Identifier, Satisfiability> buildLeFunction() {
    return this::lessThan;
  }

  private Satisfiability greaterThan(Identifier first, Identifier second) {
    return strictlyLessThan(second, first)
        ? Satisfiability.SATISFIED
        : strictlyLessThan(first, second) ? Satisfiability.NOT_SATISFIED : Satisfiability.UNKNOWN;
  }

  private Satisfiability lessThan(Identifier first, Identifier second) {
    return strictlyLessThan(first, second)
        ? Satisfiability.SATISFIED
        : strictlyLessThan(second, first) ? Satisfiability.NOT_SATISFIED : Satisfiability.UNKNOWN;
  }

  private boolean strictlyLessThan(Identifier first, Identifier second) {
    return upperbounds.getState(first).contains(second);
  }
}
