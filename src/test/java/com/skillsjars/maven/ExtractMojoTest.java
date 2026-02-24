package com.skillsjars.maven;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.stream.Stream;

import static org.junit.Assert.*;

public class ExtractMojoTest {

    private File testDir;

    @Before
    public void setUp() throws Exception {
        testDir = Files.createTempDirectory("skillsjars-test").toFile();
    }

    @After
    public void tearDown() throws Exception {
        deleteDirectory(testDir.toPath());
    }

    @Test(expected = MojoExecutionException.class)
    public void testExtractWithoutDirParameterFails() throws Exception {
        ExtractMojo mojo = new ExtractMojo();
        MavenProject project = new MavenProject();
        project.setArtifacts(new HashSet<>());
        mojo.setProject(project);
        
        mojo.execute();
    }

    @Test
    public void testExtractSkillsJar() throws Exception {
        File jarFile = createTestSkillsJar("test-skill");
        File outputDir = new File(testDir, "output");

        ExtractMojo mojo = createExtractMojo(outputDir, "test-skill", jarFile);

        mojo.execute();

        Path extractedSkillMd = Paths.get(outputDir.getAbsolutePath(), "skillsjars__org__repo__skill", "SKILL.md");
        assertTrue("SKILL.md should exist", Files.exists(extractedSkillMd));
        
        Path extractedFile = Paths.get(outputDir.getAbsolutePath(), "skillsjars__org__repo__skill", "test.txt");
        assertTrue("Extracted file should exist", Files.exists(extractedFile));
        
        String content = new String(Files.readAllBytes(extractedFile));
        assertEquals("test content", content);
        
        Path nestedFile = Paths.get(outputDir.getAbsolutePath(), "skillsjars__org__repo__skill", "foo", "nested.txt");
        assertTrue("Nested file should exist", Files.exists(nestedFile));
        
        String nestedContent = new String(Files.readAllBytes(nestedFile));
        assertEquals("nested content", nestedContent);
    }

    @Test
    public void testExtractSkillsJarWithNewPath() throws Exception {
        File jarFile = createTestSkillsJar("test-skill-new", "META-INF/skills/");
        File outputDir = new File(testDir, "output");

        ExtractMojo mojo = createExtractMojo(outputDir, "test-skill-new", jarFile);

        mojo.execute();

        Path extractedSkillMd = Paths.get(outputDir.getAbsolutePath(), "skillsjars__org__repo__skill", "SKILL.md");
        assertTrue("SKILL.md should exist", Files.exists(extractedSkillMd));

        Path extractedFile = Paths.get(outputDir.getAbsolutePath(), "skillsjars__org__repo__skill", "test.txt");
        assertTrue("Extracted file should exist", Files.exists(extractedFile));

        String content = new String(Files.readAllBytes(extractedFile));
        assertEquals("test content", content);

        Path nestedFile = Paths.get(outputDir.getAbsolutePath(), "skillsjars__org__repo__skill", "foo", "nested.txt");
        assertTrue("Nested file should exist", Files.exists(nestedFile));

        String nestedContent = new String(Files.readAllBytes(nestedFile));
        assertEquals("nested content", nestedContent);
    }

    @NonNullDecl
    private static ExtractMojo createExtractMojo(File outputDir, String artifactId, File jarFile) {
        ExtractMojo mojo = new ExtractMojo();
        mojo.setDir(outputDir.getAbsolutePath());

        MavenProject project = new MavenProject();
        Set<Artifact> artifacts = new HashSet<>();

        Artifact artifact = new DefaultArtifact(
            "com.skillsjars",
                artifactId,
            "1.0.0",
            Artifact.SCOPE_COMPILE,
            "jar",
            null,
            new DefaultArtifactHandler("jar")
        );
        artifact.setFile(jarFile);
        artifacts.add(artifact);

        project.setArtifacts(artifacts);
        mojo.setProject(project);
        return mojo;
    }

    @Test
    public void testConflictingPathsThrowsError() throws Exception {
        File jarFile1 = createTestSkillsJar("skill1");
        File jarFile2 = createTestSkillsJar("skill2");
        File outputDir = new File(testDir, "output");

        ExtractMojo mojo = new ExtractMojo();
        mojo.setDir(outputDir.getAbsolutePath());

        MavenProject project = new MavenProject();
        Set<Artifact> artifacts = new HashSet<>();

        Artifact artifact1 = new DefaultArtifact(
            "com.skillsjars",
            "skill1",
            "1.0.0",
            Artifact.SCOPE_COMPILE,
            "jar",
            null,
            new DefaultArtifactHandler("jar")
        );
        artifact1.setFile(jarFile1);
        artifacts.add(artifact1);

        Artifact artifact2 = new DefaultArtifact(
            "com.skillsjars",
            "skill2",
            "1.0.0",
            Artifact.SCOPE_COMPILE,
            "jar",
            null,
            new DefaultArtifactHandler("jar")
        );
        artifact2.setFile(jarFile2);
        artifacts.add(artifact2);

        project.setArtifacts(artifacts);
        mojo.setProject(project);

        try {
            mojo.execute();
            fail("Should have thrown MojoExecutionException for conflicting paths");
        } catch (MojoExecutionException e) {
            assertTrue(e.getMessage().contains("conflict"));
        }
    }

    @Test
    public void testOnlyDeletesIndividualSkillDirectory() throws Exception {
        File outputDir = new File(testDir, "output");
        assertTrue(outputDir.mkdirs());

        // Create a pre-existing file in the output directory
        File preExisting = new File(outputDir, "existing-file.txt");
        Files.write(preExisting.toPath(), "should not be deleted".getBytes());

        // Create a pre-existing skill directory that should be deleted
        File preExistingSkillDir = new File(outputDir, "skillsjars__org__repo__skill");
        assertTrue(preExistingSkillDir.mkdirs());
        File oldSkillFile = new File(preExistingSkillDir, "old-file.txt");
        Files.write(oldSkillFile.toPath(), "should be deleted".getBytes());

        // Now extract a skill
        File jarFile = createTestSkillsJar("test-skill");
        ExtractMojo mojo = createExtractMojo(outputDir, "test-skill", jarFile);

        mojo.execute();

        // Pre-existing file should still exist
        assertTrue("Pre-existing file should not be deleted", preExisting.exists());
        String content = new String(Files.readAllBytes(preExisting.toPath()));
        assertEquals("should not be deleted", content);

        // Old skill file should be deleted
        assertFalse("Old skill file should be deleted", oldSkillFile.exists());

        // New skill files should exist
        Path extractedFile = Paths.get(outputDir.getAbsolutePath(), "skillsjars__org__repo__skill", "test.txt");
        assertTrue("New skill file should exist", Files.exists(extractedFile));
    }

    private File createTestSkillsJar(String name) throws Exception {
        return createTestSkillsJar(name, "META-INF/resources/skills/");
    }

    private File createTestSkillsJar(String name, String prefix) throws Exception {
        File jarFile = new File(testDir, name + ".jar");

        try (JarOutputStream jos = new JarOutputStream(new FileOutputStream(jarFile))) {
            // Add SKILL.md marker
            JarEntry skillMd = new JarEntry(prefix + "org/repo/skill/SKILL.md");
            jos.putNextEntry(skillMd);
            jos.write("# Test Skill".getBytes());
            jos.closeEntry();

            // Add file at root of skill
            JarEntry entry = new JarEntry(prefix + "org/repo/skill/test.txt");
            jos.putNextEntry(entry);
            jos.write("test content".getBytes());
            jos.closeEntry();

            // Add nested file
            JarEntry nested = new JarEntry(prefix + "org/repo/skill/foo/nested.txt");
            jos.putNextEntry(nested);
            jos.write("nested content".getBytes());
            jos.closeEntry();
        }

        return jarFile;
    }

    private void deleteDirectory(Path path) throws Exception {
        if (!Files.exists(path)) {
            return;
        }
        
        try (Stream<Path> pathStream = Files.walk(path)) {
            pathStream
                    .sorted(Comparator.reverseOrder())
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
