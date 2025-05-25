package it.unive.scsr.tests879899;

import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.scsr.Intervals;
import it.unive.scsr.checkers.DivisionByZeroChecker;
import it.unive.scsr.resources.ProgramResource;
import org.junit.Test;

import java.nio.file.Path;
import java.util.Set;
import java.util.function.Function;

import static it.unive.scsr.utils.FilesUtils.*;

public class IntervalsDivisionByZeroTest {

  // A private static nested class representing a program resource specialized for division by zero
  // checking, extending ProgramResource and parameterized with DivisionByZeroChecker and a
  // ValueEnvironment of Intervals. It is initialized with input and output directory paths.
  private static class DivisionByZeroProgram
      extends ProgramResource<
          DivisionByZeroChecker<ValueEnvironment<Intervals>>, ValueEnvironment<Intervals>> {
    public DivisionByZeroProgram(String inputFile, String outputDirectory) {
      super(inputFile, outputDirectory);
    }
  }

  @Test
  public void test() {
    // Defines a function that takes a Path and creates a new DivisionByZeroProgram instance, using
    // the path's string representation as the input file and a default output directory derived
    // from the filename.
    Function<Path, DivisionByZeroProgram> overflowProgram =
        path ->
            new DivisionByZeroProgram(
                path.toString(), defaultOutputDirectory(removeExt(path.getFileName())));

    // Retrieves filenames from the default input directory, creates an OverflowProgram for each,
    // converts each program to a LiSARunner with the provided checkers and initial value
    // environment, and then runs each LiSARunner.
    filenames(defaultInputDirectory()).stream()
        .map(overflowProgram)
        .map(
            program ->
                program.toLiSARunner(
                    Set.of(new DivisionByZeroChecker<>()), new ValueEnvironment<>(new Intervals())))
        .forEach(Runnable::run);
  }
}
