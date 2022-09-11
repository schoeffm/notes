package de.bender.notes.boundary;

import de.bender.notes.control.Config;
import picocli.CommandLine;
import picocli.CommandLine.Parameters;

import javax.inject.Inject;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

@CommandLine.Command(
        name = "search",
        aliases = {"s"},
        description = "Searches the given needle in your haystack of notes")
public class SearchCmd implements Callable<Integer> {

    @Inject
    Config config;

    @Parameters(description = "The string you'd like to look for in your notes")
    List<String> needle = new ArrayList<>();

    @Override
    public Integer call() throws Exception {
        Path documentPath = config.getDocumentPath();

        String needle = String.join(" ", this.needle);

        Process process = new ProcessBuilder( "/bin/sh", "-c",
                "grep -i '"+ needle +"' " + documentPath.toString()+"/*.md")
                .start();
        process.waitFor();

        Notes.readProcessOutput(process).forEach(System.out::println);

        return 0;
    }
}
