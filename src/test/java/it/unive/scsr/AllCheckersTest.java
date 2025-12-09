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
import it.unive.scsr.checkers.OverflowChecker.NumericalSize;
import it.unive.scsr.checkers.TaintThreeLevelsChecker;
import org.junit.Test;

import java.io.File;

public class AllCheckersTest {

    @Test
    public void testAllPrograms() throws ParsingException, AnalysisException {
        File baseDir = new File("inputs");
        if (!baseDir.exists() || !baseDir.isDirectory()) {
            System.err.println("Input directory 'inputs' not found.");
            return;
        }

        for (File folder : baseDir.listFiles()) {
            if (folder.isDirectory()) {
                runFolder(folder);
            }
        }
    }

    private void runFolder(File folder) throws ParsingException, AnalysisException {
        String folderName = folder.getName();
        File[] impFiles = folder.listFiles((d, name) -> name.endsWith(".imp"));
        if (impFiles == null) return;

        for (File impFile : impFiles) {
            runAnalysis(impFile, folderName);
        }
    }

    private void runAnalysis(File impFile, String folderName) throws ParsingException, AnalysisException {
        System.out.println("Analyzing: " + impFile.getName() + " [" + folderName + "]");

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

        switch (folderName.toLowerCase()) {
            case "overflows":
                // Run multiple checkers to cover different student test cases
                conf.semanticChecks.add(new OverflowChecker(NumericalSize.INT32));
                conf.semanticChecks.add(new OverflowChecker(NumericalSize.FLOAT8));
                break;
            case "divzero":
                conf.semanticChecks.add(new DivisionByZeroChecker(NumericalSize.INT32));
                break;
            case "taintthree":
                conf.semanticChecks.add(new TaintThreeLevelsChecker());
                break;
            default:
                break;
        }

        LiSA lisa = new LiSA(conf);
        lisa.run(program);
    }
}