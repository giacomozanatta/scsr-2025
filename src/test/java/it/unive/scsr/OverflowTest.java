package it.unive.scsr;

import static org.junit.Assert.assertTrue;
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
import it.unive.lisa.program.Program;
import it.unive.scsr.checkers.OverflowChecker;
import it.unive.scsr.checkers.OverflowChecker.NumericalSize;

public class OverflowTest{
	

	@Test
	public void testAllOverflowSizes() throws ParsingException, AnalysisException {
		// we parse the program to get the CFG representation of the code in it
		Program program = IMPFrontend.processFile("inputs/overflow.imp");
        
        for (NumericalSize sz : NumericalSize.values()) {
		
            // we build a new configuration for the analysis
            LiSAConfiguration conf = new DefaultConfiguration();

            // we specify where we want files to be generated
            conf.workdir = "outputs/overflow";

            // we specify the visual format of the analysis results
            conf.analysisGraphs = GraphType.HTML;
            
            // we specify the create a json file containing warnings triggered by the analysis
            conf.jsonOutput= true;

            // we specify the analysis that we want to execute
            
            conf.abstractState = DefaultConfiguration.simpleState(
                    DefaultConfiguration.defaultHeapDomain(),
                    new ValueEnvironment<>(new Intervals()),
                    DefaultConfiguration.defaultTypeDomain());
            
            // we specify to perform an interprocedural analysis (require to recognize calls to sources, sanitizers, and sinks)
            conf.interproceduralAnalysis = new ContextBasedAnalysis<>(FullStackToken.getSingleton());
            
            // the TaintChecker is executed after the Taint analysis and it checks if a tainted value is flowed in a sink
            conf.semanticChecks.add(new OverflowChecker(sz));
            
            // we instantiate LiSA with our configuration
            LiSA lisa = new LiSA(conf);
            lisa.run(program);

            assertTrue("No overflow/underflow alert for size " + sz, true);
        }   

	}

	
}
