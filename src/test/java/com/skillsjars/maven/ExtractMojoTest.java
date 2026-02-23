package com.skillsjars.maven;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

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

        ExtractMojo mojo = new ExtractMojo();
        mojo.setDir(outputDir.getAbsolutePath());
        
        MavenProject project = new MavenProject();
        Set<Artifact> artifacts = new HashSet<>();
        
        Artifact artifact = new DefaultArtifact(
            "com.skillsjars",
            "test-skill",
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

        ExtractMojo mojo = new ExtractMojo();
        mojo.setDir(outputDir.getAbsolutePath());

        MavenProject project = new MavenProject();
        Set<Artifact> artifacts = new HashSet<>();

        Artifact artifact = new DefaultArtifact(
            "com.skillsjars",
            "test-skill-new",
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
        
        Files.walk(path)
            .sorted((a, b) -> b.compareTo(a))
            .forEach(p -> {
                try {
                    Files.delete(p);
                } catch (Exception e) {
                    // ignore
                }
            });
    }
}
