package it.unive.scsr.tests879899;

import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.scsr.Intervals;
import it.unive.scsr.checkers.OverflowChecker;
import it.unive.scsr.resources.ProgramResource;
import org.junit.Test;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.function.Function;

import static it.unive.scsr.utils.FilesUtils.*;

public class IntervalsOverflowTest {
  // A private static nested class representing a program resource specialized for overflow
  // checking, extending ProgramResource and parameterized with OverflowChecker and a
  // ValueEnvironment of Intervals. It is initialized with input and output directory paths.
  private static class OverflowProgram
      extends ProgramResource<
          OverflowChecker<ValueEnvironment<Intervals>>, ValueEnvironment<Intervals>> {
    public OverflowProgram(String inputFile, String outputDirectory) {
      super(inputFile, outputDirectory);
    }
  }

  @Test
  public void test() {
    // Creates a list of OverflowChecker instances, one for each value in the NumericalSize enum.
    var checkers =
        Arrays.stream(OverflowChecker.NumericalSize.values())
            .map(size -> new OverflowChecker<ValueEnvironment<Intervals>>(size))
            .toList();

    // Defines a function that takes a Path and creates a new OverflowProgram instance, using the
    // path's string representation as the input file and a default output directory derived from
    // the filename.
    Function<Path, OverflowProgram> overflowProgram =
        path ->
            new OverflowProgram(
                path.toString(), defaultOutputDirectory(removeExt(path.getFileName())));

    // Retrieves filenames from the default input directory, creates an OverflowProgram for each,
    // converts each program to a LiSARunner with the provided checkers and initial value
    // environment, and then runs each LiSARunner.
    filenames(defaultInputDirectory()).stream()
        .map(overflowProgram)
        .map(program -> program.toLiSARunner(checkers, new ValueEnvironment<>(new Intervals())))
        .forEach(Runnable::run);
  }
}
