package io.winstonhoyle.practicespeaking.prompt

import android.content.Context

/**
 * Fills the tutor prompt template for one language. The output depends only on the language
 * (no dates or IDs), so it is byte-identical every time and prompt caching works.
 */
class PromptRenderer(private val template: String) {
    fun render(language: TutorLanguage): String {
        val values =
            mapOf(
                "TUTOR_NAME" to language.tutorName,
                "LANGUAGE_NAME" to language.displayName,
                "INFORMAL_RULE" to language.informalRule,
                "GRAMMAR_PRIORITIES" to language.grammarPriorities,
                "EXTRA_RULES" to language.extraRules,
                "EXAMPLE_CONNECTORS" to language.connectors,
                "EXAMPLE_QUESTIONS" to language.exampleQuestions,
                "NUMBER_EXAMPLE" to language.numberExample,
                "NO_FORMATTING" to language.noFormatting,
                "HOW_DO_I_SAY" to language.howDoISay,
                "CMD_SLOWER" to language.commandWords.slower,
                "CMD_HARDER" to language.commandWords.harder,
                "CMD_TRANSLATE" to language.commandWords.translate,
                "CMD_END" to language.commandWords.end,
            )
        val rendered =
            PLACEHOLDER.replace(template) { match ->
                values[match.groupValues[1]] ?: error("Unknown placeholder ${match.value}")
            }
        return rendered
    }

    companion object {
        private val PLACEHOLDER = Regex("""\{\{([A-Z_]+)\}\}""")
        const val TEMPLATE_ASSET = "tutor_prompt_template.md"

        fun fromAssets(context: Context): PromptRenderer =
            PromptRenderer(
                context.assets
                    .open(TEMPLATE_ASSET)
                    .bufferedReader()
                    .use { it.readText() },
            )
    }
}
