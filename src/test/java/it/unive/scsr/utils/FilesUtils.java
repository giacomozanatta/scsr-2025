package it.unive.scsr.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.StackWalker.Option.RETAIN_CLASS_REFERENCE;

public class FilesUtils {

    /**
     * Given a directory name, all top-level file names are retrieved.
     * @throws RuntimeException If for any reason the recovery fails, the file names cannot be listed.
     * @param directoryName Name of the directory where the file names should be retrieved.
     * @return A set of file names within the specified directory name.
     */
    public static Set<String> filenames(String directoryName) {
        // Set variable that contains the reference to the set of file names.
        Set<String> filenames;

        // Opens the file stream for the specified directory, automatically closing it if an exception occurs.
        try (Stream<Path> stream = Files.list(Paths.get(directoryName))) {
            filenames = stream
                    .filter(file -> !Files.isDirectory(file))
                    .map(Path::toString)
                    .collect(Collectors.toSet());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // Finally returning the set.
        return filenames;
    }

    /**
     * @return The input directory composed by examining the caller class.
     */
    public static String defaultInputDirectory() {
        return "inputs/" + StackWalker
                .getInstance(RETAIN_CLASS_REFERENCE)
                .getCallerClass()
                .getSimpleName()
                .toLowerCase();
    }

    /**
     * @return The output directory composed by examining the caller class.
     */
    public static String defaultOutputDirectory() {
        return "outputs/" + StackWalker
                .getInstance(RETAIN_CLASS_REFERENCE)
                .getCallerClass()
                .getSimpleName()
                .toLowerCase();
    }
}
