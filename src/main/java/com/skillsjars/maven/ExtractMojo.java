package com.skillsjars.maven;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

@Mojo(name = "extract", requiresDependencyResolution = ResolutionScope.TEST)
public class ExtractMojo extends AbstractMojo {

    @Parameter(property = "dir")
    private String dir;

    @Parameter
    private List<String> scopes;

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    @Parameter(defaultValue = "${mojoExecution}", readonly = true)
    private MojoExecution mojoExecution;

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

    void setMojoExecution(MojoExecution mojoExecution) {
        this.mojoExecution = mojoExecution;
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
        getLog().info("Extracting SkillsJars to: " + dir);
        getLog().info("Using scopes: " + allowedScopes);

        Path outputPath = Paths.get(dir);
        
        try {
            deleteDirectory(outputPath);
            Files.createDirectories(outputPath);
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to prepare output directory: " + outputPath, e);
        }

        Set<Artifact> skillsJars = findSkillsJars(allowedScopes);
        getLog().info("Found " + skillsJars.size() + " SkillsJar(s)");

        Map<String, String> extractedPaths = new HashMap<>();
        
        for (Artifact artifact : skillsJars) {
            try {
                extractSkillsJar(artifact, outputPath, extractedPaths);
            } catch (IOException e) {
                throw new MojoExecutionException("Failed to extract: " + artifact, e);
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

        // Collect from project dependencies (compile, provided, runtime, test, system)
        Set<Artifact> projectArtifacts = project.getArtifacts();
        for (Artifact artifact : projectArtifacts) {
            if (SKILLSJARS_GROUP.equals(artifact.getGroupId()) &&
                allowedScopes.contains(artifact.getScope())) {
                result.add(artifact);
            }
        }

        // Collect from plugin dependencies (<plugin><dependencies>...</dependencies></plugin>)
        if (mojoExecution != null) {
            var mojoDescriptor = mojoExecution.getMojoDescriptor();
            if (mojoDescriptor != null) {
                var pluginDescriptor = mojoDescriptor.getPluginDescriptor();
                if (pluginDescriptor != null && pluginDescriptor.getArtifacts() != null) {
                    for (Artifact artifact : pluginDescriptor.getArtifacts()) {
                        if (SKILLSJARS_GROUP.equals(artifact.getGroupId())) {
                            result.add(artifact);
                        }
                    }
                }
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
            getLog().warn("Artifact file not found: " + artifact);
            return;
        }

        getLog().info("Extracting: " + artifact);

        // First pass: find all SKILL.md files to identify skill roots
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

        // Second pass: extract files using the skill roots
        try (JarFile jar = new JarFile(jarFile)) {
            Enumeration<JarEntry> entries = jar.entries();

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
                Path targetPath = outputPath.resolve("skillsjars__" + flattenedRoot).resolve(remainder);
                
                String conflictKey = "skillsjars__" + flattenedRoot + "/" + remainder;
                if (extractedPaths.containsKey(conflictKey)) {
                    throw new MojoExecutionException(
                        "Path conflict detected: " + conflictKey + 
                        " exists in both " + extractedPaths.get(conflictKey) + 
                        " and " + artifact
                    );
                }
                
                extractedPaths.put(conflictKey, artifact.toString());
                
                Files.createDirectories(targetPath.getParent());
                
                try (InputStream is = jar.getInputStream(entry)) {
                    Files.copy(is, targetPath);
                }
                
                getLog().debug("Extracted: " + conflictKey);
            }
        }
    }

    private void deleteDirectory(Path path) throws IOException {
        if (!Files.exists(path)) {
            return;
        }
        
        Files.walk(path)
            .sorted(Comparator.reverseOrder())
            .forEach(p -> {
                try {
                    Files.delete(p);
                } catch (IOException e) {
                    getLog().warn("Failed to delete: " + p);
                }
            });
    }
}
