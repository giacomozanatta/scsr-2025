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
        runAnalysis(new ValueEnvironment<>(new Intervals()));
    }

    private void runAnalysis(ValueEnvironment<Intervals> valueEnv)
            throws ParsingException, AnalysisException {

        // Parse the IMP program
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
                valueEnv,
                DefaultConfiguration.defaultTypeDomain());

        // Add DivisionByZeroChecker (now requires NumericalSize)
        conf.semanticChecks.add(new DivisionByZeroChecker(NumericalSize.INT32));

        // Run analysis
        LiSA lisa = new LiSA(conf);
        lisa.run(program);
    }
}
