package de.bender.notes.boundary;

import de.bender.notes.control.Config;
import de.bender.notes.control.NoteService;
import io.quarkus.qute.Template;
import org.commonmark.Extension;
import org.commonmark.ext.autolink.AutolinkExtension;
import org.commonmark.ext.gfm.strikethrough.StrikethroughExtension;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.ext.heading.anchor.HeadingAnchorExtension;
import org.commonmark.ext.image.attributes.ImageAttributesExtension;
import org.commonmark.ext.task.list.items.TaskListItemsExtension;
import org.commonmark.node.Image;
import org.commonmark.node.Link;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.NodeRenderer;
import org.commonmark.renderer.html.HtmlNodeRendererContext;
import org.commonmark.renderer.html.HtmlRenderer;
import org.commonmark.renderer.html.HtmlWriter;
import picocli.CommandLine.Command;

import javax.inject.Inject;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.Callable;

import static java.nio.file.StandardOpenOption.APPEND;

@Command(name = "render",
        aliases = {"r"},
        description = "Renders HTML")
public class RenderCmd implements Callable<Integer> {

    @Inject
    Config config;

    @Inject
    NoteService notes;

    @Inject
    Template render;

    @Override
    public Integer call() throws Exception {
        notes.ensureNotesDirExists();
        notes.reinitOutputDir();

        List<Path> markdownFiles = Files.list(config.getDocumentPath())
                .filter(p -> p.toString().endsWith("md"))
                .toList();

        List<Extension> extensions = List.of(
                AutolinkExtension.create(),
                StrikethroughExtension.create(),
                HeadingAnchorExtension.create(),
                ImageAttributesExtension.create(),
                TaskListItemsExtension.create(),
                TablesExtension.create()
        );
        Parser parser = Parser.builder()
                .extensions(extensions)
                .build();
        HtmlRenderer htmlRenderer = HtmlRenderer.builder()
                .extensions(extensions)
                .percentEncodeUrls(true)
                .nodeRendererFactory(LinkRenderer::new)
                .build();

        for (Path filePath : markdownFiles) {
            Node document = parser.parse(String.join("\n", Files.readAllLines(filePath)));
            String output = htmlRenderer.render(document);

            String htmlOutput = render
                    .data("markdown_output", output)
                    .render();

            Path file = Files.createFile(Paths.get(config.getDocumentOutputPath().toString(), filePath.getFileName() + ".html"));
            Files.writeString(file, htmlOutput, APPEND);
        }

        return 0;
    }

    /**
     * I post-process image links so that I don't have to copy the originals around
     */
    public static class LinkRenderer implements NodeRenderer {

        private final HtmlWriter html;

        LinkRenderer(HtmlNodeRendererContext context) {
            this.html = context.getWriter();
        }

        @Override
        public Set<Class<? extends Node>> getNodeTypes() {
            return Collections.singleton(Image.class);
        }

        @Override
        public void render(Node node) {
            Image image = (Image) node;

            html.tag("img",
                    Map.of(
                        "src", image.getDestination().replace("/img", "./img"),
                        "alt", Optional.ofNullable(image.getTitle()).orElse("")
                    ));
        }
    }
}
