package it.unive.scsr.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.StackWalker.Option.RETAIN_CLASS_REFERENCE;

public class FilesUtils {

    /**
     * Given a directory name, all top-level file names are retrieved.
     *
     * @param directoryName Name of the directory where the file names should be retrieved.
     * @return A set of file names within the specified directory name.
     * @throws RuntimeException If for any reason the recovery fails, the file names cannot be listed.
     */
    public static Set<Path> filenames(String directoryName) {
        // Set variable that contains the reference to the set of file names.
        Set<Path> filenames;

        // Opens the file stream for the specified directory, automatically closing it if an exception occurs.
        try (Stream<Path> stream = Files.list(Paths.get(directoryName))) {
            filenames = stream
                    .filter(file -> !Files.isDirectory(file))
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
        return "inputs" + File.separator + StackWalker
                .getInstance(RETAIN_CLASS_REFERENCE)
                .getCallerClass()
                .getSimpleName()
                .toLowerCase();
    }

    /**
     * @param suffix String to add to the default path.
     * @return The output directory composed by examining the caller class.
     */
    public static String defaultOutputDirectory(String suffix) {
        return "outputs" + File.separator + StackWalker
                .getInstance(RETAIN_CLASS_REFERENCE)
                .getCallerClass()
                .getSimpleName()
                .toLowerCase() + (suffix == null ? "" : File.separator + suffix);
    }

    public static String removeExt(Path fileName) {
        return Arrays
                .stream(fileName.toString().split("\\."))
                .findFirst()
                .orElseThrow();
    }
}
