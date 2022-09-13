package de.bender.notes.boundary;

import de.bender.notes.control.Config;
import io.smallrye.mutiny.tuples.Tuple2;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import javax.inject.Inject;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.IntStream;

import static java.lang.System.out;

@Command(
        name = "search",
        aliases = {"s"},
        description = "Searches the given needle in your haystack of notes. You can define several search-terms that will be OR-combined - i.e. notes search java codeing")
public class SearchCmd implements Callable<Integer> {

    @Inject
    Config config;

    @Parameters(description = "The string you'd like to look for in your notes")
    List<String> needle = new ArrayList<>();

    @Override
    public Integer call() throws Exception {
        Path documentPath = config.getDocumentPath();

        String needle = String.join("|", this.needle);

        List<Path> allNotes = Files.list(documentPath)
                .filter(Files::isRegularFile)
                .toList();
        for (Path note : allNotes) {
            List<String> allLines = Files.readAllLines(note);
            List<Tuple2<Integer, String>> matches = IntStream.range(0, allLines.size())
                    .filter(i -> allLines.get(i).matches("(?i).*(" + needle + ").*"))
                    .mapToObj(i -> Tuple2.of(i, allLines.get(i)))
                    .toList();

            matches.forEach(m -> out.println(note.getFileName() + ":" + m.getItem1() + "| " + m.getItem2()));
            out.println();
        }
        return 0;
    }
}
