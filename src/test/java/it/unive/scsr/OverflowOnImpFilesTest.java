package it.unive.scsr;

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
import it.unive.lisa.program.Program;
import it.unive.scsr.checkers.OverflowChecker;
import org.junit.Test;

public class OverflowOnImpFilesTest {

    @Test
    public void testOverflowCheckerOnImpFile() throws ParsingException, AnalysisException {
        // Parse the .imp file (adjust the path as needed)
        Program program = IMPFrontend.processFile("inputs/1001389-overflow.imp");

        // Build configuration
        LiSAConfiguration conf = new DefaultConfiguration();
        conf.workdir = "outputs/overflow-eval";
        conf.analysisGraphs = GraphType.HTML;
        conf.jsonOutput = true;

        // Set up the abstract state with your Intervals domain
        conf.abstractState = DefaultConfiguration.simpleState(
                DefaultConfiguration.defaultHeapDomain(),
                new ValueEnvironment<>(new Intervals()),
                DefaultConfiguration.defaultTypeDomain());

        // Enable interprocedural analysis
        conf.interproceduralAnalysis = new ContextBasedAnalysis<>(FullStackToken.getSingleton());
        // Run the checker for all relevant sizes
        conf.semanticChecks.add(new OverflowChecker(OverflowChecker.NumericalSize.INT8));
        conf.semanticChecks.add(new OverflowChecker(OverflowChecker.NumericalSize.UINT8));
        conf.semanticChecks.add(new OverflowChecker(OverflowChecker.NumericalSize.INT16));
        conf.semanticChecks.add(new OverflowChecker(OverflowChecker.NumericalSize.UINT16));
        conf.semanticChecks.add(new OverflowChecker(OverflowChecker.NumericalSize.INT32));
        conf.semanticChecks.add(new OverflowChecker(OverflowChecker.NumericalSize.UINT32));

        // Run LiSA
        LiSA lisa = new LiSA(conf);
        lisa.run(program);
    }
}
