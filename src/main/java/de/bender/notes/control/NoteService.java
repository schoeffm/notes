package de.bender.notes.control;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Set;

import static java.nio.file.attribute.PosixFilePermission.*;
import static java.nio.file.attribute.PosixFilePermissions.asFileAttribute;

@ApplicationScoped
public class NoteService {

    @Inject
    Config config;

    public Path ensureNotesFileExists() throws IOException {
        return this.ensureNotesFileExists(null);
    }

    public Path ensureNotesFileExists(String fileName) throws IOException {
        Path noteFile = Optional.ofNullable(fileName)
                .map(s -> config.getNotesFilePath(s))
                .orElse(config.getNotesFilePath());

        if (Files.notExists(noteFile)) {
            Files.createFile(noteFile, asFileAttribute(Set.of(OWNER_EXECUTE, OWNER_READ, OWNER_WRITE)));
        }
        return noteFile;
    }

    public void ensureNotesDirExists() throws IOException {
        if (Files.notExists(config.getDocumentPath())) {
            Files.createDirectories(config.getDocumentPath(),
                    asFileAttribute(Set.of(OWNER_EXECUTE, OWNER_READ, OWNER_WRITE)));
        }
    }
}
