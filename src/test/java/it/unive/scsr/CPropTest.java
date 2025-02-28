package it.unive.scsr;

import org.junit.Test;
import it.unive.lisa.AnalysisException;
import it.unive.lisa.DefaultConfiguration;
import it.unive.lisa.LiSA;
import it.unive.lisa.analysis.dataflow.DefiniteDataflowDomain;
import it.unive.lisa.conf.LiSAConfiguration;
import it.unive.lisa.conf.LiSAConfiguration.GraphType;
import it.unive.lisa.imp.IMPFrontend;
import it.unive.lisa.imp.ParsingException;
import it.unive.lisa.program.Program;

public class CPropTest {

    @Test
    public void testCProp() throws ParsingException, AnalysisException {
    // Get test cases from the dedicated IMP file
        Program program = IMPFrontend.processFile("inputs/CProp.imp");

    // Build a new configuration for the analysis
        LiSAConfiguration conf = new DefaultConfiguration();
    // Output files in the specified directory
        conf.workdir = "outputs/cprop";
    // Visualize the ControlFlow Graph in HTML format
        conf.analysisGraphs = GraphType.HTML;
    // Execute the following analysis
        conf.abstractState = DefaultConfiguration.simpleState(
                DefaultConfiguration.defaultHeapDomain(),
                new DefiniteDataflowDomain<>(new CProp()),
                DefaultConfiguration.defaultTypeDomain()
        );

    // Instantiate LiSA with the given configuration
        LiSA lisa = new LiSA(conf);
        lisa.run(program);
    }
}