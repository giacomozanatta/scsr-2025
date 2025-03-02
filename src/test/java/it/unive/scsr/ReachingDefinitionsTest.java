package it.unive.scsr;

import it.unive.lisa.analysis.dataflow.DefiniteDataflowDomain;
import org.junit.Test;

import it.unive.lisa.AnalysisException;
import it.unive.lisa.DefaultConfiguration;
import it.unive.lisa.LiSA;
import it.unive.lisa.analysis.SimpleAbstractState;
import it.unive.lisa.analysis.dataflow.PossibleDataflowDomain;
import it.unive.lisa.analysis.heap.MonolithicHeap;
import it.unive.lisa.analysis.nonrelational.value.TypeEnvironment;
import it.unive.lisa.analysis.types.InferredTypes;
import it.unive.lisa.conf.LiSAConfiguration;
import it.unive.lisa.conf.LiSAConfiguration.GraphType;
import it.unive.lisa.imp.IMPFrontend;
import it.unive.lisa.imp.ParsingException;
import it.unive.lisa.program.Program;

public class ReachingDefinitionsTest {

    @Test
    public void testRD() throws ParsingException, AnalysisException {
        Program p = IMPFrontend.processFile("inputs/reaching-definitions.imp");

        LiSAConfiguration conf = new DefaultConfiguration();
        conf.workdir = "outputs/rd";
        conf.analysisGraphs = GraphType.HTML;

        conf.abstractState = new SimpleAbstractState<>(
                // Track what we have in the heap
                new MonolithicHeap(),
                // The domain we are tracking
                new PossibleDataflowDomain<>(new ReachingDefinitions()),
                // Keep track of variable types
                new TypeEnvironment<>(new InferredTypes())
        );

        LiSA lisa = new LiSA(conf);
        lisa.run(p);
    }
}