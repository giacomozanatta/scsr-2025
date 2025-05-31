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

public class DivisionByZeroOnImpMultipleFilesTest {

    @Test
    public void testDivisionByZeroCheckerOnMultipleImpFiles() throws Exception {
        String[] impFiles = {
            "inputs/DivByZero/879899-3-by-zero.imp",
            "inputs/DivByZero/880036_divby0_test_v0.imp",
            "inputs/DivByZero/880092-division-by-zero.imp",
            "inputs/DivByZero/884046-DivByZero.imp",
            "inputs/DivByZero/885768_divbyzero.imp",
            "inputs/DivByZero/885768_divbyzero_testcases.imp",
            "inputs/DivByZero/890488_890441divzero.imp",
            "inputs/DivByZero/890550_divbyzero.imp",
            "inputs/DivByZero/892631-divbyzero.imp",
            "inputs/DivByZero/908677-benchmark-divbyzero.imp",
        };

        for (String impFile : impFiles) {
            System.out.println("=== Running DivisionByZeroChecker on: " + impFile + " ===");
            Program program = IMPFrontend.processFile(impFile);

            LiSAConfiguration conf = new LiSAConfiguration();
            conf.workdir = "outputs/divzero-eval";
            conf.analysisGraphs = GraphType.HTML;
            conf.jsonOutput = true;

            // Set lower widening thresholds to avoid endless fixpoint
            conf.wideningThreshold = 2;
            conf.recursionWideningThreshold = 2;

            conf.abstractState = new SimpleAbstractState<>(
                    new PointBasedHeap(),
                    new ValueEnvironment<>(new it.unive.scsr.Intervals()),
                    new TypeEnvironment<>(new InferredTypes())
            );

            conf.callGraph = new RTACallGraph();

            conf.interproceduralAnalysis = new ContextBasedAnalysis<>(FullStackToken.getSingleton());
            conf.semanticChecks.add(new DivisionByZeroChecker(OverflowChecker.NumericalSize.INT32));

            LiSA lisa = new LiSA(conf);
            lisa.run(program);
            System.out.println("=== Finished: " + impFile + " ===");
        }
    }
}
