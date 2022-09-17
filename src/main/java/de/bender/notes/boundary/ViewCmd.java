package de.bender.notes.boundary;

import de.bender.notes.control.NoteService;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import javax.inject.Inject;
import java.nio.file.Path;
import java.util.concurrent.Callable;

@Command(name = "view",
        aliases = {"v"},
        description = "Views the current log-file (using mdcat)")
public class ViewCmd implements Callable<Integer> {

    @Inject
    NoteService notes;

    @Option(names = {"-f", "--file"},
            description = "Optional note-filename to be viewed")
    String fileName;

    @Override
    public Integer call() throws Exception {
        Path noteFile = notes.getNoteFile(fileName);

        Process process = new ProcessBuilder("mdcat", noteFile.toString())
                .inheritIO()
                .start();
        process.waitFor();

        return 0;
    }
}
