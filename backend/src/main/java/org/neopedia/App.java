package org.neopedia;

import io.javalin.Javalin;
import io.javalin.http.staticfiles.Location;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Main application class for The Neopedia Project static-site generator and web server.
 * Initializes the compiler, triggers initial build, configures Javalin server, and sets up API endpoints.
 */
public class App {

    private static final Logger logger = LoggerFactory.getLogger(App.class);
    private static final int PORT = 7070;

    /**
     * DTO record representing the JSON response structure for the build API endpoint.
     *
     * @param status        Status string ("success" or "error").
     * @param compiledCount Count of Markdown files compiled into HTML.
     * @param message       Informational message describing the result.
     */
    public record BuildResponse(String status, long compiledCount, String message) {}

    /**
     * DTO record representing a search result for JSON API responses.
     */
    public record SearchResultResponse(String title, String path, String url, double score) {}

    /**
     * DTO record representing the JSON response structure for the search API endpoint.
     */
    public record SearchResponse(String query, List<SearchResultResponse> results, int totalCount, String message) {}

    /**
     * Application entry point.
     *
     * @param args Command line arguments.
     */
    public static void main(String[] args) {
        Compiler compiler = new Compiler();

        // Perform initial static site compilation on startup
        logger.info("Executing initial site build...");
        long initialCount = compiler.compileAll();
        logger.info("Initial build completed with {} file(s).", initialCount);

        Path publicDirPath = compiler.getPublicDir();
        logger.info("Serving static files from external directory: {}", publicDirPath);

        Javalin app = Javalin.create(config -> {
            config.staticFiles.add(staticFile -> {
                staticFile.hostedPath = "/";
                staticFile.directory = publicDirPath.toAbsolutePath().toString();
                staticFile.location = Location.EXTERNAL;
            });
        });

        // Health check endpoint
        app.get("/api/health", ctx -> {
            ctx.status(200);
            ctx.result("Neopedia Java Engine Running OK");
        });

        // Trigger build endpoint
        app.post("/api/build", ctx -> {
            try {
                long count = compiler.compileAll();
                ctx.status(200);
                ctx.json(new BuildResponse("success", count, "Site rebuild completed successfully."));
            } catch (Exception e) {
                logger.error("Error during triggered build: ", e);
                ctx.status(500);
                ctx.json(new BuildResponse("error", 0, "Build failed: " + e.getMessage()));
            }
        });

        // Search API endpoint
        app.get("/api/search", ctx -> {
            try {
                String query = ctx.queryParam("q");
                String limitStr = ctx.queryParam("limit");
                int limit = 10;
                if (limitStr != null && !limitStr.isBlank()) {
                    try { limit = Math.min(Integer.parseInt(limitStr), 100); } catch (NumberFormatException ex) { limit = 10; }
                }

                List<Compiler.SearchResult> results = compiler.search(query, limit);

                List<SearchResultResponse> responseResults = results.stream()
                        .map(r -> new SearchResultResponse(r.title(), r.path(), r.url(), r.score()))
                        .toList();

                ctx.status(200);
                ctx.json(new SearchResponse(
                        query != null ? query : "",
                        responseResults,
                        results.size(),
                        "Search completed successfully"
                ));
            } catch (Exception e) {
                logger.error("Error during search: ", e);
                ctx.status(500);
                ctx.json(new SearchResponse("", List.of(), 0, "Search failed: " + e.getMessage()));
            }
        });

        // Search page endpoint (serves HTML)
        app.get("/search", ctx -> {
            try {
                String query = ctx.queryParam("q");
                String limitStr = ctx.queryParam("limit");
                int limit = 10;
                if (limitStr != null && !limitStr.isBlank()) {
                    try { limit = Math.min(Integer.parseInt(limitStr), 100); } catch (NumberFormatException ex) { limit = 10; }
                }
                boolean showAll = limit >= 50;

                List<Compiler.SearchResult> results = compiler.search(query, limit);
                String html = compiler.generateSearchResultsPage(query, results, showAll);

                ctx.html(html);
            } catch (Exception e) {
                logger.error("Error generating search page: ", e);
                ctx.status(500);
                ctx.result("Error processing search: " + e.getMessage());
            }
        });

        // JIT compilation: Compile markdown files on-demand when HTML is requested
        app.before("/{path:.*}", ctx -> {
            String path = ctx.path();
            if (path != null && path.toLowerCase().endsWith(".html") && !path.equals("/index.html")) {
                String relativePath = path.substring(1);
                String mdPath = relativePath.replaceAll("(?i)\\.html$", ".md");
                Path targetFile = publicDirPath.resolve(relativePath);
                if (!Files.exists(targetFile)) {
                    logger.debug("JIT compilation triggered for: {}", mdPath);
                    boolean success = compiler.compileSingleJIT(mdPath);
                    if (!success) {
                        Path contentFile = compiler.getContentDir().resolve(mdPath);
                        if (Files.exists(contentFile)) {
                            compiler.compileSingleJIT(mdPath);
                        }
                    }
                }
            }
        });

        // Serve index.html as the default page for root
        app.get("/", ctx -> {
            Path indexFile = publicDirPath.resolve("index.html");
            if (Files.exists(indexFile)) {
                ctx.html(Files.readString(indexFile));
            } else {
                String indexMd = "# Welcome to Neopedia\n\nThe free and open educational platform.";
                String html = compiler.compileToHtml("Neopedia", indexMd);
                ctx.html(html);
            }
        });

        app.start(PORT);
        logger.info("Neopedia server started on port {}", PORT);
    }
}
