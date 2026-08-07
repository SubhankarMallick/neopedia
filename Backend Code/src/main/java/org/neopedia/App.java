package org.neopedia;

import io.javalin.Javalin;
import io.javalin.http.staticfiles.Location;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

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

        Path targetDirPath = compiler.getTargetDir();
        logger.info("Serving static files from external directory: {}", targetDirPath);

        Javalin app = Javalin.create(config -> {
            config.staticFiles.add(staticFile -> {
                staticFile.hostedPath = "/";
                staticFile.directory = targetDirPath.toAbsolutePath().toString();
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

        app.start(PORT);
        logger.info("Neopedia server started on port {}", PORT);
    }
}
