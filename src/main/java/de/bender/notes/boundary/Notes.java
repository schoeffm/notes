package de.bender.notes.boundary;

import io.quarkus.picocli.runtime.annotations.TopCommand;
import picocli.CommandLine.Command;
import picocli.CommandLine.HelpCommand;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.concurrent.Callable;

@TopCommand
@Command(name = "notes", mixinStandardHelpOptions = true,
        version = "1.0.0",
        subcommands = {
                Completion.class,
                HelpCommand.class,
                ViewCmd.class,
                OpenCmd.class,
                EditCmd.class,
                TodoCmd.class,
                ConfigurationCmd.class,
                AdditionCmd.class,
                ListCmd.class,
                RenderCmd.class,
                SearchCmd.class},
        description = "Simple notes-taking app")
public class Notes implements Callable<Integer> {

    @Override
    public Integer call() { return 5; }


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
