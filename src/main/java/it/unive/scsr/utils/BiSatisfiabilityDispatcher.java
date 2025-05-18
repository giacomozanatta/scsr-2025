package it.unive.scsr.utils;

import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.lattices.Satisfiability;
import it.unive.lisa.symbolic.value.operator.ComparisonOperator;
import it.unive.lisa.symbolic.value.operator.binary.*;

import java.util.Map;
import java.util.function.BiFunction;

public abstract class BiSatisfiabilityDispatcher<T extends Lattice<T>> {

  private final Map<Class<?>, BiFunction<T, T, Satisfiability>> functionByClass;

  protected BiSatisfiabilityDispatcher() {
    this.functionByClass =
        Map.ofEntries(
            Map.entry(ComparisonEq.class, buildEqFunction()),
            Map.entry(ComparisonNe.class, buildNeFunction()),
            Map.entry(ComparisonGt.class, buildGtFunction()),
            Map.entry(ComparisonLt.class, buildLtFunction()),
            Map.entry(ComparisonGe.class, buildGeFunction()),
            Map.entry(ComparisonLe.class, buildLeFunction()));
  }

  public BiFunction<T, T, Satisfiability> findBy(ComparisonOperator operator) {
    return functionByClass.keySet().stream()
        .filter(k -> k.isAssignableFrom(operator.getClass()))
        .findFirst()
        .map(functionByClass::get)
        .orElse(null);
  }

  public abstract BiFunction<T, T, Satisfiability> buildEqFunction();

  public abstract BiFunction<T, T, Satisfiability> buildNeFunction();

  public abstract BiFunction<T, T, Satisfiability> buildGtFunction();

  public abstract BiFunction<T, T, Satisfiability> buildLtFunction();

  public abstract BiFunction<T, T, Satisfiability> buildGeFunction();

  public abstract BiFunction<T, T, Satisfiability> buildLeFunction();
}
