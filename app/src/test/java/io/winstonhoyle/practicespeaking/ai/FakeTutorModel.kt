package io.winstonhoyle.practicespeaking.ai

import io.winstonhoyle.practicespeaking.error.AppException
import io.winstonhoyle.practicespeaking.error.TutorError

/**
 * Scripted [TutorModel] for unit and UI tests. Each call to [reply] returns the next scripted
 * result; [requests] records what the app sent.
 */
class FakeTutorModel(
    private val script: MutableList<Result<TutorReply>> = mutableListOf(),
    override val provider: Provider = Provider.ANTHROPIC,
    override val model: String = "fake-model",
    var keyResult: Result<Unit> = Result.success(Unit),
    var modelsResult: Result<List<String>> = Result.success(listOf("fake-model", "fake-model-large")),
) : TutorModel {
    val requests = mutableListOf<List<Turn>>()

    fun enqueue(text: String) = script.add(Result.success(TutorReply(text, """{"fake":${requests.size + script.size}}""")))

    fun enqueueError(error: TutorError) = script.add(Result.failure(AppException(error)))

    override suspend fun reply(systemPrompt: String, history: List<Turn>): Result<TutorReply> {
        requests += history
        return if (script.isEmpty()) Result.success(TutorReply("…", null)) else script.removeAt(0)
    }

    override suspend fun testKey(): Result<Unit> = keyResult

    override suspend fun listModels(): Result<List<String>> = modelsResult

    companion object {
        const val WITH_CORRECTION =
            "Ты идёшь в магазин? Что ты хочешь купить?\n" +
                "CORRECTION: в магазине → в магазин | Use the accusative after a verb of motion."
        const val WITH_SUMMARY =
            "Пока! До встречи.\nSUMMARY\nMistakes: в магазине → в магазин\n" +
                "New words: хлеб — bread\nTip: Practise the accusative after в."
    }
}
