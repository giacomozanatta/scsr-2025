package it.unive.scsr.notes;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.taint.BaseTaint;
import it.unive.lisa.util.representation.StructuredRepresentation;

public class Taint extends BaseTaint<Taint> {
    /*
    *      TOP
    *    /     \
    * TAINT   CLEAN
    *    \     /
    *    BOTTOM
    *
    *
    *
    *  TOP
    *   |
    * TAINT
    *   |
    * CLEAN
    *
    * TOP + CLEAN = TOP
    * TOP + TAINT = TOP
    *
    * TAINT + CLEAN = TOP: we definitely know that a value is TAINT and
    * the other value is CLEAN, but we don't know if the result is TAINT or CLEAN
    *
    *
    */


    private Taint TAINT = new Taint(true);
    private Taint CLEAN = new Taint(false);
    private Taint BOTTOM = new Taint(null);
    private Taint TOP = new Taint(null);

    private Boolean isTainted;


    public Taint(Boolean isTainted) {
        this.isTainted = isTainted;
    }

    @Override
    protected Taint tainted() {
        return TAINT;
    }

    @Override
    protected Taint clean() {
        return CLEAN;
    }

    @Override
    public boolean isAlwaysTainted() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isPossiblyTainted() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Taint lubAux(Taint other) throws SemanticException {
        return new Taint(true);
    }

    @Override
    public boolean lessOrEqualAux(Taint other) throws SemanticException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Taint top() {
        return TOP;
    }

    @Override
    public Taint bottom() {
        return BOTTOM;
    }

    @Override
    public StructuredRepresentation representation() {
        return null;
    }
}
