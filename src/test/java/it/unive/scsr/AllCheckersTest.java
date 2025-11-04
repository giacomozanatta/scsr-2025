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
import it.unive.scsr.checkers.OverflowChecker;
import it.unive.scsr.checkers.TaintThreeLevelsChecker;
import it.unive.scsr.checkers.OverflowChecker.NumericalSize;
import org.junit.Test;

import java.io.File;

public class AllCheckersTest {

    @Test
    public void testAllPrograms() throws ParsingException, AnalysisException {
        File baseDir = new File("inputs");
        for (File folder : baseDir.listFiles()) {
            if (folder.isDirectory()) {
                runFolder(folder);
            }
        }
    }

    private void runFolder(File folder) throws ParsingException, AnalysisException {
        String folderName = folder.getName();
        for (File impFile : folder.listFiles((d, name) -> name.endsWith(".imp"))) {
            runAnalysis(impFile, folderName);
        }
    }

    private void runAnalysis(File impFile, String folderName) throws ParsingException, AnalysisException {
        Program program = IMPFrontend.processFile(impFile.getAbsolutePath());

        LiSAConfiguration conf = new DefaultConfiguration();
        conf.workdir = "outputs/" + folderName + "/" + impFile.getName().replace(".imp", "");
        conf.analysisGraphs = GraphType.HTML;
        conf.jsonOutput = true;
        conf.serializeResults = true;

        // Abstract state with Intervals
        conf.abstractState = DefaultConfiguration.simpleState(
                DefaultConfiguration.defaultHeapDomain(),
                new ValueEnvironment<>(new Intervals()),
                DefaultConfiguration.defaultTypeDomain());

        // Add correct checker(s) based on folder name
        switch (folderName.toLowerCase()) {
            case "overflows":
                conf.semanticChecks.add(new OverflowChecker());
                break;
            case "divzero":
                // Now requires a NumericalSize argument
                conf.semanticChecks.add(new DivisionByZeroChecker(NumericalSize.INT32));
                break;
            case "taintthree":
                conf.semanticChecks.add(new TaintThreeLevelsChecker());
                break;
            default:
                System.err.println("⚠ Unknown folder " + folderName + " → skipping " + impFile.getName());
                return;
        }

        LiSA lisa = new LiSA(conf);
        lisa.run(program);
    }
}
