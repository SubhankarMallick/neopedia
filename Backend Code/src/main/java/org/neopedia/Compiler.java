package org.neopedia;

import com.vladsch.flexmark.ext.gitlab.GitLabExtension;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.data.MutableDataSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

/**
 * Static site compiler engine for The Neopedia Project.
 * Reads Markdown files, compiles them into HTML with KaTeX math rendering support,
 * and writes the compiled output to a public directory.
 */
public class Compiler {

    private static final Logger logger = LoggerFactory.getLogger(Compiler.class);

    private final Parser parser;
    private final HtmlRenderer renderer;
    private final Path contentDir;
    private final Path publicDir;

    /**
     * Default constructor initializing Flexmark parser and renderer
     * with default content and public directories.
     */
    public Compiler() {
        this(resolveDefaultContentDir(), resolveDefaultPublicDir());
    }

    /**
     * Constructs a Compiler with explicitly specified content and public directories.
     *
     * @param contentDir The source directory containing Markdown content.
     * @param publicDir  The target public directory for compiled HTML files.
     */
    public Compiler(Path contentDir, Path publicDir) {
        this.contentDir = contentDir;
        this.publicDir = publicDir;

        MutableDataSet options = new MutableDataSet();
        options.set(Parser.EXTENSIONS, Arrays.asList(
                TablesExtension.create(),
                GitLabExtension.create()
        ));

        this.parser = Parser.builder(options).build();
        this.renderer = HtmlRenderer.builder(options).build();
    }

    /**
     * Resolves default directory location for markdown source content.
     *
     * @return Path to the content directory.
     */
    public static Path resolveDefaultContentDir() {
        List<Path> candidates = List.of(
                Path.of("content"),
                Path.of("Content"),
                Path.of("../content"),
                Path.of("../Content")
        );
        for (Path candidate : candidates) {
            if (Files.exists(candidate) && Files.isDirectory(candidate)) {
                return candidate.toAbsolutePath().normalize();
            }
        }
        return Path.of("content").toAbsolutePath().normalize();
    }

    /**
     * Resolves default directory location for compiled public output.
     *
     * @return Path to the public directory.
     */
    public static Path resolveDefaultPublicDir() {
        List<Path> candidates = List.of(
                Path.of("public"),
                Path.of("../public")
        );
        for (Path candidate : candidates) {
            if (Files.exists(candidate) && Files.isDirectory(candidate)) {
                return candidate.toAbsolutePath().normalize();
            }
        }
        return Path.of("public").toAbsolutePath().normalize();
    }

    /**
     * Compiles raw Markdown content into a fully wrapped HTML document with KaTeX support.
     *
     * @param title           Title of the HTML page.
     * @param markdownContent Raw Markdown string.
     * @return Fully formatted HTML string.
     */
    public String compileToHtml(String title, String markdownContent) {
        String bodyHtml = renderer.render(parser.parse(markdownContent == null ? "" : markdownContent));
        String pageTitle = (title != null && !title.isBlank()) ? title : "Neopedia";

        return String.format("""
                <!DOCTYPE html>
                <html lang="en">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>%s</title>
                    <!-- KaTeX CSS -->
                    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/katex@0.16.8/dist/katex.min.css" crossorigin="anonymous">
                    <!-- KaTeX JS & Auto-render plugin -->
                    <script defer src="https://cdn.jsdelivr.net/npm/katex@0.16.8/dist/katex.min.js" crossorigin="anonymous"></script>
                    <script defer src="https://cdn.jsdelivr.net/npm/katex@0.16.8/dist/contrib/auto-render.min.js" crossorigin="anonymous" onload="renderKaTeX()"></script>
                    <style>
                        :root {
                            --bg-color: #ffffff;
                            --text-color: #24292f;
                            --primary-color: #0969da;
                            --border-color: #d0d7de;
                            --code-bg: #f6f8fa;
                        }
                        @media (prefers-color-scheme: dark) {
                            :root {
                                --bg-color: #0d1117;
                                --text-color: #c9d1d9;
                                --primary-color: #58a6ff;
                                --border-color: #30363d;
                                --code-bg: #161b22;
                            }
                        }
                        body {
                            font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
                            background-color: var(--bg-color);
                            color: var(--text-color);
                            line-height: 1.6;
                            margin: 0;
                            padding: 2rem 1rem;
                        }
                        .container {
                            max-width: 850px;
                            margin: 0 auto;
                        }
                        h1, h2, h3, h4, h5, h6 {
                            color: var(--text-color);
                            border-bottom: 1px solid var(--border-color);
                            padding-bottom: 0.3em;
                            margin-top: 1.5em;
                        }
                        a {
                            color: var(--primary-color);
                            text-decoration: none;
                        }
                        a:hover {
                            text-decoration: underline;
                        }
                        pre, code {
                            background-color: var(--code-bg);
                            border-radius: 6px;
                            font-family: "SFMono-Regular", Consolas, "Liberation Mono", Menlo, Courier, monospace;
                        }
                        code {
                            padding: 0.2em 0.4em;
                            font-size: 85%%;
                        }
                        pre code {
                            padding: 1em;
                            display: block;
                            overflow-x: auto;
                        }
                        table {
                            border-collapse: collapse;
                            width: 100%%;
                            margin: 1rem 0;
                        }
                        th, td {
                            border: 1px solid var(--border-color);
                            padding: 8px 12px;
                            text-align: left;
                        }
                        th {
                            background-color: var(--code-bg);
                        }
                        blockquote {
                            margin: 0;
                            padding: 0 1em;
                            color: #57606a;
                            border-left: 0.25em solid var(--border-color);
                        }
                    </style>
                    <script>
                        function renderKaTeX() {
                            if (typeof renderMathInElement === 'function') {
                                renderMathInElement(document.body, {
                                    delimiters: [
                                        {left: '$$', right: '$$', display: true},
                                        {left: '$', right: '$', display: false},
                                        {left: '\\(', right: '\\)', display: false},
                                        {left: '\\[', right: '\\]', display: true}
                                    ],
                                    throwOnError: false
                                });
                                document.querySelectorAll('span.math.inline, span.math.display').forEach(function(el) {
                                    var displayMode = el.classList.contains('display');
                                    var tex = el.textContent;
                                    if (typeof katex !== 'undefined') {
                                        try {
                                            katex.render(tex, el, { displayMode: displayMode, throwOnError: false });
                                        } catch(e) {}
                                    }
                                });
                            }
                        }
                        document.addEventListener("DOMContentLoaded", renderKaTeX);
                    </script>
                </head>
                <body>
                    <main class="container">
                %s
                    </main>
                </body>
                </html>
                """, pageTitle, bodyHtml);
    }

    /**
     * Recursively scans the content directory for Markdown (.md) files,
     * compiles each into an HTML file inside the public directory, preserving relative path structure.
     *
     * @return Total count of compiled files.
     */
    public long compileAll() {
        if (!Files.exists(contentDir)) {
            logger.warn("Content directory does not exist: {}", contentDir);
            try {
                Files.createDirectories(contentDir);
            } catch (IOException e) {
                logger.error("Failed to create content directory: {}", contentDir, e);
            }
            return 0;
        }

        try {
            Files.createDirectories(publicDir);
        } catch (IOException e) {
            logger.error("Failed to create public directory: {}", publicDir, e);
        }

        long count = 0;
        try (Stream<Path> stream = Files.walk(contentDir)) {
            List<Path> mdFiles = stream
                    .filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().toLowerCase().endsWith(".md"))
                    .toList();

            for (Path mdPath : mdFiles) {
                try {
                    compileSingleFile(mdPath);
                    count++;
                } catch (Exception e) {
                    logger.error("Error compiling markdown file: {}", mdPath, e);
                }
            }
        } catch (IOException e) {
            logger.error("Error walking content directory: {}", contentDir, e);
        }

        logger.info("Compilation finished. Successfully compiled {} file(s) into {}", count, publicDir);
        return count;
    }

    /**
     * Compiles a single Markdown file to its target HTML path.
     *
     * @param mdPath Path to the source Markdown file.
     * @throws IOException If reading or writing fails.
     */
    private void compileSingleFile(Path mdPath) throws IOException {
        Path relativePath = contentDir.relativize(mdPath);
        String relativeString = relativePath.toString();
        String htmlRelativeString = relativeString.replaceAll("(?i)\\.md$", ".html");

        Path targetHtmlPath = publicDir.resolve(htmlRelativeString);
        Files.createDirectories(targetHtmlPath.getParent());

        String rawContent = Files.readString(mdPath);
        String title = extractTitle(rawContent, mdPath.getFileName().toString());
        String compiledHtml = compileToHtml(title, rawContent);

        Files.writeString(targetHtmlPath, compiledHtml);
        logger.debug("Compiled: {} -> {}", relativePath, htmlRelativeString);
    }

    /**
     * Helper method to extract or derive a human-readable title for the Markdown document.
     *
     * @param markdownContent Content of the document.
     * @param fileName        Fallback filename.
     * @return Document title.
     */
    private String extractTitle(String markdownContent, String fileName) {
        if (markdownContent != null) {
            for (String line : markdownContent.lines().toList()) {
                String trimmed = line.trim();
                if (trimmed.startsWith("# ")) {
                    return trimmed.substring(2).trim();
                }
            }
        }
        if (fileName != null && !fileName.isBlank()) {
            String nameWithoutExt = fileName.replaceAll("(?i)\\.md$", "");
            String[] words = nameWithoutExt.split("[-_]");
            StringBuilder sb = new StringBuilder();
            for (String w : words) {
                if (!w.isBlank()) {
                    if (!sb.isEmpty()) sb.append(" ");
                    sb.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1));
                }
            }
            return sb.toString();
        }
        return "Neopedia Document";
    }

    /**
     * Gets the content directory path used by this Compiler.
     *
     * @return Content directory path.
     */
    public Path getContentDir() {
        return contentDir;
    }

    /**
     * Gets the public directory path used by this Compiler.
     *
     * @return Public directory path.
     */
    public Path getPublicDir() {
        return publicDir;
    }
}
