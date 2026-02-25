# SkillsJars Maven Plugin Spec

SkillsJars are AI Agent Skills on Maven Central (https://www.skillsjars.com/)

Users can add skills as plugin dependencies in their Maven build config (`pom.xml`) like:
```
<plugin>
    <groupId>com.skillsjars</groupId>
    <artifactId>maven-plugin</artifactId>
    <version>0.0.3</version>
    <dependencies>
        <dependency>
            <groupId>com.skillsjars</groupId>
            <artifactId>anthropics__skills__pdf</artifactId>
            <version>2026_02_06-1ed29a0</version>
        </dependency>
    </dependencies>
</plugin>
```

SkillsJars can also be added as project-level dependencies:
```
<dependency>
    <groupId>com.skillsjars</groupId>
    <artifactId>anthropics__skills__pdf</artifactId>
    <version>2026_02_06-1ed29a0</version>
</dependency>
```

The plugin extracts SkillsJars contents to a directory where AI agents can pick them up. For example, the Kiro CLI AI agent picks up skills from the `.kiro/skills` directory.

There is a Maven task `skillsjars:extract` which does the extraction. The `skillsjars:extract` task is standalone only and is not bound to a Maven lifecycle phase.

The parameter for the extract directory is `dir` so users run something like `mvn skillsjars:extract -Ddir=.kiro/skills`. If the user doesn't provide a directory, the plugin fails with an error.

The plugin collects all `com.skillsjars` group dependencies from both project dependencies and plugin dependencies. SkillsJars can have transitive dependencies so the plugin looks in the classpath for all dependencies in the `com.skillsjars` group.

The structure inside SkillsJars is a path like `META-INF/skills/ORG/REPO/SKILL` or `META-INF/resources/skills/ORG/REPO/SKILL`. Only files under a directory containing a `SKILL.md` marker are extracted. The extracted output drops the `META-INF/skills` (or `META-INF/resources/skills`) prefix and flattens the skill root path with `__` separators under a `skillsjars__` prefix in the user-specified directory. For example, `META-INF/skills/org/repo/skill/test.txt` extracts to `<dir>/skillsjars__org__repo__skill/test.txt`.

On extraction, each individual skill directory is deleted and re-extracted. Other files in the output directory are preserved.

Maven does not allow multiple versions of the same jar on the classpath.

On extract, if two SkillsJars have overlapping paths, then an error is thrown.
