package it.unive.scsr.tests879899;

import it.unive.scsr.intervals.numbers.*;
import it.unive.scsr.utils.Collections;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class IntervalNumberTest {

    @Test
    public void testEquality() {
        // Common numeric values to use for equality comparisons.
        var zero = IntervalNumber.ofPrimitiveOrThrow(0);
        var one = IntervalNumber.ofPrimitiveOrThrow(1);
        var minusOne = IntervalNumber.ofPrimitiveOrThrow(-1);
        var oneDecimal = IntervalNumber.ofPrimitiveOrThrow(1.0);
        var zeroDecimal = IntervalNumber.ofPrimitiveOrThrow(0.0);
        var pInfinity = PlusInfinity.INSTANCE;
        var mInfinity = MinusInfinity.INSTANCE;

        // All calculations must produce values of zero.
        var zeroList = List.of(
                zero.divide(pInfinity).isZero(),
                zero.equals(zeroDecimal));

        // All computations must produce NaN instances.
        var nanList = List.of(
                zero.divide(zero).isNaN(),
                zero.multiply(mInfinity).isNaN(),
                zero.multiply(pInfinity).isNaN(),
                pInfinity.divide(zero).isNaN(),
                one.divide(zero).isNaN());

        // All computations must produce Infinity instances.
        var infinityList = List.of(
                pInfinity.multiply(mInfinity).equals(mInfinity),
                pInfinity.multiply(pInfinity).equals(pInfinity),
                mInfinity.multiply(mInfinity).equals(pInfinity));

        // All calculations must produce numeric values.
        var numericList = List.of(
                zero.add(one).equals(oneDecimal),
                zero.subtract(one).equals(minusOne),
                zero.multiply(one).equals(zeroDecimal),
                zero.divide(one).equals(zeroDecimal));

        Collections
                .from(zeroList, nanList, infinityList, numericList)
                .forEach(aBoolean -> assertEquals(true, aBoolean));
    }
}
