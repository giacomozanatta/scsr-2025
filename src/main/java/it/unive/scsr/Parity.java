package it.unive.scsr;

import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.nonrelational.value.BaseNonRelationalValueDomain;
import it.unive.lisa.util.representation.StringRepresentation;
import it.unive.lisa.util.representation.StructuredRepresentation;

public class Parity implements BaseNonRelationalValueDomain<Parity> {

    // IMPLEMENTATION NOTE:
    // the code below is outside of the scope of the course. You can uncomment
    // it to get your code to compile. Be aware that the code is written
    // expecting that you have constants for identifying top, bottom, even and
    // odd elements as we saw for the sign domain: if you name them differently,
    // change also the code below to make it work by just using the name of your
    // choice. If you use methods instead of constants, change == with the
    // invocation of the corresponding method

    private static final Parity TOP = new Parity("TOP");
    private static final Parity EVEN = new Parity("EVEN");
    private static final Parity ODD = new Parity("ODD");
    private static final Parity BOTTOM = new Parity("BOTTOM");

    private final String parity;

    public Parity() {
        this("TOP");
    }

    public Parity(
            String parity) {
        this.parity = parity;
    }

    @Override
    public StructuredRepresentation representation() {
        if (this == TOP)
            return Lattice.topRepresentation();
        if (this == BOTTOM)
            return Lattice.bottomRepresentation();
        if (this == EVEN)
            return new StringRepresentation("EVEN");
        if (this == ODD)
            return new StringRepresentation("ODD");

        return null;
    }

    @Override
    public int hashCode() {
        return parity.hashCode();
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
        Parity other = (Parity) obj;
        if (parity != other.parity)
            return false;
        return true;
    }

    @Override
    public Parity lubAux(Parity other) throws SemanticException {
        // By documentation of this method, both "this" and "other" are neither bottom nor top
        // So, the lup is always the top element
        return TOP;
    }

    @Override
    public boolean lessOrEqualAux(Parity other) throws SemanticException {
        // By documentation of this method, both "this" and "other" are neither bottom nor top
        // So they're surely not comparable
        return false;
    }

    @Override
    public Parity top() {
        // Used to return the top element of this lattice
        return TOP;
    }

    @Override
    public Parity bottom() {
        // Used to return the bottom element of this lattice
        return BOTTOM;
    }
}