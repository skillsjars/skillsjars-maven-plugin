package com.skillsjars.maven;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;

@Mojo(name = "extract", requiresDependencyResolution = ResolutionScope.TEST)
public class ExtractMojo extends AbstractMojo {

    @Parameter(property = "dir")
    private String dir;

    @Parameter
    private List<String> scopes;

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    // Package-private setters for testing
    void setDir(String dir) {
        this.dir = dir;
    }

    void setScopes(List<String> scopes) {
        this.scopes = scopes;
    }

    void setProject(MavenProject project) {
        this.project = project;
    }

    private static final String SKILLSJARS_GROUP = "com.skillsjars";
    private static final String[] SKILLS_PREFIXES = {
        "META-INF/skills/",
        "META-INF/resources/skills/"
    };
    private static final String OUTPUT_SUBDIR = "skillsjars";

    @Override
    public void execute() throws MojoExecutionException {
        if (dir == null || dir.trim().isEmpty()) {
            throw new MojoExecutionException("The 'dir' parameter is required. Use -Ddir=<path>");
        }

        Set<String> allowedScopes = getAllowedScopes();
        getLog().info(String.format("Extracting SkillsJars to: %s", dir));
        getLog().info("Using scopes: " + allowedScopes);

        Path outputPath = Paths.get(dir);
        
        try {
            Files.createDirectories(outputPath);
        } catch (IOException e) {
            throw new MojoExecutionException(String.format("Failed to prepare output directory: %s", outputPath), e);
        }

        Set<Artifact> skillsJars = findSkillsJars(allowedScopes);
        getLog().info(String.format("Found %d SkillsJar(s)", skillsJars.size()));

        Map<String, String> extractedPaths = new HashMap<>();
        
        for (Artifact artifact : skillsJars) {
            try {
                extractSkillsJar(artifact, outputPath, extractedPaths);
            } catch (IOException e) {
                throw new MojoExecutionException(String.format("Failed to extract: %s", artifact), e);
            }
        }

        getLog().info("Successfully extracted SkillsJars");
    }

    private Set<String> getAllowedScopes() {
        if (scopes == null || scopes.isEmpty()) {
            return new HashSet<>(Arrays.asList(
                Artifact.SCOPE_COMPILE,
                Artifact.SCOPE_PROVIDED,
                Artifact.SCOPE_RUNTIME,
                Artifact.SCOPE_TEST,
                Artifact.SCOPE_SYSTEM
            ));
        }
        return new HashSet<>(scopes);
    }

    private Set<Artifact> findSkillsJars(Set<String> allowedScopes) {
        Set<Artifact> result = new HashSet<>();
        Set<Artifact> artifacts = project.getArtifacts();
        
        for (Artifact artifact : artifacts) {
            if (SKILLSJARS_GROUP.equals(artifact.getGroupId()) && 
                allowedScopes.contains(artifact.getScope())) {
                result.add(artifact);
            }
        }
        
        return result;
    }

    private static String matchSkillsPrefix(String entryName) {
        for (String prefix : SKILLS_PREFIXES) {
            if (entryName.startsWith(prefix)) {
                return prefix;
            }
        }
        return null;
    }

    private void extractSkillsJar(Artifact artifact, Path outputPath, Map<String, String> extractedPaths)
            throws IOException, MojoExecutionException {
        File jarFile = artifact.getFile();
        if (jarFile == null || !jarFile.exists()) {
            getLog().warn(String.format("Artifact file not found: %s", artifact));
            return;
        }

        getLog().info("Extracting: " + artifact);

        // First pass: find all SKILL.md files to identify skill roots
        Map<String, String> skillRoots = identifySkillRoots(jarFile);

        // Second pass: extract files using the skill roots
        extractFiles(artifact, outputPath, extractedPaths, jarFile, skillRoots);
    }

    @NonNullDecl
    private static Map<String, String> identifySkillRoots(File jarFile) throws IOException {
        Map<String, String> skillRoots = new HashMap<>();
        try (JarFile jar = new JarFile(jarFile)) {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String entryName = entry.getName();
                String prefix = matchSkillsPrefix(entryName);

                if (prefix != null && entryName.endsWith("/SKILL.md")) {
                    String relativePath = entryName.substring(prefix.length());
                    String skillRoot = relativePath.substring(0, relativePath.length() - "/SKILL.md".length());
                    String flattenedRoot = skillRoot.replace("/", "__");
                    skillRoots.put(skillRoot + "/", flattenedRoot);
                }
            }
        }
        return skillRoots;
    }

    private void extractFiles(Artifact artifact, Path outputPath, Map<String, String> extractedPaths, File jarFile, Map<String, String> skillRoots) throws IOException, MojoExecutionException {
        try (JarFile jar = new JarFile(jarFile)) {
            Enumeration<JarEntry> entries = jar.entries();
            Set<String> deletedSkillDirs = new HashSet<>();

            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String entryName = entry.getName();
                String prefix = matchSkillsPrefix(entryName);

                if (prefix == null) {
                    continue;
                }

                if (entry.isDirectory()) {
                    continue;
                }

                String relativePath = entryName.substring(prefix.length());
                
                // Find the skill root for this file
                String skillRoot = null;
                String flattenedRoot = null;
                for (Map.Entry<String, String> root : skillRoots.entrySet()) {
                    if (relativePath.startsWith(root.getKey())) {
                        skillRoot = root.getKey();
                        flattenedRoot = root.getValue();
                        break;
                    }
                }
                
                if (skillRoot == null) {
                    getLog().warn("Skipping file not under a SKILL.md root: " + relativePath);
                    continue;
                }
                
                // Build target path: skillsjars__{flattenedRoot}/{remainder}
                String remainder = relativePath.substring(skillRoot.length());
                String skillDirName = String.format("skillsjars__%s", flattenedRoot);
                Path skillDir = outputPath.resolve(skillDirName);
                Path targetPath = skillDir.resolve(remainder);

                // Delete the skill directory before first extraction
                if (!deletedSkillDirs.contains(skillDirName)) {
                    deleteDirectory(skillDir);
                    deletedSkillDirs.add(skillDirName);
                }

                String conflictKey = skillDirName + "/" + remainder;
                if (extractedPaths.containsKey(conflictKey)) {
                    throw new MojoExecutionException(
                            String.format("Path conflict detected: %s exists in both %s and %s", conflictKey, extractedPaths.get(conflictKey), artifact)
                    );
                }
                
                extractedPaths.put(conflictKey, artifact.toString());
                
                Files.createDirectories(targetPath.getParent());
                
                try (InputStream is = jar.getInputStream(entry)) {
                    Files.copy(is, targetPath);
                }

                getLog().debug(String.format("Extracted: %s", conflictKey));
            }
        }
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
                        } catch (IOException e) {
                            getLog().warn(String.format("Failed to delete: %s", p));
                        }
                    });
        }
    }
}
