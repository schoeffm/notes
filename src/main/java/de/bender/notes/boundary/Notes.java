package de.bender.notes.boundary;

import de.bender.notes.control.Config;
import de.bender.notes.control.NoteService;
import io.quarkus.picocli.runtime.annotations.TopCommand;
import picocli.CommandLine.Command;
import picocli.CommandLine.HelpCommand;

import javax.inject.Inject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;

@TopCommand
@Command(name = "notes", mixinStandardHelpOptions = true,
        version = "1.0.0",
        subcommands = { Completion.class, HelpCommand.class, ConfigurationCmd.class, AdditionCmd.class, SearchCmd.class },
        description = "Simple notes-taking app")
public class Notes implements Callable<Integer> {

    @Inject
    Config config;

    @Inject
    NoteService notes;

    @Command(name = "edit",
            aliases = {"e"},
            description = "Opens the current log-file for editing")
    Integer edit() throws InterruptedException, IOException {
        notes.ensureNotesDirExists();
        Path noteFile = notes.ensureNotesFileExists();

        Process process = new ProcessBuilder(config.getEditor(), noteFile.toString()).inheritIO().start();
        process.waitFor();

        return 0;
    }

    @Override
    public Integer call() { return 0; }


    public static List<String> readProcessOutput(Process process) throws IOException, InterruptedException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

        String line;
        List<String> outputLines = new java.util.ArrayList<>(List.of());
        while ((line = reader.readLine()) != null) {
            outputLines.add(line);
        }

        process.waitFor();
        return outputLines;
    }
}
