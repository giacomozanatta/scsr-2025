package it.unive.scsr;

/**
 * Represents the abstract domain of parity analysis.
 * It tracks whether integer values are EVEN, ODD, UNKNOWN (TOP), or UNREACHABLE (BOTTOM).
 */
public class Parity {

    public static final Parity EVEN = new Parity("EVEN");
    public static final Parity ODD = new Parity("ODD");
    public static final Parity TOP = new Parity("TOP");   // Unknown value
    public static final Parity BOTTOM = new Parity("BOTTOM"); // Unreachable state

    private final String value;

    private Parity(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }

    // ------------------- Lattice Operations -------------------

    /**
     * Least upper bound (LUB) operation for the lattice.
     */
    public Parity lub(Parity other) {
        if (this == BOTTOM) return other;
        if (other == BOTTOM) return this;
        if (this == other) return this;
        return TOP; // If different (EVEN vs ODD), result is unknown (TOP)
    }

    // ------------------- Arithmetic Operations -------------------

    /**
     * Computes the parity of the sum (x + y).
     */
    public Parity plus(Parity other) {
        if (this == BOTTOM || other == BOTTOM) return BOTTOM;
        if (this == TOP || other == TOP) return TOP;
        if (this == EVEN && other == EVEN) return EVEN;
        if (this == ODD && other == ODD) return EVEN;
        return ODD; // Even + Odd or Odd + Even
    }

    /**
     * Computes the parity of the subtraction (x - y).
     * Same rules as addition.
     */
    public Parity minus(Parity other) {
        return plus(other); // Subtraction follows the same rules as addition
    }

    /**
     * Computes the parity of the multiplication (x * y).
     */
    public Parity times(Parity other) {
        if (this == BOTTOM || other == BOTTOM) return BOTTOM;
        if (this == TOP || other == TOP) return TOP;
        if (this == EVEN || other == EVEN) return EVEN; // Anything multiplied by EVEN is EVEN
        return ODD; // ODD * ODD = ODD
    }

    /**
     * Computes the parity of the division (x / y).
     * Must handle special cases where division is uncertain.
     */
    public Parity div(Parity other) {
        if (this == BOTTOM || other == BOTTOM) return BOTTOM;
        if (other == EVEN) return TOP; // Dividing by an even number is uncertain
        if (this == EVEN) return EVEN; // Even / Odd may still be Even
        return TOP; // Odd / Odd can be Even or Odd, so it's unknown
    }

    /**
     * Computes the parity of negation (-x).
     * Negation does not change parity.
     */
    public Parity negate() {
        return this; // -EVEN = EVEN, -ODD = ODD
    }

    // ------------------- Equality and Hashing -------------------

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Parity)) return false;
        return this.value.equals(((Parity) obj).value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    // ------------------- Static Methods for Constant Tracking -------------------

    /**
     * Determines the parity of a given integer constant.
     */
    public static Parity fromConstant(int n) {
        return (n % 2 == 0) ? EVEN : ODD;
    }
}
