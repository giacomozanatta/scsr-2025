package it.unive.scsr;


import it.unive.lisa.interprocedural.context.ContextBasedAnalysis;
import it.unive.lisa.interprocedural.context.FullStackToken;
import it.unive.scsr.checkers.OverflowChecker.NumericalSize;
import it.unive.scsr.checkers.OverflowChecker;
import org.junit.Test;

import it.unive.lisa.AnalysisException;
import it.unive.lisa.DefaultConfiguration;
import it.unive.lisa.LiSA;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.conf.LiSAConfiguration;
import it.unive.lisa.conf.LiSAConfiguration.GraphType;
import it.unive.lisa.imp.IMPFrontend;
import it.unive.lisa.imp.ParsingException;
import it.unive.lisa.program.Program;

public class OverflowTest {

    @Test
    public void testOverflow() throws ParsingException, AnalysisException {
        // we parse the program to get the CFG representation of the code in it
        Program program = IMPFrontend.processFile("inputs/testing_overflow/885768_overflow.imp");

        // we build a new configuration for the analysis
        LiSAConfiguration conf = new DefaultConfiguration();

        // we specify where we want files to be generated
        conf.workdir = "outputs/overflow_test_output/885768";

        // we specify the visual format of the analysis results
        conf.analysisGraphs = GraphType.HTML;

        // we create a json file containing warnings triggered by the analysis
        conf.jsonOutput = true;

        // we specify the analysis that we want to execute
        conf.abstractState = DefaultConfiguration.simpleState(
                DefaultConfiguration.defaultHeapDomain(),
                new ValueEnvironment<>(new Intervals()),
                DefaultConfiguration.defaultTypeDomain());

        // Configure interprocedural analysis
        conf.interproceduralAnalysis = new ContextBasedAnalysis<>(FullStackToken.getSingleton());

        // Check every test case for all the numerical sizes
        for(NumericalSize size : NumericalSize.values())
            conf.semanticChecks.add(new OverflowChecker(size));

        // we instantiate LiSA with our configuration
        LiSA lisa = new LiSA(conf);

        // finally, we tell LiSA to analyze the program
        lisa.run(program);
    }
}