package org.neopedia;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the Compiler engine.
 */
public class CompilerTest {

    @Test
    public void testCompileToHtml() {
        Compiler compiler = new Compiler();
        String markdown = "# Test Title\n\nInline math: $E = mc^2$\n\nBlock math:\n$$a^2 + b^2 = c^2$$";
        String html = compiler.compileToHtml("Test Page", markdown);

        assertNotNull(html);
        assertTrue(html.contains("<title>Test Page</title>"));
        assertTrue(html.contains("katex.min.css"));
        assertTrue(html.contains("katex.min.js"));
        assertTrue(html.contains("auto-render.min.js"));
        assertTrue(html.contains("Test Title"));
    }

    @Test
    public void testCompileAllPreservesStructure(@TempDir Path tempDir) throws IOException {
        Path contentDir = tempDir.resolve("Content");
        Path publicDir = tempDir.resolve("public");

        Path subDir = contentDir.resolve("class-10").resolve("physics");
        Files.createDirectories(subDir);
        Path mdFile = subDir.resolve("light.md");
        Files.writeString(mdFile, "# Light - Reflection and Refraction\n\nSpeed of light $c = 3 \\times 10^8 m/s$.");

        Compiler compiler = new Compiler(contentDir, publicDir);
        long count = compiler.compileAll();

        assertEquals(1, count);

        Path expectedHtmlFile = publicDir.resolve("class-10").resolve("physics").resolve("light.html");
        assertTrue(Files.exists(expectedHtmlFile));

        String htmlContent = Files.readString(expectedHtmlFile);
        assertTrue(htmlContent.contains("<title>Light - Reflection and Refraction</title>"));
        assertTrue(htmlContent.contains("Speed of light"));
    }

    @Test
    public void testSearch(@TempDir Path tempDir) throws IOException {
        Path contentDir = tempDir.resolve("Content");
        Path publicDir = tempDir.resolve("public");
        Files.createDirectories(contentDir);

        Files.writeString(contentDir.resolve("math.md"), "# Mathematics\n\nBasic math concepts.");
        Files.writeString(contentDir.resolve("physics.md"), "# Physics\n\nPhysics concepts.");
        Files.writeString(contentDir.resolve("chemistry.md"), "# Chemistry\n\nChemical reactions.");

        Compiler compiler = new Compiler(contentDir, publicDir);
        compiler.compileAll();

        List<Compiler.SearchResult> results = compiler.search("math", 10);
        assertFalse(results.isEmpty());
        assertTrue(results.stream().anyMatch(r -> r.title().contains("Mathematics")));
    }

    @Test
    public void testJITCompilation(@TempDir Path tempDir) throws IOException {
        Path contentDir = tempDir.resolve("Content");
        Path publicDir = tempDir.resolve("public");
        Files.createDirectories(contentDir);

        Files.writeString(contentDir.resolve("new-file.md"), "# New File\n\nNew content.");

        Compiler compiler = new Compiler(contentDir, publicDir);

        Path targetFile = publicDir.resolve("new-file.html");
        assertFalse(Files.exists(targetFile));

        boolean success = compiler.compileSingleJIT("new-file.md");
        assertTrue(success);
        assertTrue(Files.exists(targetFile));
    }
}
