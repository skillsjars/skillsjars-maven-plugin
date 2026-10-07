# AGENTS.md: skillsjars-maven-plugin

Maven plugin `com.skillsjars:maven-plugin` (`skillsjars:extract`) that extracts SkillsJars (Agent Skills packaged as Maven artifacts) for AI agents.

Follow the `zen-of-projects` Skill; this file records only project-specific facts and exceptions.

## MCP

`javadocs` (https://www.javadocs.dev/mcp), configured in `.mcp.json` / `.kiro/settings/mcp.json` and approved in `.claude/settings.json`. Use its `get_latest_version` for version lookups and its source/doc tools for API questions. In Claude Code its tools are deferred: load them with ToolSearch (search `javadocs`).

## Build & test

- Full validation: `./mvnw -B -ntp verify` (`mvn clean test` in CI).

## Maintenance routine

`.factory/MAINTENANCE.md` (weekly), following the `zen-of-projects` Skill.

## Exceptions to zen-of-projects

- **No `com.jamesward:skills` dependency:** it is the SkillsJars Maven plugin itself, and its `example/` build demonstrates third-party Skills. `.factory/MAINTENANCE.md` reads the published Skill from a clone of https://github.com/jamesward/skills instead. Don't add the dependency.
