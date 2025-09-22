package it.unive.scsr;

import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.dataflow.DataflowElement;
import it.unive.lisa.analysis.dataflow.DefiniteDataflowDomain;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.*;
import it.unive.lisa.symbolic.value.operator.*;
import it.unive.lisa.symbolic.value.operator.binary.*;
import it.unive.lisa.symbolic.value.operator.unary.BitwiseNegation;
import it.unive.lisa.symbolic.value.operator.unary.NumericNegation;
import it.unive.lisa.type.NumericType;
import it.unive.lisa.util.representation.ListRepresentation;
import it.unive.lisa.util.representation.StringRepresentation;
import it.unive.lisa.util.representation.StructuredRepresentation;

import java.util.*;
import java.util.stream.Collectors;

public class CProp implements DataflowElement<DefiniteDataflowDomain<CProp>, CProp> {
    /// Identifier of the variable.
    public final Identifier id;
    /// Constant value which is known to be assigned to the variable.
    public final Constant constant;

    // simple initializing constructor
    public CProp(Identifier id, Constant constant) {
        this.id = id;
        this.constant = constant;
    }

    // default constructor
    public CProp() {
        this(null, null);
    }

    @Override
    public Collection<Identifier> getInvolvedIdentifiers() {
        // The class operates on a per-identifier basis.
        // Return a singleton with this identifier.
        return Set.of(this.id);
    }

    /// Returns a constant if it is possible to statically evaluate the expression. Otherwise, null is returned.
    /// (Simplifying assumption: only deal with constants whose `value` (`::getValue()`) is backed by `java.lang.Integer`.)
    private static Constant tryResolveConstants(SymbolicExpression expr, DefiniteDataflowDomain<CProp> domain) {
        // NOTE: tracking a `Constant` initially felt like the right choice, but I'm not sure whether synthesizing a
        // `Constant` with the `CodeLocation` of an originally non-`Constant` expression is considered valid...

        if (expr == null) return null;

        // Constant resolves to itself
        if (expr instanceof Constant thisConstant)
            return thisConstant;

        // Identifier resolves to its computed constant value, if it exists
        if (expr instanceof Identifier) {
            return domain.getDataflowElements()
                    .stream()
                    .filter(it -> expr.equals(it.id))
                    .findAny()
                    .map(it -> it.constant)
                    .orElse(null);
        }

        // Apply any operations we know how to compute
        if (expr instanceof UnaryExpression unExpr) {
            final SymbolicExpression innerExpr = unExpr.getExpression();
            final Constant innerVal = tryResolveConstants(innerExpr, domain);
            if (innerVal == null
                    || !(innerVal.getStaticType() instanceof NumericType numType) // Ignore non-numeric, non-integral constants
                    || !numType.isIntegral()
                    || !(innerVal.getValue() instanceof Integer innerNum))
                return null;
            final int resultNum;
            if (unExpr.getOperator() instanceof NumericNegation) {
                resultNum = -innerNum;
            } else if (unExpr.getOperator() instanceof BitwiseNegation) {
                resultNum = ~innerNum;
            } else {
                // unsupported unary operator means we can't resolve constant
                return null;
            }

            return new Constant(numType, resultNum, expr.getCodeLocation());
        }

        if (expr instanceof BinaryExpression binExpr) {
            final Constant leftVal = tryResolveConstants(binExpr.getLeft(), domain);
            if (leftVal == null) return null;
            final Constant rightVal = tryResolveConstants(binExpr.getRight(), domain);
            if (rightVal == null) return null;

            if (!(leftVal.getStaticType() instanceof NumericType leftType)
                    || !leftType.isIntegral()
                    || !(leftVal.getValue() instanceof Integer l)
                    || !(rightVal.getStaticType() instanceof NumericType rightType)
                    || !rightType.isIntegral()
                    || !(rightVal.getValue() instanceof Integer r))
                return null;

            final int resultNum;
            final BinaryOperator op = binExpr.getOperator();
            if (op instanceof AdditionOperator) {
                resultNum = l + r;
            } else if (op instanceof SubtractionOperator) {
                resultNum = l - r;
            } else if (op instanceof MultiplicationOperator) {
                resultNum = l * r;
            } else if (op instanceof DivisionOperator) {
                resultNum = l / r;
            } else if (op instanceof ModuloOperator) {
                resultNum = l % r;
            } else if (op instanceof BitwiseAnd) {
                resultNum = l & r;
            } else if (op instanceof BitwiseOr) {
                resultNum = l | r;
            } else if (op instanceof BitwiseShiftLeft) {
                resultNum = l << r;
            } else if (op instanceof BitwiseShiftRight) {
                resultNum = l >> r;
            } else if (op instanceof BitwiseXor) {
                resultNum = l ^ r;
            } else if (op instanceof BitwiseUnsignedShiftRight) {
                resultNum = l >>> r;
            } else {
                // unsupported binary operation means we can't resolve constant
                return null;
            }

            return new Constant(leftType.commonSupertype(rightType), resultNum, expr.getCodeLocation());

        }

        // Consider ternary expressions out of scope, as they do not deal with integers only.

        // if (expr instanceof TernaryExpression) {
        //     ...
        // }

        // if all else fails, the constant can't be resolved
        return null;
    }

    @Override
    public Collection<CProp> gen(Identifier id, ValueExpression expression, ProgramPoint pp, DefiniteDataflowDomain<CProp> domain) throws SemanticException {
        final Constant maybeConstant = tryResolveConstants(expression, domain);
        if (maybeConstant == null)
            return Set.of();
        return Set.of(new CProp(id, maybeConstant));
    }

    @Override
    public Collection<CProp> gen(ValueExpression valueExpression, ProgramPoint pp, DefiniteDataflowDomain<CProp> cPropDefiniteDataflowDomain) throws SemanticException {
        // if not assigning to an identifier, nothing is generated
        return Set.of();
    }

    @Override
    public Collection<CProp> kill(Identifier identifier, ValueExpression valueExpression, ProgramPoint pp, DefiniteDataflowDomain<CProp> domain) throws SemanticException {
        // we are redefining the variable referred to by `identifier`, so any old constant deductions must go
        return domain.getDataflowElements()
                .stream()
                .filter(it -> it.id.equals(identifier))
                .collect(Collectors.toSet());
    }

    @Override
    public Collection<CProp> kill(ValueExpression valueExpression, ProgramPoint pp, DefiniteDataflowDomain<CProp> cPropDefiniteDataflowDomain) throws SemanticException {
        // if not assigning to an identifier, nothing is killed
        return Set.of();
    }


    // IMPLEMENTATION NOTE:
    // the code below is outside of the scope of the course. You can uncomment
    // it to get your code to compile. Be aware that the code is written
    // expecting that a field named "id" and a field named "constant" exist
    // in this class: if you name them differently, change also the code below
    // to make it work by just using the name of your choice instead of
    // "id"/"constant". If you don't have these fields in your
    // solution, then you should make sure that what you are doing is correct :)

    // - Implement your solution using the DefiniteDataFlowDomain.
    //   - What would happen if you used a PossibleDataFlowDomain instead? Think about it (or try it), but remember to deliver the Definite version.
    // - Keep it simple: track only integer values. Any non-integer values should be ignored.
    // - To test your implementation, you can use the inputs/cprop.imp file or define your own test cases.
    // - Refer to the Java test methods discussed in class and adjust them accordingly to work with your domain.
    // - How should integer constant values be propagated?
    //   - Consider the following code snippet:
    //       1. x = 1
    //       2. y = x + 2
    //     The expected output should be:
    //       1. [x,1]
    //       2. [x,1] [y,3]
    //   - How can you retrieve the constant value of `x` to use at program point 2?
    //   - When working with an object of type `Constant`, you can obtain its value by calling the `getValue()` method.

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
            ScopeToken scope) {
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
        return Objects.hash(id, constant);
    }
}