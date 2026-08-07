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
        Path contentDir = tempDir.resolve("content");
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
}
