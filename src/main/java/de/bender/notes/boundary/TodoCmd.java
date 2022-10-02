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
import java.util.Objects;
import java.util.concurrent.Callable;

@Command(name = "todo",
        aliases = {"t"},
        description = "Creates a new todo entry to the current todo-list")
public class TodoCmd implements Callable<Integer> {

    private static String DONE_PREFIX = "- [X]";
    private static String OPEN_PREFIX = "- [ ]";

    @Inject
    NoteService notes;

    @Option(names = {"-c", "--clear"},
            description = "Clears tasks that were marked done (deletes 'em)",
            defaultValue = "false")
    boolean clear;

    @Parameters(description = "Notes content to be added to the notes-log")
    List<String> newTodoContent = new ArrayList<>();

    @Override
    public Integer call() throws Exception {
        Path noteFile = notes.ensureTodoFileExists();
        List<String> currentFileContent = Files.readAllLines(noteFile);
        Files.deleteIfExists(noteFile);
        Files.createFile(noteFile);

        StringBuilder fileContent = new StringBuilder();

        // first entry is the new todo
        if (! this.newTodoContent.isEmpty()) {
            fileContent.append(OPEN_PREFIX)
                    .append(" ")
                    .append(String.join(" ", this.newTodoContent))
                    .append("\n");
        }

        for (String line : currentFileContent) {
            if (!todoIsMarkedAsDone(line)) {
                fileContent
                        .append(line)
                        .append("\n");
            }
        }

        if (fileContent.length() > 0) {
            Files.writeString(noteFile, fileContent.toString(), StandardOpenOption.WRITE);
        }

        return 0;
    }

    private boolean todoIsMarkedAsDone(String line) {
        return Objects.nonNull(line)
                && line.length() >= DONE_PREFIX.length()
                && (line.startsWith(DONE_PREFIX) || line.startsWith(DONE_PREFIX.toLowerCase()));
    }
}
