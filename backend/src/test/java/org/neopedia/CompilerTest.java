package org.neopedia;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.util.List;
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

    @Test
    public void testSearch(@TempDir Path tempDir) throws IOException {
        Path contentDir = tempDir.resolve("content");
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
        Path contentDir = tempDir.resolve("content");
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

    @Test
    public void testCompileAllRemovesDeletedMarkdownOutput(@TempDir Path tempDir) throws IOException {
        Path contentDir = tempDir.resolve("content");
        Path publicDir = tempDir.resolve("public");
        Path article = contentDir.resolve("class-10").resolve("chemistry").resolve("chemical-reactions.md");
        Files.createDirectories(article.getParent());
        Files.writeString(article, "# Chemical Reactions");

        Compiler compiler = new Compiler(contentDir, publicDir);
        compiler.compileAll();
        Path generatedArticle = publicDir.resolve("class-10").resolve("chemistry").resolve("chemical-reactions.html");
        assertTrue(Files.exists(generatedArticle));

        Files.delete(article);
        compiler.compileAll();
        assertFalse(Files.exists(generatedArticle));
    }

    @Test
    public void testHomepageIsExcludedAndSearchRequiresRelevantTerms(@TempDir Path tempDir) throws IOException {
        Path contentDir = tempDir.resolve("content");
        Path publicDir = tempDir.resolve("public");
        Files.createDirectories(contentDir);
        Files.writeString(contentDir.resolve("index.md"), "# Neopedia Homepage");
        Files.writeString(contentDir.resolve("chemical-reactions.md"), "# Chemical Reactions");
        Files.writeString(contentDir.resolve("types-of-chemical-reactions.md"), "# Types of Chemical Reactions");
        Files.writeString(contentDir.resolve("cell-structure.md"), "# Cell Structure");

        Compiler compiler = new Compiler(contentDir, publicDir);
        compiler.compileAll();

        assertTrue(Files.exists(publicDir.resolve("index.html")));
        assertTrue(compiler.search("homepage", 10).isEmpty());
        List<Compiler.SearchResult> results = compiler.search("chemical reactions", 10);
        assertEquals(2, results.size());
        assertTrue(results.stream().allMatch(result -> result.title().contains("Chemical")));
        assertEquals(2, compiler.search("chemcial reactions", 10).size());
    }

    @Test
    public void testLevelsComeFromTheFirstH1RatherThanTheFilename(@TempDir Path tempDir) throws IOException {
        Path contentDir = tempDir.resolve("content");
        Path publicDir = tempDir.resolve("public");
        Files.createDirectories(contentDir);
        Files.writeString(contentDir.resolve("ProgrammingLevel1.md"), "# Programming : Level 1\n\nLearn the basics.");
        Files.writeString(contentDir.resolve("QuantumMechanicsLevel6.md"), "# Quantum Mechanics: Level 6\n\nAdvanced physics.");
        Files.writeString(contentDir.resolve("any-name-at-all.md"), "# Calculus : Level 4\n\nDerivatives and integrals.");
        Files.writeString(contentDir.resolve("bad-level.md"), "# Programming : Level 7\n\nStill published.");
        Files.writeString(contentDir.resolve("word-level.md"), "# Programming : Level One\n\nStill published.");
        Files.writeString(contentDir.resolve("missing-level.md"), "# Programming\n\nStill published.");

        Compiler compiler = new Compiler(contentDir, publicDir);
        assertEquals(6, compiler.compileAll());

        String programmingHtml = Files.readString(publicDir.resolve("ProgrammingLevel1.html"));
        assertTrue(programmingHtml.contains("<h1>Programming</h1>"));
        assertTrue(programmingHtml.contains("[ 6–8 ]"));
        assertFalse(programmingHtml.contains("Programming : Level 1"));
        assertTrue(Files.readString(publicDir.resolve("QuantumMechanicsLevel6.html")).contains("[ PhD ]"));
        assertTrue(Files.readString(publicDir.resolve("any-name-at-all.html")).contains("[ Undergraduate ]"));
        assertTrue(Files.readString(publicDir.resolve("bad-level.html")).contains("[ Unknown ]"));
        assertTrue(Files.readString(publicDir.resolve("word-level.html")).contains("[ Unknown ]"));
        assertTrue(Files.readString(publicDir.resolve("missing-level.html")).contains("[ Unknown ]"));
    }

    @Test
    public void testSearchRanksTitlesAboveBodyOnlyMatches(@TempDir Path tempDir) throws IOException {
        Path contentDir = tempDir.resolve("content");
        Path publicDir = tempDir.resolve("public");
        Files.createDirectories(contentDir);
        Files.writeString(contentDir.resolve("index.md"), "# Neopedia Homepage");
        Files.writeString(contentDir.resolve("NewtonLawsLevel2.md"), "# Newton's Laws of Motion : Level 2\n\nNewton's laws describe forces.");
        Files.writeString(contentDir.resolve("NewtonFirstLevel2.md"), "# Newton's First Law : Level 2\n\nAn object remains at rest.");
        Files.writeString(contentDir.resolve("NewtonianMechanicsLevel4.md"), "# Newtonian Mechanics : Level 4\n\nClassical mechanics.");
        Files.writeString(contentDir.resolve("history.md"), "# History of Physics : Level 2\n\nNewton laws are mentioned here.");

        Compiler compiler = new Compiler(contentDir, publicDir);
        compiler.compileAll();

        List<Compiler.SearchResult> results = compiler.search("newton laws", 10);
        assertEquals("Newton's Laws of Motion", results.getFirst().title());
        assertTrue(results.stream().anyMatch(result -> result.title().equals("Newton's First Law")));
        assertTrue(results.stream().anyMatch(result -> result.title().equals("Newtonian Mechanics")));
        assertFalse(results.stream().anyMatch(result -> result.title().equals("History of Physics")));
        assertEquals("9–10", results.getFirst().level());
        assertFalse(results.getFirst().excerpt().isBlank());
        assertTrue(compiler.search("homepage", 10).isEmpty());
        assertTrue(compiler.search("unrelated astronomy", 10).isEmpty());
    }
}
