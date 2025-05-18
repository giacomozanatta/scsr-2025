package it.unive.scsr.resources;

import it.unive.lisa.DefaultConfiguration;
import it.unive.lisa.LiSA;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.checks.semantic.SemanticCheck;
import it.unive.lisa.conf.LiSAConfiguration;
import it.unive.lisa.interprocedural.context.ContextBasedAnalysis;
import it.unive.lisa.interprocedural.context.FullStackToken;
import it.unive.scsr.utils.ProgramsUtils;
import java.util.Collection;
import java.util.function.Consumer;

public class ProgramResource<S extends SemanticCheck<?>, V extends ValueDomain<V>> {

    private final String inputFile;
    private final String outputDirectory;

    /**
     * @param inputFile Path to the IMP file to create a LiSA program.
     * @param outputDirectory Output directory where the analysis results will be saved.
     */
    public ProgramResource(String inputFile, String outputDirectory) {
        this.inputFile = inputFile;
        this.outputDirectory = outputDirectory;
    }

    /**
     * Create an instance of {@link Runnable} to perform semantic checks on this program.
     * @param checkers The semantic checks to be performed.
     * @param domain Domain used to perform the analysis.
     * @param consumer Allow changing the default configuration for {@link LiSAConfiguration}.
     * @return A {@link Runnable} instance to perform semantic checks on this program.
     */
    public Runnable toLiSARunner(
            Collection<S> checkers,
            V domain,
            Consumer<LiSAConfiguration> consumer) {
        return () -> {

            // Create the default configuration and set the fields to some default values.
            final LiSAConfiguration configuration = new DefaultConfiguration();

            configuration.workdir = outputDirectory;
            configuration.analysisGraphs = LiSAConfiguration.GraphType.HTML;
            configuration.jsonOutput = true;

            configuration.abstractState =
                    DefaultConfiguration.simpleState(
                            DefaultConfiguration.defaultHeapDomain(),
                            domain,
                            DefaultConfiguration.defaultTypeDomain());

            configuration.interproceduralAnalysis = new ContextBasedAnalysis<>(FullStackToken.getSingleton());
            configuration.semanticChecks.addAll(checkers);

            // If the specified consumer is not null, the default configuration can be changed.
            if (consumer != null) {
                consumer.accept(configuration);
            }

            // Compile LiSA and run this program.
            new LiSA(configuration).run(ProgramsUtils.buildOrThrow(inputFile));
        };
    }
}
