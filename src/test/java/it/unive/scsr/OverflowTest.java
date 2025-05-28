package it.unive.scsr;

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
import it.unive.scsr.checkers.OverflowChecker;
import it.unive.scsr.checkers.OverflowChecker.NumericalSize;

public class OverflowTest {

	@Test
	public void testOverflowAnalysis() throws ParsingException, AnalysisException {
		// we parse the program to get the CFG representation of the code in it
		Program program = IMPFrontend.processFile("inputs/overflow.imp");

		// we build a new configuration for the analysis
		LiSAConfiguration conf = new DefaultConfiguration();

		// we specify where we want files to be generated
		conf.workdir = "outputs/overflow";

		// we specify the visual format of the analysis results
		conf.analysisGraphs = GraphType.HTML;

		// we specify to create a json file containing warnings triggered by the
		// analysis
		conf.jsonOutput = true;

		// we specify the analysis that we want to execute
		conf.abstractState = DefaultConfiguration.simpleState(
				DefaultConfiguration.defaultHeapDomain(),
				new ValueEnvironment<>(new Overflow()), // Using the Overflow domain
				DefaultConfiguration.defaultTypeDomain());

		// We add the OverflowChecker to the semantic checks
		// This checker will use the results of the Overflow analysis (or any other
		// interval-like domain)
		// For this test, we'll configure the checker for INT8.
		// Note: The Overflow domain itself doesn't know about NumericalSize.
		// The checker interprets the results of an interval domain (like the base
		// Interval, or potentially a more refined one)
		// against a specific NumericalSize.
		// For this example, we'll assume the checker is meant to be used with a
		// more precise interval domain if strict bounds checking is the primary goal.
		// However, the Overflow domain can still provide "OF" (Overflowed) which the
		// checker might interpret.
		// For a more direct test of the Overflow domain's OF/NO states,
		// one would typically inspect the abstract state directly.
		// Adding the checker here demonstrates its integration.
		// conf.semanticChecks.add(new OverflowChecker(NumericalSize.INT8));
		// Let's assume for now the test is primarily for the Overflow domain itself,
		// and checker tests would be more specific with an Interval domain.
		// If the intention is to test Overflow domain with the checker,
		// the checker would need to be adapted or the ValueEnvironment would use
		// Intervals.

		// Re-evaluating: The previous turn was about implementing OverflowChecker
		// which *uses* an Interval-like domain.
		// The Overflow domain itself is simpler (TOP/OF/NO/BOT).
		// To test OverflowChecker correctly, it needs an interval domain.
		// Let's adjust the test to use the Interval domain for the checker,
		// and keep the Overflow domain for a separate conceptual test if needed.
		// For this specific request, we'll test the Overflow domain's logic.

		// we instantiate LiSA with our configuration
		LiSA lisa = new LiSA(conf);

		// finally, we tell LiSA to analyze the program
		lisa.run(program);
	}

	@Test
	public void testOverflowCheckerWithIntervals() throws ParsingException, AnalysisException {
		// we parse the program to get the CFG representation of the code in it
		Program program = IMPFrontend.processFile("inputs/overflow.imp");

		// we build a new configuration for the analysis
		LiSAConfiguration conf = new DefaultConfiguration();

		// we specify where we want files to be generated
		conf.workdir = "outputs/overflow_checker_int8";

		// we specify the visual format of the analysis results
		conf.analysisGraphs = GraphType.HTML;

		// we specify to create a json file containing warnings triggered by the
		// analysis
		conf.jsonOutput = true;

		// we specify the analysis that we want to execute, using Intervals for the
		// checker
		conf.abstractState = DefaultConfiguration.simpleState(
				DefaultConfiguration.defaultHeapDomain(),
				new ValueEnvironment<>(new Intervals()), // Using the Intervals domain for the checker
				DefaultConfiguration.defaultTypeDomain());

		// Add the OverflowChecker, configured for INT8
		conf.semanticChecks.add(new OverflowChecker(NumericalSize.INT8));

		// we instantiate LiSA with our configuration
		LiSA lisa = new LiSA(conf);

		// finally, we tell LiSA to analyze the program
		lisa.run(program);
	}
}