package it.unive.scsr;

import org.junit.Test;

import it.unive.lisa.AnalysisException;
import it.unive.lisa.DefaultConfiguration;
import it.unive.lisa.LiSA;
import it.unive.lisa.conf.LiSAConfiguration;
import it.unive.lisa.conf.LiSAConfiguration.GraphType;
import it.unive.lisa.imp.IMPFrontend;
import it.unive.lisa.imp.ParsingException;
import it.unive.lisa.program.Program;
import it.unive.scsr.checkers.OverflowChecker;
import it.unive.scsr.checkers.OverflowChecker.NumericalSize;
import it.unive.scsr.Pentagons;

public class OverflowsTest {

    @Test
    public void testOverflows() throws ParsingException, AnalysisException {
        // Parse the program to get the CFG representation of the code in it
        Program program = IMPFrontend.processFile("inputs/overflows.imp");

        // Build a new configuration for the analysis
        LiSAConfiguration conf = new DefaultConfiguration();

        // Specify where we want files to be generated
        conf.workdir = "outputs/overflows";

        // Specify the visual format of the analysis results
        conf.analysisGraphs = GraphType.HTML;

        // Enable JSON output for warnings
        conf.jsonOutput = true;

        // Specify the analysis that we want to execute
        conf.abstractState = DefaultConfiguration.simpleState(
                DefaultConfiguration.defaultHeapDomain(),
                new Pentagons(),  // ← REMOVE ValueEnvironment wrapper
                DefaultConfiguration.defaultTypeDomain());

        // Add the overflow checker - CHANGED TO INT32
        conf.semanticChecks.add(new OverflowChecker(NumericalSize.INT32));

        // Instantiate LiSA with our configuration
        LiSA lisa = new LiSA(conf);

        // Finally, we tell LiSA to analyze the program
        lisa.run(program);
    }
}