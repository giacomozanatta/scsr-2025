package it.unive.scsr;

import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.lattices.Satisfiability;
import it.unive.lisa.analysis.nonrelational.value.BaseNonRelationalValueDomain;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.symbolic.value.operator.*;
import it.unive.lisa.symbolic.value.operator.binary.*;
import it.unive.lisa.symbolic.value.operator.unary.NumericNegation;
import it.unive.lisa.symbolic.value.operator.unary.UnaryOperator;
import it.unive.lisa.util.numeric.MathNumber;
import it.unive.lisa.util.representation.StringRepresentation;
import it.unive.lisa.util.representation.StructuredRepresentation;

import java.util.Objects;

public class FloatIntervals implements BaseNonRelationalValueDomain<FloatIntervals>, Comparable<FloatIntervals> {
    public final FloatInterval interval;
    public static final FloatIntervals ZERO = new FloatIntervals(FloatInterval.ZERO);
    public static final FloatIntervals TOP = new FloatIntervals(FloatInterval.INFINITY);
    public static final FloatIntervals BOTTOM = new FloatIntervals(null);

    public FloatIntervals(FloatInterval interval){
        this.interval = interval;
    }

    public FloatIntervals(double lower, double upper){
        this(new FloatInterval(lower, upper));
    }

    public FloatIntervals(){this(FloatInterval.INFINITY);}

    @Override
    public FloatIntervals lubAux(FloatIntervals other) throws SemanticException {
        FloatInterval a = this.interval;
        FloatInterval b = other.interval;

        double lA = a.getLow();
        double lB = b.getLow();
        double uA = a.getHigh();
        double uB = b.getHigh();

        double newLower = Math.min(lA, lB);
        double newUpper = Math.max(uA, uB);

        /*if(Double.compare(lA, uA)>0 || Double.compare(lB, uB) > 0)
            return BOTTOM;*/
        if(compareMath(lA, uA)>0 || compareMath(lB, uB) > 0)
            return BOTTOM;
        FloatIntervals newInterval = new FloatIntervals(newLower, newUpper);

       /* if (newLower == Double.NEGATIVE_INFINITY && newUpper==Double.POSITIVE_INFINITY)
            return top();

        return new FloatIntervals(newLower, newUpper);*/
        return (Double.isInfinite(newLower) && newLower < 0 && Double.isInfinite(newUpper) && newUpper > 0)
                ? top()
                : newInterval;
    }




    @Override
    public boolean lessOrEqualAux(FloatIntervals other) throws SemanticException {
        return other.interval.includes(this.interval);
    }

    @Override
    public FloatIntervals top() {
        return TOP;
    }

    @Override
    public boolean isTop() {
        return interval != null && interval.isInfinity();
    }

    @Override
    public boolean isBottom() {
        return interval==null;
    }

    @Override
    public FloatIntervals bottom() {
        return BOTTOM;
    }

    @Override
    public FloatIntervals glbAux(FloatIntervals other) throws SemanticException {
        FloatInterval a = this.interval;
        FloatInterval b = other.interval;

        double lA = a.getLow();
        double lB = b.getLow();
        double uA = a.getHigh();
        double uB = b.getHigh();

        if (compareMath(lA, uA) > 0 || compareMath(lB, uB) > 0)
            return BOTTOM;

        double newLower = Math.max(lA, lB);
        double newUpper = Math.min(uA, uB);

        /*if (newLower == Double.NEGATIVE_INFINITY && newUpper==Double.POSITIVE_INFINITY)
            return top();

        return new FloatIntervals(newLower, newUpper); */

        if (compareMath(newLower, Double.POSITIVE_INFINITY) >= 0 ||
                compareMath(newUpper, Double.NEGATIVE_INFINITY) <= 0 ||
                newLower > newUpper) {
            return bottom();
        }

        FloatIntervals result = new FloatIntervals(newLower, newUpper);
        return result.isTop() ? top() : result;
    }

  /*  @Override
    public FloatIntervals glb(FloatIntervals other) throws SemanticException {
        double newLow = Math.max(this.interval.getLow(), other.interval.getLow());
        double newHigh = Math.min(this.interval.getHigh(), other.interval.getHigh());

        if (newLow > newHigh)
            return bottom();

        return new FloatIntervals(newLow, newHigh);
    }*/

    @Override
    public StructuredRepresentation representation() {
        if(this.isBottom())
            return Lattice.bottomRepresentation();

        return new StringRepresentation("["+this.interval.getLow()+" , "+this.interval.getHigh()+"]");
    }

    @Override
    public int compareTo(FloatIntervals o) {
        if (isBottom())
            return o.isBottom() ? 0 : -1;
        if(isTop())
            return o.isTop() ? 0 : 1;
        if(o.isBottom())
            return 1;
        if(o.isTop())
            return -1;
        return interval.compareTo(o.interval);
    }

    @Override
    public FloatIntervals evalNonNullConstant(Constant constant, ProgramPoint pp, SemanticOracle oracle) throws SemanticException {
        if (constant.getValue() instanceof Float || constant.getValue() instanceof Double) {
            double d = ((Number) constant.getValue()).doubleValue();
            return new FloatIntervals(d, d);
        }
        return top();
    }

    public static double subtract(double a, double b) {
        if (!Double.isNaN(a) && !Double.isNaN(b)) {
            if (Double.isInfinite(a)) {
                if (Double.isFinite(b)) {
                    return a;
                } else {
                    return a == b ? Double.NaN : a;
                }
            } else {
                if (Double.isInfinite(b)) {
                    return -b;
                } else {
                    return a - b;
                }
            }
        } else {
            return Double.NaN;
        }
    }

    public static double add(double a, double b) {
        if (!Double.isNaN(a) && !Double.isNaN(b)) {
            if (Double.isInfinite(a)) {
                if (Double.isFinite(b)) {
                    return a;
                } else {
                    return a == b ? a : Double.NaN;
                }
            } else {
                return Double.isInfinite(b) ? b : a + b;
            }
        } else {
            return Double.NaN;
        }
    }

    public static double multiply(double a, double b) {
        if (!Double.isNaN(a) && !Double.isNaN(b)) {
            if (Double.isInfinite(a)) {
                if (b == 0.0) {
                    return Double.NaN;
                } else {
                    return Math.copySign(Double.POSITIVE_INFINITY, a * b);
                }
            } else if (Double.isInfinite(b)) {
                if (a == 0.0) {
                    return Double.NaN;
                } else {
                    return Math.copySign(Double.POSITIVE_INFINITY, a * b);
                }
            } else {
                return a * b;
            }
        } else {
            return Double.NaN;
        }
    }


    public static double divide(double a, double b) {
        if (!Double.isNaN(a) && !Double.isNaN(b) && b != 0.0 && !(Double.isInfinite(a) && Double.isInfinite(b))) {
            if (a == 0.0) {
                return 0.0;
            } else if (!Double.isInfinite(b)) {
                if (!Double.isInfinite(a)) {
                    return a / b;
                } else {
                    return Math.copySign(Double.POSITIVE_INFINITY, a * b);
                }
            } else {
                return 0.0;
            }
        } else {
            return Double.NaN;
        }
    }

    @Override
    public FloatIntervals evalUnaryExpression(UnaryOperator operator, FloatIntervals arg, ProgramPoint pp, SemanticOracle oracle) throws SemanticException {
        if(operator instanceof NegatableOperator || operator instanceof NumericNegation){
            if(arg.isTop())
                return top();
            else{
                FloatInterval a = arg.interval;
                double lA = a.getLow();
                double uA = a.getHigh();

                return new FloatIntervals(multiply(uA, -1.0), multiply(lA, -1.0));
            }
        }

        return top();
    }

    @Override
    public FloatIntervals evalBinaryExpression(BinaryOperator operator, FloatIntervals left, FloatIntervals right, ProgramPoint pp, SemanticOracle oracle) throws SemanticException {

        if(left.isBottom() || right.isBottom()){
            return bottom();
        }
        FloatInterval a = left.interval;
        FloatInterval b = right.interval;

        if(operator instanceof AdditionOperator){
            double lA = a.getLow();
            double lB = b.getLow();
            double uA = a.getHigh();
            double uB = b.getHigh();

            double newLower = add(lA, lB);
            double newUpper = add(uA, uB);

            return new FloatIntervals(Double.min(newLower, newUpper), Double.max(newLower, newUpper));
        }
        else if(operator instanceof SubtractionOperator){
            double newLow = subtract(a.getLow(), b.getHigh());
            double newHigh = subtract(a.getHigh(), b.getLow());

            return new FloatIntervals(newLow, newHigh);
        }
        else if(operator instanceof MultiplicationOperator){
            double ac = multiply(a.getLow(), b.getLow());
            double ad = multiply(a.getLow(), b.getHigh());
            double bc = multiply(a.getHigh(), b.getLow());
            double bd = multiply(a.getHigh(), b.getHigh());

            double min = Double.min(Double.min(ac, ad), Double.min(bc, bd));
            double max = Double.max(Double.max(ac, ad), Double.max(bc, bd));

            return new FloatIntervals(min, max);

        }
        else if(operator instanceof DivisionOperator){
            double zero = 0.0;
            if(Double.compare(b.getLow(), zero) == 0 && Double.compare(b.getHigh(), zero) == 0){
                return bottom();
            }

            double aLow = a.getLow();
            double aHigh = a.getHigh();
            double bLow = b.getLow();
            double bHigh = b.getHigh();

            double div1 = divide(aLow, bLow);
            double div2 = divide(aLow, bHigh);
            double div3 = divide(aHigh, bLow);
            double div4 = divide(aHigh, bHigh);

            double min = Double.min(Double.min(div1, div2), Double.min(div3, div4));
            double max = Double.max(Double.max(div1, div2), Double.max(div3, div4));

            return new FloatIntervals(min, max);

        }

        return top();
    }


    @Override
    public FloatIntervals wideningAux(FloatIntervals other) throws SemanticException {
        double newLower, newUpper;
        if(compareMath(other.interval.getHigh(), interval.getHigh()) > 0)
            newUpper = Double.POSITIVE_INFINITY;
        else
            newUpper = interval.getHigh();
        if(compareMath(other.interval.getLow(), interval.getLow()) < 0)
            newLower = Double.NEGATIVE_INFINITY;
        else
            newLower = interval.getLow();

        return newLower==Double.NEGATIVE_INFINITY && newUpper==Double.POSITIVE_INFINITY ? top() : new FloatIntervals(newLower, newUpper);
    }

    @Override
    public FloatIntervals narrowingAux(FloatIntervals other) throws SemanticException {
        double newLow, newHigh;
        newHigh = Double.isInfinite(interval.getHigh()) ? other.interval.getHigh() : interval.getHigh();
        newLow = Double.isInfinite(interval.getLow()) ? other.interval.getLow() : interval.getLow();
        return new FloatIntervals(newLow, newHigh);
    }

    @Override
    public int hashCode() {
        return Objects.hash(interval);
    }

    @Override
    public boolean equals(Object obj) {
        if(this==obj)
            return true;
        if(obj==null)
            return false;
        if(getClass() != obj.getClass())
            return false;
        FloatIntervals other = (FloatIntervals) obj;
        return Objects.equals(interval, other.interval);
    }

    /*public FloatIntervals evaluateExpression(ValueExpression expr, ValueEnvironment<FloatIntervals> env, ProgramPoint pp, SemanticOracle oracle) throws SemanticException {
        EvaluationVisitor visitor = new EvaluationVisitor(this);
        Object[] ctx = new Object[] { env, pp, oracle };
        return (FloatIntervals) expr.accept(visitor, ctx);
    }*/

   /* @Override
    public ValueEnvironment<FloatIntervals> assumeBinaryExpression(
            ValueEnvironment<FloatIntervals> environment,
            BinaryOperator operator,
            ValueExpression left,
            ValueExpression right,
            ProgramPoint src,
            ProgramPoint dest,
            SemanticOracle oracle) throws SemanticException {

        // Gestione speciale: assume(b == true) o assume(b == false)
       // if (operator == ComparisonEq.INSTANCE || operator == ComparisonNe.INSTANCE) {
            ValueExpression constExpr = null;
            Identifier idExpr = null;

            if (left instanceof Identifier && right instanceof Constant) {
                idExpr = (Identifier) left;
                constExpr = right;
            } else if (right instanceof Identifier && left instanceof Constant) {
                idExpr = (Identifier) right;
                constExpr = left;
            }

            if (idExpr != null && constExpr instanceof Constant) {
                Constant c = (Constant) constExpr;
                Object value = c.getValue();

                if (value instanceof Boolean) {
                    boolean boolVal = (Boolean) value;
                    // Se è un confronto di disuguaglianza, nega il valore
                    if (operator == ComparisonNe.INSTANCE)
                        boolVal = !boolVal;

                    // In FloatIntervals: true = 1.0, false = 0.0
                    double boolDouble = boolVal ? 1.0 : 0.0;
                    FloatIntervals newInterval = new FloatIntervals(boolDouble, boolDouble);
                    return environment.putState(idExpr, newInterval);
                }
            }
        //}

        Identifier id;
        FloatIntervals eval;
        boolean rightIsExpr;

        if (left instanceof Identifier) {
            eval = eval(right, environment, src, oracle);
            id = (Identifier) left;
            rightIsExpr = true;
        } else if (right instanceof Identifier) {
            eval = eval(left, environment, src, oracle);
            id = (Identifier) right;
            rightIsExpr = false;
        } else {
            return environment;
        }

        FloatIntervals starting = environment.getState(id);
        if (eval.isBottom() || starting.isBottom())
            return environment.bottom();

        boolean lowIsMinusInfinity = eval.interval.lowIsMinusInfinity();

        // Per confronti floating point: usare nextAfter per i "±1"
        double low = eval.interval.getLow();
        double high = eval.interval.getHigh();
        double nextUp = Math.nextAfter(low, Double.POSITIVE_INFINITY);
        double nextDown = Math.nextAfter(high, Double.NEGATIVE_INFINITY);

        FloatIntervals low_inf = new FloatIntervals(low, Double.POSITIVE_INFINITY);
        FloatIntervals lowp1_inf = new FloatIntervals(nextUp, Double.POSITIVE_INFINITY);
        FloatIntervals inf_high = new FloatIntervals(Double.NEGATIVE_INFINITY, high);
        FloatIntervals inf_highm1 = new FloatIntervals(Double.NEGATIVE_INFINITY, nextDown);

        FloatIntervals update = null;

        if (operator == ComparisonEq.INSTANCE) {
            update = eval;
        } else if (operator == ComparisonGe.INSTANCE) {
            if (rightIsExpr)
                update = lowIsMinusInfinity ? null : starting.glb(low_inf);
            else
                update = starting.glb(inf_high);
        } else if (operator == ComparisonGt.INSTANCE) {
            if (rightIsExpr)
                update = lowIsMinusInfinity ? null : starting.glb(lowp1_inf);
            else
                update = lowIsMinusInfinity ? eval : starting.glb(inf_highm1);
        } else if (operator == ComparisonLe.INSTANCE) {
            if (rightIsExpr)
                update = starting.glb(inf_high);
            else
                update = lowIsMinusInfinity ? null : starting.glb(low_inf);
        } else if (operator == ComparisonLt.INSTANCE) {
            if (rightIsExpr)
                update = lowIsMinusInfinity ? eval : starting.glb(inf_highm1);
            else
                update = lowIsMinusInfinity ? null : starting.glb(lowp1_inf);
        }

        if (update == null)
            return environment;
        else if (update.isBottom())
            return environment.bottom();
        else
            return environment.putState(id, update);
    }



    @Override
    public Satisfiability satisfiesBinaryExpression(
            BinaryOperator operator,
            FloatIntervals left,
            FloatIntervals right,
            ProgramPoint pp,
            SemanticOracle oracle) {
        if (left.isTop() || right.isTop())
            return Satisfiability.UNKNOWN;

        if (operator == ComparisonEq.INSTANCE) {
            FloatIntervals glb = null;
            try {
                glb = left.glb(right);
            } catch (SemanticException e) {
                return Satisfiability.UNKNOWN;
            }

            if (glb.isBottom())
                return Satisfiability.NOT_SATISFIED;
            else if (left.interval.isSingleton() && left.equals(right))
                return Satisfiability.SATISFIED;
            return Satisfiability.UNKNOWN;
        } else if (operator == ComparisonGe.INSTANCE)
            return satisfiesBinaryExpression(ComparisonLe.INSTANCE, right, left, pp, oracle);
        else if (operator == ComparisonGt.INSTANCE)
            return satisfiesBinaryExpression(ComparisonLt.INSTANCE, right, left, pp, oracle);
        else if (operator == ComparisonLe.INSTANCE) {
            FloatIntervals glb = null;
            try {
                glb = left.glb(right);
            } catch (SemanticException e) {
                return Satisfiability.UNKNOWN;
            }

            if (glb.isBottom())
                return Satisfiability.fromBoolean(Double.compare(left.interval.getHigh(), right.interval.getLow()) <= 0);
// we might have a singleton as glb if the two intervals share a
// bound
            if (glb.interval.isSingleton() && Double.compare(left.interval.getHigh(), right.interval.getLow()) == 0)
                return Satisfiability.SATISFIED;
            return Satisfiability.UNKNOWN;
        } else if (operator == ComparisonLt.INSTANCE) {
            FloatIntervals glb = null;
            try {
                glb = left.glb(right);
            } catch (SemanticException e) {
                return Satisfiability.UNKNOWN;
            }

            if (glb.isBottom())
                return Satisfiability.fromBoolean(Double.compare(left.interval.getHigh(), right.interval.getLow()) < 0);
            return Satisfiability.UNKNOWN;
        } else if (operator == ComparisonNe.INSTANCE) {
            FloatIntervals glb = null;
            try {
                glb = left.glb(right);
            } catch (SemanticException e) {
                return Satisfiability.UNKNOWN;
            }
            if (glb.isBottom())
                return Satisfiability.SATISFIED;
            return Satisfiability.UNKNOWN;
        }
        return Satisfiability.UNKNOWN;
    }

*/








    @Override
    public ValueEnvironment<FloatIntervals> assumeBinaryExpression(ValueEnvironment<FloatIntervals> environment,
                                                                   BinaryOperator operator, ValueExpression left, ValueExpression right, ProgramPoint src, ProgramPoint dest,
                                                                   SemanticOracle oracle) throws SemanticException {
        Identifier id;
        FloatIntervals eval;
        boolean rightIsExpr;
        if (left instanceof Identifier) {
            eval = eval(right, environment, src, oracle);
            id = (Identifier) left;
            rightIsExpr = true;
        } else if (right instanceof Identifier) {
            eval = eval(left, environment, src, oracle);
            id = (Identifier) right;
            rightIsExpr = false;
        } else
            return environment;

        FloatIntervals starting = environment.getState(id);
        if (eval.isBottom() || starting.isBottom())
            return environment.bottom();

        boolean lowIsMinusInfinity = eval.interval.lowIsMinusInfinity();
        FloatIntervals low_inf = new FloatIntervals(eval.interval.getLow(), Double.POSITIVE_INFINITY);
        FloatIntervals lowp1_inf = new FloatIntervals(add(eval.interval.getLow(), 1.0), Double.POSITIVE_INFINITY);
        FloatIntervals inf_high = new FloatIntervals(Double.NEGATIVE_INFINITY, eval.interval.getHigh());
        FloatIntervals inf_highm1 = new FloatIntervals(Double.NEGATIVE_INFINITY, subtract(eval.interval.getHigh(), 1.0));

        FloatIntervals update = null;
        //if (operator == ComparisonEq.INSTANCE)
        if (operator instanceof ComparisonEq)
                update = eval;
        else if (operator instanceof ComparisonGe)//else if (operator == ComparisonGe.INSTANCE)
            if (rightIsExpr)
                update = lowIsMinusInfinity ? null : starting.glb(low_inf);
            else
                update = starting.glb(inf_high);
        else if (operator instanceof ComparisonGt) //else if (operator == ComparisonGt.INSTANCE)
            if (rightIsExpr)
                update = lowIsMinusInfinity ? null : starting.glb(lowp1_inf);
            else
                update = lowIsMinusInfinity ? eval : starting.glb(inf_highm1);
        else if (operator instanceof ComparisonLe) //else if (operator == ComparisonLe.INSTANCE)
            if (rightIsExpr)
                update = starting.glb(inf_high);
            else
                update = lowIsMinusInfinity ? null : starting.glb(low_inf);
        else if (operator instanceof ComparisonLt) //else if (operator == ComparisonLt.INSTANCE)
            if (rightIsExpr)
                update = lowIsMinusInfinity ? eval : starting.glb(inf_highm1);
            else
                update = lowIsMinusInfinity ? null : starting.glb(lowp1_inf);

        if (update == null)
            return environment;
        else if (update.isBottom())
            return environment.bottom();
        else
            return environment.putState(id, update);	}

    @Override
    public Satisfiability satisfiesBinaryExpression(
            BinaryOperator operator,
            FloatIntervals left,
            FloatIntervals right,
            ProgramPoint pp,
            SemanticOracle oracle) {
        if (left.isTop() || right.isTop())
            return Satisfiability.UNKNOWN;

        if (operator == ComparisonEq.INSTANCE) {
            FloatIntervals glb = null;
            try {
                glb = left.glb(right);
            } catch (SemanticException e) {
                return Satisfiability.UNKNOWN;
            }

            if (glb.isBottom())
                return Satisfiability.NOT_SATISFIED;
            else if (left.interval.isSingleton() && left.equals(right))
                return Satisfiability.SATISFIED;
            return Satisfiability.UNKNOWN;
        } else if (operator == ComparisonGe.INSTANCE)
            return satisfiesBinaryExpression(ComparisonLe.INSTANCE, right, left, pp, oracle);
        else if (operator == ComparisonGt.INSTANCE)
            return satisfiesBinaryExpression(ComparisonLt.INSTANCE, right, left, pp, oracle);
        else if (operator == ComparisonLe.INSTANCE) {
            FloatIntervals glb = null;
            try {
                glb = left.glb(right);
            } catch (SemanticException e) {
                return Satisfiability.UNKNOWN;
            }

            if (glb.isBottom())
                return Satisfiability.fromBoolean(Double.compare(left.interval.getHigh(), right.interval.getLow()) <= 0);
// we might have a singleton as glb if the two intervals share a
// bound
            if (glb.interval.isSingleton() && Double.compare(left.interval.getHigh(), right.interval.getLow()) == 0)
                return Satisfiability.SATISFIED;
            return Satisfiability.UNKNOWN;
        } else if (operator == ComparisonLt.INSTANCE) {
            FloatIntervals glb = null;
            try {
                glb = left.glb(right);
            } catch (SemanticException e) {
                return Satisfiability.UNKNOWN;
            }

            if (glb.isBottom())
                return Satisfiability.fromBoolean(Double.compare(left.interval.getHigh(), right.interval.getLow()) < 0);
            return Satisfiability.UNKNOWN;
        } else if (operator == ComparisonNe.INSTANCE) {
            FloatIntervals glb = null;
            try {
                glb = left.glb(right);
            } catch (SemanticException e) {
                return Satisfiability.UNKNOWN;
            }
            if (glb.isBottom())
                return Satisfiability.SATISFIED;
            return Satisfiability.UNKNOWN;
        }
        return Satisfiability.UNKNOWN;
    }



    public static int compareMath(double a, double b) {
        // Trattamento speciale per NaN
        boolean aNaN = Double.isNaN(a);
        boolean bNaN = Double.isNaN(b);

        if (aNaN && bNaN)
            return 0;
        if (aNaN)
            return -1; // oppure 1 se vuoi trattarlo come maggiore
        if (bNaN)
            return 1;

        // Stesso segno? Confronto normale
        int signCompare = Double.compare(Math.signum(a), Math.signum(b));
        if (signCompare != 0)
            return signCompare;

        // Trattamento speciale infiniti
        if (Double.isInfinite(a) || Double.isInfinite(b)) {
            if (a == b)
                return 0;
            return a < b ? -1 : 1;
        }

        // Valori finiti e definiti: confronto numerico diretto
        return Double.compare(a, b);
    }



}
