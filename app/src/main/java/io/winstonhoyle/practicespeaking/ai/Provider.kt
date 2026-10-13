package io.winstonhoyle.practicespeaking.ai

import androidx.annotation.DrawableRes
import io.winstonhoyle.practicespeaking.R

/**
 * The AI providers a user can bring a key for. [defaultModel] is only a starting point;
 * Settings lets the user override the model per provider. [shortName] labels badges, and
 * [keyPageUrl] is where the user creates a key. [logo] is a monochrome vector of the
 * provider's mark (see docs/licenses/TRADEMARKS.md).
 */
enum class Provider(
    val displayName: String,
    val shortName: String,
    val defaultModel: String,
    val keyHint: String,
    val keyPageUrl: String,
    @param:DrawableRes val logo: Int,
) {
    ANTHROPIC(
        "Anthropic (Claude)",
        "Claude",
        "claude-haiku-5-5",
        "sk-ant-…",
        "https://platform.claude.com/settings/keys",
        R.drawable.ic_logo_claude,
    ),
    OPENAI("OpenAI", "OpenAI", "gpt-5-mini", "sk-…", "https://platform.openai.com/api-keys", R.drawable.ic_logo_openai),
    GEMINI("Google Gemini", "Gemini", "gemini-2.5-flash-lite", "AIza…", "https://aistudio.google.com/apikey", R.drawable.ic_logo_gemini),
}
