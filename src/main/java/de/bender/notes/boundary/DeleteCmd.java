package de.bender.notes.boundary;

import de.bender.notes.control.NoteService;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import javax.inject.Inject;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.Callable;

import static java.lang.System.err;
import static java.lang.System.out;

@Command(name = "delete",
        aliases = {"d"},
        description = "Deletes the given notes file (if it's empty)")
public class DeleteCmd implements Callable<Integer> {

    @Inject
    NoteService notes;

    @Option(names = {"-f", "--file"},
            required = true,
            description = "Optional note-filename to be viewed")
    String fileName;

    @Override
    public Integer call() throws Exception {
        Path file = notes.getNoteFile(fileName);
        if (! Files.exists(file)) {
            err.println("Well, there is no file called '" + fileName + "' => hence, nothing to delete.");
            return 12;
        }

        List<String> fileContent = Files.readAllLines(file);

        if (! fileContent.isEmpty()) {
            err.println("The given file is not empty - are you really sure you'd like to remove that file? Y/N");
            Scanner scanner = new Scanner(System.in);
            String line = scanner.nextLine();
            if (line.equalsIgnoreCase("Y")) {
                out.println("Alright, *puff* and " + fileName + " is  gone ...");
                Files.delete(file);
            } else {
                out.println("Puh, that was close ... but relax, we keep " + fileName);
            }
        } else {
            out.println("Alright, *puff* and " + fileName + " is  gone ... (it was empty anyways)");
            Files.delete(file);
        }

        return 0;
    }
}
