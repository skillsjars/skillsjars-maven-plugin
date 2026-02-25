# SkillsJars Maven Plugin

Maven plugin to extract SkillsJars from dependencies to a directory for AI agents.

## Usage

1. Find Agent SkillsJars on [SkillsJars.com](https://skillsjars.com/)
2. Add the plugin and SkillsJar dependencies:
    ```xml
    <build>
        <plugins>
            <plugin>
                <groupId>com.skillsjars</groupId>
                <artifactId>maven-plugin</artifactId>
                <version>0.0.3</version>
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
3. Extract SkillsJars to the directory your AI uses, like:
    ```bash
    mvn skillsjars:extract -Ddir=.kiro/skills
    ```
