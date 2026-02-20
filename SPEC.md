# SkillsJars Maven Plugin Spec

SkillsJars are AI Agent Skills on Maven Central (https://www.skillsjars.com/)

Users can add these skills to their Maven build config (`pom.xml`) like:
```
<dependency>
    <groupId>com.skillsjars</groupId>
    <artifactId>anthropics__skills__pdf</artifactId>
    <version>2026_02_06-1ed29a0</version>
</dependency>
```

We need a Maven Plugin that enables users to extract the SkillsJars contents to a directory where AI agents can pick them up. For example, the Kiro CLI AI agent picks up skills from the `.kiro/skills` directory.

There should be a Maven task `skillsjars:extract` which does the extraction.

Users should be able to specify the directory to extract to, ideally as a parameter when they run the extract task.

If the user doesn't provide a directory, fail with an error.

SkillsJars can have transitive dependencies so the extract task will need to look in the classpath for all dependencies in the `com.skillsjars` group.

By default, the plugin will extract dependencies from all Maven dependency scopes but this should be configurable in the plugin settings under a `<scopes>` array parameter like `<scopes><scope>compile</scope><scope>test</scope></scopes>`.

The structure inside the SkillsJars is a path like `META-INF/resources/skills/ORG/REPO/SKILL` and the extracted contents should drop the `META-INF/resources/skills` prefix but add a subdir of `skillsjars` inside the user specified dir.  Everything in the `META-INF/resources/skills` directory should be extracted.

Maven does not allow mutliple versions of the same jar on the classpath.

The `skillsjars:extract` task should be standalone only. Do not bind it to a Maven lifecycle phase.

The extraction should match the contents of the SkillsJars so the easiest approach might be to delete the `user-specified/skillsjars` dir and re-extract everything when the user runs the task.

On extract, if two SkillsJars have overlapping paths, then an error should be thrown.

The parameter for the extract directory should be `dir` so users will run something like `mvn skillsjars:extract -Ddir=.kiro/skills`
