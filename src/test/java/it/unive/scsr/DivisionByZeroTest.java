package it.unive.scsr;

import it.unive.lisa.AnalysisException;
import it.unive.lisa.DefaultConfiguration;
import it.unive.lisa.LiSA;
import it.unive.lisa.analysis.heap.pointbased.PointBasedHeap;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.conf.LiSAConfiguration;
import it.unive.lisa.imp.IMPFrontend;
import it.unive.lisa.imp.ParsingException;
import it.unive.lisa.interprocedural.context.ContextBasedAnalysis;
import it.unive.lisa.interprocedural.context.FullStackToken;
import it.unive.lisa.program.Program;
import it.unive.scsr.checkers.DivisionByZeroChecker;
import it.unive.scsr.checkers.OverflowChecker;
import org.junit.Test;

import java.io.File;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class DivisionByZeroTest {

    @Test
    public void testDivisionByZero() throws ParsingException, AnalysisException {
        File dir = new File("inputs/exam/division/");

        // Get all input file names
        List<String> filePaths = Stream.of(dir.listFiles())
                .filter(Predicate.not(File::isDirectory))
                .map(File::getPath)
                .toList();


        Program[] programs = new Program[filePaths.size()];

        // Process all files
        for  (int i = 0; i < filePaths.size(); i++)
        {
            programs[i] = IMPFrontend.processFile(filePaths.get(i));
        }

        // we build a new configuration for the analysis
        LiSAConfiguration conf = new DefaultConfiguration();

        // we specify where we want files to be generated
        conf.workdir = "outputs/division";

        // we specify the visual format of the analysis results
        conf.analysisGraphs = LiSAConfiguration.GraphType.HTML;

        // we specify the create a json file containing warnings triggered by the analysis
        conf.jsonOutput= true;

        // we specify the analysis that we want to execute

        conf.abstractState = DefaultConfiguration.simpleState(
                // DefaultConfiguration.defaultHeapDomain(),
                // The monolithic heap is less precise, we use the point based heap
                new PointBasedHeap(),
                new ValueEnvironment<>(new Intervals()),
                DefaultConfiguration.defaultTypeDomain());

        // we specify to perform an interprocedural analysis (require to recognize calls to sources, sanitizers, and sinks)
        conf.interproceduralAnalysis = new ContextBasedAnalysis<>(FullStackToken.getSingleton());

        // Execute the Overflow Checker
        conf.semanticChecks.add(new DivisionByZeroChecker(OverflowChecker.NumericalSize.INT32));

        // we instantiate LiSA with our configuration
        LiSA lisa = new LiSA(conf);


        // finally, we tell LiSA to analyze the program


        System.out.println(programs.length);

        lisa.run(programs);
    }

}
