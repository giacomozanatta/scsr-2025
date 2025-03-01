package it.unive.scsr;

import org.junit.Test;

import it.unive.lisa.AnalysisException;
import it.unive.lisa.DefaultConfiguration;
import it.unive.lisa.LiSA;
import it.unive.lisa.analysis.dataflow.DefiniteDataflowDomain;
import it.unive.lisa.conf.LiSAConfiguration;
import it.unive.lisa.conf.LiSAConfiguration.GraphType;
import it.unive.lisa.imp.IMPFrontend;
import it.unive.lisa.imp.ParsingException;
import it.unive.lisa.program.Program;

public class CPropTest {
    @Test
    public void TestCProp() throws ParsingException, AnalysisException{
        Program prm = IMPFrontend.processFile("inputs/cprop.imp"); //obtain test cases
        LiSAConfiguration cfg = new DefaultConfiguration(); //build new configuration
        //configure the output files - cproptest folder, HTML format
        cfg.workdir = "outputs/cproptest";
        cfg.analysisGraphs = GraphType.HTML;
        //specify the analysis
        cfg.abstractState = DefaultConfiguration.simpleState(
                DefaultConfiguration.defaultHeapDomain(),
                new DefiniteDataflowDomain<>(new CProp()),
                DefaultConfiguration.defaultTypeDomain()
        );
        //instantiate LiSA with given configuration
        LiSA lisa = new LiSA(cfg);
        lisa.run(prm);
    }
}
