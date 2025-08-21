package it.unive.scsr;

import it.unive.lisa.DefaultConfiguration;
import it.unive.lisa.LiSA;
import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.heap.pointbased.PointBasedHeap;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.checks.semantic.SemanticCheck;
import it.unive.lisa.conf.LiSAConfiguration;
import it.unive.lisa.imp.IMPFrontend;
import it.unive.lisa.imp.ParsingException;
import it.unive.lisa.interprocedural.context.ContextBasedAnalysis;
import it.unive.lisa.interprocedural.context.FullStackToken;
import it.unive.lisa.program.ClassUnit;
import it.unive.lisa.program.Program;
import it.unive.lisa.program.Unit;
import it.unive.lisa.program.cfg.CodeMember;
import it.unive.lisa.program.cfg.Parameter;
import it.unive.scsr.checkers.DivisionByZeroChecker;
import it.unive.scsr.checkers.OverflowChecker;
import it.unive.scsr.checkers.TaintThreeLevelsChecker;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class TestExam {

    public Program[] getPrograms(String dirPath) throws ParsingException {
        File dir = new File(dirPath);

        // Get all input file names
        List<String> filePaths = Stream.of(dir.listFiles())
                .filter(Predicate.not(File::isDirectory))
                .map(File::getPath)
                .toList();

        Program[] programs = new Program[filePaths.size()];

        // Process all files
        for (int i = 0; i < filePaths.size(); i++) {
            programs[i] = IMPFrontend.processFile(filePaths.get(i));
        }

        return programs;
    }

    public <T extends AbstractState<T>, S extends ValueDomain<S>> LiSAConfiguration createConfiguration(
            S valueDomain,
            SemanticCheck<T> checker,
            String workdir
    ) {
        // we build a new configuration for the analysis
        LiSAConfiguration conf = new DefaultConfiguration();

        // we specify where we want files to be generated
        conf.workdir = workdir;

        // we specify the visual format of the analysis results
        conf.analysisGraphs = LiSAConfiguration.GraphType.HTML;

        // we specify the create a json file containing warnings triggered by the analysis
        conf.jsonOutput= true;

        // we specify to perform an interprocedural analysis (require to recognize calls to sources, sanitizers, and sinks)
        conf.interproceduralAnalysis = new ContextBasedAnalysis<>(FullStackToken.getSingleton());

        // we specify the abstract state for the analysis
        conf.abstractState = DefaultConfiguration.simpleState(
                new PointBasedHeap(),
                valueDomain,
                DefaultConfiguration.defaultTypeDomain());

        // we specify the analysis that we want to execute
        conf.semanticChecks.add(checker);

        return conf;
    }

    @Test
    public void testOverflow() throws ParsingException {
        Program[] programs = getPrograms("inputs/final_programs/overflow/");
        // we build a new configuration for the analysis

        String workdir = "outputs/final_programs/overflow";

        int capacity = OverflowChecker.NumericalSize.values().length;
        List<LiSA> liSAs = new ArrayList<>(capacity);

        for (var size : OverflowChecker.NumericalSize.values()) {
            LiSAConfiguration conf = createConfiguration(
                    new ValueEnvironment<>(new Intervals()),
                    new OverflowChecker(size),
                    workdir + "/" + size.name().toLowerCase()
            );
            liSAs.add(new LiSA(conf));
        }

        for (LiSA liSA : liSAs) {
            liSA.run(programs);
        }
    }

    @Test
    public void testDivision() throws ParsingException {
        Program[] programs = getPrograms("inputs/final_programs/division/");
        String workdir = "outputs/final_programs/division";

        LiSAConfiguration conf = createConfiguration(
                new ValueEnvironment<>(new Intervals()),
                new DivisionByZeroChecker(),
                workdir
        );
        LiSA lisa = new LiSA(conf);

        lisa.run(programs);
    }

    @Test
    public void testTaint() throws ParsingException {
        Program[] programs = getPrograms("inputs/final_programs/taint/");
        String workdir = "outputs/final_programs/taint";

        for (Program program : programs) {
            loadAnnotations(program);
        }

        LiSAConfiguration conf = createConfiguration(
                new ValueEnvironment<>(new TaintThreeLevels()),
                new it.unive.scsr.checkers.TaintThreeLevelsChecker(),
                workdir
        );



        LiSA lisa = new LiSA(conf);

        lisa.run(programs);
    }


    private void loadAnnotations(Program program) {

        for(Unit unit : program.getUnits()) {
            if(unit instanceof ClassUnit) {
                ClassUnit cunit = (ClassUnit) unit;
                for(CodeMember cm : cunit.getInstanceCodeMembers(false)) {
                    if(isSource(cm))
                        cm.getDescriptor().getAnnotations().addAnnotation(TaintThreeLevels.TAINTED_ANNOTATION);
                    else if(isSanitizer(cm))
                        cm.getDescriptor().getAnnotations().addAnnotation(TaintThreeLevels.CLEAN_ANNOTATION);
                    else if(isSink(cm))
                        for(Parameter param : cm.getDescriptor().getFormals()) {
                            param.addAnnotation(TaintThreeLevelsChecker.SINK_ANNOTATION);
                        }
                }
            }
        }

    }

    private final Pattern[] sourcePatterns = {
            Pattern.compile("source.*", Pattern.CASE_INSENSITIVE)
    };

    private final Pattern[] sanitizerPatterns = {
            Pattern.compile("santitizer.*", Pattern.CASE_INSENSITIVE)
    };

    private final Pattern [] sinkPatterns = {
            Pattern.compile("sink.*", Pattern.CASE_INSENSITIVE)
    };

    private boolean matchesAnyPattern(Pattern[] patterns, CodeMember cm) {
        String signature = cm.getDescriptor().getName();
        for (Pattern pattern : patterns) {
            if (pattern.matcher(signature).matches()) {
                return true;
            }
        }
        return false;
    }

    private boolean isSource(CodeMember cm) {
        return matchesAnyPattern(sourcePatterns, cm);
    }


    private boolean isSanitizer(CodeMember cm) {
        return matchesAnyPattern(sanitizerPatterns, cm);
    }


    private boolean isSink(CodeMember cm) {
        return  matchesAnyPattern(sinkPatterns, cm);
    }
}
