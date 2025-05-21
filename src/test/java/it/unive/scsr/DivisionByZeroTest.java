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
import it.unive.scsr.checkers.DivisionByZeroChecker;
import it.unive.scsr.checkers.OverflowChecker;
import org.junit.Test;

public class DivisionByZeroTest {

	@Test
	public void testDivisionByZero() throws ParsingException, AnalysisException {
		// we parse the program to get the CFG representation of the code in it
		Program program = IMPFrontend.processFile("inputs/SCSR_FinalTask2025_Programs/879899-by-zero.imp");

		// we build a new configuration for the analysis
		LiSAConfiguration conf = new DefaultConfiguration();

		// we specify where we want files to be generated
		conf.workdir = "outputs/division-by-zero";

		// we specify the visual format of the analysis results
		conf.analysisGraphs = GraphType.HTML;

		// we specify the create a json file containing warnings triggered by the analysis
		conf.jsonOutput= true;

		// we specify the analysis that we want to execute
		conf.abstractState = DefaultConfiguration.simpleState(
				DefaultConfiguration.defaultHeapDomain(),
				new ValueEnvironment<>(new Intervals()),
				DefaultConfiguration.defaultTypeDomain());


		// TODO: useful?
		conf.interproceduralAnalysis = new ContextBasedAnalysis<>(FullStackToken.getSingleton());

		conf.semanticChecks.add(new DivisionByZeroChecker(OverflowChecker.NumericalSize.FLOAT32));

		// we instantiate LiSA with our configuration
		LiSA lisa = new LiSA(conf);

		// finally, we tell LiSA to analyze the program
		lisa.run(program);
	}
}