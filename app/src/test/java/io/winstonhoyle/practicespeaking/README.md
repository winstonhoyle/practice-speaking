# Tests

Everything here runs on the JVM with `./gradlew testDebugUnitTest`; Robolectric stands in for Android where a test needs it. The folders mirror the app's packages in the [source map](../../../../../main/java/io/winstonhoyle/practicespeaking/README.md); the [project README](../../../../../../../README.md#tests) has the commands CI runs.

| Folder | What it covers |
|---|---|
| `ai/` | [`TutorModelContractTest`](ai/TutorModelContractTest.kt), one shared suite every provider runs against MockWebServer (replies, refusals, errors, history replay, token usage, key check, model list), plus each provider's own request details. `FakeTutorModel` is the scripted tutor other tests use. |
| `prompt/` | The Russian prompt against a byte-for-byte snapshot ([`russian_tutor_system_prompt.md`](../../../../resources/russian_tutor_system_prompt.md)), every language and level, the reply parser, and stress marks. |
| `data/` | Room and full-text search, settings storage, and voice speeds. |
| `speech/` | Hold-to-talk against Robolectric's speech recognizer. |
| `error/` | Every error code is unique and has a message and an action. |
| `ui/` | Each ViewModel with real repositories and a fake tutor, the components, and the screenshot renders. |
| `live/` | Real API calls. They run only with `-Plive` and a key in `.env`, and skip otherwise. |

Shared helpers:

- `TestApp.kt` builds real repositories on an in-memory database and a temporary DataStore, with a fake tutor, a fake speaker, and `await`, which polls a flow until a condition holds.
- `data/FakeCipher.kt` replaces the Keystore cipher.

Screenshots: `./gradlew testDebugUnitTest -Pscreenshots --tests '*ScreensScreenshotTest*' --tests '*GalleryScreenshotTest*'` renders the README and DESIGN.md images into `docs/images/`.
