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
import it.unive.scsr.checkers.OverflowChecker;
import org.junit.Test;

public class OverflowOnMultipleIMPFilesTest {

    @Test
    public void testOverflowCheckerOnMultipleImpFiles() throws ParsingException, AnalysisException {
        // List all your IMP files here
        String[] impFiles = {
                "inputs/OverflowImpFiles/875290-Overflow.imp",
                "inputs/OverflowImpFiles/879899-1-overflow.imp",
                "inputs/OverflowImpFiles/879899-2-overflow.imp",
                "inputs/OverflowImpFiles/879899-3-overflow.imp",
                "inputs/OverflowImpFiles/879899-4-overflow.imp",
                "inputs/OverflowImpFiles/880036_overflow_test_v0.imp",
                "inputs/OverflowImpFiles/884046-Overflow.imp",
                "inputs/OverflowImpFiles/885768_overflow.imp",
                "inputs/OverflowImpFiles/890488_890441overflow.imp",
                "inputs/OverflowImpFiles/908677-benchmark-overflow.imp",
        };

        for (String impFile : impFiles) {
            System.out.println("=== Running OverflowChecker on: " + impFile + " ===");
            Program program = IMPFrontend.processFile(impFile);

            LiSAConfiguration conf = new DefaultConfiguration();
            conf.workdir = "outputs/overflow-eval";
            conf.analysisGraphs = GraphType.HTML;
            conf.jsonOutput = true;

            conf.abstractState = DefaultConfiguration.simpleState(
                    DefaultConfiguration.defaultHeapDomain(),
                    new ValueEnvironment<>(new Intervals()),
                    DefaultConfiguration.defaultTypeDomain());

            conf.interproceduralAnalysis = new ContextBasedAnalysis<>(FullStackToken.getSingleton());
            conf.semanticChecks.add(new OverflowChecker(OverflowChecker.NumericalSize.INT8));
            conf.semanticChecks.add(new OverflowChecker(OverflowChecker.NumericalSize.UINT8));
            conf.semanticChecks.add(new OverflowChecker(OverflowChecker.NumericalSize.INT16));
            conf.semanticChecks.add(new OverflowChecker(OverflowChecker.NumericalSize.UINT16));
            conf.semanticChecks.add(new OverflowChecker(OverflowChecker.NumericalSize.INT32));
            conf.semanticChecks.add(new OverflowChecker(OverflowChecker.NumericalSize.UINT32));

            LiSA lisa = new LiSA(conf);
            lisa.run(program);
        }
    }
}
