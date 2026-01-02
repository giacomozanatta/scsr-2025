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
import it.unive.scsr.checkers.DivisionByZeroChecker;
import it.unive.scsr.checkers.OverflowChecker.NumericalSize;
import org.junit.Test;

public class DivisionByZeroTest {

    @Test
    public void testDivisionByZero() throws ParsingException, AnalysisException {
        runAnalysis();
    }

    private void runAnalysis() throws ParsingException, AnalysisException {
        // Parse the IMP program with division cases
        Program program = IMPFrontend.processFile("inputs/divzero.imp");

        // Build configuration
        LiSAConfiguration conf = new DefaultConfiguration();
        conf.workdir = "outputs/divzero";
        conf.analysisGraphs = GraphType.HTML;
        conf.jsonOutput = true;
        conf.serializeResults = true;

        // Abstract state with Intervals
        conf.abstractState = DefaultConfiguration.simpleState(
                DefaultConfiguration.defaultHeapDomain(),
                new ValueEnvironment<>(new Intervals()),
                DefaultConfiguration.defaultTypeDomain());

        // Add division by zero checker
        conf.semanticChecks.add(new DivisionByZeroChecker());

        // Run analysis
        LiSA lisa = new LiSA(conf);
        lisa.run(program);
    }
}