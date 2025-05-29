package it.unive.scsr;

import it.unive.scsr.checkers.HybridDivisionByZeroPentagonsChecker;
import it.unive.scsr.checkers.HybridOverflowPentagonsChecker;
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
import it.unive.scsr.checkers.HybridDivisionByZeroChecker;
import it.unive.scsr.HybridIntervals;

public class HybridDivByZeroTest {

    @Test
    public void testDivByZeroInterval() throws ParsingException, AnalysisException {
        runAnalysis(new ValueEnvironment<>(new HybridIntervals()), NumericalSize.UINT8, "intervals-divbyzero");
    }

    @Test
    public void testtestDivByZeroPentagons() throws ParsingException, AnalysisException {
        runAnalysis(new HybridPentagons(), NumericalSize.UINT8, "intervals-pentagons");
    }

    private <V extends ValueDomain<V>> void runAnalysis(V valueEnv, NumericalSize size, String path) throws ParsingException{
        // we parse the program to get the CFG representation of the code in it
        //Program program = IMPFrontend.processFile("inputs/890488_890441divzero.imp");
        Program program = IMPFrontend.processFile("inputs/test_div.imp");

        // we build a new configuration for the analysis
        LiSAConfiguration conf = new DefaultConfiguration();

        // we specify where we want files to be generated
        conf.workdir = "outputs/hyb-divbyzero/"+path;

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

        if(valueEnv instanceof HybridPentagons){
            conf.semanticChecks.add(new HybridDivisionByZeroPentagonsChecker(size));
        }
        else if(valueEnv instanceof ValueEnvironment){
            conf.semanticChecks.add(new HybridDivisionByZeroChecker(size));
        }
        // the OverflowChecker is executed after the numerical analysis and it checks if a abstract numerical value leads to an overflow/underflow


        // we instantiate LiSA with our configuration
        LiSA lisa = new LiSA(conf);


        // finally, we tell LiSA to analyze the program
        lisa.run(program);
    }


}
