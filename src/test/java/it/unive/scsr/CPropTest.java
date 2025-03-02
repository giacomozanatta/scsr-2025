package it.unive.scsr;

import it.unive.lisa.AnalysisException;
import it.unive.lisa.DefaultConfiguration;
import it.unive.lisa.LiSA;
import it.unive.lisa.analysis.SimpleAbstractState;
import it.unive.lisa.analysis.dataflow.DefiniteDataflowDomain;
import it.unive.lisa.analysis.heap.MonolithicHeap;
import it.unive.lisa.analysis.nonrelational.value.TypeEnvironment;
import it.unive.lisa.analysis.types.InferredTypes;
import it.unive.lisa.conf.LiSAConfiguration;
import it.unive.lisa.imp.IMPFrontend;
import it.unive.lisa.imp.ParsingException;
import it.unive.lisa.program.Program;
import org.junit.Test;

public class CPropTest {

    @Test
    public void testCProp() throws ParsingException, AnalysisException {
        Program p = IMPFrontend.processFile("inputs/cprop.imp");

        LiSAConfiguration conf = new DefaultConfiguration();
        conf.workdir = "outputs/cprop";
        conf.analysisGraphs = LiSAConfiguration.GraphType.HTML;

        conf.abstractState = new SimpleAbstractState<>(
                // Track what we have in the heap
                new MonolithicHeap(),
                // The domain we are tracking
                new DefiniteDataflowDomain<>(new CProp()),
                // Keep track of variable types
                new TypeEnvironment<>(new InferredTypes())
        );

        LiSA lisa = new LiSA(conf);
        lisa.run(p);
    }
}
