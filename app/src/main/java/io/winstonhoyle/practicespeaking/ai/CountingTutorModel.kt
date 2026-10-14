package io.winstonhoyle.practicespeaking.ai

/**
 * Wraps a [TutorModel] and hands the token usage of every successful reply to [record]: tutor
 * turns and translations alike. A refused or failed request reports no usage, so it isn't counted.
 */
class CountingTutorModel(private val inner: TutorModel, private val record: suspend (Provider, TokenUsage) -> Unit) : TutorModel by inner {
    override suspend fun reply(systemPrompt: String, history: List<Turn>): Result<TutorReply> =
        inner.reply(systemPrompt, history).also { result -> result.getOrNull()?.usage?.let { record(provider, it) } }
}
