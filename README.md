# SkillsJars Maven Plugin

Maven plugin to extract and package SkillsJars for AI agents.

## Extracting SkillsJars

Extract SkillsJars from dependencies to a directory for AI agents.

1. Find Agent SkillsJars on [SkillsJars.com](https://skillsjars.com/)
2. Add the plugin and SkillsJar dependencies:
    ```xml
    <build>
        <plugins>
            <plugin>
                <groupId>com.skillsjars</groupId>
                <artifactId>maven-plugin</artifactId>
                <version>0.0.4</version>
                <dependencies>
                    <!-- Your SkillsJars -->
                    <dependency>
                        <groupId>com.skillsjars</groupId>
                        <artifactId>SKILLJAR_ARTIFACT_ID</artifactId>
                        <version>SKILLJAR_VERSION</version>
                    </dependency>
                </dependencies>
            </plugin>
        </plugins>
    </build>
    ```
3. Extract SkillsJars to the directory your AI uses:
    ```bash
    mvn skillsjars:extract -Ddir=.kiro/skills
    ```

## Packaging SkillsJars

Create SkillsJars from your project's skills directory.

1. Create a `skills` directory in your project root
2. Add skill subdirectories, each containing a `SKILL.md` marker file
3. Add the plugin to your build:
    ```xml
    <build>
        <plugins>
            <plugin>
                <groupId>com.skillsjars</groupId>
                <artifactId>maven-plugin</artifactId>
                <version>0.0.4</version>
                <executions>
                    <execution>
                        <goals>
                            <goal>package</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
    ```
4. Package your skills:
    ```bash
    mvn package
    ```

The plugin packages skills into `META-INF/skills/org/repo/skill` following SkillsJar conventions. If your project has GitHub SCM configured, it uses the org/repo from the URL. Otherwise, it uses the project's groupId.

You can customize the skills directory location:
```xml
<plugin>
    <groupId>com.skillsjars</groupId>
    <artifactId>maven-plugin</artifactId>
    <version>0.0.4</version>
    <executions>
        <execution>
            <goals>
                <goal>package</goal>
            </goals>
        </execution>
    </executions>
    <configuration>
        <skillsDir>${project.basedir}/my-skills</skillsDir>
    </configuration>
</plugin>
```
