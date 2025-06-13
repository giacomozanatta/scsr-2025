package it.unive.scsr.overflowhelp;

import it.unive.scsr.overflowhelp.FloatInterval;

public class InfiniteFloatIterationExeption extends RuntimeException{
    /**
     * Builds the exception.
     *
     * @param i the non-finite interval on which some iterates
     */
    public InfiniteFloatIterationExeption(
            FloatInterval i) {
        super("Cannot iterate over the interval " + i);
    }
}

