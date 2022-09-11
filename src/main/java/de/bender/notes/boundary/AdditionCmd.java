package de.bender.notes.boundary;

import de.bender.notes.control.NoteService;
import io.quarkus.qute.Template;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import javax.inject.Inject;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

@Command(name = "add",
        aliases = {"a"},
        description = "Creates a new entry to the notes-log")
public class AdditionCmd implements Callable<Integer> {

    private static final String TS_PATTERN_FORMAT = "HH:mm:ss";

    @Inject
    NoteService notes;

    @Inject
    Template note;

    @Parameters(description = "Notes content to be added to the notes-log")
    List<String> content = new ArrayList<>();

    @Option(names = {"-f", "--file"},
            required = false,
            description = "Optional filename the given note should be added")
    String fileName;

    @Override
    public Integer call() throws Exception {
        notes.ensureNotesDirExists();
        Path noteFile = notes.ensureNotesFileExists(fileName);
        var tsFormatter = DateTimeFormatter.ofPattern(TS_PATTERN_FORMAT).withZone(ZoneId.systemDefault());

        if (! this.content.isEmpty()) {
            String newEntry = note
                    .data("headline", content.get(0))
                    .data( "body", content.size() > 1 ? String.join("\n", content) : "")
                    .data( "timestamp", tsFormatter.format(Instant.now()))
                    .render();

            Files.writeString(noteFile, newEntry, StandardOpenOption.APPEND);
        }

        return 0;
    }
}
