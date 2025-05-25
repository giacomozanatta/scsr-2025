package it.unive.scsr.tests879899;

import static it.unive.scsr.utils.FilesUtils.*;

import it.unive.scsr.Pentagons;
import it.unive.scsr.checkers.DivisionByZeroChecker;
import it.unive.scsr.resources.ProgramResource;
import java.nio.file.Path;
import java.util.Set;
import java.util.function.Function;
import org.junit.Test;

public class PentagonsDivisionByZeroTest {

  // A private static nested class representing a program resource specialized for division by zero
  // checking, extending ProgramResource and parameterized with DivisionByZeroChecker and a
  // ValueEnvironment of Intervals. It is initialized with input and output directory paths.
  private static class DivisionByZeroProgram
      extends ProgramResource<DivisionByZeroChecker<Pentagons>, Pentagons> {
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
            program -> program.toLiSARunner(Set.of(new DivisionByZeroChecker<>()), new Pentagons()))
        .forEach(Runnable::run);
  }
}
