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


        LiSAConfiguration conf = new DefaultConfiguration();


        conf.workdir = "outputs/overflows";


        conf.analysisGraphs = GraphType.HTML;


        conf.jsonOutput = true;


        conf.abstractState = DefaultConfiguration.simpleState(
                DefaultConfiguration.defaultHeapDomain(),
                new Pentagons(),
                DefaultConfiguration.defaultTypeDomain());


        conf.semanticChecks.add(new OverflowChecker(NumericalSize.INT32));


        LiSA lisa = new LiSA(conf);


        lisa.run(program);
    }
}