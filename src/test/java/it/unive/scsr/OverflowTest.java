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
import it.unive.scsr.checkers.OverflowChecker;
import it.unive.scsr.checkers.OverflowChecker.NumericalSize;

public class OverflowTest {
	

	// @Test
	// public void testOverflowIntervalsUINT8() throws ParsingException, AnalysisException {
	// 	runAnalysis(new ValueEnvironment<>(new Intervals()), NumericalSize.UINT8, "intervals-uint8");
	// }
	
	// @Test
	// public void testOverflowIntervalsINT16() throws ParsingException, AnalysisException {
	// 	runAnalysis(new ValueEnvironment<>(new Intervals()), NumericalSize.INT16, "intervals-int16");
	// }
	
	// @Test
	// public void testOverflowIntervalsFLOAT8() throws ParsingException, AnalysisException {
	// 	runAnalysis(new ValueEnvironment<>(new Intervals()), NumericalSize.FLOAT8, "intervals-float8");
	// }
	
	// @Test
	// public void testOverflowPentagonsUINT8() throws ParsingException, AnalysisException {
	// 	runAnalysis(new Pentagons(), NumericalSize.UINT8, "pentagons-uint8");
	// }
	
	// @Test
	// public void testOverflowPentagonsINT16() throws ParsingException, AnalysisException {
	// 	runAnalysis(new Pentagons(), NumericalSize.INT16, "pentagons-int16");
	// }
	
	// @Test
	// public void testOverflowPentagonsFLOAT8() throws ParsingException, AnalysisException {
	// 	runAnalysis(new Pentagons(), NumericalSize.FLOAT8, "pentagons-float8");
	// }
	
	@Test
	public void testOverflowPentagonsINT8() throws ParsingException, AnalysisException {
		runAnalysis(new Pentagons(), NumericalSize.INT8, "pentagons-int8");
	}

	@Test
	public void testOverflowPentagonsINT16() throws ParsingException, AnalysisException {
		runAnalysis(new Pentagons(), NumericalSize.INT16, "pentagons-int16");
	}

	@Test
	public void testOverflowPentagonsINT32() throws ParsingException, AnalysisException {
		runAnalysis(new Pentagons(), NumericalSize.INT32, "pentagons-int32");
	}

	@Test
	public void testOverflowPentagonsUINT8() throws ParsingException, AnalysisException {
		runAnalysis(new Pentagons(), NumericalSize.UINT8, "pentagons-uint8");
	}

	@Test
	public void testOverflowPentagonsUINT16() throws ParsingException, AnalysisException {
		runAnalysis(new Pentagons(), NumericalSize.UINT16, "pentagons-uint16");
	}

	@Test
	public void testOverflowPentagonsUINT32() throws ParsingException, AnalysisException {
		runAnalysis(new Pentagons(), NumericalSize.UINT32, "pentagons-uint32");
	}

	@Test
	public void testOverflowPentagonsFLOAT8() throws ParsingException, AnalysisException {
		runAnalysis(new Pentagons(), NumericalSize.FLOAT8, "pentagons-float8");
	}

		@Test
	public void testOverflowPentagonsFLOAT16() throws ParsingException, AnalysisException {
		runAnalysis(new Pentagons(), NumericalSize.FLOAT16, "pentagons-float16");
	}

	@Test
	public void testOverflowPentagonsFLOAT32() throws ParsingException, AnalysisException {
		runAnalysis(new Pentagons(), NumericalSize.FLOAT32, "pentagons-float32");
	}

	@Test
	public void testOverflowIntervalsINT8() throws ParsingException, AnalysisException {
		runAnalysis(new ValueEnvironment<>(new Intervals()), NumericalSize.INT8, "intervals-int8");
	}
	@Test
	public void testOverflowIntervalsINT16() throws ParsingException, AnalysisException {
		runAnalysis(new ValueEnvironment<>(new Intervals()), NumericalSize.INT16, "intervals-int16");
	}

	@Test
	public void testOverflowIntervalsINT32() throws ParsingException, AnalysisException {
		runAnalysis(new ValueEnvironment<>(new Intervals()), NumericalSize.INT32, "intervals-int32");
	}

	@Test
	public void testOverflowIntervalsUINT8() throws ParsingException, AnalysisException {
		runAnalysis(new ValueEnvironment<>(new Intervals()), NumericalSize.UINT8, "intervals-uint8");
	}
	@Test
	public void testOverflowIntervalsUINT16() throws ParsingException, AnalysisException {
		runAnalysis(new ValueEnvironment<>(new Intervals()), NumericalSize.UINT16, "intervals-uint16");
	}

	@Test
	public void testOverflowIntervalsUINT32() throws ParsingException, AnalysisException {
		runAnalysis(new ValueEnvironment<>(new Intervals()), NumericalSize.UINT32, "intervals-uint32");
	}

	@Test
	public void testOverflowIntervalsFLOAT8() throws ParsingException, AnalysisException {
		runAnalysis(new ValueEnvironment<>(new Intervals()), NumericalSize.FLOAT8, "intervals-float8");
	}

	@Test
	public void testOverflowIntervalsFLOAT16() throws ParsingException, AnalysisException {
		runAnalysis(new ValueEnvironment<>(new Intervals()), NumericalSize.FLOAT16, "intervals-float16");
	}

	@Test
	public void testOverflowIntervalsFLOAT32() throws ParsingException, AnalysisException {
		runAnalysis(new ValueEnvironment<>(new Intervals()), NumericalSize.FLOAT32, "intervals-float32");
	}



	private <V extends ValueDomain<V>> void runAnalysis(V valueEnv, NumericalSize size, String path) throws ParsingException{
		// we parse the program to get the CFG representation of the code in it
		Program program = IMPFrontend.processFile("inputs/community-programs/overflow/889536-overflow.imp");

		// we build a new configuration for the analysis
		LiSAConfiguration conf = new DefaultConfiguration();

		// we specify where we want files to be generated
		conf.workdir = "outputs/community-programs/overflow/889536-overflow/"+path;

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
		conf.semanticChecks.add(new OverflowChecker(size));
		 
		// we instantiate LiSA with our configuration
		LiSA lisa = new LiSA(conf);
		

		// finally, we tell LiSA to analyze the program
		lisa.run(program);
	}

	
}
