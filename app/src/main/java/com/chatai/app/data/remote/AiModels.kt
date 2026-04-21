package com.chatai.app.data.remote

object AiModels {

    val freeModels: List<AiModel> = listOf(
        AiModel("google/gemma-4-31b-it:free", "Gemma 4 31B", "Google", 32000, "Latest Gemma 4, excellent reasoning and coding"),
        AiModel("google/gemma-4-26b-a4b-it:free", "Gemma 4 26B MoE", "Google", 32000, "MoE architecture, efficient and fast"),
        AiModel("google/gemma-3-27b-it:free", "Gemma 3 27B", "Google", 8192, "Strong multilingual support"),
        AiModel("google/gemma-3-12b-it:free", "Gemma 3 12B", "Google", 8192, "Lightweight and fast"),
        AiModel("google/gemma-3-4b-it:free", "Gemma 3 4B", "Google", 8192, "Ultra lightweight, mobile-friendly"),
        AiModel("google/gemma-3n-e4b-it:free", "Gemma 3N E4B", "Google", 32768, "Efficient Gemma 3N, great for on-device"),
        AiModel("google/gemma-3n-e2b-it:free", "Gemma 3N E2B", "Google", 32768, "Smallest Gemma 3N variant"),
        AiModel("meta-llama/llama-3.3-70b-instruct:free", "Llama 3.3 70B", "Meta", 131072, "Powerful Llama with 128K context"),
        AiModel("meta-llama/llama-3.2-3b-instruct:free", "Llama 3.2 3B", "Meta", 131072, "Fast lightweight model"),
        AiModel("qwen/qwen3-coder:free", "Qwen3 Coder", "Alibaba", 32768, "Specialized for coding tasks"),
        AiModel("qwen/qwen3-next-80b-a3b-instruct:free", "Qwen3 Next 80B", "Alibaba", 32768, "MoE architecture, high quality"),
        AiModel("nousresearch/hermes-3-llama-3.1-405b:free", "Hermes 3 405B", "NousResearch", 131072, "Based on Llama 3.1 405B"),
        AiModel("nvidia/nemotron-3-super-120b-a12b:free", "Nemotron 3 Super 120B", "NVIDIA", 131072, "NVIDIA's latest MoE model"),
        AiModel("nvidia/nemotron-nano-12b-v2:free", "Nemotron Nano 12B v2", "NVIDIA", 131072, "Small but capable"),
        AiModel("nvidia/nemotron-3-nano-30b-a3b:free", "Nemotron 3 Nano 30B", "NVIDIA", 131072, "MoE with 30B total params"),
        AiModel("nvidia/nemotron-nano-9b-v2-vl:free", "Nemotron 9B v2 VL", "NVIDIA", 4096, "Vision-Language model"),
        AiModel("openai/gpt-oss-120b:free", "GPT OSS 120B", "OpenAI", 131072, "OpenAI open-source model"),
        AiModel("openai/gpt-oss-20b:free", "GPT OSS 20B", "OpenAI", 131072, "OpenAI open-source 20B"),
        AiModel("minimax/minimax-m2.5:free", "MiniMax M2.5", "MiniMax", 131072, "MiniMax latest model"),
        AiModel("inclusionai/ling-2.6-flash:free", "Ling 2.6 Flash", "InclusionAI", 32768, "Fast inference"),
        AiModel("liquid/lfm-2.5-1.2b-instruct:free", "LFM 2.5 1.2B", "Liquid AI", 32768, "Ultra-efficient Liquid model"),
        AiModel("liquid/lfm-2.5-1.2b-thinking:free", "LFM 2.5 1.2B Think", "Liquid AI", 32768, "Reasoning-focused"),
        AiModel("cognitivecomputations/dolphin-mistral-24b-venice-edition:free", "Dolphin Mistral 24B", "Cognitive", 131072, "Uncensored, creative writing"),
        AiModel("arcee-ai/trinity-large-preview:free", "Trinity Large", "Arcee AI", 8192, "Arcee's latest model"),
        AiModel("z-ai/glm-4.5-air:free", "GLM 4.5 Air", "Z-AI", 131072, "Lightweight GLM 4.5")
    )

    private val defaultModelId = "google/gemma-4-31b-it:free"

    fun getDefaultModel(): AiModel = getModelById(defaultModelId) ?: freeModels.first()

    fun getModelById(id: String): AiModel? = freeModels.find { it.id == id }
}
