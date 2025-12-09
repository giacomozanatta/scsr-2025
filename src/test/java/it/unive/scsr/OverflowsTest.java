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
    public void testAllOverflows() throws ParsingException, AnalysisException {
        runAnalysis(new ValueEnvironment<>(new Intervals()));
    }

    private void runAnalysis(ValueEnvironment<Intervals> valueEnv)
            throws ParsingException, AnalysisException {

        // Parse the IMP program with overflow cases
        Program program = IMPFrontend.processFile("inputs/overflows.imp");

        // Build configuration
        LiSAConfiguration conf = new DefaultConfiguration();
        conf.workdir = "outputs/mainoverflows";   // ✅ single folder for all overflow results
        conf.analysisGraphs = GraphType.HTML;
        conf.jsonOutput = true;
        conf.serializeResults = true;

        // Abstract state with Intervals
        conf.abstractState = DefaultConfiguration.simpleState(
                DefaultConfiguration.defaultHeapDomain(),
                valueEnv,
                DefaultConfiguration.defaultTypeDomain());

        // ✅ Add only one checker: it now handles ALL sizes internally
        // You need to import NumericalSize if it's not already imported:
// import it.unive.scsr.checkers.OverflowChecker.NumericalSize;

        conf.semanticChecks.add(new OverflowChecker(OverflowChecker.NumericalSize.INT32));

        // Run analysis
        LiSA lisa = new LiSA(conf);
        lisa.run(program);
    }
}
