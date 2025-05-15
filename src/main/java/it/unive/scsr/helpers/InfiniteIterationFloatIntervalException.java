package it.unive.scsr.helpers;

public class InfiniteIterationFloatIntervalException extends RuntimeException {
    /**
     * Builds the exception.
     *
     * @param i the non-finite interval on which some iterates
     */
    public InfiniteIterationFloatIntervalException(
            FloatInterval i) {
        super("Cannot iterate over the interval " + i);
    }
}
