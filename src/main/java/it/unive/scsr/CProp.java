package it.unive.scsr;

import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.dataflow.DataflowElement;
import it.unive.lisa.analysis.dataflow.DefiniteDataflowDomain;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.util.representation.ListRepresentation;
import it.unive.lisa.util.representation.StringRepresentation;
import it.unive.lisa.util.representation.StructuredRepresentation;
import it.unive.scsr.cp.ConstantPropagationData;
import it.unive.scsr.cp.ConstantPropagationEvaluator;
import it.unive.scsr.cp.EmptyHandler;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.stream.Collectors;

public class CProp implements DataflowElement<DefiniteDataflowDomain<CProp>, CProp> {

    public final Identifier id;
    public final Constant constant;

    public CProp(Identifier id, Constant constant) {
        this.id = id;
        this.constant = constant;
    }

    public CProp() {
        this(null, null);
    }

    @Override
    public Collection<Identifier> getInvolvedIdentifiers() {
        // This domain only deals with altering a data structure in which only one identifier is saved, i.e. the
        // variable that can potentially contain a constant value.
        return Collections.singleton(id);
    }

    @Override
    public Collection<CProp> gen(
            Identifier identifier,
            ValueExpression valueExpression,
            ProgramPoint programPoint,
            DefiniteDataflowDomain<CProp> domain) throws SemanticException {
        // Depending on the given expression, the evaluation of the constant can differ significantly. Instead of
        // calculating everything in one method, a specific class will be instantiated based on the given expression to
        // handle the specific scenario in isolation. This will also allow to mentally divide each task into an
        // appropriate scope. To calculate the constant, only the identifier and the domain are passed to the evaluator.
        return ConstantPropagationEvaluator
                .defaultRegistry()
                .evaluatorFor(valueExpression, EmptyHandler.INSTANCE)
                .evaluate(new ConstantPropagationData(identifier, domain))
                .map(Collections::singleton)
                .orElse(Collections.emptySet());
    }

    @Override
    public Collection<CProp> gen(
            ValueExpression valueExpression,
            ProgramPoint programPoint,
            DefiniteDataflowDomain<CProp> cPropPossibleDataflowDomain) throws SemanticException {
        // When no assignment is involved, there is no need to add any dataflow element.
        return Collections.emptyList();
    }

    @Override
    public Collection<CProp> kill(Identifier identifier, ValueExpression valueExpression, ProgramPoint programPoint, DefiniteDataflowDomain<CProp> domain) throws SemanticException {
        // In case of an assignment, this method will remove every dataflow element of the domain that contains an
        // identifier that is equal to the given one.
        return domain
                .getDataflowElements()
                .stream()
                .filter(elem -> elem.id.equals(identifier))
                .collect(Collectors.toSet());
    }

    @Override
    public Collection<CProp> kill(
            ValueExpression valueExpression,
            ProgramPoint programPoint,
            DefiniteDataflowDomain<CProp> domain) throws SemanticException {
        // When no assignment is involved, there is no need to remove any dataflow element.
        return Collections.emptyList();
    }

	@Override
	public StructuredRepresentation representation() {
		return new ListRepresentation(
				new StringRepresentation(id),
				new StringRepresentation(constant));
	}

	@Override
	public CProp pushScope(
			ScopeToken scope)
			throws SemanticException {
		return this;
	}

	@Override
	public CProp popScope(
			ScopeToken scope)
			throws SemanticException {
		return this;
	}

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        CProp cProp = (CProp) o;
        return Objects.equals(id, cProp.id) && Objects.equals(constant, cProp.constant);
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(id);
        result = 31 * result + Objects.hashCode(constant);
        return result;
    }
}