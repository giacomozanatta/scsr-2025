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
import it.unive.scsr.checkers.OverflowChecker;
import it.unive.scsr.checkers.OverflowChecker.NumericalSize;


public class OverflowCheckerTest {

    @Test
    public void testOverflow() throws ParsingException, AnalysisException {

        // Parse input program
        Program program = IMPFrontend.processFile("inputs/studentsIMP/879899-overflow-wip.imp");

        // Configure LiSA
        LiSAConfiguration conf = new DefaultConfiguration();
        conf.workdir = "outputs/overflow-checker";
        conf.analysisGraphs = GraphType.NONE;          // No graphs or HTML
        conf.jsonOutput = true;                        // JSON report

        // Configure abstract state
        conf.abstractState = DefaultConfiguration.simpleState(
                DefaultConfiguration.defaultHeapDomain(),                  // PointBasedHeap
                new ValueEnvironment<>(new Intervals()),                   // Uses interval
                DefaultConfiguration.defaultTypeDomain());

        // Configure interprocedural analysis
        conf.interproceduralAnalysis = new ContextBasedAnalysis<>(
                FullStackToken.getSingleton());

        // Check every test case for every numerical size
        for (NumericalSize size : NumericalSize.values())
           conf.semanticChecks.add(new OverflowChecker(size));

        // Launch LiSA
        LiSA lisa = new LiSA(conf);
        lisa.run(program);
    }
}