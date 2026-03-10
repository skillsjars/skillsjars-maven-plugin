package com.skillsjars.maven;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@Mojo(name = "package", defaultPhase = LifecyclePhase.GENERATE_RESOURCES)
public class PackageMojo extends AbstractMojo {

    @Parameter(property = "skillsDir", defaultValue = "${project.basedir}/skills")
    private File skillsDir;

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    void setSkillsDir(File skillsDir) {
        this.skillsDir = skillsDir;
    }

    void setProject(MavenProject project) {
        this.project = project;
    }

    static final String PROPERTY_PREFIX = "skillsjars.skill.";

    @Override
    public void execute() throws MojoExecutionException {
        if (!skillsDir.exists() || !skillsDir.isDirectory()) {
            getLog().debug("Skills directory not found: " + skillsDir);
            return;
        }

        getLog().info("Packaging skills from: " + skillsDir);

        File[] skillDirs = skillsDir.listFiles(File::isDirectory);
        if (skillDirs == null || skillDirs.length == 0) {
            getLog().warn("No skill directories found in: " + skillsDir);
            return;
        }

        File outputDir = new File(project.getBuild().getOutputDirectory());
        outputDir.mkdirs();

        try {
            for (File skillDir : skillDirs) {
                File skillMarker = new File(skillDir, "SKILL.md");
                if (!skillMarker.exists()) {
                    getLog().warn("Skipping directory without SKILL.md: " + skillDir.getName());
                    continue;
                }

                validateAllowedToolsProperty(skillMarker);

                String skillPath = parseSkillPath(skillDir.getName());
                copySkill(skillDir, outputDir, skillPath);
            }
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to package skills", e);
        }

        getLog().info("Skills packaged to: " + outputDir);
    }

    void validateAllowedToolsProperty(File skillMdFile) throws MojoExecutionException {
        try {
            String content = new String(Files.readAllBytes(skillMdFile.toPath()));
            String frontmatter = extractFrontmatter(content);
            if (frontmatter == null) return;

            String skillName = extractFrontmatterValue(frontmatter, "name");
            String allowedTools = extractFrontmatterValue(frontmatter, "allowed-tools");
            if (allowedTools == null || skillName == null) return;

            String propertyName = PROPERTY_PREFIX + skillName + ".allowed-tools";
            Properties props = project.getProperties();
            String pomValue = props.getProperty(propertyName);

            if (pomValue == null) {
                throw new MojoExecutionException("SKILL.md for '" + skillName + "' has allowed-tools but POM is missing property '" + propertyName + "'. " +
                        "Add <" + propertyName + ">" + allowedTools + "</" + propertyName + "> to <properties> in your pom.xml.");
            } else if (!pomValue.equals(allowedTools)) {
                throw new MojoExecutionException("Property '" + propertyName + "' value '" + pomValue +
                        "' does not match SKILL.md allowed-tools '" + allowedTools + "'");
            }
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to read SKILL.md: " + skillMdFile, e);
        }
    }

    static String extractFrontmatter(String content) {
        String[] lines = content.split("\n", -1);
        if (lines.length == 0 || !lines[0].trim().equals("---")) return null;
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i < lines.length; i++) {
            if (lines[i].trim().equals("---")) return sb.toString();
            sb.append(lines[i]).append("\n");
        }
        return null;
    }

    static String extractFrontmatterValue(String frontmatter, String key) {
        Pattern pattern = Pattern.compile("^" + Pattern.quote(key) + ":\\s*(.+)$", Pattern.MULTILINE);
        Matcher matcher = pattern.matcher(frontmatter);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return null;
    }

    private String parseSkillPath(String skillName) {
        String scmUrl = project.getScm() != null ? project.getScm().getUrl() : null;
        
        if (scmUrl != null && scmUrl.contains("github.com/")) {
            String[] parts = scmUrl.split("github.com/")[1].split("/");
            if (parts.length >= 2) {
                String org = parts[0];
                String repo = parts[1].replace(".git", "");
                return String.format("META-INF/skills/%s/%s/%s/", org, repo, skillName);
            }
        }

        String groupId = project.getGroupId().replace('.', '/');
        return String.format("META-INF/skills/%s/%s/", groupId, skillName);
    }

    private void copySkill(File skillDir, File outputDir, String basePath) throws IOException {
        Path skillPath = skillDir.toPath();
        
        try (Stream<Path> paths = Files.walk(skillPath)) {
            paths.filter(Files::isRegularFile).forEach(file -> {
                try {
                    String relativePath = skillPath.relativize(file).toString().replace('\\', '/');
                    File targetFile = new File(outputDir, basePath + relativePath);
                    targetFile.getParentFile().mkdirs();
                    
                    Files.copy(file, targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    getLog().debug("Copied: " + basePath + relativePath);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }
}
