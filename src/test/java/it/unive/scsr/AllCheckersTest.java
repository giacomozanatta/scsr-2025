package it.unive.scsr;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import it.unive.scsr.checkers.TaintThreeLevelsChecker;
import org.junit.Test;

import it.unive.lisa.AnalysisException;
import it.unive.lisa.DefaultConfiguration;
import it.unive.lisa.LiSA;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
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
import it.unive.scsr.checkers.DivisionByZeroChecker;
import it.unive.scsr.checkers.OverflowChecker;
import it.unive.scsr.checkers.OverflowChecker.NumericalSize;

public class AllCheckersTest {

    @Test
    public void testAllOverflowFiles() throws IOException {
        System.out.println("=== Analyzing Overflow Files ===");
        analyzeDirectory("inputs/overflows", "outputs/overflows", AnalysisType.OVERFLOW);
    }

    @Test
    public void testAllDivisionFiles() throws IOException {
        System.out.println("=== Analyzing Division By Zero Files ===");
        analyzeDirectory("inputs/divzero", "outputs/divzero", AnalysisType.DIVISION);
    }

    @Test
    public void testAllTaintFiles() throws IOException {
        System.out.println("=== Analyzing Taint Files ===");
        analyzeDirectory("inputs/taintthree", "outputs/taintthree", AnalysisType.TAINT);
    }

    @Test
    public void testAllFiles() throws IOException {
        System.out.println("=== Analyzing All Files ===");
        testAllOverflowFiles();
        testAllDivisionFiles();
        testAllTaintFiles();
    }

    private enum AnalysisType {
        OVERFLOW, DIVISION, TAINT
    }

    /**
     * Analyze all .imp files in a directory
     */
    private void analyzeDirectory(String inputDir, String outputBaseDir, AnalysisType analysisType) throws IOException {
        File directory = new File(inputDir);

        if (!directory.exists() || !directory.isDirectory()) {
            System.err.println("Directory does not exist: " + inputDir);
            return;
        }

        List<File> impFiles = findImpFiles(directory);

        if (impFiles.isEmpty()) {
            System.out.println("No .imp files found in: " + inputDir);
            return;
        }

        System.out.println("Found " + impFiles.size() + " files in " + inputDir);

        int successCount = 0;
        int failCount = 0;

        for (File impFile : impFiles) {
            String fileName = impFile.getName();
            String fileNameWithoutExt = fileName.substring(0, fileName.lastIndexOf('.'));
            String outputDir = outputBaseDir + "/" + fileNameWithoutExt;

            System.out.println("\nAnalyzing: " + impFile.getPath());

            try {
                analyzeFile(impFile.getPath(), outputDir, analysisType);
                successCount++;
                System.out.println("✓ Success: " + fileName);
            } catch (Exception e) {
                failCount++;
                System.err.println("✗ Failed: " + fileName);
                System.err.println("  Error: " + e.getMessage());
                // Continue with next file
            }
        }

        System.out.println("\n=== Summary for " + inputDir + " ===");
        System.out.println("Total files: " + impFiles.size());
        System.out.println("Successful: " + successCount);
        System.out.println("Failed: " + failCount);
    }

    /**
     * Find all .imp files in a directory (non-recursive)
     */
    private List<File> findImpFiles(File directory) {
        List<File> impFiles = new ArrayList<>();
        File[] files = directory.listFiles();

        if (files != null) {
            for (File file : files) {
                if (file.isFile() && file.getName().endsWith(".imp")) {
                    impFiles.add(file);
                }
            }
        }

        return impFiles;
    }

    /**
     * Analyze a single file with the appropriate checker
     */
    private void analyzeFile(String inputFile, String outputDir, AnalysisType analysisType)
            throws ParsingException, AnalysisException {

        // Parse the program
        Program program = IMPFrontend.processFile(inputFile);

        // CRITICAL: Load taint annotations before running taint analysis
        if (analysisType == AnalysisType.TAINT) {
            loadTaintAnnotations(program);
        }

        // Build configuration
        LiSAConfiguration conf = new DefaultConfiguration();
        conf.workdir = outputDir;
        conf.analysisGraphs = GraphType.HTML;
        conf.jsonOutput = true;

        // Set up the abstract state based on analysis type
        if (analysisType == AnalysisType.TAINT) {
            // Taint analysis uses ValueEnvironment<TaintThreeLevels>
            conf.abstractState = DefaultConfiguration.simpleState(
                    DefaultConfiguration.defaultHeapDomain(),
                    new ValueEnvironment<>(new TaintThreeLevels()),
                    DefaultConfiguration.defaultTypeDomain());

            // Taint analysis requires interprocedural analysis to track flows
            conf.interproceduralAnalysis = new ContextBasedAnalysis<>(
                    FullStackToken.getSingleton());
        } else {
            // Overflow and Division by Zero use Pentagons
            conf.abstractState = DefaultConfiguration.simpleState(
                    DefaultConfiguration.defaultHeapDomain(),
                    new Pentagons(),
                    DefaultConfiguration.defaultTypeDomain());
        }

        // Add the appropriate checker based on analysis type
        switch (analysisType) {
            case OVERFLOW:
                conf.semanticChecks.add(new OverflowChecker(NumericalSize.INT32));
                break;
            case DIVISION:
                conf.semanticChecks.add(new DivisionByZeroChecker(NumericalSize.INT32));
                break;
            case TAINT:
                conf.semanticChecks.add(new TaintThreeLevelsChecker());
                break;
        }

        // Run the analysis
        LiSA lisa = new LiSA(conf);
        lisa.run(program);
    }

    /**
     * Load taint annotations for sources, sanitizers, and sinks.
     * This is REQUIRED for taint analysis to work properly.
     *
     * Without this step, the taint checker cannot identify which methods
     * are sources (produce tainted data), sanitizers (clean tainted data),
     * or sinks (should not receive tainted data).
     */
    private void loadTaintAnnotations(Program program) {
        // Define method name patterns for sources, sanitizers, and sinks
        // These patterns match common naming conventions in taint test programs
        String[] sources = {
                // Your test program
                "source1", "source2",
                // Student test programs
                "source_SQL", "source_HTML", "source_PATH",
                "getSource", "getUserInput", "getTainted"
        };

        String[] sanitizers = {
                // Your test program
                "sanitizer1", "sanitizer2",
                // Student test programs
                "sanitize_SQL", "sanitize_HTML", "sanitize_PATH",
                "sanitize", "clean", "escape"
        };

        String[] sinks = {
                // Your test program
                "sink1", "sinks",
                // Student test programs
                "sink_SQL", "sink_HTML", "sink_PATH",
                "execute", "eval", "render"
        };

        // Iterate through all units (classes) in the program
        for (Unit unit : program.getUnits()) {
            if (unit instanceof ClassUnit) {
                ClassUnit cunit = (ClassUnit) unit;

                // Iterate through all methods in the class
                for (CodeMember cm : cunit.getInstanceCodeMembers(false)) {
                    String methodName = cm.getDescriptor().getName();

                    // Check if this method is a source
                    if (isInArray(methodName, sources)) {
                        cm.getDescriptor().getAnnotations()
                                .addAnnotation(TaintThreeLevels.TAINTED_ANNOTATION);
                        System.out.println("  Marked as SOURCE: " + methodName);
                    }
                    // Check if this method is a sanitizer
                    else if (isInArray(methodName, sanitizers)) {
                        cm.getDescriptor().getAnnotations()
                                .addAnnotation(TaintThreeLevels.CLEAN_ANNOTATION);
                        System.out.println("  Marked as SANITIZER: " + methodName);
                    }
                    // Check if this method is a sink
                    else if (isInArray(methodName, sinks)) {
                        // For sinks, we annotate the parameters, not the method
                        for (Parameter param : cm.getDescriptor().getFormals()) {
                            param.addAnnotation(TaintThreeLevelsChecker.SINK_ANNOTATION);
                        }
                        System.out.println("  Marked as SINK: " + methodName);
                    }
                }
            }
        }
    }

    /**
     * Helper method to check if a string is in an array
     */
    private boolean isInArray(String name, String[] array) {
        for (String s : array) {
            if (name.equals(s)) {
                return true;
            }
        }
        return false;
    }
}