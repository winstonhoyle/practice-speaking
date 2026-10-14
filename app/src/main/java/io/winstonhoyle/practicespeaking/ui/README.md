# ui/

Part of the [source map](../README.md). [docs/DESIGN.md](../../../../../../../../docs/DESIGN.md) covers how the screens look and lists every component.

| Folder | What's in it |
|---|---|
| `conversation/` | The practice screen: mic, chat, commands, end-of-session summary. |
| `history/` | Past sessions, swipe to delete, and full-text search. |
| `session/` | One saved session: read, translate, hear, or continue it. |
| `settings/` | Provider, key, model, language, level, voice speed, theme, and token totals. |
| `components/` | The component library every screen is built from, listed in [DESIGN.md](../../../../../../../../docs/DESIGN.md#components-uicomponents). |
| `theme/` | Colors, type, shapes, spacing, and sizes. The only place literal colors and sizes appear. |

Each screen folder has two files. The `…ViewModel` holds a `StateFlow` of UI state and plain functions for events. The `…Screen` file holds a `…Route`, which connects the ViewModel and Android APIs (mic, permissions, lifecycle), and a stateless `…Screen` that tests and screenshots call directly.

Shared pieces at this level:

| File | Role |
|---|---|
| `AppNavHost.kt` | The routes, and which ViewModel each screen gets from `AppContainer`. |
| `Errors.kt` | Each `AppError` mapped to a message and one action, shown by `AppErrorBanner`. |
| `Translations.kt` | The translate button's logic (`TranslationToggles`), used by the conversation and saved-session screens. |
| `DatabaseFlows.kt` | `catchDatabase`, which turns a failed database read into an error state instead of a crash. |
| `Format.kt` | Date and number formatting. |
