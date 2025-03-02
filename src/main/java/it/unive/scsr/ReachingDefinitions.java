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
        implements DataflowElement<PossibleDataflowDomain<ReachingDefinitions>, ReachingDefinitions> {

    // This is the definition we want to track. In this case, we want to track all assignments
    private Identifier identifier;
    // Location in the code of the definition we are tracking.
    private final CodeLocation location;

    public ReachingDefinitions(Identifier identifier, CodeLocation location) {
        this.identifier = identifier;
        this.location = location;
    }

    // The empty constructor is required to initialize the domain with empty values
    public ReachingDefinitions() {
        this(null, null);
    }

    @Override
    public Collection<Identifier> getInvolvedIdentifiers() {
        // In this case we only have one identifier
        Set<Identifier> result = new HashSet<>();
        result.add(identifier);
        return result;
    }

    @Override
    public Collection<ReachingDefinitions> gen(Identifier id, ValueExpression expression, ProgramPoint pp, PossibleDataflowDomain<ReachingDefinitions> reachingDefinitionsPossibleDataflowDomain) throws SemanticException {
        // We want to generate a reaching definition element
        // We use the id given by the parameter. To obtain the location, we use the ProgramPoint, that is an instruction
        // being pointed
        ReachingDefinitions def = new ReachingDefinitions(id, pp.getLocation());
        // Then we return it in a set
        Set<ReachingDefinitions> result = new HashSet<>();
        result.add(def);
        return result;
    }

    @Override
    public Collection<ReachingDefinitions> gen(ValueExpression valueExpression, ProgramPoint programPoint, PossibleDataflowDomain<ReachingDefinitions> reachingDefinitionsPossibleDataflowDomain) throws SemanticException {
        // Notice that this overload of gen does not have the identifier. This is because it represents
        // values computed but not assigned to an element. In this case we have no interest in tracking them.
        return new HashSet<>();
    }

    @Override
    public Collection<ReachingDefinitions> kill(Identifier id, ValueExpression expression, ProgramPoint pp, PossibleDataflowDomain<ReachingDefinitions> domain) throws SemanticException {
        // The function returns all the elements that are killed by the statement.
        // We want to kill id in this case. So we loop through all elements in the domain
        // and if the elementc contains id, then we want to add it to the set
        Set<ReachingDefinitions> result = new HashSet<>();
        for (ReachingDefinitions rd : domain.getDataflowElements()) {
            if (rd.getInvolvedIdentifiers().contains(id)) {
                result.add(rd);
            }
        }

        return result;
    }

    @Override
    public Collection<ReachingDefinitions> kill(ValueExpression valueExpression, ProgramPoint programPoint, PossibleDataflowDomain<ReachingDefinitions> reachingDefinitionsPossibleDataflowDomain) throws SemanticException {
        // We have no identifier, we don't kill anything
        return new HashSet<>();
    }

    // How to show the element in the HTML
    @Override
    public StructuredRepresentation representation() {
        return new ListRepresentation(
                new StringRepresentation(identifier),
                new StringRepresentation(location)
        );
    }

    // Needed by LiSA
    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        ReachingDefinitions that = (ReachingDefinitions) o;
        return Objects.equals(identifier, that.identifier) && Objects.equals(location, that.location);
    }

    // Needed by LiSA
    @Override
    public int hashCode() {
        int result = Objects.hashCode(identifier);
        result = 31 * result + Objects.hashCode(location);
        return result;
    }


    // OUT OF SCOPE BELOW

    @Override
    public ReachingDefinitions pushScope(ScopeToken scopeToken) throws SemanticException {
        return this;
    }

    @Override
    public ReachingDefinitions popScope(ScopeToken scopeToken) throws SemanticException {
        return this;
    }
}

