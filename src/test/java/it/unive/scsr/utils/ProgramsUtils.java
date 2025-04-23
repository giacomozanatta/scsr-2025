package it.unive.scsr.utils;

import it.unive.lisa.imp.IMPFrontend;
import it.unive.lisa.imp.ParsingException;
import it.unive.lisa.program.Program;

public class ProgramsUtils {

    /**
     * Create a program by specifying the path to the IMP file.
     * @param filename Path to the IMP file.
     * @return A program that LiSA can read.
     */
    public static Program buildOrThrow(String filename) {
        try {
            return IMPFrontend.processFile(filename);
        } catch (ParsingException e) {
            throw new RuntimeException();
        }
    }
}
