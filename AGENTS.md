# Repository Guidelines

## Project Structure & Module Organization
This repository is a Gradle-based Java project (`settings.gradle`, `build.gradle`) targeting JDK 8.

- `src/main/java/org/zlab/ocov/...`: runtime format-coverage tracker and invariant logic.
- `src/main/java/org/zlab/net/...`: network trace and diff utilities.
- `src/test/java/...`: JUnit 5 tests, generally mirroring production packages.
- `input/`: JSON inputs used by runtime/invariant workflows.
- `scripts/`: small local helper scripts (for example, `scripts/rm.sh` clears `/tmp/coverage.log`).

Prefer adding new code under existing package roots rather than creating parallel top-level namespaces.

## Build, Test, and Development Commands
Use the Gradle wrapper so versions stay consistent.

- `./gradlew clean build`: compile and run all tests.
- `./gradlew test`: run unit tests only.
- `./gradlew fatJar`: build `build/libs/ssgFatJar.jar` for embedding into target systems.
- `./gradlew shadowJar`: build `build/libs/ocov-shadow.jar`.
- `./gradlew spotlessCheck`: verify Java formatting.
- `./gradlew spotlessApply`: auto-format Java sources.

## Coding Style & Naming Conventions
Formatting is enforced with Spotless using Eclipse Google-style config (`.settings/eclipse-java-google-style.xml`).

- Use 4-space indentation and keep formatting tool-compatible.
- Classes: `PascalCase` (e.g., `ObjectGraphCoverage`).
- Methods/fields: `camelCase`.
- Constants: `UPPER_SNAKE_CASE`.
- Packages: lowercase dot notation (e.g., `org.zlab.ocov.tracker`).

## Testing Guidelines
Tests run on JUnit Jupiter (`useJUnitPlatform()` in `build.gradle`).

- Place tests in `src/test/java` with package parity to source.
- Prefer naming patterns already present: `TestXxx` (and keep existing `XxxTest` files unchanged).
- Keep tests deterministic and local; avoid long-running loops or network-only debug flows in default test runs.
- Run `./gradlew test` before opening a PR.

## Commit & Pull Request Guidelines
Recent history favors short, imperative, lower-case subjects (e.g., `fix bug`, `add test`, `update log format`).

- Commit format: `<verb> <scope>` with one logical change per commit.
- PRs should include: purpose, key code paths touched, and test evidence (`./gradlew test` result).
- Link related issues/tasks and call out runtime-impacting config changes (especially environment variables).

## Runtime Configuration Tips
Runtime collection is controlled by environment variables, notably:

- `ENABLE_FORMAT_COVERAGE=true` to enable coverage.
- `ENABLE_FORMAT_COVERAGE_SAMPLE` and `FORMAT_COVERAGE_SAMPLE_RATE` for sampling.

Default logs/config paths reference `/tmp`; keep that in mind for local and CI environments.
