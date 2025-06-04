package it.unive.scsr;

import it.unive.scsr.checkers.DivisionByZeroChecker;
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
import it.unive.lisa.interprocedural.context.ContextBasedAnalysis;
import it.unive.lisa.interprocedural.context.FullStackToken;
import it.unive.lisa.program.ClassUnit;
import it.unive.lisa.program.Program;
import it.unive.lisa.program.Unit;
import it.unive.lisa.program.cfg.CodeMember;
import it.unive.lisa.program.cfg.Parameter;
import it.unive.scsr.checkers.TaintChecker;

import static it.unive.scsr.checkers.OverflowChecker.NumericalSize.FLOAT32;

public class DivisionBy0Test {


    @Test
    public void TestDivision() throws ParsingException, AnalysisException {
        // we parse the program to get the CFG representation of the code in it
        Program program = IMPFrontend.processFile("inputs/div0.imp");


        // we build a new configuration for the analysis
        LiSAConfiguration conf = new DefaultConfiguration();

        // we specify where we want files to be generated
        conf.workdir = "outputs/div0";

        // we specify the visual format of the analysis results
        conf.analysisGraphs = GraphType.HTML;

        // we specify the create a json file containing warnings triggered by the analysis
        conf.jsonOutput = true;

        // we specify the analysis that we want to execute

        conf.abstractState = DefaultConfiguration.simpleState(
                DefaultConfiguration.defaultHeapDomain(),
                new ValueEnvironment<>(new Intervals()),
                DefaultConfiguration.defaultTypeDomain());


        // the TaintChecker is executed after the Taint analysis and it checks if a tainted value is flowed in a sink
        conf.semanticChecks.add(new DivisionByZeroChecker(FLOAT32));

        // we instantiate LiSA with our configuration
        LiSA lisa = new LiSA(conf);


        // finally, we tell LiSA to analyze the program
        lisa.run(program);
    }
}

