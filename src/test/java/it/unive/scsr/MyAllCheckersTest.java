package it.unive.scsr;

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
import it.unive.scsr.checkers.TaintThreeLevelsChecker;

/**
 * Test class for running the overflow, division by zero, and taint checkers
 * on a set of IMP programs. Programs are organized in separate input folders
 * per checker. Results are written to the outputs folder.
 *
 * @author 903021
 */
public class MyAllCheckersTest {

    // -------------------------------------------------------------------------
    // Input/output folder definitions
    // -------------------------------------------------------------------------

    private static final String OVERFLOW_INPUT  = "inputs/overflows";
    private static final String DIVZERO_INPUT   = "inputs/divzero";
    private static final String TAINT_INPUT     = "inputs/taintthree";

    private static final String OVERFLOW_OUTPUT = "outputs/903021/overflows";
    private static final String DIVZERO_OUTPUT  = "outputs/903021/divzero";
    private static final String TAINT_OUTPUT    = "outputs/903021/taintthree";

    // Method names used to identify sources, sanitizers and sinks in taint programs
    private static final String[] SOURCES    = {"source1", "source2"};
    private static final String[] SANITIZERS = {"sanitizer1", "sanitizer2"};
    private static final String[] SINKS      = {"sink1", "sinks"};

    // -------------------------------------------------------------------------
    // Test entry points — one per checker
    // -------------------------------------------------------------------------

    @Test
    public void testOverflow() throws Exception {
        System.out.println("=== OVERFLOW CHECKER ===");
        runCheckerOnFolder(OVERFLOW_INPUT, OVERFLOW_OUTPUT, CheckerType.OVERFLOW);
    }

    @Test
    public void testDivisionByZero() throws Exception {
        System.out.println("=== DIVISION BY ZERO CHECKER ===");
        runCheckerOnFolder(DIVZERO_INPUT, DIVZERO_OUTPUT, CheckerType.DIVISION_BY_ZERO);
    }

    @Test
    public void testTaint() throws Exception {
        System.out.println("=== TAINT CHECKER ===");
        runCheckerOnFolder(TAINT_INPUT, TAINT_OUTPUT, CheckerType.TAINT);
    }

    // -------------------------------------------------------------------------
    // Core logic
    // -------------------------------------------------------------------------

    private enum CheckerType {
        OVERFLOW, DIVISION_BY_ZERO, TAINT
    }

    /**
     * Iterates over all .imp files in inputFolder and runs the
     * checker identified by type on each of them, writing results
     * under outputFolder/<filename>/.
     */
    private void runCheckerOnFolder(String inputFolder, String outputFolder, CheckerType type)
            throws Exception {

        java.io.File folder = new java.io.File(inputFolder);

        if (!folder.exists() || !folder.isDirectory()) {
            System.err.println("Input folder not found: " + inputFolder);
            return;
        }

        java.io.File[] impFiles = folder.listFiles(
                (dir, name) -> name.endsWith(".imp"));

        if (impFiles == null || impFiles.length == 0) {
            System.out.println("No .imp files found in: " + inputFolder);
            return;
        }

        int passed = 0, failed = 0;

        for (java.io.File file : impFiles) {
            String outDir = outputFolder + "/" + stripExtension(file.getName());
            System.out.println("Analyzing: " + file.getName());
            try {
                runSingleAnalysis(file.getPath(), outDir, type);
                System.out.println("  -> OK");
                passed++;
            } catch (Exception e) {
                System.err.println("  -> ERROR: " + e.getMessage());
                failed++;
            }
        }

        System.out.println("Done. Passed: " + passed + ", Failed: " + failed);
    }

    /**
     * Runs the analysis on a single IMP file.
     */
    private void runSingleAnalysis(String inputFile, String outputDir, CheckerType type)
            throws ParsingException, AnalysisException {

        Program program = IMPFrontend.processFile(inputFile);

        LiSAConfiguration conf = new DefaultConfiguration();
        conf.workdir = outputDir;
        conf.analysisGraphs = GraphType.HTML;
        conf.jsonOutput = true;
        conf.interproceduralAnalysis = new ContextBasedAnalysis<>(FullStackToken.getSingleton());

        switch (type) {
            case OVERFLOW:
                conf.abstractState = DefaultConfiguration.simpleState(
                        DefaultConfiguration.defaultHeapDomain(),
                        new Pentagons(),
                        DefaultConfiguration.defaultTypeDomain());
                conf.semanticChecks.add(new OverflowChecker(NumericalSize.INT32));
                break;

            case DIVISION_BY_ZERO:
                conf.abstractState = DefaultConfiguration.simpleState(
                        DefaultConfiguration.defaultHeapDomain(),
                        new Pentagons(),
                        DefaultConfiguration.defaultTypeDomain());
                conf.semanticChecks.add(new DivisionByZeroChecker(NumericalSize.INT32));
                break;

            case TAINT:
                annotateProgram(program);
                conf.abstractState = DefaultConfiguration.simpleState(
                        DefaultConfiguration.defaultHeapDomain(),
                        new ValueEnvironment<>(new TaintThreeLevels()),
                        DefaultConfiguration.defaultTypeDomain());
                conf.semanticChecks.add(new TaintThreeLevelsChecker());
                break;
        }

        new LiSA(conf).run(program);
    }

    /**
     * Marks methods in the program as taint sources, sanitizers, or sinks
     * based on their name, so the taint checker can track data flows.
     */
    private void annotateProgram(Program program) {
        for (Unit unit : program.getUnits()) {
            if (!(unit instanceof ClassUnit)) continue;
            for (CodeMember cm : ((ClassUnit) unit).getInstanceCodeMembers(false)) {
                String name = cm.getDescriptor().getName();
                if (matches(name, SOURCES)) {
                    cm.getDescriptor().getAnnotations()
                            .addAnnotation(TaintThreeLevels.TAINTED_ANNOTATION);
                } else if (matches(name, SANITIZERS)) {
                    cm.getDescriptor().getAnnotations()
                            .addAnnotation(TaintThreeLevels.CLEAN_ANNOTATION);
                } else if (matches(name, SINKS)) {
                    for (Parameter p : cm.getDescriptor().getFormals())
                        p.addAnnotation(TaintThreeLevelsChecker.SINK_ANNOTATION);
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // Utilities
    // -------------------------------------------------------------------------

    private boolean matches(String name, String[] list) {
        for (String s : list)
            if (name.equals(s)) return true;
        return false;
    }

    private String stripExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot > 0 ? filename.substring(0, dot) : filename;
    }
}