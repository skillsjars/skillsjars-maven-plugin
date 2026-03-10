package com.skillsjars.maven;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.model.Build;
import org.apache.maven.model.Scm;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;

import static org.junit.Assert.*;

public class PackageMojoTest {

    private File testDir;

    @Before
    public void setUp() throws Exception {
        testDir = Files.createTempDirectory("skillsjars-package-test").toFile();
    }

    @After
    public void tearDown() throws Exception {
        deleteDirectory(testDir.toPath());
    }

    @Test
    public void testPackageWithoutSkillsDirDoesNothing() throws Exception {
        PackageMojo mojo = new PackageMojo();
        File nonExistent = new File(testDir, "nonexistent");
        mojo.setSkillsDir(nonExistent);
        
        MavenProject project = createTestProject();
        mojo.setProject(project);
        
        mojo.execute();
        
        File outputDir = new File(testDir, "target/classes");
        assertFalse("Output dir should not be created", outputDir.exists() || 
                    (outputDir.exists() && outputDir.list().length == 0));
    }

    @Test
    public void testPackageSkillWithGitHubScm() throws Exception {
        File skillsDir = new File(testDir, "skills");
        File skillDir = new File(skillsDir, "test-skill");
        assertTrue(skillDir.mkdirs());
        
        Files.write(new File(skillDir, "SKILL.md").toPath(), "# Test Skill".getBytes());
        Files.write(new File(skillDir, "test.txt").toPath(), "test content".getBytes());
        
        File nestedDir = new File(skillDir, "nested");
        assertTrue(nestedDir.mkdirs());
        Files.write(new File(nestedDir, "nested.txt").toPath(), "nested content".getBytes());

        PackageMojo mojo = new PackageMojo();
        mojo.setSkillsDir(skillsDir);
        
        MavenProject project = createTestProject();
        Scm scm = new Scm();
        scm.setUrl("https://github.com/testorg/testrepo");
        project.setScm(scm);
        mojo.setProject(project);

        mojo.execute();

        File outputDir = new File(testDir, "target/classes");
        assertTrue("Output directory should be created", outputDir.exists());

        File skillMd = new File(outputDir, "META-INF/skills/testorg/testrepo/test-skill/SKILL.md");
        File testTxt = new File(outputDir, "META-INF/skills/testorg/testrepo/test-skill/test.txt");
        File nestedTxt = new File(outputDir, "META-INF/skills/testorg/testrepo/test-skill/nested/nested.txt");
        
        assertTrue("Should contain SKILL.md", skillMd.exists());
        assertTrue("Should contain test.txt", testTxt.exists());
        assertTrue("Should contain nested file", nestedTxt.exists());
    }

    @Test
    public void testPackageSkillWithoutScm() throws Exception {
        File skillsDir = new File(testDir, "skills");
        File skillDir = new File(skillsDir, "my-skill");
        assertTrue(skillDir.mkdirs());
        
        Files.write(new File(skillDir, "SKILL.md").toPath(), "# My Skill".getBytes());
        Files.write(new File(skillDir, "data.txt").toPath(), "data".getBytes());

        PackageMojo mojo = new PackageMojo();
        mojo.setSkillsDir(skillsDir);
        
        MavenProject project = createTestProject();
        mojo.setProject(project);

        mojo.execute();

        File outputDir = new File(testDir, "target/classes");
        File skillMd = new File(outputDir, "META-INF/skills/com/example/test/my-skill/SKILL.md");
        File dataTxt = new File(outputDir, "META-INF/skills/com/example/test/my-skill/data.txt");
        
        assertTrue("Should contain SKILL.md with groupId path", skillMd.exists());
        assertTrue("Should contain data.txt", dataTxt.exists());
    }

    @Test
    public void testPackageMultipleSkills() throws Exception {
        File skillsDir = new File(testDir, "skills");
        
        File skill1 = new File(skillsDir, "skill-one");
        assertTrue(skill1.mkdirs());
        Files.write(new File(skill1, "SKILL.md").toPath(), "# Skill One".getBytes());
        Files.write(new File(skill1, "one.txt").toPath(), "one".getBytes());
        
        File skill2 = new File(skillsDir, "skill-two");
        assertTrue(skill2.mkdirs());
        Files.write(new File(skill2, "SKILL.md").toPath(), "# Skill Two".getBytes());
        Files.write(new File(skill2, "two.txt").toPath(), "two".getBytes());

        PackageMojo mojo = new PackageMojo();
        mojo.setSkillsDir(skillsDir);
        
        MavenProject project = createTestProject();
        Scm scm = new Scm();
        scm.setUrl("https://github.com/org/repo");
        project.setScm(scm);
        mojo.setProject(project);

        mojo.execute();

        File outputDir = new File(testDir, "target/classes");
        assertTrue(new File(outputDir, "META-INF/skills/org/repo/skill-one/SKILL.md").exists());
        assertTrue(new File(outputDir, "META-INF/skills/org/repo/skill-one/one.txt").exists());
        assertTrue(new File(outputDir, "META-INF/skills/org/repo/skill-two/SKILL.md").exists());
        assertTrue(new File(outputDir, "META-INF/skills/org/repo/skill-two/two.txt").exists());
    }

    @Test
    public void testSkipDirectoryWithoutSkillMarker() throws Exception {
        File skillsDir = new File(testDir, "skills");
        
        File validSkill = new File(skillsDir, "valid");
        assertTrue(validSkill.mkdirs());
        Files.write(new File(validSkill, "SKILL.md").toPath(), "# Valid".getBytes());
        
        File invalidDir = new File(skillsDir, "invalid");
        assertTrue(invalidDir.mkdirs());
        Files.write(new File(invalidDir, "data.txt").toPath(), "no skill marker".getBytes());

        PackageMojo mojo = new PackageMojo();
        mojo.setSkillsDir(skillsDir);
        
        MavenProject project = createTestProject();
        mojo.setProject(project);

        mojo.execute();

        File outputDir = new File(testDir, "target/classes");
        assertTrue("Should contain valid skill", 
                   new File(outputDir, "META-INF/skills/com/example/test/valid/SKILL.md").exists());
        assertFalse("Should not contain invalid dir", 
                    new File(outputDir, "META-INF/skills/com/example/test/invalid").exists());
    }

    @Test
    public void testExtractFrontmatter() {
        String content = "---\nname: my-skill\ndescription: A skill\nallowed-tools: Bash Read\n---\n# Content";
        String fm = PackageMojo.extractFrontmatter(content);
        assertNotNull(fm);
        assertEquals("my-skill", PackageMojo.extractFrontmatterValue(fm, "name"));
        assertEquals("Bash Read", PackageMojo.extractFrontmatterValue(fm, "allowed-tools"));
        assertNull(PackageMojo.extractFrontmatterValue(fm, "license"));
    }

    @Test
    public void testExtractFrontmatterNoFrontmatter() {
        assertNull(PackageMojo.extractFrontmatter("# Just content"));
    }

    @Test
    public void testValidateAllowedToolsMatchingProperty() throws Exception {
        File skillsDir = new File(testDir, "skills");
        File skillDir = new File(skillsDir, "test-skill");
        assertTrue(skillDir.mkdirs());

        String skillContent = "---\nname: test-skill\ndescription: A skill\nallowed-tools: Bash Read Edit\n---\n# Skill";
        Files.write(new File(skillDir, "SKILL.md").toPath(), skillContent.getBytes());

        PackageMojo mojo = new PackageMojo();
        mojo.setSkillsDir(skillsDir);

        MavenProject project = createTestProject();
        project.getProperties().setProperty("skillsjars.skill.test-skill.allowed-tools", "Bash Read Edit");
        mojo.setProject(project);

        // Should not throw
        mojo.execute();
    }

    @Test
    public void testValidateAllowedToolsMismatchThrows() throws Exception {
        File skillsDir = new File(testDir, "skills");
        File skillDir = new File(skillsDir, "test-skill");
        assertTrue(skillDir.mkdirs());

        String skillContent = "---\nname: test-skill\ndescription: A skill\nallowed-tools: Bash Read Edit\n---\n# Skill";
        Files.write(new File(skillDir, "SKILL.md").toPath(), skillContent.getBytes());

        PackageMojo mojo = new PackageMojo();
        mojo.setSkillsDir(skillsDir);

        MavenProject project = createTestProject();
        project.getProperties().setProperty("skillsjars.skill.test-skill.allowed-tools", "Bash");
        mojo.setProject(project);

        try {
            mojo.execute();
            fail("Expected MojoExecutionException for mismatched allowed-tools");
        } catch (MojoExecutionException e) {
            assertTrue(e.getMessage().contains("does not match"));
        }
    }

    @Test
    public void testValidateAllowedToolsMissingPropertyFails() throws Exception {
        File skillsDir = new File(testDir, "skills");
        File skillDir = new File(skillsDir, "test-skill");
        assertTrue(skillDir.mkdirs());

        String skillContent = "---\nname: test-skill\ndescription: A skill\nallowed-tools: Bash Read Edit\n---\n# Skill";
        Files.write(new File(skillDir, "SKILL.md").toPath(), skillContent.getBytes());

        PackageMojo mojo = new PackageMojo();
        mojo.setSkillsDir(skillsDir);

        MavenProject project = createTestProject();
        // No property set - should fail
        mojo.setProject(project);

        try {
            mojo.execute();
            fail("Expected MojoExecutionException for missing allowed-tools property");
        } catch (MojoExecutionException e) {
            assertTrue(e.getMessage().contains("missing property"));
        }
    }

    @Test
    public void testNoAllowedToolsNoValidation() throws Exception {
        File skillsDir = new File(testDir, "skills");
        File skillDir = new File(skillsDir, "test-skill");
        assertTrue(skillDir.mkdirs());

        String skillContent = "---\nname: test-skill\ndescription: A skill\n---\n# Skill";
        Files.write(new File(skillDir, "SKILL.md").toPath(), skillContent.getBytes());

        PackageMojo mojo = new PackageMojo();
        mojo.setSkillsDir(skillsDir);

        MavenProject project = createTestProject();
        mojo.setProject(project);

        // Should not throw - no allowed-tools means nothing to validate
        mojo.execute();
    }

    private MavenProject createTestProject() {
        MavenProject project = new MavenProject();
        project.setGroupId("com.example.test");
        project.setArtifactId("test-artifact");
        project.setVersion("1.0.0");
        
        Build build = new Build();
        build.setDirectory(new File(testDir, "target").getAbsolutePath());
        build.setOutputDirectory(new File(testDir, "target/classes").getAbsolutePath());
        build.setFinalName("test-artifact-1.0.0");
        project.setBuild(build);
        
        Artifact artifact = new DefaultArtifact(
            "com.example.test",
            "test-artifact",
            "1.0.0",
            Artifact.SCOPE_COMPILE,
            "jar",
            null,
            new DefaultArtifactHandler("jar")
        );
        project.setArtifact(artifact);
        
        return project;
    }

    private void deleteDirectory(Path path) throws Exception {
        if (!Files.exists(path)) {
            return;
        }
        
        try (Stream<Path> pathStream = Files.walk(path)) {
            pathStream.sorted(Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.delete(p);
                } catch (Exception e) {
                    // ignore
                }
            });
        }
    }
}
