package it.unive.scsr;

import org.junit.Test;

import it.unive.lisa.AnalysisException;
import it.unive.lisa.DefaultConfiguration;
import it.unive.lisa.LiSA;
import it.unive.lisa.analysis.heap.pointbased.PointBasedHeap;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.conf.LiSAConfiguration;
import it.unive.lisa.conf.LiSAConfiguration.GraphType;
import it.unive.lisa.imp.IMPFrontend;
import it.unive.lisa.imp.ParsingException;
import it.unive.lisa.interprocedural.context.ContextBasedAnalysis;
import it.unive.lisa.interprocedural.context.FullStackToken;
import it.unive.lisa.program.Program;
import it.unive.scsr.Intervals;
import it.unive.scsr.checkers.DivisionByZeroChecker;
import it.unive.scsr.checkers.OverflowChecker.NumericalSize;


public class DivisionByZeroCheckerTest {

    @Test
    public void testDivisionByZero() throws ParsingException, AnalysisException {

        // Parse input program
        Program program = IMPFrontend.processFile("inputs/studentsIMP/908677-benchmark-divbyzero.imp");

        // Configure LiSA
        LiSAConfiguration conf = new DefaultConfiguration();
        conf.workdir = "outputs/divbyzero-checker/students";
        conf.analysisGraphs = GraphType.HTML;
        conf.jsonOutput = true;                        // JSON report

        // Configure abstract state
        conf.abstractState = DefaultConfiguration.simpleState(
                DefaultConfiguration.defaultHeapDomain(),                  // PointBasedHeap
                new ValueEnvironment<>(new Intervals()),                   // Uses intervals
                DefaultConfiguration.defaultTypeDomain());

        // Configure interprocedural analysis
        conf.interproceduralAnalysis = new ContextBasedAnalysis<>(
                FullStackToken.getSingleton());

        // Add semantic check for each numerical size
       // for (NumericalSize size : NumericalSize.values())  adds redundancy if num size = size
            conf.semanticChecks.add(new DivisionByZeroChecker(NumericalSize.FLOAT16));

        // Launch LiSA
        LiSA lisa = new LiSA(conf);
        lisa.run(program);
    }
}