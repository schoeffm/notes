package de.bender.notes.control;

import javax.enterprise.context.ApplicationScoped;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static java.nio.file.LinkOption.NOFOLLOW_LINKS;
import static java.nio.file.StandardOpenOption.APPEND;
import static java.nio.file.attribute.PosixFilePermission.*;

@ApplicationScoped
public class Config {

    public static final String OUTPUT_INDEX_FILE_NAME = "index.html";


    private static final String CONF_KEY_EDITOR = "NOTES_EDITOR";
    private static final String CONF_VALUE_EDITOR = "$EDITOR";
    private static final String CONF_KEY_NOTES_DOC_DIR = "NOTES_DOC_DIR";
    private static final String CONF_VALUE_NOTES_DOC_DIR = "Documents/notes";
    private static final String CONF_KEY_NOTES_OUTPUT_DIR = "NOTES_OUTPUT_DIR";
    private static final String CONF_VALUE_NOTES_OUTPUT_DIR = "Documents/notes/output";
    private static final String CONF_KEY_TODO_FILE_NAME = "TODO_FILE_NAME";
    private static final String CONF_VALUE_TODO_FILE_NAME = "todo.md";


    private static final String DATE_PATTERN_FORMAT = "yyyy-MM-dd";

    private static final Map<String, String> DEFAULT_CONFIG = Map.of(
            CONF_KEY_NOTES_DOC_DIR, Paths.get(System.getProperty("user.home"), CONF_VALUE_NOTES_DOC_DIR).toString(),
            CONF_KEY_NOTES_OUTPUT_DIR, Paths.get(System.getProperty("user.home"), CONF_VALUE_NOTES_OUTPUT_DIR).toString(),
            CONF_KEY_EDITOR, CONF_VALUE_EDITOR,
            CONF_KEY_TODO_FILE_NAME, CONF_VALUE_TODO_FILE_NAME
    );

    private static final String CONFIG_DIR = String.format("%s%s%s", System.getProperty("user.home"), FileSystems.getDefault().getSeparator(), ".config");
    private static final String CONFIG_FILE = String.format("%s%s%s", CONFIG_DIR, FileSystems.getDefault().getSeparator(), "notes");

    public Path getDocumentPath() {
        return Paths.get(readConfigValue(CONF_KEY_NOTES_DOC_DIR));
    }
    public Path getStaticContentPath() {
        return Paths.get(readConfigValue(CONF_KEY_NOTES_DOC_DIR), "img");
    }

    public Path getDocumentOutputPath() {
        return Paths.get(readConfigValue(CONF_KEY_NOTES_OUTPUT_DIR));
    }

    /**
     * @return the default-editor as configured in the system or 'vi' as default
     */
    public String getEditor() {
        return Optional.ofNullable(System.getenv("EDITOR"))
                .orElse("vi");
    }

    /**
     * @return the file-{@link Path} for the default notes
     */
    public Path getNotesFilePath() {
        var formatter = DateTimeFormatter
                .ofPattern(DATE_PATTERN_FORMAT)
                .withZone(ZoneId.systemDefault());
        return getNotesFilePath(formatter.format(Instant.now()));
    }

    /**
     * Crafts the {@link Path}-representation for the given `notes`-filename
     * </p>
     * Notice: The given filename does _not_ contain the file-suffix
     * </p>
     * @param fileName of the file whose document-{@link Path} should be created
     * @return {@link Path} of the notes-file
     */
    public Path getNotesFilePath(String fileName) {
        var notesDirectory = readConfigValue(CONF_KEY_NOTES_DOC_DIR);
        return Paths.get(notesDirectory, (fileName.matches(".*(.md|.MD)$")) ? fileName : fileName + ".md");
    }

    /**
     * Crafts the {@link Path}-representation for the `todo` file (as configured in the configs)
     * @return {@link Path} of the todo-file
     */
    public Path getTodoFilePath() {
        return Paths.get(readConfigValue(CONF_KEY_NOTES_DOC_DIR), readConfigValue(CONF_KEY_TODO_FILE_NAME));
    }

    public void reset() throws IOException {
        Files.deleteIfExists(Paths.get(CONFIG_FILE));

        createConfigIfNecessary();
    }

    public void edit() throws InterruptedException, IOException {
        createConfigIfNecessary();

        Process process =
                new ProcessBuilder("vi", CONFIG_FILE)      // start vim to configure the current config
                        .inheritIO()                                 // overtake current IO (so vim comes to the foreground)
                        .start();

        process.waitFor();
    }

    public void view() throws IOException {
        createConfigIfNecessary();

        Files.readAllLines(Paths.get(CONFIG_FILE))
                .forEach(System.out::println);
    }

    private String readConfigValue(String confKeyNotesDocDir) {
        List<String> lines = null;
        try {
            lines = Files.readAllLines(Paths.get(CONFIG_FILE));
        } catch (IOException e) {
            System.err.println("Couldn't read config-file content: " + CONFIG_FILE);
            lines = List.of();
        }

        return lines.stream()
                .map(line -> line.split("="))
                .filter(parts -> parts[0].startsWith(confKeyNotesDocDir))
                .map(parts -> parts[1])
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Cannot read '" + confKeyNotesDocDir + "' in configuration"));
    }

    private void createConfigIfNecessary() throws IOException {
        if (Files.notExists(Paths.get(CONFIG_DIR), NOFOLLOW_LINKS)) {
            Files.createDirectories(Paths.get(CONFIG_DIR),
                    PosixFilePermissions.asFileAttribute(Set.of(OWNER_EXECUTE, OWNER_READ, OWNER_WRITE)));
        }

        if (Files.notExists(Paths.get(CONFIG_FILE), NOFOLLOW_LINKS)) {
            Files.createFile(Paths.get(CONFIG_FILE),
                    PosixFilePermissions.asFileAttribute(Set.of(OWNER_EXECUTE, OWNER_READ, OWNER_WRITE)));

            for (Map.Entry<String, String> entry : DEFAULT_CONFIG.entrySet()) {
                Files.writeString(Paths.get(CONFIG_FILE), String.format("%s=%s%n", entry.getKey(), entry.getValue()), APPEND);
            }
        }
    }
}
