package de.bender.notes.boundary;

import de.bender.notes.control.NoteService;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import javax.inject.Inject;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

@Command(name = "todo",
        aliases = {"t"},
        description = "Creates a new todo entry to the current todo-list")
public class TodoCmd implements Callable<Integer> {

    private static String DONE_PREVIX = "- [X]";
    private static String OPEN_PREVIX = "- [ ]";

    @Inject
    NoteService notes;

    @Option(names = {"-c", "--clear"},
            description = "Clears tasks that were marked done (deletes 'em)",
            defaultValue = "false")
    boolean clear;

    @Parameters(description = "Notes content to be added to the notes-log")
    List<String> content = new ArrayList<>();

    @Override
    public Integer call() throws Exception {
        Path noteFile = notes.ensureTodoFileExists();
        Map<String, List<String>> prefixedLines = Files.readAllLines(noteFile)
                .stream()
                .collect(Collectors.groupingBy(s -> s.substring(0, DONE_PREVIX.length())));


        Files.deleteIfExists(noteFile);
        Files.createFile(noteFile);

        StringBuilder content = new StringBuilder();

        // first entry is the new todo
        // String newEntry = "- [ ] " + String.join(" ", this.content) + "\n";
        if (! this.content.isEmpty()) {
            content.append("- [ ] ").append(String.join(" ", this.content)).append("\n");
            // Files.writeString(noteFile, newEntry, StandardOpenOption.APPEND);
        }

        // write open tasks first
        if (prefixedLines.containsKey(OPEN_PREVIX)) {
            for (String line : prefixedLines.get(OPEN_PREVIX)) {
                content.append(line).append("\n");
                // Files.writeString(noteFile, line + "\n", StandardOpenOption.APPEND);
            }
        }
        // then append all closed ones
        if (prefixedLines.containsKey(DONE_PREVIX) && !clear) {
            for (String line : prefixedLines.get(DONE_PREVIX)) {
                content.append(line).append("\n");
                // Files.writeString(noteFile, line + "\n", StandardOpenOption.APPEND);
            }
        }

        if (content.length() > 0) {
            Files.writeString(noteFile, content.toString(), StandardOpenOption.WRITE);
        }

        return 0;
    }
}
