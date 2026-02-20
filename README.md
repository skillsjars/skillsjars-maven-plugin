# SkillsJars Maven Plugin

Maven plugin to extract SkillsJars from dependencies to a directory for AI agents.

## Usage

1. Find Agent SkillsJars on [SkillsJars.com](https://skillsjars.com/)
1. Add SkillsJars dependencies to your project
1. Add the plugin:
    ```xml
    <build>
        <plugins>
            <plugin>
                <groupId>com.skillsjars</groupId>
                <artifactId>skillsjars-maven-plugin</artifactId>
                <version>1.0.0-SNAPSHOT</version>
            </plugin>
        </plugins>
    </build>
    ```
1. Extract SkillsJars to the directory your AI uses, like: 
    ```bash
    mvn skillsjars:extract -Ddir=.kiro/skills
    ```

## Configuration

### Scopes

By default, the plugin extracts SkillsJars from all dependency scopes. You can configure specific scopes:

```xml
<plugin>
    <groupId>com.skillsjars</groupId>
    <artifactId>skillsjars-maven-plugin</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <configuration>
        <scopes>
            <scope>compile</scope>
            <scope>runtime</scope>
        </scopes>
    </configuration>
</plugin>
```
