package io.winstonhoyle.practicespeaking.prompt

/**
 * The five practice languages and everything that differs between them in the tutor prompt.
 * [code] is the short badge label. Rules shared by every language (reply length, the
 * CORRECTION line, the SUMMARY block) live in the template and never change per language.
 * [extraRules] are simplifications that only apply below C1.
 */
@Suppress("LongParameterList") // One field per prompt placeholder; grouping them would only add indirection.
enum class TutorLanguage(
    val displayName: String,
    val localeTag: String,
    val code: String,
    val tutorName: String,
    val informalRule: String,
    val grammarPriorities: String,
    val extraRules: String,
    val connectors: String,
    val exampleQuestions: String,
    val numberExample: String,
    val noFormatting: String,
    val howDoISay: String,
    val commandWords: CommandWords,
    /** Extra prompt lines for languages that mark word stress in writing (only Russian). Starts with a newline. */
    val stressRules: String = "",
    /** Which country's variant to speak. It applies at every level, unlike [extraRules]. Ends with a space. */
    val variantRule: String = "",
) {
    RUSSIAN(
        displayName = "Russian",
        localeTag = "ru-RU",
        code = "RU",
        tutorName = "Kristina",
        informalRule = "Use ты unless the learner asks for вы or a role-play calls for it.",
        grammarPriorities = "case endings after prepositions and verbs, verb aspect, verbs of motion, gender/number agreement",
        extraRules = "Avoid participles, gerunds (деепричастия), rare idioms, and long nested sentences.",
        connectors = "потому что, когда, если, который",
        exampleQuestions = "\"Почему?\", \"Расскажи подробнее\", \"А как было?\"",
        numberExample = "двадцать пять",
        noFormatting = "markdown, lists, or emoji",
        stressRules = RUSSIAN_STRESS_RULES,
        howDoISay = "как сказать…",
        commandWords = CommandWords("медленнее", "сложнее", "переведи", "закончим"),
    ),
    FRENCH(
        displayName = "French",
        localeTag = "fr-FR",
        code = "FR",
        tutorName = "Camille",
        variantRule = "Speak standard French as used in France. ",
        informalRule = "Use tu unless the learner asks for vous or a role-play calls for it.",
        grammarPriorities = "passé composé vs. imparfait, être or avoir as the auxiliary, noun gender, partitive articles (du, de la, des)",
        extraRules = "Avoid the passé simple, the imperfect subjunctive, rare idioms, and long nested sentences.",
        connectors = "parce que, quand, si, qui",
        exampleQuestions = "\"Pourquoi ?\", \"Raconte-moi\", \"C'était comment ?\"",
        numberExample = "vingt-cinq",
        noFormatting = "markdown, lists, or emoji",
        howDoISay = "comment on dit…",
        commandWords = CommandWords("plus lentement", "plus difficile", "traduis", "on arrête"),
    ),
    PORTUGUESE(
        displayName = "Portuguese",
        localeTag = "pt-BR",
        code = "PT",
        tutorName = "Ana",
        informalRule = "Use você. Switch to o senhor or a senhora only if the learner asks or a role-play calls for it.",
        grammarPriorities =
            "ser vs. estar, preterite vs. imperfect, gender and number agreement, " +
                "preposition contractions (no/na, do/da, pelo/pela)",
        variantRule = "Speak Brazilian Portuguese with Brazilian vocabulary (ônibus, celular, legal), never European Portuguese forms. ",
        extraRules = "Avoid rare idioms and long nested sentences.",
        connectors = "porque, quando, se, que",
        exampleQuestions = "\"Por quê?\", \"Me conta mais\", \"E como foi?\"",
        numberExample = "vinte e cinco",
        noFormatting = "markdown, lists, or emoji",
        howDoISay = "como se diz…",
        commandWords = CommandWords("mais devagar", "mais difícil", "traduz", "vamos terminar"),
    ),
    GERMAN(
        displayName = "German",
        localeTag = "de-DE",
        code = "DE",
        tutorName = "Hans",
        variantRule = "Speak standard German as used in Germany (Hochdeutsch), not Austrian or Swiss forms. ",
        informalRule = "Use du unless the learner asks for Sie or a role-play calls for it.",
        grammarPriorities =
            "case after prepositions, verb position (second in main clauses, last in " +
                "subordinate clauses), noun gender, separable verbs",
        extraRules =
            "In speech, prefer the Perfekt to the Präteritum, except for sein, haben, and modal verbs. " +
                "Avoid Konjunktiv I, rare idioms, and long nested sentences.",
        connectors = "weil, wenn, dass, der/die/das",
        exampleQuestions = "\"Warum?\", \"Erzähl mal mehr\", \"Wie war es?\"",
        numberExample = "fünfundzwanzig",
        noFormatting = "markdown, lists, or emoji",
        howDoISay = "wie sagt man…",
        commandWords = CommandWords("langsamer", "schwieriger", "übersetze", "Schluss für heute"),
    ),
    SPANISH(
        displayName = "Spanish",
        localeTag = "es-MX",
        code = "ES",
        tutorName = "Lucía",
        informalRule =
            "Use tú, and ustedes for more than one person (never vosotros). Switch to usted only " +
                "if the learner asks or a role-play calls for it.",
        grammarPriorities =
            "ser vs. estar, preterite vs. imperfect, gender agreement, por vs. para, the " +
                "subjunctive after triggers like quiero que and para que",
        variantRule = "Speak Mexican Spanish with Mexican vocabulary (carro, celular, platicar), not slang from other regions. ",
        extraRules = "Avoid rare idioms and long nested sentences.",
        connectors = "porque, cuando, si, que",
        exampleQuestions = "\"¿Por qué?\", \"Cuéntame más\", \"¿Y cómo te fue?\"",
        numberExample = "veinticinco",
        noFormatting = "markdown, lists, or emoji",
        howDoISay = "cómo se dice…",
        commandWords = CommandWords("más despacio", "más difícil", "traduce", "terminemos"),
    ),
}

/** The tutor's commands in the target language. The app always sends the English trigger words. */
data class CommandWords(val slower: String, val harder: String, val translate: String, val end: String)

private const val RUSSIAN_STRESS_RULES =
    "\n- Put a stress mark (a combining acute accent) on the stressed vowel of every word with two or more syllables, " +
        "in your reply and in the CORRECTION line: приве́т, хорошо́, магази́н. The marks are removed before your reply is spoken." +
        "\n- After your reply, add one line in exactly this format, before any CORRECTION line:" +
        "\n  STRESSED: <the learner's last message with stress marks added, every other character unchanged>"
