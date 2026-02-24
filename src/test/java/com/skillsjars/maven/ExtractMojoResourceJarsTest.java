package com.skillsjars.maven;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.project.MavenProject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.jar.JarFile;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;

import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;

/**
 * Tests ExtractMojo functionality using real skills jar files from test resources.
 */
public class ExtractMojoResourceJarsTest {

    private File testDir;
    private File resourcesDir;

    @Before
    public void setUp() throws Exception {
        testDir = Files.createTempDirectory("skillsjars-resource-test").toFile();
        resourcesDir = getResourcesDirectory();
    }

    @After
    public void tearDown() throws Exception {
        deleteDirectory(testDir.toPath());
    }

    private File getResourcesDirectory() throws URISyntaxException {
        URI resourcesUri = Objects.requireNonNull(getClass().getClassLoader().getResource(".")).toURI();
        return new File(resourcesUri)
                .getParentFile().toPath()
                .resolve("test-classes")
                .toFile();
    }

    private File findResourceJar(String namePattern) {
        File[] jars = resourcesDir.listFiles((dir, name) ->
                name.endsWith(".jar") && name.contains(namePattern));
        return (jars != null && jars.length > 0) ? jars[0] : null;
    }

    private boolean isValidJar(File jarFile) {
        return jarFile != null && jarFile.exists() && jarFile.length() > 0;
    }

    @Test
    public void testExtractJdbDebuggerSkillsJar() throws Exception {
        File jarFile = findResourceJar("jdb-agentic-debugger");
        assumeTrue("JDB debugger jar not available or empty", isValidJar(jarFile));

        File outputDir = new File(testDir, "output");

        ExtractMojo mojo = createExtractMojo(outputDir, jarFile);
        mojo.execute();

        assertTrue("Output directory should exist", outputDir.exists());

        File[] skillsDirs = outputDir.listFiles((dir, name) -> name.startsWith("skillsjars__"));
        assertNotNull("Skills directories should exist", skillsDirs);
        assertTrue("At least one skills directory should be extracted", skillsDirs.length > 0);

        assertTrue("SKILL.md files should exist and be readable", verifySkillFiles(skillsDirs));
    }

    @Test
    public void testExtractSpringBootSkillsJar() throws Exception {
        File jarFile = findResourceJar("sivalabs-agent-skills");
        assumeTrue("Spring Boot skills jar not available or empty", isValidJar(jarFile));

        File outputDir = new File(testDir, "output");

        ExtractMojo mojo = createExtractMojo(outputDir, jarFile);
        mojo.execute();

        assertTrue("Output directory should exist", outputDir.exists());

        File[] skillsDirs = outputDir.listFiles((dir, name) -> name.startsWith("skillsjars__"));
        assertNotNull("Skills directories should exist", skillsDirs);
        assertTrue("At least one skills directory should be extracted", skillsDirs.length > 0);

        assertTrue("SKILL.md files should exist and be readable", verifySkillFiles(skillsDirs));
    }

    @Test
    public void testExtractMultipleResourceJars() throws Exception {
        File jdbJar = findResourceJar("jdb-agentic-debugger");
        File springJar = findResourceJar("sivalabs-agent-skills");

        assumeTrue("Both resource jars must be available and non-empty",
                isValidJar(jdbJar) && isValidJar(springJar));

        File outputDir = new File(testDir, "output");

        ExtractMojo mojo = createExtractMojo(outputDir, jdbJar, springJar);
        mojo.execute();

        assertTrue("Output directory should exist", outputDir.exists());

        File[] skillsDirs = outputDir.listFiles((dir, name) -> name.startsWith("skillsjars__"));
        assertNotNull("Skills directories should exist", skillsDirs);
        assertTrue("Multiple skills directories should be extracted", skillsDirs.length >= 2);

        assertTrue("SKILL.md files should exist and be readable", verifySkillFiles(skillsDirs));
    }

    private static boolean verifySkillFiles(File[] skillsDirs) {
        return Arrays.stream(skillsDirs).
                map(dir -> new File(dir, "SKILL.md")).
                allMatch(skillMd -> skillMd.exists() && skillMd.canRead() && skillMd.length() > 0);
    }

    @Test
    public void testReExtractOverwritesExisting() throws Exception {
        File jdbJar = findResourceJar("jdb-agentic-debugger");
        File springJar = findResourceJar("sivalabs-agent-skills");

        assumeTrue("Both resource jars must be available and non-empty",
                isValidJar(jdbJar) && isValidJar(springJar));

        File outputDir = new File(testDir, "output");

        // First extraction
        ExtractMojo mojo1 = createExtractMojo(outputDir, jdbJar, springJar);
        mojo1.execute();

        File[] skillsDirs = outputDir.listFiles((dir, name) -> name.startsWith("skillsjars__"));
        assertNotNull("Skills directories should exist", skillsDirs);
        assertTrue("Skills directories should be extracted", skillsDirs.length > 0);

        // Add a marker file to all skills directories
        for (File skillDir : skillsDirs) {
            File markerFile = new File(skillDir, "marker.txt");
            Files.write(markerFile.toPath(), "marker".getBytes());
            assertTrue("Marker file should exist in " + skillDir.getName(), markerFile.exists());
        }

        // The second extraction should remove all marker files
        ExtractMojo mojo2 = createExtractMojo(outputDir, jdbJar, springJar);
        mojo2.execute();

        for (File skillDir : skillsDirs) {
            File markerFile = new File(skillDir, "marker.txt");
            assertFalse("Marker file should be removed after re-extraction in " + skillDir.getName(), markerFile.exists());
        }
    }

    private ExtractMojo createExtractMojo(File outputDir, File... jarFiles) throws IOException {
        ExtractMojo mojo = new ExtractMojo();
        mojo.setDir(outputDir.getAbsolutePath());

        MavenProject project = new MavenProject();
        Set<Artifact> artifacts = new HashSet<>();

        for (File jarFile : jarFiles) {
            // Read Maven metadata from the jar's pom.properties
            Properties pomProps = readPomProperties(jarFile);
            String groupId = pomProps.getProperty("groupId", "com.skillsjars");
            String artifactId = pomProps.getProperty("artifactId", jarFile.getName().replace(".jar", ""));
            String version = pomProps.getProperty("version", "1.0.0");

            Artifact artifact = new DefaultArtifact(
                    groupId,
                    artifactId,
                    version,
                    Artifact.SCOPE_COMPILE,
                    "jar",
                    null,
                    new DefaultArtifactHandler("jar")
            );
            artifact.setFile(jarFile);
            artifacts.add(artifact);
        }

        project.setArtifacts(artifacts);
        mojo.setProject(project);
        return mojo;
    }

    private Properties readPomProperties(File jarFile) throws IOException {
        Properties props = new Properties();
        try (JarFile jar = new JarFile(jarFile)) {
            ZipEntry pomPropsEntry = jar.stream()
                    .filter(e -> e.getName().startsWith("META-INF/maven/") && e.getName().endsWith("/pom.properties"))
                    .findFirst()
                    .orElse(null);

            if (pomPropsEntry != null) {
                try (InputStream is = jar.getInputStream(pomPropsEntry)) {
                    props.load(is);
                }
            }
        }
        return props;
    }

    private void deleteDirectory(Path path) throws IOException {
        if (!Files.exists(path)) {
            return;
        }

        try (Stream<Path> pathStream = Files.walk(path)) {
            pathStream.sorted(Comparator.reverseOrder())
                    .forEach(p -> {
                        try {
                            Files.delete(p);
                        } catch (Exception e) {
                            // ignore
                        }
                    });
        }
    }
}
