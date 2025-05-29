package it.unive.scsr;

import it.unive.lisa.util.numeric.MathNumber;

public class CustomMathNumberConversionException extends Exception {
    private static final long serialVersionUID = 8946152199095876219L;

    public CustomMathNumberConversionException(CustomMathNumber m) {
        super("Cannot convert " + m + " to numerical value");
    }
}
