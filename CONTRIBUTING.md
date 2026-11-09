# Contribute to InkNest

[Tiếng Việt](docs/vi/contributing.md) · [Documentation](docs/en/README.md)

## Report a bug or propose a feature

Use the repository's issue tracker. For bugs include the app version/build variant, device model, Android and firmware versions, KOReader version when relevant, reproduction steps, expected behavior and actual behavior. Identify emulator versus physical-device results. Include small sanitized logs or screenshots only when needed.

Do not upload private books, document URIs, source databases, signing keys or passwords. Use synthetic examples and redact paths or screenshots containing personal information. For features describe the user problem, expected workflow and relevant offline/E-Ink constraints before proposing implementation details.

## Prepare a pull request

1. Follow [development setup](docs/en/development.md), inspect existing code/tests and keep the change focused.
2. Follow the [architecture](docs/en/architecture.md) and [design system](docs/en/design-system.md). Keep the single production `:app` module, manual DI and Repository → ViewModel → UI state → Route/Screen flow.
3. Use `.editorconfig`, string resources and shared theme/gesture tokens. Add dependencies through the grouped version catalog only when necessary. Do not hand-edit generated Room schemas or profiles.
4. Add meaningful tests for changed behavior, particularly failures, cancellation, restoration, source isolation and retained snapshots. Coroutine tests share a scheduler across injected dispatchers and fake clocks; native Room/SAF behavior belongs in Android tests.
5. Run the applicable [checks](docs/en/development.md). Describe the problem, resulting behavior, checks actually run and remaining limitations in the PR. Include layout evidence for UI changes and identify physical E-Ink acceptance separately.
6. Review the final diff for unrelated changes, private data and generated output. Use Conventional Commits when creating commits.

A contribution should preserve cancellation, report failed writes and retain successful data when expected reads fail. Do not add network sync, telemetry, broad production storage access or schema migrations incidentally. Stable cache upgrade policy must be decided explicitly before a stable release. Keep signing material and build/measurement artifacts outside version control.

## Pull request checks

Every PR into `master`, including drafts and documentation changes, runs the `build` check: formatting, debug/release JVM tests, debug/release lint and APK/test compilation. Keep the check green and resolve review conversations before squash merge. An additional approving reviewer is not required. Android emulator tests are dispatched manually; benchmark and physical-device acceptance stay separate. See [GitHub Actions](docs/en/development.md#github-actions) for artifacts and [security reporting](SECURITY.md) for sensitive issues.

## Documentation and translations

Each document in `docs/en` has a same-named counterpart in `docs/vi`. English is the technical reference; update both versions together and retain the cross-language link. Keep identifiers, commands, paths and units unchanged. Match UI labels to the corresponding Android string resources and use natural Vietnamese with consistent terminology.

Write task instructions, reference contracts or explanations for readers. Keep conversations, implementation progress, dated QA logs, handoffs and local artifact paths out of documentation. Document durable limits and repeatable checks instead. Verify relative links, anchors and command examples after moves or heading changes.

## License

InkNest code and documentation use the [MIT license](LICENSE). Keep third-party copyright and license notices intact; these components retain their own terms. APK signing certificates are separate from source licensing.
