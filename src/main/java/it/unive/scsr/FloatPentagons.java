package it.unive.scsr;

import it.unive.lisa.analysis.*;
import it.unive.lisa.analysis.lattices.Satisfiability;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.symbolic.value.operator.SubtractionOperator;
import it.unive.lisa.symbolic.value.operator.binary.BinaryOperator;
import it.unive.lisa.util.numeric.IntInterval;
import it.unive.lisa.util.numeric.MathNumber;
import it.unive.lisa.util.representation.MapRepresentation;
import it.unive.lisa.util.representation.StringRepresentation;
import it.unive.lisa.util.representation.StructuredRepresentation;
import org.apache.commons.collections4.CollectionUtils;

import java.util.*;
import java.util.function.Predicate;

public class FloatPentagons implements ValueDomain<FloatPentagons>, BaseLattice<FloatPentagons>
{


    // a value environment is basically a mapping between variables (identifiers) and the corresponding vale state
    ValueEnvironment<UpperBounds> upperbounds;
    ValueEnvironment<FloatIntervals> intervals;


    public FloatPentagons() {
        this.upperbounds = new ValueEnvironment<UpperBounds>(new UpperBounds(true)).top();
        this.intervals = new ValueEnvironment<FloatIntervals>(new FloatIntervals()).top();
    }

    public FloatPentagons(ValueEnvironment<UpperBounds> upperbounds, ValueEnvironment<FloatIntervals> intervals) {
        this.upperbounds = upperbounds;
        this.intervals = intervals;
    }


    @Override
    public FloatPentagons top() {

        return new FloatPentagons(upperbounds.top(),intervals.top());
    }

    @Override
    public boolean isTop() {
        return upperbounds.isTop() && intervals.isTop();
    }


    @Override
    public FloatPentagons bottom() {
        return new FloatPentagons(upperbounds.bottom(),intervals.bottom());
    }

    @Override
    public boolean isBottom() {
        return upperbounds.isBottom() && intervals.isBottom();
    }

    @Override
    public FloatPentagons smallStepSemantics(ValueExpression expression, ProgramPoint pp, SemanticOracle oracle)
            throws SemanticException {
        return new FloatPentagons(upperbounds.smallStepSemantics(expression, pp, oracle),intervals.smallStepSemantics(expression, pp, oracle));
    }

    @Override
    public FloatPentagons assume(ValueExpression expression, ProgramPoint src, ProgramPoint dest, SemanticOracle oracle)
            throws SemanticException {
        return new FloatPentagons(upperbounds.assume(expression, src, dest, oracle), intervals.assume(expression, src, dest, oracle));
    }


    @Override
    public FloatPentagons wideningAux(
            FloatPentagons other)
            throws SemanticException {
        return new FloatPentagons(upperbounds.wideningAux(other.upperbounds), intervals.widening(other.intervals));

    }


    @Override
    public FloatPentagons lubAux(
            FloatPentagons other)
            throws SemanticException {
        ValueEnvironment<UpperBounds> newBounds = upperbounds.lub(other.upperbounds);
        for (Map.Entry<Identifier, UpperBounds> entry : upperbounds) {
            Set<Identifier> closure = new HashSet<>();
            for (Identifier bound : entry.getValue()) {
                FloatIntervals intervalState = other.intervals.getState(entry.getKey());
                FloatIntervals boundIntervalState = other.intervals.getState(bound);
                if (!intervalState.isBottom() && !boundIntervalState.isBottom() && FloatIntervals.compareMath(intervalState.interval.getHigh(), boundIntervalState.interval.getLow()) < 0)
                    closure.add(bound);
            }
            if (!closure.isEmpty())
                // glb is the union
                newBounds = newBounds.putState(entry.getKey(),
                        newBounds.getState(entry.getKey()).glb(new UpperBounds(closure)));
        }

        for (Map.Entry<Identifier, UpperBounds> entry : other.upperbounds) {
            Set<Identifier> closure = new HashSet<>();
            for (Identifier bound : entry.getValue())
                if (FloatIntervals.compareMath(intervals.getState(entry.getKey()).interval.getHigh(), intervals.getState(bound).interval.getLow()) < 0)
                    closure.add(bound);
            if (!closure.isEmpty())
                // glb is the union
                newBounds = newBounds.putState(entry.getKey(),
                        newBounds.getState(entry.getKey()).glb(new UpperBounds(closure)));
        }

        return new FloatPentagons(newBounds, intervals.lub(other.intervals));
    }

    @Override
    public boolean lessOrEqualAux(FloatPentagons other) throws SemanticException {

        if(!this.intervals.lessOrEqual(other.intervals)) {
            return false;
        }

        for(Map.Entry<Identifier, UpperBounds> entry : other.upperbounds) {
            for(Identifier bound : entry.getValue()) {
                if(!(this.upperbounds.getState(entry.getKey()).contains(bound)
                        || FloatIntervals.compareMath(this.intervals.getState(entry.getKey()).interval.getHigh(), this.intervals.getState(bound).interval.getLow()) < 0) ){
                    return false;
                }

            }

        }
        return true;
    }

    @Override
    public FloatPentagons assign(Identifier id, ValueExpression expression, ProgramPoint pp, SemanticOracle oracle)
            throws SemanticException {

        ValueEnvironment<UpperBounds> newBounds = upperbounds.assign(id, expression, pp, oracle);
        ValueEnvironment<FloatIntervals> newIntervals = intervals.assign(id, expression, pp, oracle);


        if(expression instanceof BinaryExpression) {
            BinaryExpression be = (BinaryExpression) expression;
            BinaryOperator op = be.getOperator();

            if(op instanceof SubtractionOperator) {
                if(be.getLeft() instanceof Identifier) {
                    Identifier x = (Identifier) be.getLeft();

                    if(be.getRight() instanceof Identifier) {
                        // r = x - y
                        Identifier y = (Identifier) be.getRight();
                        if(newBounds.getState(y).contains(x)) {
                            newIntervals = newIntervals.putState(id, newIntervals.getState(id)
                                    .glb(new FloatIntervals(1.0, Double.POSITIVE_INFINITY)));
                        }
                    } else if (be.getRight() instanceof Constant)
                        // r = x + 2 (where 2 is the constant)
                        newBounds = newBounds.putState(id, upperbounds.getState(x).add(x));
                }
            }

        }

        return new FloatPentagons(newBounds,newIntervals).closure();
    }


    @Override
    public FloatPentagons forgetIdentifier(
            Identifier id)
            throws SemanticException {
        return new FloatPentagons(
                upperbounds.forgetIdentifier(id), intervals.forgetIdentifier(id));
    }

    @Override
    public FloatPentagons forgetIdentifiersIf(
            Predicate<Identifier> test)
            throws SemanticException {
        return new FloatPentagons(
                upperbounds.forgetIdentifiersIf(test),
                intervals.forgetIdentifiersIf(test));
    }

    @Override
    public Satisfiability satisfies(
            ValueExpression expression,
            ProgramPoint pp,
            SemanticOracle oracle)
            throws SemanticException {
        return intervals.satisfies(expression, pp, oracle).glb(upperbounds.satisfies(expression, pp, oracle));
    }

    @Override
    public FloatPentagons pushScope(
            ScopeToken token)
            throws SemanticException {
        return new FloatPentagons(upperbounds.pushScope(token), intervals.pushScope(token));
    }

    @Override
    public FloatPentagons popScope(
            ScopeToken token)
            throws SemanticException {
        return new FloatPentagons(upperbounds.popScope(token), intervals.popScope(token));
    }

    @Override
    public StructuredRepresentation representation() {
        if (isTop())
            return Lattice.topRepresentation();
        if (isBottom())
            return Lattice.bottomRepresentation();
        Map<StructuredRepresentation, StructuredRepresentation> mapping = new HashMap<>();
        for (Identifier id : CollectionUtils.union(intervals.getKeys(), upperbounds.getKeys()))
            mapping.put(new StringRepresentation(id),
                    new StringRepresentation(intervals.getState(id).representation() + ", " +
                            upperbounds.getState(id).representation()));
        return new MapRepresentation(mapping);
    }


    @Override
    public int hashCode() {
        return Objects.hash(intervals, upperbounds);
    }

    @Override
    public boolean equals(
            Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        FloatPentagons other = (FloatPentagons) obj;
        return Objects.equals(intervals, other.intervals) && Objects.equals(upperbounds, other.upperbounds);
    }

    @Override
    public String toString() {
        return representation().toString();
    }

    @Override
    public boolean knowsIdentifier(
            Identifier id) {
        return intervals.knowsIdentifier(id) || upperbounds.knowsIdentifier(id);
    }

	/*private Pentagons closure() throws SemanticException {
		ValueEnvironment<UpperBounds> newBounds = new ValueEnvironment<UpperBounds>(upperbounds.lattice, upperbounds.getMap());

		for (Identifier id1 : intervals.getKeys()) {
			Set<Identifier> closure = new HashSet<>();
			for (Identifier id2 : intervals.getKeys())
				if (!id1.equals(id2))
					if (intervals.getState(id1).interval.getHigh()
							.compareTo(intervals.getState(id2).interval.getLow()) < 0)
						closure.add(id2);
			if (!closure.isEmpty())
				// glb is the union
				newBounds = newBounds.putState(id1,
						newBounds.getState(id1).glb(new UpperBounds(closure)));
		}

		return new Pentagons(newBounds, intervals);
	}*/

    private FloatPentagons closure() throws SemanticException {
        ValueEnvironment<UpperBounds> newBounds = new ValueEnvironment<>(upperbounds.lattice, upperbounds.getMap());

        for (Identifier id1 : intervals.getKeys()) {
            Set<Identifier> closure = new HashSet<>();
            for (Identifier id2 : intervals.getKeys()) {
                if (!id1.equals(id2)) {
                    FloatInterval interval1 = intervals.getState(id1).interval;
                    FloatInterval interval2 = intervals.getState(id2).interval;

                    // Check for nulls
                    if (interval1 != null && interval2 != null) {
                        if (FloatIntervals.compareMath(interval1.getHigh(), interval2.getLow()) < 0) {
                            closure.add(id2);
                        }
                    }
                }
            }
            if (!closure.isEmpty()) {
                UpperBounds existing = newBounds.getState(id1);
                UpperBounds newUB = existing != null ? existing.glb(new UpperBounds(closure)) : new UpperBounds(closure);
                newBounds = newBounds.putState(id1, newUB);
            }
        }

        return new FloatPentagons(newBounds, intervals);
    }


    public ValueEnvironment<FloatIntervals> getIntervals() {
        return intervals;
    }
}
