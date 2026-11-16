# Contribute to InkNest

[Tiếng Việt](../vi/contributing.md) · [Documentation](README.md)

Report issues, prepare focused pull requests and keep both documentation languages aligned.

## Report a bug or propose a feature

Use [Issues](https://github.com/2techy/inknest/issues). Bugs need app version/variant, device, Android/firmware, relevant KOReader version, reproduction steps and expected/actual results. Identify emulator versus physical results; include only necessary sanitized logs/screenshots. Features should describe the user problem and offline/E-Ink workflow.

Never upload private books, document URIs, source databases, signing keys or passwords. Use synthetic examples and redact personal paths. Report sensitive issues through [security reporting](../../SECURITY.md).

## Prepare a pull request

1. Follow [development](development.md), inspect existing code/tests and keep changes focused.
2. Preserve [architecture](architecture.md), [design rules](design-system.md), string resources and shared tokens. Use the version catalog for dependencies; never hand-edit generated schemas/profiles.
3. Test meaningful failures, cancellation, restoration, source isolation and retained snapshots. Follow existing tests; native Room/SAF behavior belongs in Android tests.
4. Run applicable [checks](development.md). Describe the problem, resulting behavior, checks actually run and limitations. UI changes need layout evidence; physical E-Ink acceptance is separate.
5. Review the diff for unrelated/generated/private data. Use Conventional Commits when committing.

Do not add network sync, telemetry, broad storage access or migrations incidentally. Schema changes require explicit upgrade review. Keep signing/build/measurement artifacts outside Git.

## Pull request checks

Follow the [CI rules and artifacts](development.md#github-actions). Keep applicable checks green and resolve conversations before squash merge; no additional approving reviewer is required. Emulator tests are manual; benchmarks/device acceptance are separate.

## Documentation and translations

Keep same-named `docs/en` and `docs/vi` pages aligned, using English as the technical reference. Preserve cross-language links, identifiers, commands, paths and units. Match UI labels to string resources; use natural, consistent Vietnamese.

Write the minimum needed to perform a task or understand a contract. Give each detail one primary home and link to it elsewhere. Verify relative links, anchors and commands after edits.

## License

Code/docs use [MIT](../../LICENSE). Preserve third-party notices and terms; signing certificates are separate from licensing.
