# Maintenance Routine

If there are other open PRs for this work, update that PR instead of creating a new one.

0. Load the project's MCP tools before anything else. `AGENTS.md` names the MCP server:
   `sbt-mcp-<project>` for sbt projects, `javadocs` for Maven and Gradle projects. In Claude Code
   these tools are deferred, so load them with ToolSearch (search for the server name). They include
   the javadocs.dev tools such as `get_latest_version`, and for sbt also `sbt-task`. Use them for
   the rest of the run, and fall back to the build tool's launcher and `curl` only when they are
   unavailable. Say which one you used.
1. Get the zen-of-projects Skill. This repo has no `com.jamesward:skills` dependency (see
   `AGENTS.md`), so read the published Skill from its repository:

   ```bash
   git clone -q --depth 1 https://github.com/jamesward/skills /tmp/jamesward-skills || git -C /tmp/jamesward-skills pull -q
   ```

   If it fails, quote the exact error and stop.
2. Read `/tmp/jamesward-skills/skills/zen-of-projects/SKILL.md` and follow its "Maintenance Routine" section, using
   `AGENTS.md` for this project's commands and documented exceptions. Skip any step about the
   Skills dependency or extracting Skills. While an unreleased version of the Skill is being
   tested, `.factory/skills/zen-of-projects/SKILL.md` exists. Read that file instead, and don't
   delete it.
