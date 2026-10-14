package io.winstonhoyle.practicespeaking.prompt

/**
 * The learner's level on the CEFR scale. [promptRule] tells the tutor how to pitch vocabulary
 * and grammar; it goes into the prompt, so changing it changes every cached prompt.
 */
enum class CefrLevel(val label: String, val promptRule: String) {
    A1(
        "Beginner",
        "Use very short sentences in the present tense and the most common everyday words. Ask simple questions about familiar things.",
    ),
    A2("Elementary", "Use common verbs and everyday words, present, past, and future tenses, and short linked sentences."),
    B1("Intermediate", "Use everyday vocabulary with some less common words, all the main tenses, and subordinate clauses."),
    B2("Upper intermediate", "Use a wide vocabulary, an idiom now and then, and complex sentences, as you would with a confident speaker."),
    C1("Advanced", "Speak naturally, with rich vocabulary, idioms, and the full range of grammar."),
    C2("Proficient", "Speak as you would with an educated native speaker, with nuance, idioms, and changes of register."),
    ;

    /** Level-specific limits such as "avoid participles" stop applying from C1 up. */
    val usesSimplifications: Boolean get() = this < C1

    companion object {
        val DEFAULT = A2
    }
}
