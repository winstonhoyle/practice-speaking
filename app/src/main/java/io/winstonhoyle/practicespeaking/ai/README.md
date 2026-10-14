# ai/

Part of the [source map](../README.md). The boundary between the app and the AI providers. Code outside this package uses `TutorModel`, `Provider`, and plain strings; SDK types stay in here.

| File | Role |
|---|---|
| `TutorModel.kt` | The interface: `reply()`, `testKey()`, and `listModels()`, plus `Turn`, `TutorReply`, and `TokenUsage`. |
| `Provider.kt` | The three providers: display name, default model, key hint, key page, and logo. |
| `TutorModelFactory.kt` | The one place that picks a vendor class for a provider. |
| `AnthropicTutorModel.kt` | Claude through the official Java SDK. Caches the prompt and the history, and sends low effort only to models whose metadata says they support it. |
| `OpenAiTutorModel.kt` | The Responses API through the official Java SDK, stateless, replaying saved output items. |
| `GeminiTutorModel.kt` | Gemini's REST API over OkHttp and Jackson. |
| `ProviderSupport.kt` | `providerCall`, which runs a call off the main thread and turns any failure into a `TutorError`, plus HTTP status mapping and `SdkJson`. |
| `CountingTutorModel.kt` | A wrapper that adds each reply's tokens to the totals in Settings. `AppContainer` wraps every model in it. |
| `UsageLog.kt` | Builds `TokenUsage` and logs it in debug builds. |

Each assistant turn keeps the provider's raw response, so the next request replays it exactly, reasoning included. History is never trimmed or edited.

Adding a provider is one `TutorModel` class, one `Provider` entry, one branch in `TutorModelFactory`, and one subclass of [`TutorModelContractTest`](../../../../../../test/java/io/winstonhoyle/practicespeaking/ai/TutorModelContractTest.kt) in the tests. The [project README](../../../../../../../../README.md#add-a-provider) lists the steps.
