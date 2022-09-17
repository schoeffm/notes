package de.bender.notes.boundary;


import de.bender.notes.control.Config;
import picocli.CommandLine.Command;

import javax.inject.Inject;
import java.io.IOException;
import java.util.concurrent.Callable;

@Command(
        name = "config",
        aliases = {"c"},
        description = "Handles all concerns around configuration - initializes, resets and manages the tools config"
)
public class ConfigurationCmd implements Callable<Integer> {

    @Inject
    Config config;

    @Command(name = "reset",
            description = "Resets the current config back to the default settings")
    int reset() throws IOException {
        config.reset();
        return 0;
    }
    @Command(name = "view",
            description = "Shows the current configuration file (and where it is stored)")
    int view() throws IOException {
        config.view();
        return 0;
    }

    @Command(name = "edit",
            description = "Edits the current configuration (creates a default if none exists)")
    int edit() throws Exception {
        config.edit();
        return 0;
    }

    @Override
    public Integer call() throws Exception {
        return edit();
    }

}
