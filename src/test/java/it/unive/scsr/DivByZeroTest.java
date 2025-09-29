package it.unive.scsr;

import org.junit.Test;

import it.unive.lisa.AnalysisException;
import it.unive.lisa.DefaultConfiguration;
import it.unive.lisa.LiSA;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.conf.LiSAConfiguration;
import it.unive.lisa.conf.LiSAConfiguration.GraphType;
import it.unive.lisa.imp.IMPFrontend;
import it.unive.lisa.imp.ParsingException;
import it.unive.lisa.interprocedural.context.ContextBasedAnalysis;
import it.unive.lisa.interprocedural.context.FullStackToken;
import it.unive.lisa.program.Program;
import it.unive.scsr.checkers.DivisionByZeroChecker;
import it.unive.scsr.checkers.OverflowChecker.NumericalSize;

public class DivByZeroTest {
	

	@Test
	public void testDivByZeroInterval() throws ParsingException, AnalysisException {
		runAnalysis(new ValueEnvironment<>(new Intervals()), NumericalSize.FLOAT8, "intervals-divbyzero-FLOAT8");
	}
	
	@Test
	public void testtestDivByZeroPentagons() throws ParsingException, AnalysisException {
		runAnalysis(new Pentagons(), NumericalSize.FLOAT8, "pentagons-divbyzero-FLOAT8");
	}
	
	private <V extends ValueDomain<V>> void runAnalysis(V valueEnv, NumericalSize size, String path) throws ParsingException{
		// we parse the program to get the CFG representation of the code in it
		Program program = IMPFrontend.processFile("inputs/community-programs/division/885768_divbyzero.imp");

		// we build a new configuration for the analysis
		LiSAConfiguration conf = new DefaultConfiguration();

		conf.wideningThreshold = 15;  // Aumenta da 5 a 10 per evitare loop infiniti su Pentagons (provato fino a 20 se necessario)
		conf.glbThreshold = 10;       // Aumenta da 5 a 10 per ridurre calcoli ridondanti
		conf.recursionWideningThreshold = 10;  // Aumenta da 5 a 10 per gestire ricorsioni meglio
		conf.useWideningPoints = true;  // Lascia true, ma se hai problemi, prova false

		// we specify where we want files to be generated
		conf.workdir = "outputs/community-programs/division/test_ver3/885768_divbyzero/"+path;

		// we specify the visual format of the analysis results
		conf.analysisGraphs = GraphType.HTML;
		
		// we specify the create a json file containing warnings triggered by the analysis
		conf.jsonOutput= true;

		// we specify the analysis that we want to execute
		
		conf.abstractState = DefaultConfiguration.simpleState(
				DefaultConfiguration.defaultHeapDomain(),
				valueEnv,
				DefaultConfiguration.defaultTypeDomain());
		 
		// we specify to perform an interprocedural analysis
		conf.interproceduralAnalysis = new ContextBasedAnalysis<>(FullStackToken.getSingleton());
		 
		// the OverflowChecker is executed after the numerical analysis and it checks if a abstract numerical value leads to an overflow/underflow
		conf.semanticChecks.add(new DivisionByZeroChecker());
		 
		// we instantiate LiSA with our configuration
		LiSA lisa = new LiSA(conf);
		

		// finally, we tell LiSA to analyze the program
		lisa.run(program);
	}

	
}
