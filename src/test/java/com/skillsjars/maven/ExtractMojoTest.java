package com.skillsjars.maven;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.project.MavenProject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
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
        File jarFile = createTestSkillsJar("test-skill-new", "META-INF/skills/org/repo/skill/");
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
    public void testExtractSkillsJarFromPluginDependencies() throws Exception {
        File jarFile = createTestSkillsJar("plugin-skill", "META-INF/skills/org/repo/skill/");
        File outputDir = new File(testDir, "output");

        Artifact pluginArtifact = new DefaultArtifact(
            "com.skillsjars",
            "skill2",
            "1.0.0",
            Artifact.SCOPE_COMPILE,
            "jar",
            null,
            new DefaultArtifactHandler("jar")
        );
        pluginArtifact.setFile(jarFile);

        PluginDescriptor pluginDescriptor = new PluginDescriptor();
        pluginDescriptor.setArtifacts(Collections.singletonList(pluginArtifact));

        MojoDescriptor mojoDescriptor = new MojoDescriptor();
        mojoDescriptor.setPluginDescriptor(pluginDescriptor);

        MojoExecution mojoExecution = new MojoExecution(mojoDescriptor);

        ExtractMojo mojo = new ExtractMojo();
        mojo.setDir(outputDir.getAbsolutePath());

        MavenProject project = new MavenProject();
        project.setArtifacts(new HashSet<>());
        mojo.setProject(project);
        mojo.setMojoExecution(mojoExecution);

        mojo.execute();

        Path extractedSkillMd = Paths.get(outputDir.getAbsolutePath(), "skillsjars__org__repo__skill", "SKILL.md");
        assertTrue("SKILL.md from plugin dependency should exist", Files.exists(extractedSkillMd));

        Path extractedFile = Paths.get(outputDir.getAbsolutePath(), "skillsjars__org__repo__skill", "test.txt");
        assertTrue("Extracted file from plugin dependency should exist", Files.exists(extractedFile));
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

    @Test
    public void testDoesNotExtractMavenPluginArtifacts() throws Exception {
        File skillsJar = createTestSkillsJar("test-skill");
        File pluginJar = createTestSkillsJar("maven-plugin");
        File outputDir = new File(testDir, "output");

        ExtractMojo mojo = new ExtractMojo();
        mojo.setDir(outputDir.getAbsolutePath());

        MavenProject project = new MavenProject();
        Set<Artifact> artifacts = new HashSet<>();

        // Add a regular jar artifact
        Artifact skillArtifact = new DefaultArtifact(
            "com.skillsjars",
            "test-skill",
            "1.0.0",
            Artifact.SCOPE_COMPILE,
            "jar",
            null,
            new DefaultArtifactHandler("jar")
        );
        skillArtifact.setFile(skillsJar);
        artifacts.add(skillArtifact);

        // Add a maven-plugin artifact
        Artifact pluginArtifact = new DefaultArtifact(
            "com.skillsjars",
            "maven-plugin",
            "0.0.4",
            Artifact.SCOPE_COMPILE,
            "maven-plugin",
            null,
            new DefaultArtifactHandler("maven-plugin")
        );
        pluginArtifact.setFile(pluginJar);
        artifacts.add(pluginArtifact);

        project.setArtifacts(artifacts);
        mojo.setProject(project);

        mojo.execute();

        // Should extract the regular jar
        Path extractedSkill = Paths.get(outputDir.getAbsolutePath(), "skillsjars__org__repo__skill", "SKILL.md");
        assertTrue("Regular jar should be extracted", Files.exists(extractedSkill));
    }

    @Test
    public void testExtractsFromAllGroupIds() throws Exception {
        File skillsJar1 = createTestSkillsJar("test-skill-1");
        File skillsJar2 = createTestSkillsJar("test-skill-2", "META-INF/skills/jamesward/skills/zen/");
        File outputDir = new File(testDir, "output");

        ExtractMojo mojo = new ExtractMojo();
        mojo.setDir(outputDir.getAbsolutePath());

        MavenProject project = new MavenProject();
        Set<Artifact> artifacts = new HashSet<>();

        Artifact artifact1 = new DefaultArtifact(
            "com.skillsjars",
            "test-skill-1",
            "1.0.0",
            Artifact.SCOPE_COMPILE,
            "jar",
            null,
            new DefaultArtifactHandler("jar")
        );
        artifact1.setFile(skillsJar1);
        artifacts.add(artifact1);

        Artifact artifact2 = new DefaultArtifact(
            "com.jamesward",
            "test-skill-2",
            "1.0.0",
            Artifact.SCOPE_COMPILE,
            "jar",
            null,
            new DefaultArtifactHandler("jar")
        );
        artifact2.setFile(skillsJar2);
        artifacts.add(artifact2);

        project.setArtifacts(artifacts);
        mojo.setProject(project);

        mojo.execute();

        // Should extract both
        Path extracted1 = Paths.get(outputDir.getAbsolutePath(), "skillsjars__org__repo__skill", "SKILL.md");
        assertTrue("com.skillsjars artifact should be extracted", Files.exists(extracted1));

        Path extracted2 = Paths.get(outputDir.getAbsolutePath(), "skillsjars__jamesward__skills__zen", "SKILL.md");
        assertTrue("com.jamesward artifact should be extracted", Files.exists(extracted2));
    }

    @Test
    public void testExtractsFromAllScopes() throws Exception {
        File compileJar = createTestSkillsJar("compile-skill");
        File runtimeJar = createTestSkillsJar("runtime-skill", "META-INF/skills/runtime/repo/skill/");
        File outputDir = new File(testDir, "output");

        ExtractMojo mojo = new ExtractMojo();
        mojo.setDir(outputDir.getAbsolutePath());

        MavenProject project = new MavenProject();
        Set<Artifact> artifacts = new HashSet<>();

        // Compile scope dependency
        Artifact compileArtifact = new DefaultArtifact(
            "com.skillsjars",
            "compile-skill",
            "1.0.0",
            Artifact.SCOPE_COMPILE,
            "jar",
            null,
            new DefaultArtifactHandler("jar")
        );
        compileArtifact.setFile(compileJar);
        artifacts.add(compileArtifact);

        // Runtime scope dependency
        Artifact runtimeArtifact = new DefaultArtifact(
            "com.skillsjars",
            "runtime-skill",
            "1.0.0",
            Artifact.SCOPE_RUNTIME,
            "jar",
            null,
            new DefaultArtifactHandler("jar")
        );
        runtimeArtifact.setFile(runtimeJar);
        artifacts.add(runtimeArtifact);

        project.setArtifacts(artifacts);
        mojo.setProject(project);

        mojo.execute();

        // Should extract both
        Path compileExtracted = Paths.get(outputDir.getAbsolutePath(), "skillsjars__org__repo__skill", "SKILL.md");
        assertTrue("Compile scope dependency should be extracted", Files.exists(compileExtracted));

        Path runtimeExtracted = Paths.get(outputDir.getAbsolutePath(), "skillsjars__runtime__repo__skill", "SKILL.md");
        assertTrue("Runtime scope dependency should be extracted", Files.exists(runtimeExtracted));
    }

    @Test
    public void testUseSkillsNameAsDirectory() throws Exception {
        File jarFile = createTestSkillsJar("test-skill");
        File outputDir = new File(testDir, "output");

        ExtractMojo mojo = createExtractMojo(outputDir, "test-skill", jarFile);
        mojo.setUseSkillsNameAsDirectory(true);

        mojo.execute();

        // Should use last segment "skill" instead of "skillsjars__org__repo__skill"
        Path extractedSkillMd = Paths.get(outputDir.getAbsolutePath(), "skill", "SKILL.md");
        assertTrue("SKILL.md should exist under skill name directory", Files.exists(extractedSkillMd));

        Path extractedFile = Paths.get(outputDir.getAbsolutePath(), "skill", "test.txt");
        assertTrue("Extracted file should exist under skill name directory", Files.exists(extractedFile));

        String content = new String(Files.readAllBytes(extractedFile));
        assertEquals("test content", content);

        Path nestedFile = Paths.get(outputDir.getAbsolutePath(), "skill", "foo", "nested.txt");
        assertTrue("Nested file should exist under skill name directory", Files.exists(nestedFile));
    }

    @Test
    public void testUseSkillsNameAsDirectoryWithNewPath() throws Exception {
        File jarFile = createTestSkillsJar("test-skill-new", "META-INF/skills/org/repo/zen/");
        File outputDir = new File(testDir, "output");

        ExtractMojo mojo = createExtractMojo(outputDir, "test-skill-new", jarFile);
        mojo.setUseSkillsNameAsDirectory(true);

        mojo.execute();

        Path extractedSkillMd = Paths.get(outputDir.getAbsolutePath(), "zen", "SKILL.md");
        assertTrue("SKILL.md should exist under 'zen' directory", Files.exists(extractedSkillMd));

        Path extractedFile = Paths.get(outputDir.getAbsolutePath(), "zen", "test.txt");
        assertTrue("Extracted file should exist under 'zen' directory", Files.exists(extractedFile));
    }

    @Test
    public void testUseSkillsNameAsDirectoryConflictThrowsError() throws Exception {
        // Two jars with different skill roots but same last segment "skill"
        File jarFile1 = createTestSkillsJar("skill1", "META-INF/skills/org/repo1/skill/");
        File jarFile2 = createTestSkillsJar("skill2", "META-INF/skills/org/repo2/skill/");
        File outputDir = new File(testDir, "output");

        ExtractMojo mojo = new ExtractMojo();
        mojo.setDir(outputDir.getAbsolutePath());
        mojo.setUseSkillsNameAsDirectory(true);

        MavenProject project = new MavenProject();
        Set<Artifact> artifacts = new HashSet<>();

        Artifact artifact1 = new DefaultArtifact(
            "com.skillsjars", "skill1", "1.0.0",
            Artifact.SCOPE_COMPILE, "jar", null, new DefaultArtifactHandler("jar")
        );
        artifact1.setFile(jarFile1);
        artifacts.add(artifact1);

        Artifact artifact2 = new DefaultArtifact(
            "com.skillsjars", "skill2", "1.0.0",
            Artifact.SCOPE_COMPILE, "jar", null, new DefaultArtifactHandler("jar")
        );
        artifact2.setFile(jarFile2);
        artifacts.add(artifact2);

        project.setArtifacts(artifacts);
        mojo.setProject(project);

        try {
            mojo.execute();
            fail("Should have thrown MojoExecutionException for conflicting skill name directories");
        } catch (MojoExecutionException e) {
            assertTrue(e.getMessage().contains("conflict"));
        }
    }

    @Test
    public void testUseSkillsNameAsDirectoryNoConflict() throws Exception {
        // Two jars with different last segments should not conflict
        File jarFile1 = createTestSkillsJar("skill1", "META-INF/skills/org/repo/foo/");
        File jarFile2 = createTestSkillsJar("skill2", "META-INF/skills/org/repo/bar/");
        File outputDir = new File(testDir, "output");

        ExtractMojo mojo = new ExtractMojo();
        mojo.setDir(outputDir.getAbsolutePath());
        mojo.setUseSkillsNameAsDirectory(true);

        MavenProject project = new MavenProject();
        Set<Artifact> artifacts = new HashSet<>();

        Artifact artifact1 = new DefaultArtifact(
            "com.skillsjars", "skill1", "1.0.0",
            Artifact.SCOPE_COMPILE, "jar", null, new DefaultArtifactHandler("jar")
        );
        artifact1.setFile(jarFile1);
        artifacts.add(artifact1);

        Artifact artifact2 = new DefaultArtifact(
            "com.skillsjars", "skill2", "1.0.0",
            Artifact.SCOPE_COMPILE, "jar", null, new DefaultArtifactHandler("jar")
        );
        artifact2.setFile(jarFile2);
        artifacts.add(artifact2);

        project.setArtifacts(artifacts);
        mojo.setProject(project);

        mojo.execute();

        Path fooSkill = Paths.get(outputDir.getAbsolutePath(), "foo", "SKILL.md");
        assertTrue("'foo' skill should exist", Files.exists(fooSkill));

        Path barSkill = Paths.get(outputDir.getAbsolutePath(), "bar", "SKILL.md");
        assertTrue("'bar' skill should exist", Files.exists(barSkill));
    }

    @Test
    public void testUseSkillsNameAsDirectoryDeletesOldDirectory() throws Exception {
        File outputDir = new File(testDir, "output");
        assertTrue(outputDir.mkdirs());

        // Create a pre-existing skill directory named "skill" that should be deleted
        File preExistingSkillDir = new File(outputDir, "skill");
        assertTrue(preExistingSkillDir.mkdirs());
        File oldFile = new File(preExistingSkillDir, "old-file.txt");
        Files.write(oldFile.toPath(), "should be deleted".getBytes());

        // Pre-existing file at output root should not be deleted
        File preExisting = new File(outputDir, "existing-file.txt");
        Files.write(preExisting.toPath(), "should not be deleted".getBytes());

        File jarFile = createTestSkillsJar("test-skill");
        ExtractMojo mojo = createExtractMojo(outputDir, "test-skill", jarFile);
        mojo.setUseSkillsNameAsDirectory(true);

        mojo.execute();

        assertFalse("Old file should be deleted", oldFile.exists());
        assertTrue("Pre-existing file should not be deleted", preExisting.exists());

        Path newFile = Paths.get(outputDir.getAbsolutePath(), "skill", "test.txt");
        assertTrue("New skill file should exist", Files.exists(newFile));
    }

    @Test
    public void testUseSkillsNameAsDirectoryWithPluginDependencies() throws Exception {
        File jarFile = createTestSkillsJar("plugin-skill", "META-INF/skills/org/repo/myplugin/");
        File outputDir = new File(testDir, "output");

        Artifact pluginArtifact = new DefaultArtifact(
            "com.skillsjars", "plugin-skill", "1.0.0",
            Artifact.SCOPE_COMPILE, "jar", null, new DefaultArtifactHandler("jar")
        );
        pluginArtifact.setFile(jarFile);

        PluginDescriptor pluginDescriptor = new PluginDescriptor();
        pluginDescriptor.setArtifacts(Collections.singletonList(pluginArtifact));

        MojoDescriptor mojoDescriptor = new MojoDescriptor();
        mojoDescriptor.setPluginDescriptor(pluginDescriptor);

        MojoExecution mojoExecution = new MojoExecution(mojoDescriptor);

        ExtractMojo mojo = new ExtractMojo();
        mojo.setDir(outputDir.getAbsolutePath());
        mojo.setUseSkillsNameAsDirectory(true);

        MavenProject project = new MavenProject();
        project.setArtifacts(new HashSet<>());
        mojo.setProject(project);
        mojo.setMojoExecution(mojoExecution);

        mojo.execute();

        Path extractedSkillMd = Paths.get(outputDir.getAbsolutePath(), "myplugin", "SKILL.md");
        assertTrue("SKILL.md from plugin dependency should exist under skill name directory", Files.exists(extractedSkillMd));
    }

    @Test
    public void testExtractsTransitiveDependencies() throws Exception {
        File directJar = createTestSkillsJar("direct-skill");
        File transitiveJar = createTestSkillsJar("transitive-skill", "META-INF/skills/transitive/repo/skill/");
        File outputDir = new File(testDir, "output");

        ExtractMojo mojo = new ExtractMojo();
        mojo.setDir(outputDir.getAbsolutePath());

        MavenProject project = new MavenProject();
        
        // Create direct dependency
        Artifact directArtifact = new DefaultArtifact(
            "com.example",
            "direct-skill",
            "1.0.0",
            Artifact.SCOPE_COMPILE,
            "jar",
            null,
            new DefaultArtifactHandler("jar")
        );
        directArtifact.setFile(directJar);
        
        // Create transitive dependency
        Artifact transitiveArtifact = new DefaultArtifact(
            "com.skillsjars",
            "transitive-skill",
            "1.0.0",
            Artifact.SCOPE_COMPILE,
            "jar",
            null,
            new DefaultArtifactHandler("jar")
        );
        transitiveArtifact.setFile(transitiveJar);
        
        // Set up dependency trail to show transitive relationship
        List<String> dependencyTrail = new ArrayList<>();
        dependencyTrail.add("com.example:test-project:jar:1.0.0");
        dependencyTrail.add("com.example:direct-skill:jar:1.0.0");
        dependencyTrail.add("com.skillsjars:transitive-skill:jar:1.0.0");
        transitiveArtifact.setDependencyTrail(dependencyTrail);
        
        // project.getArtifacts() includes both direct and transitive
        Set<Artifact> artifacts = new HashSet<>();
        artifacts.add(directArtifact);
        artifacts.add(transitiveArtifact);
        
        project.setArtifacts(artifacts);
        mojo.setProject(project);

        mojo.execute();

        // Should extract both direct and transitive
        Path directExtracted = Paths.get(outputDir.getAbsolutePath(), "skillsjars__org__repo__skill", "SKILL.md");
        assertTrue("Direct dependency should be extracted", Files.exists(directExtracted));

        Path transitiveExtracted = Paths.get(outputDir.getAbsolutePath(), "skillsjars__transitive__repo__skill", "SKILL.md");
        assertTrue("Transitive dependency should be extracted", Files.exists(transitiveExtracted));
    }

    private File createTestSkillsJar(String name) throws Exception {
        return createTestSkillsJar(name, "META-INF/resources/skills/org/repo/skill/");
    }

    private File createTestSkillsJar(String name, String skillPath) throws Exception {
        File jarFile = new File(testDir, name + ".jar");

        try (JarOutputStream jos = new JarOutputStream(new FileOutputStream(jarFile))) {
            // Ensure path ends with /
            String path = skillPath.endsWith("/") ? skillPath : skillPath + "/";
            
            // Add SKILL.md marker
            JarEntry skillMd = new JarEntry(path + "SKILL.md");
            jos.putNextEntry(skillMd);
            jos.write("# Test Skill".getBytes());
            jos.closeEntry();

            // Add file at root of skill
            JarEntry entry = new JarEntry(path + "test.txt");
            jos.putNextEntry(entry);
            jos.write("test content".getBytes());
            jos.closeEntry();

            // Add nested file
            JarEntry nested = new JarEntry(path + "foo/nested.txt");
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
