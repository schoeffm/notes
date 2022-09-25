package de.bender.notes.boundary;

import de.bender.notes.control.Config;
import de.bender.notes.control.NoteService;
import io.quarkus.qute.Template;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.smallrye.mutiny.tuples.Tuple2;
import org.commonmark.Extension;
import org.commonmark.ext.autolink.AutolinkExtension;
import org.commonmark.ext.gfm.strikethrough.StrikethroughExtension;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.ext.heading.anchor.HeadingAnchorExtension;
import org.commonmark.ext.image.attributes.ImageAttributesExtension;
import org.commonmark.ext.task.list.items.TaskListItemsExtension;
import org.commonmark.node.*;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.NodeRenderer;
import org.commonmark.renderer.html.CoreHtmlNodeRenderer;
import org.commonmark.renderer.html.HtmlNodeRendererContext;
import org.commonmark.renderer.html.HtmlRenderer;
import org.commonmark.renderer.html.HtmlWriter;
import org.commonmark.renderer.text.*;
import picocli.CommandLine.Command;

import javax.inject.Inject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.nio.file.StandardOpenOption.APPEND;

@Command(name = "render",
        aliases = {"r"},
        description = "Renders a DocSet which is ready to be imported/used by a doc-browser like Dash. If you don't have such a thing you can just use `notes open` to show an index.html in our default browser.")
public class RenderCmd implements Callable<Integer> {

    private static final String DOCSET_NAME = "Notes.docset";
    private static final String CONTENTS = "Contents";
    private static final String RESOURCES = "Resources";
    private static final String DOCUMENTS = "Documents";

    private static final List<Extension> markdownExtensions = List.of(
            AutolinkExtension.create(),
            StrikethroughExtension.create(),
            HeadingAnchorExtension.create(),
            ImageAttributesExtension.create(),
            TaskListItemsExtension.create(),
            TablesExtension.create()
    );

    @Inject
    Config config;

    @Inject
    NoteService notes;

    @Inject
    Template render;

    @Inject
    Template main;

    @Inject
    Template plist;

    @Inject
    Template toc;

    @Inject
    Template index;


    @Override
    public Integer call() throws Exception {
        notes.ensureDocumentsDirExists();           // just make sure that the documents-dir exists (we read from it)
        notes.reinitOutput(documentsPath());        // make sure the output-dir-structure exists and is empty

        copyStaticContent();
        renderInfoPlist();
        renderMarkdownFiles();                      // process markdown and render HTML output
        renderSqliteDb();                           // use sqlite3 CLI client to create and fill the searchIndex
        renderMainPage();                           // dash also has the possibility to render an overview page (called MainPage)
        renderTocFrame();                           // for those that don't use a docset-reader create a frame-set index

        return 0;
    }

    private void copyStaticContent() throws IOException {
        Files.walk(config.getStaticContentPath())
                .forEach(sourcePath -> {
                    Path destination = pathOf(documentsPath(), sourcePath.getFileName().toString());
                    try {
                        Files.copy(sourcePath, destination);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
    }

    /**
     * Since I couldn't get sqlite jdbc driver running in native-image I use the sqlite3 CLI tool as a
     * poor-mans alternative.
     */
    private void renderSqliteDb() throws IOException, InterruptedException {
        try (Stream<Path> list = Files.list(documentsPath())) {
            List<String> sqlLiteCmd = List.of("sqlite3", pathOf(resourcesPath(), "docSet.dsidx").toString());

            List<String> creations = List.of(
                    "CREATE TABLE searchIndex(id INTEGER PRIMARY KEY, name TEXT, type TEXT, path TEXT);",
                    "CREATE UNIQUE INDEX anchor ON searchIndex (name, type, path);");

            List<String> inserts = list
                    .map(Path::getFileName)
                    .filter(p -> p.getFileName().toString().endsWith("html"))
                    .map(Path::toString)
                    .map(f -> Tuple2.of(removeAnyExtension(f), f))
                    .map(t -> String.format("INSERT OR IGNORE INTO searchIndex(name, type, path) VALUES ('%s', 'Entry', '%s');", t.getItem1(), t.getItem2()))
                    .toList();

            List<String> command = new ArrayList<>(sqlLiteCmd);
            command.addAll(creations);
            command.addAll(inserts);


            Process process = new ProcessBuilder(command).start();
            process.waitFor();
        }
    }

    private void renderInfoPlist() throws IOException {
        String pInfoListContent = plist
                .data("bundleIdentifier", "notes")
                .data("bundleName", "Notes")
                .render();

        Path file = Files.createFile(pathOf(contentsPath(), "Info.plist"));
        Files.writeString(file, pInfoListContent, APPEND);
    }


    private void renderMainPage() throws IOException {
        try (Stream<Path> paths = Files.list(config.getDocumentPath())) {
            List<Month> items = paths
                    .filter(p -> p.toString().endsWith("md"))
                    .filter(p -> !p.getFileName().toString().startsWith("todo"))
                    .collect(Collectors.groupingBy(p -> p.getFileName().toString().substring(0, 7)))
                    .entrySet().stream()
                    .map(entry -> new Month(
                            entry.getKey(),
                            entry.getValue().stream()
                                    .map(filePath -> new Day(
                                            removeAnyExtension(filePath.getFileName().toString()),
                                            pathOf(documentsPath(), filePath.getFileName().toString() + ".html").toString(),
                                            extractHeadlinesFrom(filePath)))
                                    .sorted((day1, day2) -> day2.name.compareTo(day1.name)) // within a month sort by day-name (reverse order)
                                    .toList()))
                    .sorted((month1, month2) -> month2.name.compareTo(month1.name)) // between months sort by month-name (reverse order)
                    .toList();

            Files.writeString(Files.createFile(pathOf(documentsPath(), "index.html")),
                    main.data("items", items).render(),
                    APPEND);
        }
    }

    private void renderTocFrame() throws IOException {
        try (Stream<Path> paths = Files.list(documentsPath())) {
            List<Month> items = paths
                    .filter(p -> p.toString().endsWith("html"))
                    .filter(p -> !p.toString().startsWith("toc"))
                    .filter(p -> !p.toString().startsWith("index"))
                    .map(p -> p.getFileName().toString())
                    .sorted(Comparator.reverseOrder())
                    .collect(Collectors.groupingBy(p -> p.substring(0, 7)))
                    .entrySet().stream()
                    .map(e -> new Month(e.getKey(), e.getValue().stream()
                            .map(f -> new Day(
                                    removeAnyExtension(f),
                                    pathOf(documentsPath(), f).toString(),
                                    null)).toList()))
                    .toList();
            String currentFileName = items.stream()
                    .reduce((first, second) -> second)
                    .flatMap(m -> m.days().stream().findFirst().map(Day::fileName))
                    .orElse("N/A");

            Files.writeString(Files.createFile(pathOf(config.getDocumentOutputPath(), "toc.html")),
                    toc.data("months", items).render(),
                    APPEND);
            Files.writeString(Files.createFile(pathOf(config.getDocumentOutputPath(), "index.html")),
                    index.data("current", currentFileName).render(),
                    APPEND);
        }
    }


    private String removeAnyExtension(String fileName) {
        return fileName.substring(0, fileName.indexOf("."));
    }

    private String extractHeadlinesFrom(Path path) {
        List<String> lines;
        try {
            lines = Files.readAllLines(path);
        } catch (Exception e) {
            lines = List.of();
        }
        return lines.stream()
                .filter(line -> line.startsWith("# "))
                .map(line -> line.substring(2))
                .collect(Collectors.joining(" | "));
    }

    private void renderMarkdownFiles() throws IOException {
        try (Stream<Path> paths = Files.list(config.getDocumentPath())) {

            List<Path> markdownFiles = paths.filter(p -> p.toString().endsWith("md")).toList();

            Parser parser = Parser.builder()
                    .extensions(markdownExtensions)
                    .build();
            HtmlRenderer htmlRenderer = HtmlRenderer.builder()
                    .extensions(markdownExtensions)
                    .percentEncodeUrls(true)
                    .nodeRendererFactory(CustomHtmlRenderer::new)
                    .build();

            for (Path filePath : markdownFiles) {
                Node document = parser.parse(String.join("\n", Files.readAllLines(filePath)));
                String output = htmlRenderer.render(document);

                String htmlOutput = render
                        .data("markdown_output", output)
                        .render();

                Path file = Files.createFile(pathOf(documentsPath(), filePath.getFileName() + ".html"));
                Files.writeString(file, htmlOutput, APPEND);
            }
        }
    }

    private Path contentsPath() {
        return Paths.get(config.getDocumentOutputPath().toString(), DOCSET_NAME, CONTENTS);
    }
    private Path resourcesPath() {
        return Paths.get(config.getDocumentOutputPath().toString(), DOCSET_NAME, CONTENTS, RESOURCES);
    }
    private Path documentsPath() {
        return Paths.get(config.getDocumentOutputPath().toString(), DOCSET_NAME, CONTENTS, RESOURCES, DOCUMENTS);
    }
    private Path pathOf(Path contentsPath, String suffix) {
        return Paths.get(contentsPath.toString(), suffix);
    }

    @RegisterForReflection
    public record Month(String name, List<Day> days) {
    }

    @RegisterForReflection
    public record Day(String name, String fileName, String description) {
    }

    /**
     * I post-process image links so that I don't have to copy the originals around
     */
    public static class CustomHtmlRenderer extends CoreHtmlNodeRenderer implements NodeRenderer {

        private final HtmlWriter html;

        CustomHtmlRenderer(HtmlNodeRendererContext context) {
            super(context);
            this.html = context.getWriter();
        }

        @Override
        public Set<Class<? extends Node>> getNodeTypes() {
            return Set.of(Image.class, Heading.class);
        }


        @Override
        public void render(Node node) {
            if (Image.class.isAssignableFrom(node.getClass())) {
                Image image = (Image) node;
                html.tag("img",
                        Map.of(
                                "src", image.getDestination().replace("/img", ""),
                                "alt", Optional.ofNullable(image.getTitle()).orElse("")
                        ));
            } else if (Heading.class.isAssignableFrom(node.getClass())) {
                Heading heading = (Heading) node;
                // before each heading we place a Dash-Anchor link to support a nice outline within Dash
                // see https://kapeli.com/docsets#tableofcontents
                this.html.tag("a", Map.of(
                        "name", "//apple_ref/cpp/Entry/" + toLiteralText(heading).replaceAll(" ", "%20"),
                        "class", "dashAnchor"
                ));
                this.html.tag("/a");
                this.visit(heading);
            }
        }
    }

    /*
     * In case of nexted elements for i.e. Headline this method recursively determines all _contained_
     * Text-Nodes and renders their literal content concatenated.
     */
    static String toLiteralText(Node parent) {
        Node next;
        var titleBuffer = new StringBuilder();
        for(Node node = parent.getFirstChild(); node != null; node = next) {
            next = node.getNext();
            if (node instanceof Text t) {
                titleBuffer.append(t.getLiteral());
            } else {
                titleBuffer.append(toLiteralText(node));
            }
        }
        return titleBuffer.toString();
    }
}
