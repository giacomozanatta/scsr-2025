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
	

	// Network monitoring calculations
	@Test
	public void testDivByZeroNetwork() throws ParsingException, AnalysisException {
		runAnalysis("inputs/908883-divbyzero_test1_network.imp", new ValueEnvironment<>(new Intervals()), "network");
	}
	
	// IoT sensor data processing
	@Test
	public void testDivByZeroSensor() throws ParsingException, AnalysisException {
		runAnalysis("inputs/908883-divbyzero_test2_sensor.imp", new Pentagons(), "sensor");
	}
	
	// Mathematical/algorithmic computations
	@Test
	public void testDivByZeroAlgorithm() throws ParsingException, AnalysisException {
		runAnalysis("inputs/908883-divbyzero_test3_algorithm.imp", new ValueEnvironment<>(new Intervals()), "algorithm");
	}
	
	private <V extends ValueDomain<V>> void runAnalysis(String inputFile, V valueEnv, String path) throws ParsingException{
		// we parse the program to get the CFG representation of the code in it
		Program program = IMPFrontend.processFile(inputFile);

		// we build a new configuration for the analysis
		LiSAConfiguration conf = new DefaultConfiguration();

		// we specify where we want files to be generated
		conf.workdir = "outputs/divbyzero/"+path;

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
		conf.semanticChecks.add(new DivisionByZeroChecker(NumericalSize.INT16));
		 
		// we instantiate LiSA with our configuration
		LiSA lisa = new LiSA(conf);
		

		// finally, we tell LiSA to analyze the program
		lisa.run(program);
	}

	
}
