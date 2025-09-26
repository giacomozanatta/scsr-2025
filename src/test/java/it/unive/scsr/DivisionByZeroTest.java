package it.unive.scsr;

import it.unive.lisa.AnalysisException;
import it.unive.lisa.DefaultConfiguration;
import it.unive.lisa.LiSA;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.conf.LiSAConfiguration;
import it.unive.lisa.conf.LiSAConfiguration.GraphType;
import it.unive.lisa.imp.IMPFrontend;
import it.unive.lisa.imp.ParsingException;
import it.unive.lisa.program.Program;
import it.unive.scsr.checkers.DivisionByZeroChecker;
import it.unive.scsr.checkers.OverflowChecker.NumericalSize;
import it.unive.scsr.Intervals;
import org.junit.Test;

import java.io.File;

public class DivisionByZeroTest {

    @Test
    public void testDivisionByZero() throws ParsingException, AnalysisException {
        // Parse the class-based IMP program
        Program program = IMPFrontend.processFile("inputs/divisionbyzero.imp");

        // Prepare output directory
        String outputDir = "outputs/divisionbyzero";
        new File(outputDir).mkdirs();

        // Configure LiSA
        LiSAConfiguration conf = new DefaultConfiguration();
        conf.workdir = outputDir;
        conf.analysisGraphs = GraphType.HTML;
        conf.jsonOutput = true;
        conf.serializeResults = true;

        // Configure abstract state with intervals
        conf.abstractState = DefaultConfiguration.simpleState(
                DefaultConfiguration.defaultHeapDomain(),
                new ValueEnvironment<>(new Intervals()),
                DefaultConfiguration.defaultTypeDomain());




        // Register the division-by-zero checker
        conf.semanticChecks.add(new DivisionByZeroChecker(NumericalSize.INT32));

        // Run the analysis
        LiSA lisa = new LiSA(conf);
        lisa.run(program);
    }
}