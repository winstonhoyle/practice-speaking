# Source map

The app is one module. Each package has one job, and only `ai/` knows which AI vendors exist. The [project README](../../../../../../../README.md) covers what the app does and how to build it; the [tests](../../../../../test/java/io/winstonhoyle/practicespeaking/README.md) mirror these packages.

| Package | What's in it |
|---|---|
| `ai/` | The `TutorModel` interface, one class per provider, and token counting. See [ai/README.md](ai/README.md). |
| `prompt/` | The tutor prompt (`PromptRenderer` fills [`assets/tutor_prompt_template.md`](../../../../assets/tutor_prompt_template.md)), the five languages, the CEFR levels, the reply parser, and Russian stress marks. Only loading the template touches Android. |
| `data/db/` | Room: sessions, messages, a full-text index over them, and `SessionRepository`. |
| `data/settings/` | DataStore settings: provider, keys (encrypted with the Android Keystore), models, language, level, voice speed, theme, and token totals. |
| `data/` | `MessageTranslator`, the one-message translation request. |
| `speech/` | Hold-to-talk on `SpeechRecognizer` (`SpeechInput`) and speech output on `TextToSpeech` (`SpeechOutput`, behind the `Speaker` interface). |
| `error/` | `AppError`: every failure the learner can see, each with a stable code. |
| `ui/` | Screens, ViewModels, the component library, and the theme. See [ui/README.md](ui/README.md). |
| root | `AppContainer` (manual dependency injection), `MainActivity`, `PracticeSpeakingApp`, and `enumByName`. |

## One conversation turn

1. `ui/conversation/ConversationScreen.kt`: the learner holds the mic. `speech/SpeechInput.kt` keeps listening through pauses and returns the whole sentence on release.
2. `ui/conversation/ConversationViewModel.kt`: `send()` saves the message through `data/db/SessionRepository.kt` and calls `TutorModel.reply()` with the prompt from `prompt/PromptRenderer.kt` and every earlier turn.
3. `ai/`: the provider's class makes the request and returns the text, the raw response (replayed next turn), and the token counts. `CountingTutorModel` adds the counts to the totals in Settings.
4. `prompt/TutorResponseParser.kt`: splits the reply into the spoken text, the `CORRECTION:` line, the `STRESSED:` line, and a `SUMMARY` block.
5. The ViewModel saves the reply and the correction, then `speech/SpeechOutput.kt` reads the text aloud without stress marks.
6. On failure, the `AppError` goes to `ui/Errors.kt`, which picks the banner's message and action.
