# SkillsJars Maven Plugin

Maven plugin to extract SkillsJars from dependencies to a directory for AI agents.

## Usage

1. Find Agent SkillsJars on [SkillsJars.com](https://skillsjars.com/)
2. Add SkillsJars dependencies to your project
3. Add the plugin:
    ```xml
    <build>
        <plugins>
            <plugin>
                <groupId>com.skillsjars</groupId>
                <artifactId>maven-plugin</artifactId>
                <version>0.0.2</version>
            </plugin>
        </plugins>
    </build>
    ```
4. Extract SkillsJars to the directory your AI uses, like: 
    ```bash
    mvn skillsjars:extract -Ddir=.kiro/skills
    ```

## Configuration

### Scopes

By default, the plugin extracts SkillsJars from all dependency scopes. You can configure specific scopes:

```xml
<plugin>
    <groupId>com.skillsjars</groupId>
    <artifactId>maven-plugin</artifactId>
    <version>0.0.2</version>
    <configuration>
        <scopes>
            <scope>compile</scope>
            <scope>runtime</scope>
        </scopes>
    </configuration>
</plugin>
```
