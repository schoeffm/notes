package de.bender.notes.boundary;

import de.bender.notes.control.Config;
import picocli.CommandLine.Command;

import javax.inject.Inject;
import java.nio.file.Paths;
import java.util.concurrent.Callable;

@Command(name = "open",
        aliases = {"o"},
        description = "Opens the rendered output in your default browser")
public class OpenCmd implements Callable<Integer> {

    @Inject
    Config config;

    @Override
    public Integer call() throws Exception {
        String filePathAsString = Paths
                .get(config.getDocumentOutputPath().toString(), Config.OUTPUT_INDEX_FILE_NAME)
                .toString();

        new ProcessBuilder("open", filePathAsString) .start();

        return 0;
    }
}
