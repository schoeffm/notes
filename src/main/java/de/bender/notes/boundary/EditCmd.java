package de.bender.notes.boundary;

import de.bender.notes.control.Config;
import de.bender.notes.control.NoteService;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import javax.inject.Inject;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.concurrent.Callable;

@Command(name = "edit",
        aliases = {"e"},
        description = "Opens the current log-file for editing")
public class EditCmd implements Callable<Integer> {

    @Inject
    Config config;

    @Inject
    NoteService notes;

    @Option(names = {"-f", "--file"},
            description = "Optional note-filename to be edited")
    String fileName;

    @Override
    public Integer call() throws Exception {
        notes.ensureNotesDirExists();
        Path noteFile = Optional.ofNullable(fileName)
                .map(name -> (fileName.matches(".*(.md|.MD)$")) ? fileName : fileName + ".md")
                .map(name -> Paths.get(config.getDocumentPath().toString(), name))
                .orElse(notes.ensureNotesFileExists());

        Process process = new ProcessBuilder(config.getEditor(), noteFile.toString()).inheritIO().start();
        process.waitFor();

        return 0;
    }
}
