package io.winstonhoyle.practicespeaking.ai

/** Builds the [TutorModel] for a provider. The only place that picks a vendor class. */
object TutorModelFactory {
    fun create(provider: Provider, apiKey: String, model: String = provider.defaultModel): TutorModel =
        when (provider) {
            Provider.ANTHROPIC -> AnthropicTutorModel(apiKey, model)
            Provider.OPENAI -> OpenAiTutorModel(apiKey, model)
            Provider.GEMINI -> GeminiTutorModel(apiKey, model)
        }
}
