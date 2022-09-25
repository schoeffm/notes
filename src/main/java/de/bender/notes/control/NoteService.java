package de.bender.notes.control;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static java.nio.file.attribute.PosixFilePermission.*;
import static java.nio.file.attribute.PosixFilePermissions.asFileAttribute;

/**
 * Service class that encapsulates a bunch of helper-methods when dealing with `notes` and `todos`
 *
 * @see Config
 */
@ApplicationScoped
public class NoteService {

    @Inject
    Config config;

    /**
     * Makes sure that file referenced by the given {@code fileName} exists - if the {@code fileName}
     * is {@code null} the default (see {@link Config}) will be used
     *
     * @param fileName optional fileName to be checked if it exists in the default notes-directory
     * @return the {@link Path}-representation of the file
     * @throws IOException if the file doesn't exist and cannot be created
     */
    public Path ensureNotesFileExists(String fileName) throws IOException {
        this.ensureDocumentsDirExists();
        Path noteFile = Optional.ofNullable(fileName)
                .map(s -> config.getNotesFilePath(s))
                .orElse(config.getNotesFilePath());

        if (Files.notExists(noteFile)) {
            Files.createFile(noteFile, asFileAttribute(Set.of(OWNER_EXECUTE, OWNER_READ, OWNER_WRITE)));
        }
        return noteFile;
    }

    /**
     * Similar to {@link #ensureNotesFileExists(String)} this method makes sure that the `todo`-file is available
     * <p/>
     * Notice: Unlike a normal `notes`-file the `todo`-file exists only once and will be changed constantly - hence
     * you cannot pass in a {@code fileName} since that one is fixed (see {@link Config} for details).
     * <p/>
     * @return the {@link Path}-representation of the file
     * @throws IOException if the file doesn't exist and cannot be created
     */
    public Path ensureTodoFileExists() throws IOException {
        return this.ensureNotesFileExists(config.getTodoFilePath().getFileName().toString());
    }

    private Path ensureNotesFileExists() throws IOException {
        return this.ensureNotesFileExists(null);
    }

    /**
     * Makes sure that the directory (based on the current {@link Config}) where `notes` and/ord `todos` will be
     * saved actually exists
     *
     * @throws IOException if the directory cannot be created
     */
    public void ensureDocumentsDirExists() throws IOException {
        if (Files.notExists(config.getDocumentPath())) {
            Files.createDirectories(config.getDocumentPath(),
                    asFileAttribute(Set.of(OWNER_EXECUTE, OWNER_READ, OWNER_WRITE)));
        }
    }

    public void reinitOutput(Path toBeInitialized) throws IOException {
        if (Files.exists(config.getDocumentOutputPath())) {
            try (Stream<Path> walk = Files.walk(config.getDocumentOutputPath())) {
                walk.sorted(Comparator.reverseOrder()).forEach(this::delete);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        Files.createDirectories(toBeInitialized, asFileAttribute(Set.of(OWNER_EXECUTE, OWNER_READ, OWNER_WRITE)));
    }

    private void delete(Path path) {
        try {
            Files.delete(path);
        } catch (IOException e) {
            System.err.printf("Unable to delete this path : %s%n%s", path, e);
        }
    }


    /**
     * Takes the given filename (with or without suffix) and tries to identify the respective file in the
     * configured document-dir. If it can be found the {@link Path}-representation is returned.
     * <p/>
     * Notice: If the {@code fileName} is {@code null} we create the default-notes file for the current day.
     * <p/>
     * @param fileName to be looked for (i.e. '2022-09-12' or '2022-09-12.md')
     * @return {@link Path} representation
     *
     * @throws IOException
     */
    public Path getNoteFile(String fileName) throws IOException {
        this.ensureDocumentsDirExists();
        return Optional.ofNullable(fileName)
                .map(name -> (fileName.matches(".*(.md|.MD)$")) ? fileName : fileName + ".md")
                .map(name -> Paths.get(config.getDocumentPath().toString(), name))
                .orElse(this.ensureNotesFileExists());
    }
}
