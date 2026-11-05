# Universal Build and Materialization Service Plan

This is the agreed plan, temporarily kept in `pure-java` while the work proceeds.

## Working agreement

- Use the remote review flow: one small, coherent increment in a draft PR, then wait for feedback before continuing.
- Keep temporary-review PRs titled `tmp` with an empty description; amend the current temporary commit for corrections.
- Run the relevant full build for each implementation increment. Get an independent review before the final commit and formal PR.
- Leave all READMEs untouched during implementation. Write the final documentation once the implementation settles.
- Apply the simplification protocol to each deliverable: remove unnecessary structure, duplication, and content without sacrificing correctness or clarity.

## 1. Extract Universal Build

- Create a dedicated `universal-build` repository with one maintained implementation and its tests.
- Keep the implementation in shell, structured and tested as a software application.
- Expose UB as a Nix package from its GitHub repository. GitHub Packages is not required.
- Keep a tiny executable `./universal-build` launcher in each consuming repository. It delegates to the shared package through Nix and forwards arguments.
- Keep project-specific configuration in each project's flake, with the shared UB dependency recorded in its flake lockfile.
- Pass the consuming project's directory explicitly. The installed UB package's directory is not the project root.

The user-facing commands remain:

```sh
./universal-build build
./universal-build shell
./universal-build shell npm run dev
```

## 2. Redesign Universal Build's structure

Use the separation of responsibilities demonstrated by the `pure-java` source as the quality reference, adapted appropriately to shell.

- Make the lifecycle explicit: load configuration, validate configuration, then execute.
- Configuration loading must be clearly named. Validation must not secretly initialize configuration.
- Keep command parsing and dispatch thin, with clear function inputs and responsibilities.
- Isolate Nix, process execution, and filesystem operations.
- Separate build preparation, idiomatic compilation/checks, release assembly, and archiving.
- Use consistent failure handling and useful error messages.
- Review the complete flow before extending it. Do not add new release types by scattering conditionals through the existing script.
- Prefer simple, maintainable code over unnecessary frameworks or abstractions.

## 3. Add Universal Build capabilities

Support these release types through a consistent design:

| Type | Release contents |
| --- | --- |
| `static` | Deployable files |
| `executable` | Program artifacts, runtime dependencies, and the `ignite` launcher; no Dockerfile |
| `container` | Executable packaging plus a Dockerfile |

Add both shell modes:

- `shell`: open an interactive shell in the project's build environment.
- `shell <command> [args...]`: run the given command in that environment.

Verify observable behavior across all release types and both shell modes, including release contents, argument preservation, working directories, and failure/exit behavior.

## 4. Migrate the Pure repositories

- Replace duplicated UB implementations with the shared-package launcher.
- Keep shared behavior consistent across the Pure templates and consuming projects.
- Verify the relevant complete builds and React development commands.

## 5. Create the central materialization service

- Rename and repurpose `global-materialization-workflows` as `universal-materialization-service`.
- Reuse that existing package; no additional materialization-service package is needed. The dedicated UB repository is separate tooling.
- Centralize ignition and pipeline orchestration so each pipeline repository does not need its own bootstrap workflow.
- Update repository references, workflows, and actions for the service model.
- Keep each package's CI independent: build and test that package.
- Do not introduce dry-run-specific pipeline orchestration.
- Keep materializer repositories responsible for their definitions and state.

## 6. Implement the service

- Replace the dummy Java implementation.
- Use one thin Java `Main` with CLI subcommands, delegating to separate implementation classes.
- Have GitHub Actions invoke `ignite <command> ...` directly; Docker is not involved in executing the action.
- Use UB's `executable` release type and remove the service's Dockerfile.
- Implement the required materialization, bounce, source-selection, and dispatch behavior.

## 7. Finish documentation

Write the final documentation after the architecture and implementation settle, covering the shared UB tool, consuming-project setup, and materialization service.

## Stretch goal: simple configuration with managed Nix internals

First centralize UB with the current project flakes and behavior. Complete the structural refactor and add `executable` and `shell` through separate reviewable increments. The following abstraction is a later goal, not a prerequisite for centralization:

- Introduce `universal-build.json` as the user-facing configuration for build tools, commands, release type, runtime requirements, environment variables, and IDE paths.
- Keep the schema small and strictly validated. Maintain the shared Nix implementation once in the UB repository.
- Have UB manage the internal `flake.nix` and `flake.lock` under `.universal-build/`; users should not need to author or maintain either file.
- Keep managed configuration and lockfiles version-controlled. Ignore generated build output and disposable caches within the hidden directory.
- Retain the convenient `./universal-build` launcher and introduce the JSON configuration and hidden-file layout incrementally.

## Stretch goal: hide routine idiomatic build-tool setup

After the shared configuration foundation, introduce language presets that let users configure standard projects through `universal-build.json` without manually wiring Gradle, npm, or other idiomatic build tools.

- Provide consistent commands for common tasks such as building, testing, and development, delegating to the appropriate native tools.
- Manage routine native-tool configuration and integration behind the scenes, with customization available for project-specific needs.
- Reuse existing capabilities: Nix supplies environments, native tools compile and test, and OCI provides the container distribution format.
- Own the conventions, integration, and user experience. Do not reimplement the underlying tools or duplicate their entire configuration systems.
- Capture the integration work once so every project benefits without having to rediscover and maintain the same wiring.

## Existing checkpoints

Completed before this plan was saved:

- Removed Bazel from the global materialization repository and aligned its build foundation with `pure-java`.
- Replaced the old Java implementation with an explicit dummy stub.
- Removed dry-run-specific workflows and action behavior.
- Removed the READMEs from both materializer repositories.
- Merged those changes and preserved the old implementations with archive tags and backup bundles.

The initial UB refactor remains an unfinished draft in [pure-java PR #19](https://github.com/nader-najjar/pure-java/pull/19). It has not been merged and does not complete the redesign above.

## Separately discussed possibility

Publishing finished OCI images to GitHub Container Registry or Amazon ECR would let servers or devices pull and run releases independently of deployment orchestration. This is a possible future distribution step, not part of the agreed implementation scope above.
