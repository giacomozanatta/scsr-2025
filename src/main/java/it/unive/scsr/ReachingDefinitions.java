package it.unive.scsr;

import java.util.*;

import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.dataflow.DataflowElement;
import it.unive.lisa.analysis.dataflow.PossibleDataflowDomain;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.util.representation.ListRepresentation;
import it.unive.lisa.util.representation.StringRepresentation;
import it.unive.lisa.util.representation.StructuredRepresentation;

public class ReachingDefinitions
        // instances of this class are dataflow elements such that:
        // - their state (fields) hold the information contained into a single
        // element
        // - they provide gen and kill functions that are specific to the
        // analysis that we are executing
        implements DataflowElement<PossibleDataflowDomain<ReachingDefinitions>,ReachingDefinitions> {

    private final Identifier identifier;
    private final CodeLocation location;

    public ReachingDefinitions(Identifier identifier, CodeLocation location) {
        this.identifier = identifier;
        this.location = location;
    }

    public ReachingDefinitions() {
        this(null, null);
    }
    @Override
    public Collection<Identifier> getInvolvedIdentifiers() {
        Collection<Identifier> result = new HashSet<>();
        result.add(identifier);
        return result;
    }

    @Override
    public Collection<ReachingDefinitions> gen(Identifier id, ValueExpression expression, ProgramPoint pp, PossibleDataflowDomain<ReachingDefinitions> domain) throws SemanticException {
        ReachingDefinitions def = new ReachingDefinitions(id, pp.getLocation());
        Collection<ReachingDefinitions> result = new HashSet<>();
        result.add(def);
        return result;
    }

    @Override
    public Collection<ReachingDefinitions> gen(ValueExpression expression, ProgramPoint pp, PossibleDataflowDomain<ReachingDefinitions> domain) throws SemanticException {
        Collection<ReachingDefinitions> result = new HashSet<>();
        return result;
    }

    @Override
    public Collection<ReachingDefinitions> kill(Identifier id, ValueExpression expression, ProgramPoint pp, PossibleDataflowDomain<ReachingDefinitions> domain) throws SemanticException {
        Collection<ReachingDefinitions> result = new HashSet<>();
        for (ReachingDefinitions rd : domain.getDataflowElements()) {
            if (rd.getInvolvedIdentifiers().contains(id)) {
                result.add(rd);
            }
        }
        return result;
    }

    @Override
    public Collection<ReachingDefinitions> kill(ValueExpression expression, ProgramPoint pp, PossibleDataflowDomain<ReachingDefinitions> domain) throws SemanticException {
        Set<ReachingDefinitions> result = new HashSet<>();
        return result;
    }

    @Override
    public ReachingDefinitions pushScope(ScopeToken token) throws SemanticException {
        return this;
    }

    @Override
    public ReachingDefinitions popScope(ScopeToken token) throws SemanticException {
        return this;
    }

    @Override
    public StructuredRepresentation representation() {
        return new ListRepresentation(
                new StringRepresentation(identifier),
                new StringRepresentation(location));
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        ReachingDefinitions that = (ReachingDefinitions) o;
        return Objects.equals(identifier, that.identifier) && Objects.equals(location, that.location);
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(identifier);
        result = 31 * result + Objects.hashCode(location);
        return result;
    }
}