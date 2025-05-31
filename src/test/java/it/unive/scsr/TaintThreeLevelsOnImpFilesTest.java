package it.unive.scsr;

import org.junit.Test;

import it.unive.lisa.AnalysisException;
import it.unive.lisa.DefaultConfiguration;
import it.unive.lisa.LiSA;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.scsr.TaintThreeLevels;
import it.unive.lisa.conf.LiSAConfiguration;
import it.unive.lisa.conf.LiSAConfiguration.GraphType;
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

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Stream;

public class TaintThreeLevelsOnImpFilesTest {

    // Example signatures for sources, sanitizers, and sinks
    String[] sources = new String[] {"getSensitiveData"};
    String[] sanitizers = new String[] {"maskData"};
    String[] sinks = new String[] {"maskData"};

    @Test
    public void testTaintThreeLevelsCheckerOnSingleImpFile() throws ParsingException, AnalysisException {
        String impFile = "inputs/1001389-TainThreeLevels.imp"; // Use the provided IMP program

        Program program = IMPFrontend.processFile(impFile);

        // Annotate sources, sanitizers, and sinks
        loadAnnotationsWithDebug(program);

        LiSAConfiguration conf = new DefaultConfiguration();
        conf.workdir = "outputs/taint-eval";
        conf.analysisGraphs = GraphType.HTML;
        conf.jsonOutput = true;

        conf.abstractState = DefaultConfiguration.simpleState(
                DefaultConfiguration.defaultHeapDomain(),
                new ValueEnvironment<>(new TaintThreeLevels()),
                DefaultConfiguration.defaultTypeDomain());

        conf.interproceduralAnalysis = new ContextBasedAnalysis<>(FullStackToken.getSingleton());
        conf.semanticChecks.add(new TaintThreeLevelsChecker());

        System.out.println("=== Starting LiSA analysis for: " + impFile + " ===");
        LiSA lisa = new LiSA(conf);
        lisa.run(program);
        System.out.println("=== Finished LiSA analysis for: " + impFile + " ===");
        System.out.println("Check outputs/taint-eval/report.json for warnings.");

        // Print taint warnings from report.json
        String reportPath = "outputs/taint-eval/report.json";
        if (Files.exists(Paths.get(reportPath))) {
            try (Stream<String> lines = Files.lines(Paths.get(reportPath))) {
                System.out.println("=== Taint Warnings from report.json ===");
                lines.filter(line -> line.contains("[POSSIBLE]") || line.contains("[DEFINITE]"))
                     .forEach(System.out::println);
            } catch (Exception e) {
                System.err.println("Could not read report.json: " + e.getMessage());
            }
        }
    }

    private void loadAnnotationsWithDebug(Program program) {
        for(Unit unit : program.getUnits()) {
            if(unit instanceof ClassUnit) {
                ClassUnit cunit = (ClassUnit) unit;
                for(CodeMember cm : cunit.getInstanceCodeMembers(false)) {
                    if(isSource(cm)) {
                        cm.getDescriptor().getAnnotations().addAnnotation(TaintThreeLevels.TAINTED_ANNOTATION);
                        System.out.println("[DEBUG] Annotated as source: " + cm.getDescriptor().getName());
                    } else if(isSanitizer(cm)) {
                        cm.getDescriptor().getAnnotations().addAnnotation(TaintThreeLevels.CLEAN_ANNOTATION);
                        System.out.println("[DEBUG] Annotated as sanitizer: " + cm.getDescriptor().getName());
                    }
                    // Always annotate all parameters of sinks, even if there are no parameters
                    if(isSink(cm)) {
                        boolean hasParams = false;
                        for(Parameter param : cm.getDescriptor().getFormals()) {
                            hasParams = true;
                            param.addAnnotation(TaintThreeLevelsChecker.SINK_ANNOTATION);
                            System.out.println("[DEBUG] Annotated as sink: " + cm.getDescriptor().getName() + " param: " + param.getName());
                        }
                        if (!hasParams) {
                            System.out.println("[DEBUG] Sink method " + cm.getDescriptor().getName() + " has no parameters to annotate.");
                        }
                    }
                }
            }
        }
    }

    private boolean isSource(CodeMember cm) {
        for(String signatureName : sources) {
            if(cm.getDescriptor().getName().equals(signatureName))
                return true;
        }
        return false;
    }

    private boolean isSanitizer(CodeMember cm) {
        for(String signatureName : sanitizers) {
            if(cm.getDescriptor().getName().equals(signatureName))
                return true;
        }
        return false;
    }

    private boolean isSink(CodeMember cm) {
        for(String signatureName : sinks) {
            if(cm.getDescriptor().getName().equals(signatureName))
                return true;
        }
        return false;
    }
}
