package it.unive.scsr;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.dataflow.DataflowElement;
import it.unive.lisa.analysis.dataflow.PossibleDataflowDomain;
import it.unive.lisa.analysis.dataflow.ReachingDefinitions;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.util.representation.ListRepresentation;
import it.unive.lisa.util.representation.StringRepresentation;
import it.unive.lisa.util.representation.StructuredRepresentation;


public class codeAlong_ReachingDefinitions implements DataflowElement<PossibleDataflowDomain<codeAlong_ReachingDefinitions>, codeAlong_ReachingDefinitions> {
    private final Identifier identifier;
    private final CodeLocation location;
    public codeAlong_ReachingDefinitions(Identifier identifier, CodeLocation location){
        this.identifier = identifier;
        this.location = location;
    }
    @Override
    public Collection<Identifier> getInvolvedIdentifiers() {
        Set<Identifier> result = new HashSet<>();
        result.add(identifier);
        return result;
    }

    @Override
    public Collection<codeAlong_ReachingDefinitions> gen(Identifier identifier, ValueExpression valueExpression, ProgramPoint programPoint, PossibleDataflowDomain<codeAlong_ReachingDefinitions> codeAlongReachingDefinitionsPossibleDataflowDomain) throws SemanticException {
        codeAlong_ReachingDefinitions rd = new codeAlong_ReachingDefinitions(identifier, programPoint.getLocation());
        Set<codeAlong_ReachingDefinitions> result = new HashSet<>();
        result.add(rd);
        return result;
    }

    @Override
    public Collection<codeAlong_ReachingDefinitions> gen(ValueExpression valueExpression, ProgramPoint programPoint, PossibleDataflowDomain<codeAlong_ReachingDefinitions> codeAlongReachingDefinitionsPossibleDataflowDomain) throws SemanticException {
        Set<codeAlong_ReachingDefinitions> result = new HashSet<>();
        return result;
    }

    @Override
    public Collection<codeAlong_ReachingDefinitions> kill(Identifier identifier, ValueExpression valueExpression, ProgramPoint programPoint, PossibleDataflowDomain<codeAlong_ReachingDefinitions> domain) throws SemanticException {
        Set<codeAlong_ReachingDefinitions> result = new HashSet<>();
        for(codeAlong_ReachingDefinitions rd : domain.getDataflowElements()){
            if(rd.getInvolvedIdentifiers().contains(identifier)){
                result.add(rd);
            }
        }
        return result;
    }

    @Override
    public Collection<codeAlong_ReachingDefinitions> kill(ValueExpression valueExpression, ProgramPoint programPoint, PossibleDataflowDomain<codeAlong_ReachingDefinitions> domain) throws SemanticException {
        Set<codeAlong_ReachingDefinitions> result = new HashSet<>();
        return result;
    }

    @Override
    public codeAlong_ReachingDefinitions pushScope(ScopeToken scopeToken) throws SemanticException {
        return this;
    }

    @Override
    public codeAlong_ReachingDefinitions popScope(ScopeToken scopeToken) throws SemanticException {
        return this;
    }

    @Override
    public StructuredRepresentation representation() {
        return new ListRepresentation(
          new StringRepresentation(identifier),
          new StringRepresentation(location)
        );
    }
}
