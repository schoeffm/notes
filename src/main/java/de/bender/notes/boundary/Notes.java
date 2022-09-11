package de.bender.notes.boundary;

import com.github.rjeschke.txtmark.Processor;
import de.bender.notes.control.Config;
import de.bender.notes.control.NoteService;
import io.quarkus.picocli.runtime.annotations.TopCommand;
import io.quarkus.qute.Template;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.HelpCommand;

import javax.inject.Inject;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.nio.file.StandardOpenOption.APPEND;

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

    @Inject
    Template render;

    @CommandLine.Option(names = {"-f", "--file"},
            required = false,
            description = "Optional filename the given note should be added")
    String fileName;

    @Command(name = "edit",
            aliases = {"e"},
            description = "Opens the current log-file for editing")
    Integer edit() throws InterruptedException, IOException {
        notes.ensureNotesDirExists();
        Path noteFile = Optional.ofNullable(fileName)
                .map(Paths::get)
                .orElse(notes.ensureNotesFileExists());

        Process process = new ProcessBuilder(config.getEditor(), noteFile.toString()).inheritIO().start();
        process.waitFor();

        return 0;
    }

    @Command(name = "view",
            aliases = {"v"},
            description = "Views the current log-file (using mdcat)")
    Integer view() throws InterruptedException, IOException {
        notes.ensureNotesDirExists();
        Path noteFile = notes.ensureNotesFileExists();

        Process process = new ProcessBuilder("mdcat", noteFile.toString())
                .inheritIO()
                .start();
        process.waitFor();

        return 0;
    }

    @Command(name = "render",
            aliases = {"r"},
            description = "Renders HTML")
    Integer render() throws IOException {
        notes.ensureNotesDirExists();
        notes.reinitOutputDir();

        List<Path> markdownFiles = Files.list(config.getDocumentPath())
                .filter(p -> p.toString().endsWith("md"))
                .toList();
        for (Path filePath : markdownFiles) {
            String output = Processor
                    .process(String.join("\n", Files.readAllLines(filePath)));

            String htmlOutput = render
                    .data("markdown_output", output)
                    .render();

            Path file = Files.createFile(Paths.get(config.getDocumentOutputPath().toString(), filePath.getFileName() + ".html"));
            Files.writeString(file, htmlOutput, APPEND);
        }

        return 0;
    }
    @Command(name = "ls",
            description = "Lists the current content of the notes-directory")
    Integer ls() throws IOException, InterruptedException {
        notes.ensureNotesDirExists();

        Files.list(config.getDocumentPath())
                .filter(p -> p.toString().endsWith("md"))
                .map(Path::getFileName)
                .forEach(System.out::println);

        return 0;
    }

    @Command(name = "pandoc",
            aliases = {"p"},
            description = "Renders using pandoc ")
    Integer pandoc() throws IOException, InterruptedException {
        notes.ensureNotesDirExists();
        notes.reinitOutputDir();

        List<Path> markdownFiles = Files.list(config.getDocumentPath())
                .filter(p -> p.toString().endsWith("md"))
                .toList();

        for (Path filePath : markdownFiles) {
            Process process = new ProcessBuilder("pandoc", "--standalone", filePath.toString(), "--metadata", "title=\"rendered\"").start();
            process.waitFor();
            String renderedOutput = String.join("\n", readProcessOutput(process));
            Path file = Files.createFile(Paths.get(config.getDocumentOutputPath().toString(), filePath.getFileName() + ".html"));
            Files.writeString(file, renderedOutput, APPEND);
        }

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
