package de.bender.notes.boundary;

import de.bender.notes.control.Config;
import de.bender.notes.control.NoteService;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import javax.inject.Inject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Stream;

import static java.lang.System.out;

@Command(
        name = "list",
        aliases = {"ls"},
        description = "List the current notes")
public class ListCmd implements Callable<Integer> {

    @Inject
    Config config;
    @Inject
    NoteService notes;

    @Option(names = {"--compact", "-c"})
    boolean compact;

    @Override
    public Integer call() throws Exception {
        notes.ensureNotesDirExists();

        Stream<Path> fileStream = Files.list(config.getDocumentPath())
                .filter(p -> p.toString().endsWith("md"));

        if (compact) {
            fileStream.map(Path::getFileName).forEach(out::println);
        } else {
            fileStream.forEach(this::printTreeView);
        }

        return 0;
    }

    private void printTreeView(Path path) {
        out.println(path.getFileName());
        try {
            List<String> allHeadlines = Files.readAllLines(path).stream()
                    .filter(l -> l.startsWith("# "))
                    .map(l -> l.substring(2))
                    .toList();
            for (int i = 0; i < allHeadlines.size(); i++) {
                out.println(((i == allHeadlines.size()-1) ? "   └ " : "   ├ ") + allHeadlines.get(i));
            }
        } catch (IOException ignored) {
        }
    }
}
