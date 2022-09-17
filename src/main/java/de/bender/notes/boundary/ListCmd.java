package de.bender.notes.boundary;

import de.bender.notes.control.Config;
import de.bender.notes.control.NoteService;
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

    public static final String TREE_END_ELEMENT = "   └ ";
    public static final String TREE_MID_ELEMENT = "   ├ ";

    @Inject
    Config config;
    @Inject
    NoteService notes;

    @Option(names = {"--compact", "-c"}, description = "Shows the list-view in a compacted format (less information)")
    boolean compact;

    @Override
    public Integer call() throws Exception {
        notes.ensureDocumentsDirExists();

        Stream<Path> fileStream = Files.list(config.getDocumentPath())
                .filter(Files::isRegularFile)
                .filter(p -> p.toString().endsWith("md"))
                .sorted();

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
                    .filter(l -> l.startsWith("# "))        // look for H1 headlines
                    .map(l -> l.substring(2))     // only use the text (remove the markdown #)
                    .toList();
            for (int i = 0; i < allHeadlines.size(); i++) {
                out.println(((i == allHeadlines.size()-1) ? TREE_END_ELEMENT : TREE_MID_ELEMENT) + allHeadlines.get(i));
            }
            out.println();
        } catch (IOException ignored) {
        }
    }
}
