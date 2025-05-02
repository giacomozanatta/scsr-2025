package it.unive.scsr;

import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.scsr.checkers.OverflowChecker;
import it.unive.scsr.utils.FilesUtils;
import it.unive.scsr.resources.ProgramResource;
import org.junit.Test;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.function.Function;

public class OverflowTest {
    // A private static nested class representing a program resource specialized for overflow checking, extending
    // ProgramResource and parameterized with OverflowChecker and a ValueEnvironment of Intervals. It is initialized
    // with input and output directory paths.
    private static class OverflowProgram extends ProgramResource<OverflowChecker, ValueEnvironment<Intervals>> {
        public OverflowProgram(String inputFile, String outputDirectory) {
            super(inputFile, outputDirectory);
        }
    }

    @Test
    public void test() {
        // Creates a list of OverflowChecker instances, one for each value in the NumericalSize enum.
        var checkers = Arrays
                .stream(OverflowChecker.NumericalSize.values())
                .map(OverflowChecker::new)
                .toList();

        // Defines a function that takes a Path and creates a new OverflowProgram instance, using the path's string
        // representation as the input file and a default output directory derived from the filename.
        Function<Path, OverflowProgram> overflowProgram = path ->
                new OverflowProgram(
                        path.toString(),
                        FilesUtils.defaultOutputDirectory(FilesUtils.removeExt(path.getFileName())));

        // Retrieves filenames from the default input directory, creates an OverflowProgram for each, converts each
        // program to a LiSARunner with the provided checkers and initial value environment, and then runs each
        // LiSARunner.
        FilesUtils
                .filenames(FilesUtils.defaultInputDirectory())
                .stream()
                .map(overflowProgram)
                .map(program -> program.toLiSARunner(checkers, new ValueEnvironment<>(Intervals.TOP), null))
                .forEach(Runnable::run);
    }
}
