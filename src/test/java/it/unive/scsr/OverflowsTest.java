package it.unive.scsr;

import it.unive.lisa.AnalysisException;
import it.unive.lisa.DefaultConfiguration;
import it.unive.lisa.LiSA;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.conf.LiSAConfiguration;
import it.unive.lisa.conf.LiSAConfiguration.GraphType;
import it.unive.lisa.imp.IMPFrontend;
import it.unive.lisa.imp.ParsingException;
import it.unive.lisa.program.Program;
import it.unive.scsr.checkers.OverflowChecker;
import org.junit.Test;

public class OverflowsTest {

    @Test
    public void testSingleFile() throws ParsingException, AnalysisException {
        // Test just ONE specific file
        runSingleAnalysis("inputs/overflows/875290-Overflow.imp");
    }

    @Test
    public void testAnotherFile() throws ParsingException, AnalysisException {
        // Test another file if you have it
        runSingleAnalysis("inputs/overflows/another-test.imp");
    }

    private void runSingleAnalysis(String filePath) throws ParsingException, AnalysisException {
        System.out.println("Analyzing: " + filePath);

        Program program = IMPFrontend.processFile(filePath);

        LiSAConfiguration conf = new DefaultConfiguration();
        // Create output path based on filename
        String fileName = new java.io.File(filePath).getName().replace(".imp", "");
        conf.workdir = "outputs/overflows/" + fileName;
        conf.analysisGraphs = GraphType.HTML;
        conf.jsonOutput = true;
        conf.serializeResults = true;

        // Abstract state with Intervals
        conf.abstractState = DefaultConfiguration.simpleState(
                DefaultConfiguration.defaultHeapDomain(),
                new ValueEnvironment<>(new Intervals()),
                DefaultConfiguration.defaultTypeDomain());

        // Use type-inferring checker
        conf.semanticChecks.add(new OverflowChecker(OverflowChecker.NumericalSize.INT32, true));

        // Run analysis
        LiSA lisa = new LiSA(conf);
        lisa.run(program);

        System.out.println("✓ Analysis complete for: " + fileName);
    }
}