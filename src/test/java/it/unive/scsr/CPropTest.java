package it.unive.scsr;

import it.unive.lisa.AnalysisException;
import it.unive.lisa.DefaultConfiguration;
import it.unive.lisa.LiSA;
import it.unive.lisa.analysis.dataflow.DefiniteDataflowDomain;
import it.unive.lisa.conf.LiSAConfiguration;
import it.unive.lisa.conf.LiSAConfiguration.GraphType;
import it.unive.lisa.imp.IMPFrontend;
import it.unive.lisa.imp.ParsingException;
import it.unive.lisa.program.Program;
import org.junit.Test;

public class CPropTest {

    @Test
    public void testCProp() throws ParsingException, AnalysisException {

        Program program = IMPFrontend.processFile("inputs/cprop.imp");
        LiSAConfiguration conf = new DefaultConfiguration();

        conf.workdir = "outputs/cprop";
        conf.analysisGraphs = GraphType.HTML;
        conf.abstractState = DefaultConfiguration.simpleState(
                DefaultConfiguration.defaultHeapDomain(),
                new DefiniteDataflowDomain<>(new CProp()),
                DefaultConfiguration.defaultTypeDomain()
        );

        LiSA lisa = new LiSA(conf);
        lisa.run(program);
    }
}