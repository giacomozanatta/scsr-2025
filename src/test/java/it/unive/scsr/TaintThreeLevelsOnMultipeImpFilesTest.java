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
import java.io.File;

public class TaintThreeLevelsOnMultipeImpFilesTest {

    // Expanded sinks array to include more possible sink functions
    String[] sources = new String[] {"getSensitiveData"};
    String[] sanitizers = new String[] {"maskData"};
    String[] sinks = new String[] {
        "maskData", "print", "output", "send", "write", "display", "sink", "log", "println"
    };

    @Test
    public void testTaintThreeLevelsCheckerOnMultipleImpFiles() throws ParsingException, AnalysisException {
        String[] impFiles = {
            "inputs/TaintThreeLevel/879899-taint-3lvs.imp",
            "inputs/TaintThreeLevel/880036_taint_search_query_sanitized.imp",
            "inputs/TaintThreeLevel/880036_taint_search_query_wrong_way.imp",
            "inputs/TaintThreeLevel/880092-taint-3lvls.imp",
            "inputs/TaintThreeLevel/884046-taint-3lvs.imp",
            "inputs/TaintThreeLevel/885768_taint_3lvs.imp",
            "inputs/TaintThreeLevel/890488_890441taint.imp",
            "inputs/TaintThreeLevel/890550_taintthreelevels.imp",
            "inputs/TaintThreeLevel/892631-taint3lvs.imp",
            "inputs/TaintThreeLevel/908677-benchmark-taint-3lvs.imp",
        };

        for (String impFile : impFiles) {
            File file = new File(impFile);
            if (!file.exists()) {
                System.err.println("[WARNING] Skipping missing file: " + impFile);
                continue;
            }
            System.out.println("=== Running TaintThreeLevelsChecker on: " + impFile + " ===");
            Program program = IMPFrontend.processFile(impFile);

            // Annotate sources, sanitizers, and sinks (same logic as single-file test)
            loadAnnotationsWithDebug(program);

            LiSAConfiguration conf = new DefaultConfiguration();
            // Use a unique workdir for each file
            String baseName = new File(impFile).getName().replace(".imp", "");
            String workdir = "outputs/taint-eval/" + baseName;
            conf.workdir = workdir;
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
            System.out.println("Check " + workdir + "/report.json for warnings.");

            // Print taint warnings from the correct report.json
            String reportPath = workdir + "/report.json";
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
    }

    private void loadAnnotationsWithDebug(Program program) {
        for(Unit unit : program.getUnits()) {
            if(unit instanceof ClassUnit) {
                ClassUnit cunit = (ClassUnit) unit;
                for(CodeMember cm : cunit.getInstanceCodeMembers(false)) {
                    String name = cm.getDescriptor().getName();
                    if(isSource(cm)) {
                        cm.getDescriptor().getAnnotations().addAnnotation(TaintThreeLevels.TAINTED_ANNOTATION);
                        System.out.println("[DEBUG] Annotated as source: " + name);
                    } else if(isSanitizer(cm)) {
                        cm.getDescriptor().getAnnotations().addAnnotation(TaintThreeLevels.CLEAN_ANNOTATION);
                        System.out.println("[DEBUG] Annotated as sanitizer: " + name);
                    }
                    // Always annotate all parameters of sinks, even if there are no parameters
                    if(isSink(cm)) {
                        boolean hasParams = false;
                        for(Parameter param : cm.getDescriptor().getFormals()) {
                            hasParams = true;
                            param.addAnnotation(TaintThreeLevelsChecker.SINK_ANNOTATION);
                            System.out.println("[DEBUG] Annotated as sink: " + name + " param: " + param.getName());
                        }
                        if (!hasParams) {
                            System.out.println("[DEBUG] Sink method " + name + " has no parameters to annotate.");
                        }
                    }
                }
            }
        }
        // Print all annotated sinks for debug
        System.out.println("[DEBUG] Sinks considered: ");
        for (String sink : sinks) {
            System.out.println("  - " + sink);
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
