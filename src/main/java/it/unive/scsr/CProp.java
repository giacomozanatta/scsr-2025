package it.unive.scsr;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.dataflow.DataflowElement;
import it.unive.lisa.analysis.dataflow.DefiniteDataflowDomain;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.UnaryExpression;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.symbolic.value.operator.AdditionOperator;
import it.unive.lisa.symbolic.value.operator.DivisionOperator;
import it.unive.lisa.symbolic.value.operator.MultiplicationOperator;
import it.unive.lisa.symbolic.value.operator.SubtractionOperator;
import it.unive.lisa.symbolic.value.operator.binary.BinaryOperator;
import it.unive.lisa.symbolic.value.operator.unary.NumericNegation;
import it.unive.lisa.symbolic.value.operator.unary.UnaryOperator;
import it.unive.lisa.util.representation.ListRepresentation;
import it.unive.lisa.util.representation.StringRepresentation;
import it.unive.lisa.util.representation.StructuredRepresentation;

public class CProp
        implements DataflowElement<DefiniteDataflowDomain<CProp>, CProp> {

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

    private final Identifier id;
    private final Constant constant;

    public CProp(Identifier id, Constant constant) {
        super();
        this.id = id;
        this.constant = constant;
    }
    public CProp() {
        this(null, null);
    }


    @Override
    public int hashCode() {
        return Objects.hash(id, constant);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        CProp other = (CProp) obj;
        return Objects.equals(constant, other.constant) && Objects.equals(id, other.id);
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
    public Collection<Identifier> getInvolvedIdentifiers() {
        Set<Identifier> result = new HashSet<>();
        result.add(id);
        return result;
    }

    private static Constant evaluate(ValueExpression expression, DefiniteDataflowDomain<CProp> domain){

        if (expression == null)
            return null;

            // Se è costante
        else if (expression instanceof Constant) {
            if(expression.getStaticType().isNumericType()) {  // controlla che sia un valore Numerico in caso di stringhe o altro
                Constant constantValue = (Constant) expression;

                return constantValue;
            }
        }

        // se è identificatore
        else if(expression instanceof Identifier){
            Identifier id = (Identifier) expression;

            for (CProp cp : domain.getDataflowElements())
                if (cp.getInvolvedIdentifiers().contains(id))
                    return cp.constant;
        }


        else if (expression instanceof UnaryExpression){
            UnaryExpression unary = (UnaryExpression) expression;
            UnaryOperator operator = unary.getOperator();
            ValueExpression expr = (ValueExpression) unary.getExpression();

            Constant constant = evaluate(expr, domain);

            if (constant == null)
                return null;

            if (operator instanceof NumericNegation){
                return new Constant(constant.getStaticType(), - (Integer) constant.getValue(), constant.getCodeLocation());
            }

            return constant;
        }

        // Se è espressione binaria tipo somma
        else if (expression instanceof BinaryExpression) {
            BinaryExpression binaryExpression = (BinaryExpression) expression;


            // Verifica se entrambi i lati dell'espressione sono costanti o altro
            Constant left = evaluate((ValueExpression) binaryExpression.getLeft(), domain);
            Constant right = evaluate((ValueExpression) binaryExpression.getRight(), domain);

            if (left == null || right == null)
                return null;

            return doBinaryOperation(left, right, binaryExpression.getOperator(), binaryExpression.getCodeLocation());
        }


        return null;
    }

    @Override
    public Collection<CProp> gen(Identifier identifier, ValueExpression valueExpression, ProgramPoint programPoint, DefiniteDataflowDomain<CProp> cPropDefiniteDataflowDomain) throws SemanticException {
        Set<CProp> result = new HashSet<>();

        Constant constant = evaluate(valueExpression, cPropDefiniteDataflowDomain);
        if (constant != null)
            result.add(new CProp(identifier, constant));

        return result;
    }

    // Metodo per l'operazione binaria
    private static Constant doBinaryOperation(Constant left, Constant right, BinaryOperator operator, CodeLocation location) {

        Integer leftValue = (Integer) left.getValue();
        Integer rightValue = (Integer) right.getValue();

        if(operator instanceof AdditionOperator)
            return new Constant(left.getStaticType(), leftValue + rightValue, location);
        else if(operator instanceof SubtractionOperator)
            return new Constant(left.getStaticType(), leftValue - rightValue, location);
        else if(operator instanceof MultiplicationOperator)
            return new Constant(left.getStaticType(), leftValue * rightValue, location);
        else if(operator instanceof DivisionOperator)
            if(rightValue != 0) // controllo divisione per 0
                return new Constant(left.getStaticType(), leftValue / rightValue, location);
            else
                return null;

        return null; // Return null se l'operazione non è valida
    }

    @Override
    public Collection<CProp> gen(ValueExpression valueExpression, ProgramPoint programPoint, DefiniteDataflowDomain<CProp> cPropDefiniteDataflowDomain) throws SemanticException {
        return new HashSet<>();
    }

    @Override
    public Collection<CProp> kill(Identifier identifier, ValueExpression valueExpression, ProgramPoint programPoint, DefiniteDataflowDomain<CProp> cPropDefiniteDataflowDomain) throws SemanticException {
        Collection<CProp> killed = new HashSet<>();

        for (CProp cProp : cPropDefiniteDataflowDomain.getDataflowElements()) {
            if (cProp.getInvolvedIdentifiers().contains(identifier)) {
                killed.add(cProp);
            }
        }

        return killed;
    }

    @Override
    public Collection<CProp> kill(ValueExpression valueExpression, ProgramPoint programPoint, DefiniteDataflowDomain<CProp> cPropDefiniteDataflowDomain) throws SemanticException {
        return new HashSet<>();
    }
}