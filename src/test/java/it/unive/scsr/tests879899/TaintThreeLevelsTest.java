package it.unive.scsr.tests879899;

import static it.unive.scsr.utils.FilesUtils.*;
import static it.unive.scsr.utils.FilesUtils.defaultInputDirectory;

import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.program.ClassUnit;
import it.unive.lisa.program.Program;
import it.unive.lisa.program.Unit;
import it.unive.lisa.program.cfg.CodeMember;
import it.unive.lisa.program.cfg.Parameter;
import it.unive.scsr.TaintThreeLevels;
import it.unive.scsr.checkers.TaintThreeLevelsChecker;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

import it.unive.scsr.resources.ProgramResource;
import org.junit.Test;

public class TaintThreeLevelsTest {
  // A private static nested class representing a program resource specialized for overflow
  // checking, extending ProgramResource and parameterized with TaintThreeLevelsChecker and a
  // ValueEnvironment of TaintThreeLevels. It is initialized with input and output directory paths.
  private static class TaintThreeLevelsProgram
      extends ProgramResource<TaintThreeLevelsChecker, ValueEnvironment<TaintThreeLevels>> {

    private static final String[] sources = new String[] {"source1", "source2"};
    private static final String[] sanitizers = new String[] {"sanitizer1", "sanitizer2"};
    private static final String[] sinks = new String[] {"sink1", "sinks"};

    public TaintThreeLevelsProgram(String inputFile, String outputDirectory) {
      super(inputFile, outputDirectory);
    }

    @Override
    public Program update(Program program) {
      Predicate<CodeMember> isSource =
          codeMember ->
              Arrays.stream(sources).anyMatch(s -> codeMember.getDescriptor().getName().equals(s));

      Predicate<CodeMember> isSanitizer =
          codeMember ->
              Arrays.stream(sanitizers)
                  .anyMatch(s -> codeMember.getDescriptor().getName().equals(s));

      Predicate<CodeMember> isSink =
          codeMember ->
              Arrays.stream(sinks).anyMatch(s -> codeMember.getDescriptor().getName().equals(s));

      for (Unit unit : program.getUnits()) {
        if (unit instanceof ClassUnit cunit) {
          for (CodeMember cm : cunit.getInstanceCodeMembers(false)) {
            if (isSource.test(cm)) {
              cm.getDescriptor()
                  .getAnnotations()
                  .addAnnotation(TaintThreeLevels.TAINTED_ANNOTATION);
            } else if (isSanitizer.test(cm)) {
              cm.getDescriptor().getAnnotations().addAnnotation(TaintThreeLevels.CLEAN_ANNOTATION);
            } else if (isSink.test(cm)) {
              for (Parameter param : cm.getDescriptor().getFormals()) {
                param.addAnnotation(TaintThreeLevelsChecker.SINK_ANNOTATION);
              }
            }
          }
        }
      }

      return program;
    }
  }

  @Test
  public void test() {
    // Defines a function that takes a Path and creates a new TaintThreeLevelsProgram instance,
    // using the path's string representation as the input file and a default output directory
    // derived from the filename.
    Function<Path, TaintThreeLevelsProgram> taintProgram =
        path ->
            new TaintThreeLevelsProgram(
                path.toString(), defaultOutputDirectory(removeExt(path.getFileName())));

    filenames(defaultInputDirectory()).stream()
        .map(taintProgram)
        .map(
            program ->
                program.toLiSARunner(
                    Set.of(new TaintThreeLevelsChecker()),
                    new ValueEnvironment<>(TaintThreeLevels.TOP)))
        .forEach(Runnable::run);
  }
}
