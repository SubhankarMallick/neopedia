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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
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
                Path.of("../content")
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
                Path.of("backend", "public"),
                Path.of("..", "backend", "public")
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
                    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/katex@0.16.8/dist/katex.min.css" crossorigin="anonymous">
                    <link href="https://fonts.googleapis.com/css2?family=Orbitron:wght@700&family=Poppins:wght@400;600&display=swap" rel="stylesheet">
                    <script defer src="https://cdn.jsdelivr.net/npm/katex@0.16.8/dist/katex.min.js" crossorigin="anonymous"></script>
                    <script defer src="https://cdn.jsdelivr.net/npm/katex@0.16.8/dist/contrib/auto-render.min.js" crossorigin="anonymous" onload="renderKaTeX()"></script>
                    <style>
                        :root {
                            --bg-color: #ffffff;
                            --text-color: #24292f;
                            --primary-color: #0969da;
                            --border-color: #d0d7de;
                            --code-bg: #f6f8fa;
                            --accent-color: #6366f1;
                            --logo-gradient: linear-gradient(135deg, #6366f1 0%%, #8b5cf6 100%%);
                        }
                        @media (prefers-color-scheme: dark) {
                            :root {
                                --bg-color: #0d1117;
                                --text-color: #c9d1d9;
                                --primary-color: #58a6ff;
                                --border-color: #30363d;
                                --code-bg: #161b22;
                                --accent-color: #8b5cf6;
                            }
                        }
                        * { margin: 0; padding: 0; box-sizing: border-box; }
                        body {
                            font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
                            background-color: var(--bg-color);
                            color: var(--text-color);
                            line-height: 1.6;
                            min-height: 100vh;
                        }
                        .header {
                            background: var(--logo-gradient);
                            color: white;
                            padding: 1.5rem 1rem;
                            box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
                        }
                        .header-content {
                            max-width: 1200px;
                            margin: 0 auto;
                            display: flex;
                            flex-direction: column;
                            gap: 1rem;
                        }
                        .logo {
                            font-family: 'Orbitron', sans-serif;
                            font-size: 2.5rem;
                            font-weight: 700;
                            text-align: center;
                            text-shadow: 2px 2px 4px rgba(0, 0, 0, 0.3);
                            letter-spacing: 0.3em;
                        }
                        .logo span {
                            display: inline-block;
                            background: linear-gradient(45deg, #00d4ff, #ffffff, #6366f1);
                            background-size: 300%%;
                            -webkit-background-clip: text;
                            -webkit-text-fill-color: transparent;
                            background-clip: text;
                            animation: gradient 3s ease infinite;
                        }
                        @keyframes gradient {
                            0%%, 100%% { background-position: 0%% 50%%; }
                            50%% { background-position: 100%% 50%%; }
                        }
                        .tagline {
                            font-family: 'Poppins', sans-serif;
                            font-size: 1rem;
                            text-align: center;
                            opacity: 0.9;
                        }
                        .search-container { max-width: 1200px; margin: 1rem auto; padding: 0 1rem; }
                        .search-bar {
                            display: flex;
                            gap: 0.5rem;
                            margin-bottom: 1rem;
                        }
                        .search-bar input {
                            flex: 1;
                            padding: 0.75rem 1rem;
                            border: 2px solid var(--border-color);
                            border-radius: 8px;
                            font-size: 1rem;
                            background-color: var(--bg-color);
                            color: var(--text-color);
                        }
                        .search-bar input:focus { outline: none; border-color: var(--primary-color); }
                        .search-bar button {
                            padding: 0.75rem 1.5rem;
                            background: var(--primary-color);
                            color: white;
                            border: none;
                            border-radius: 8px;
                            cursor: pointer;
                            font-size: 1rem;
                            font-weight: 600;
                        }
                        .search-bar button:hover { background: var(--accent-color); }
                        .search-results-link { text-align: right; }
                        .search-results-link a { color: var(--primary-color); font-size: 0.9rem; }
                        .main-container { max-width: 850px; margin: 2rem auto; padding: 0 1rem; }
                        .container { max-width: 850px; margin: 0 auto; }
                        h1, h2, h3, h4, h5, h6 {
                            color: var(--text-color);
                            border-bottom: 1px solid var(--border-color);
                            padding-bottom: 0.3em;
                            margin-top: 1.5em;
                        }
                        a { color: var(--primary-color); text-decoration: none; }
                        a:hover { text-decoration: underline; }
                        pre, code {
                            background-color: var(--code-bg);
                            border-radius: 6px;
                            font-family: "SFMono-Regular", Consolas, "Liberation Mono", Menlo, Courier, monospace;
                        }
                        code { padding: 0.2em 0.4em; font-size: 85%%; }
                        pre code { padding: 1em; display: block; overflow-x: auto; }
                        table { border-collapse: collapse; width: 100%%; margin: 1rem 0; }
                        th, td { border: 1px solid var(--border-color); padding: 8px 12px; text-align: left; }
                        th { background-color: var(--code-bg); }
                        blockquote { margin: 0; padding: 0 1em; color: #57606a; border-left: 0.25em solid var(--border-color); }
                        .footer {
                            text-align: center;
                            padding: 2rem 1rem;
                            border-top: 1px solid var(--border-color);
                            margin-top: 2rem;
                            color: #6e7681;
                            font-size: 0.9rem;
                        }
                        .search-results-list { list-style: none; padding: 0; }
                        .search-result-item {
                            background: var(--code-bg);
                            border-radius: 8px;
                            padding: 1rem;
                            margin-bottom: 1rem;
                            border-left: 4px solid var(--primary-color);
                            transition: transform 0.2s, box-shadow 0.2s;
                        }
                        .search-result-item:hover { transform: translateX(5px); box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1); }
                        .search-result-item a { display: block; text-decoration: none; color: var(--text-color); }
                        .search-result-item h3 { margin: 0 0 0.5rem 0; border-bottom: none; padding-bottom: 0; margin-top: 0; }
                        .result-path { font-size: 0.85rem; color: #6e7681; display: block; }
                        @media (max-width: 768px) {
                            .header { padding: 1rem; }
                            .logo { font-size: 1.8rem; }
                            .search-bar { flex-direction: column; }
                            .search-bar button { width: 100%%; }
                        }
                    </style>
                    <script>
                        function renderKaTeX() {
                            if (typeof renderMathInElement === 'function') {
                                renderMathInElement(document.body, {
                                    delimiters: [
                                        {left: '$$', right: '$$', display: true},
                                        {left: '$', right: '$', display: false}
                                    ],
                                    throwOnError: false
                                });
                            }
                        }
                        function searchContent() {
                            const query = document.getElementById('search-input').value;
                            if (query.trim() === '') return;
                            window.location.href = '/search?q=' + encodeURIComponent(query) + '&limit=5';
                        }
                        function viewAllResults() {
                            const query = document.getElementById('search-input').value;
                            window.location.href = '/search?q=' + encodeURIComponent(query) + '&limit=100';
                        }
                        document.addEventListener("DOMContentLoaded", function() {
                            renderKaTeX();
                            const searchInput = document.getElementById('search-input');
                            if (searchInput) {
                                searchInput.addEventListener('keypress', function(e) {
                                    if (e.key === 'Enter') searchContent();
                                });
                            }
                        });
                    </script>
                </head>
                <body>
                    <header class="header">
                        <div class="header-content">
                            <a class="logo" href="/" aria-label="Neopedia home"><span>NEOPEDIA</span></a>
                            <div class="tagline">Free. Open. Education for All.</div>
                        </div>
                    </header>
                    <div class="search-container">
                        <div class="search-bar">
                            <input type="text" id="search-input" placeholder="Search for topics, chapters, concepts...">
                            <button onclick="searchContent()">Search</button>
                        </div>
                        <div class="search-results-link">
                            <a href="javascript:viewAllResults()">View All Results</a>
                        </div>
                    </div>
                    <main class="main-container">
                        <div class="container">
                            %s
                        </div>
                    </main>
                    <footer class="footer">
                        <p>Powered by The Neopedia Foundation | Content licensed under CC BY-SA 4.0 | Code licensed under GPLv3</p>
                    </footer>
                </body>
                </html>
                """, pageTitle, bodyHtml);
    }

    /**
     * Helper method to extract or derive a human-readable title for the Markdown document.
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
     * Just-in-Time compilation for single file.
     */
    public boolean compileSingleJIT(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) return false;
        Path mdPath = contentDir.resolve(relativePath).normalize();
        if (!mdPath.startsWith(contentDir.normalize())) {
            logger.warn("JIT compilation blocked: path traversal attempt: {}", relativePath);
            return false;
        }
        if (!Files.exists(mdPath) || !mdPath.toString().toLowerCase().endsWith(".md")) return false;
        try {
            compileSingleFile(mdPath);
            logger.debug("JIT compiled: {}", relativePath);
            return true;
        } catch (IOException e) {
            logger.error("JIT compilation failed for: {}", relativePath, e);
            return false;
        }
    }

    /**
     * Record for search results.
     */
    public record SearchResult(String title, String path, String url, double score) {}

    /**
     * Gets all markdown files for search indexing.
     */
    public List<SearchResult> getAllMarkdownFiles() {
        List<SearchResult> results = new ArrayList<>();
        if (!Files.exists(contentDir)) return results;
        try (Stream<Path> stream = Files.walk(contentDir)) {
            List<Path> mdFiles = stream
                    .filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().toLowerCase().endsWith(".md"))
                    .sorted(Comparator.naturalOrder())
                    .toList();
            for (Path mdPath : mdFiles) {
                try {
                    Path relativePath = contentDir.relativize(mdPath);
                    if (relativePath.getNameCount() == 1 && "index.md".equalsIgnoreCase(relativePath.getFileName().toString())) {
                        continue;
                    }
                    String relativeString = relativePath.toString();
                    String htmlRelativeString = relativeString.replaceAll("(?i)\\.md$", ".html");
                    String rawContent = Files.readString(mdPath);
                    String title = extractTitle(rawContent, mdPath.getFileName().toString());
                    String url = "/" + htmlRelativeString.replace("\\", "/");
                    if (url.startsWith("//")) url = url.substring(1);
                    results.add(new SearchResult(title, relativeString, url, 0.0));
                } catch (IOException e) {
                    logger.error("Error reading file for search: {}", mdPath, e);
                }
            }
        } catch (IOException e) {
            logger.error("Error walking content directory: {}", contentDir, e);
        }
        return results;
    }

    /**
     * Calculates similarity score.
     */
    private double calculateSimilarity(String query, String target) {
        if (query == null || query.isBlank() || target == null || target.isBlank()) return 0.0;
        String normalizedQuery = normalizeText(query);
        String normalizedTarget = normalizeText(target);
        if (normalizedQuery.isBlank() || normalizedTarget.isBlank()) return 0.0;
        if (normalizedTarget.equals(normalizedQuery)) return 1.0;
        if (normalizedTarget.contains(normalizedQuery)) return 0.95;

        List<String> queryWords = tokenize(normalizedQuery);
        List<String> targetWords = tokenize(normalizedTarget);
        int matchedWords = 0;
        double matchQuality = 0.0;
        for (String queryWord : queryWords) {
            double best = targetWords.stream().mapToDouble(targetWord -> wordSimilarity(queryWord, targetWord)).max().orElse(0.0);
            if (best >= 0.72) {
                matchedWords++;
                matchQuality += best;
            }
        }
        int minimumMatches = queryWords.size() == 1 ? 1 : (int) Math.ceil(queryWords.size() * 0.6);
        if (matchedWords < minimumMatches) return 0.0;
        return 0.55 * ((double) matchedWords / queryWords.size()) + 0.45 * (matchQuality / matchedWords);
    }

    private String normalizeText(String value) {
        return value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", " ").trim();
    }

    private List<String> tokenize(String value) {
        return Arrays.stream(value.split("\\s+"))
                .filter(word -> !word.isBlank())
                .map(this::singularize)
                .toList();
    }

    private String singularize(String word) {
        return word.length() > 3 && word.endsWith("s") ? word.substring(0, word.length() - 1) : word;
    }

    private double wordSimilarity(String left, String right) {
        if (left.equals(right)) return 1.0;
        if (left.length() >= 3 && right.length() >= 3 && (left.startsWith(right) || right.startsWith(left))) return 0.9;
        int longest = Math.max(left.length(), right.length());
        if (longest < 4) return 0.0;
        return 1.0 - (double) computeLevenshteinDistance(left, right) / longest;
    }

    /**
     * Computes Levenshtein distance.
     */
    private int computeLevenshteinDistance(String a, String b) {
        int[][] dp = new int[a.length() + 1][b.length() + 1];
        for (int i = 0; i <= a.length(); i++) dp[i][0] = i;
        for (int j = 0; j <= b.length(); j++) dp[0][j] = j;
        for (int i = 1; i <= a.length(); i++) {
            for (int j = 1; j <= b.length(); j++) {
                int cost = (a.charAt(i - 1) == b.charAt(j - 1)) ? 0 : 1;
                dp[i][j] = Math.min(Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1), dp[i - 1][j - 1] + cost);
            }
        }
        return dp[a.length()][b.length()];
    }

    /**
     * Searches markdown files.
     */
    public List<SearchResult> search(String query, int maxResults) {
        if (query == null || query.isBlank()) {
            return getAllMarkdownFiles().stream().limit(maxResults).toList();
        }
        List<SearchResult> allFiles = getAllMarkdownFiles();
        return allFiles.stream()
                .map(result -> {
                    double titleScore = calculateSimilarity(query, result.title());
                    double pathScore = calculateSimilarity(query, result.path());
                    double combinedScore = (titleScore * 0.8) + (pathScore * 0.2);
                    return new SearchResult(result.title(), result.path(), result.url(), combinedScore);
                })
                .filter(result -> result.score() >= 0.45)
                .sorted(Comparator.comparingDouble(SearchResult::score).reversed())
                .limit(maxResults)
                .collect(Collectors.toList());
    }

    /**
     * Generates search results page.
     */
    public String generateSearchResultsPage(String query, List<SearchResult> results, boolean showAll) {
        StringBuilder resultsHtml = new StringBuilder();
        if (results.isEmpty()) {
            resultsHtml.append("<p>No results found for <strong>").append(escapeHtml(query)).append("</strong></p>");
        } else {
            resultsHtml.append("<h2>Search Results for \"").append(escapeHtml(query)).append("\"</h2>");
            resultsHtml.append("<p>Found ").append(results.size()).append(" result(s)</p>");
            resultsHtml.append("<ul class=\"search-results-list\">");
            for (SearchResult result : results) {
                resultsHtml.append("<li class=\"search-result-item\">");
                resultsHtml.append("<a href=\"").append(escapeHtml(result.url())).append("\">");
                resultsHtml.append("<h3>").append(escapeHtml(result.title())).append("</h3>");
                resultsHtml.append("<span class=\"result-path\">").append(escapeHtml(result.path())).append("</span>");
                resultsHtml.append("</a></li>");
            }
            resultsHtml.append("</ul>");
        }
        String pageTitle = "Search Results for " + (query != null ? query : "All Content");
        return compileToHtml(pageTitle, resultsHtml.toString());
    }

    /**
     * HTML escaping.
     */
    private String escapeHtml(String input) {
        if (input == null) return "";
        return input.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }

    /**
     * Compiles a single markdown file.
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
     * Generates index pages for folders.
     */
    private void generateFolderIndexPages() throws IOException {
        List<Path> directories = Files.walk(contentDir)
                .filter(Files::isDirectory)
                .filter(p -> !p.equals(contentDir))
                .sorted(Comparator.reverseOrder())
                .toList();
        for (Path dir : directories) {
            Path relativePath = contentDir.relativize(dir);
            Path targetDir = publicDir.resolve(relativePath);
            Files.createDirectories(targetDir);
            Path indexMd = dir.resolve("index.md");
            if (Files.exists(indexMd)) {
                compileSingleFile(indexMd);
            } else {
                StringBuilder folderContent = new StringBuilder();
                folderContent.append("# ").append(capitalizePath(relativePath)).append("\\n\\n## Contents\\n\\n");
                try (Stream<Path> files = Files.list(dir)) {
                    List<Path> mdFiles = files
                            .filter(Files::isRegularFile)
                            .filter(p -> p.getFileName().toString().toLowerCase().endsWith(".md"))
                            .sorted(Comparator.naturalOrder())
                            .toList();
                    if (!mdFiles.isEmpty()) {
                        folderContent.append("- ");
                        for (int i = 0; i < mdFiles.size(); i++) {
                            Path file = mdFiles.get(i);
                            String fileName = file.getFileName().toString();
                            String displayName = fileName.replaceAll("(?i)\\.md$", "");
                            if (i > 0) folderContent.append("\\n- ");
                            folderContent.append("[").append(displayName).append("](").append(fileName.replaceAll("(?i)\\.md$", ".html")).append(")");
                        }
                    }
                }
                String htmlContent = compileToHtml(capitalizePath(relativePath), folderContent.toString());
                Files.writeString(targetDir.resolve("index.html"), htmlContent);
                logger.debug("Generated index: {} -> index.html", relativePath);
            }
        }
        Path rootIndexMd = contentDir.resolve("index.md");
        Path rootIndexHtml = publicDir.resolve("index.html");
        if (!Files.exists(rootIndexHtml)) {
            if (Files.exists(rootIndexMd)) {
                compileSingleFile(rootIndexMd);
            } else {
                String defaultHomepage = "# Welcome to Neopedia\\n\\nThe Neopedia Foundation mission: Make the reaches of human knowledge extend to every person from every background, every learning level, and every corner of the earth—completely, permanently, and unconditionally free.\\n\\n## Explore\\n\\n- [Class 10 Chemistry](./class-10/chemistry/)\\n\\n## License\\n\\nContent licensed under CC BY-SA 4.0 | Code licensed under GPLv3";
                Files.writeString(rootIndexHtml, compileToHtml("Neopedia", defaultHomepage));
                logger.info("Generated default homepage: index.html");
            }
        }
    }

    /**
     * Capitalizes path for display.
     */
    private String capitalizePath(Path path) {
        return Arrays.stream(path.toString().split("[\\\\/]"))
                .map(word -> word.isBlank() ? word : Character.toUpperCase(word.charAt(0)) + word.substring(1))
                .collect(Collectors.joining(" "));
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
            try { Files.createDirectories(contentDir); } catch (IOException e) { logger.error("Failed to create content directory: {}", contentDir, e); }
            return 0;
        }
        try { Files.createDirectories(publicDir); } catch (IOException e) { logger.error("Failed to create public directory: {}", publicDir, e); }
        long count = 0;
        try (Stream<Path> stream = Files.walk(contentDir)) {
            List<Path> mdFiles = stream.filter(Files::isRegularFile).filter(p -> p.getFileName().toString().toLowerCase().endsWith(".md")).toList();
            for (Path mdPath : mdFiles) {
                try { compileSingleFile(mdPath); count++; } catch (Exception e) { logger.error("Error compiling markdown file: {}", mdPath, e); }
            }
            generateFolderIndexPages();
            removeStaleGeneratedHtml();
        } catch (IOException e) { logger.error("Error walking content directory: {}", contentDir, e); }
        logger.info("Compilation finished. Successfully compiled {} file(s) into {}", count, publicDir);
        return count;
    }

    /**
     * Removes generated article pages whose source Markdown no longer exists. Generated
     * directory indexes are also removed when their corresponding content directory is gone.
     */
    private void removeStaleGeneratedHtml() throws IOException {
        if (!Files.exists(publicDir)) return;
        try (Stream<Path> stream = Files.walk(publicDir)) {
            List<Path> staleFiles = stream.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".html"))
                    .filter(this::isGeneratedPage)
                    .filter(this::hasNoSource)
                    .toList();
            for (Path staleFile : staleFiles) {
                Files.deleteIfExists(staleFile);
                logger.info("Removed stale generated page: {}", publicDir.relativize(staleFile));
            }
        }
        pruneEmptyGeneratedDirectories();
    }

    private boolean isGeneratedPage(Path htmlFile) {
        try {
            return Files.readString(htmlFile).contains("<span>NEOPEDIA</span>");
        } catch (IOException e) {
            logger.warn("Unable to inspect generated page: {}", htmlFile, e);
            return false;
        }
    }

    private boolean hasNoSource(Path htmlFile) {
        Path relative = publicDir.relativize(htmlFile);
        if (relative.getNameCount() == 1 && "index.html".equalsIgnoreCase(relative.getFileName().toString())) return false;
        if ("index.html".equalsIgnoreCase(relative.getFileName().toString())) {
            return !Files.isDirectory(contentDir.resolve(relative).getParent());
        }
        String markdownName = relative.getFileName().toString().replaceFirst("(?i)\\.html$", ".md");
        return !Files.exists(contentDir.resolve(relative).resolveSibling(markdownName));
    }

    private void pruneEmptyGeneratedDirectories() throws IOException {
        try (Stream<Path> stream = Files.walk(publicDir)) {
            List<Path> directories = stream.filter(Files::isDirectory)
                    .filter(path -> !path.equals(publicDir))
                    .sorted(Comparator.reverseOrder())
                    .toList();
            for (Path directory : directories) {
                try (Stream<Path> children = Files.list(directory)) {
                    if (children.findAny().isEmpty()) Files.delete(directory);
                }
            }
        }
    }

    /**
     * Gets the content directory path used by this Compiler.
     */
    public Path getContentDir() { return contentDir; }

    /**
     * Gets the public directory path used by this Compiler.
     */
    public Path getPublicDir() { return publicDir; }
}
