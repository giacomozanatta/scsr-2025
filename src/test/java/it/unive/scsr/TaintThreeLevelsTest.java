package it.unive.scsr;

import it.unive.lisa.AnalysisException;
import it.unive.lisa.DefaultConfiguration;
import it.unive.lisa.LiSA;
import it.unive.lisa.analysis.heap.pointbased.PointBasedHeap;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
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
import it.unive.scsr.checkers.TaintThreeLevelsChecker;
import org.junit.Test;

import java.io.File;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class TaintThreeLevelsTest {

    @Test
    public void testTaint() throws ParsingException, AnalysisException {
        File dir = new File("inputs/exam/taint/");

        // Get all input file names
        List<String> filePaths = Stream.of(dir.listFiles())
                .filter(Predicate.not(File::isDirectory))
                .map(File::getPath)
                .toList();


        Program[] programs = new Program[filePaths.size()];

        // Process all files
        for  (int i = 0; i < filePaths.size(); i++)
        {
            programs[i] = IMPFrontend.processFile(filePaths.get(i));
            loadAnnotations(programs[i]);
        }

        // we build a new configuration for the analysis
        LiSAConfiguration conf = new DefaultConfiguration();

        // we specify where we want files to be generated
        conf.workdir = "outputs/taintThreeLevels/";

        // we specify the visual format of the analysis results
        conf.analysisGraphs = LiSAConfiguration.GraphType.HTML;

        // we specify the create a json file containing warnings triggered by the analysis
        conf.jsonOutput= true;

        // we specify the analysis that we want to execute

        conf.abstractState = DefaultConfiguration.simpleState(
                // DefaultConfiguration.defaultHeapDomain(),
                // The monolithic heap is less precise, we use the point based heap
                DefaultConfiguration.defaultHeapDomain(),
                new ValueEnvironment<>(new TaintThreeLevels()),
                DefaultConfiguration.defaultTypeDomain());

        // we specify to perform an interprocedural analysis (require to recognize calls to sources, sanitizers, and sinks)
        conf.interproceduralAnalysis = new ContextBasedAnalysis<>(FullStackToken.getSingleton());

        // Execute the Overflow Checker
        conf.semanticChecks.add(new TaintThreeLevelsChecker());

        // we instantiate LiSA with our configuration
        LiSA lisa = new LiSA(conf);


        // finally, we tell LiSA to analyze the program


        System.out.println(programs.length);

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
            Pattern.compile("source.*"),
    };

    private final Pattern[] sanitizerPatterns = {
            Pattern.compile("sanitize.*", Pattern.CASE_INSENSITIVE),
    };

    private final Pattern [] sinkPatterns = {
            Pattern.compile("convert"),
            Pattern.compile("saveFile"),
            Pattern.compile("getFile"),
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
