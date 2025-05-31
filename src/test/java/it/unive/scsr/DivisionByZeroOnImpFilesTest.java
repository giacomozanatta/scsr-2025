package it.unive.scsr;

import it.unive.lisa.LiSA;
import it.unive.lisa.analysis.SimpleAbstractState;
import it.unive.lisa.analysis.heap.pointbased.PointBasedHeap;
import it.unive.lisa.analysis.nonrelational.value.TypeEnvironment;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.analysis.types.InferredTypes;
import it.unive.lisa.conf.LiSAConfiguration;
import it.unive.lisa.conf.LiSAConfiguration.GraphType;
import it.unive.lisa.imp.IMPFrontend;
import it.unive.lisa.interprocedural.callgraph.RTACallGraph;
import it.unive.lisa.interprocedural.context.ContextBasedAnalysis;
import it.unive.lisa.interprocedural.context.FullStackToken;
import it.unive.lisa.program.Program;
import it.unive.scsr.checkers.DivisionByZeroChecker;
import it.unive.scsr.checkers.OverflowChecker;
import org.junit.Test;

public class DivisionByZeroOnImpFilesTest {

    @Test
    public void testDivisionByZeroCheckerOnSingleImpFile() throws Exception {
        String impFile = "inputs/1001389-DivByZero.imp";

        System.out.println("=== Running DivisionByZeroChecker on: " + impFile + " ===");
        Program program = IMPFrontend.processFile(impFile);

        LiSAConfiguration conf = new LiSAConfiguration();
        conf.workdir = "outputs/divzero-eval";
        conf.analysisGraphs = GraphType.HTML;
        conf.jsonOutput = true;

        conf.abstractState = new SimpleAbstractState<>(
                new PointBasedHeap(),
                new ValueEnvironment<>(new it.unive.scsr.Intervals()),
                new TypeEnvironment<>(new InferredTypes())
        );

        // Add a call graph for interprocedural analysis
        conf.callGraph = new RTACallGraph();

        conf.interproceduralAnalysis = new ContextBasedAnalysis<>(FullStackToken.getSingleton());
        conf.semanticChecks.add(new DivisionByZeroChecker(OverflowChecker.NumericalSize.INT32));

        LiSA lisa = new LiSA(conf);
        lisa.run(program);
    }
}