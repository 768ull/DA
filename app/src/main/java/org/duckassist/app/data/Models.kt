package org.duckassist.app.data

/**
 * Modelo de IA disponível no duck.ai, com identificador usado pela API
 * e rótulo amigável exibido na interface.
 */
data class DuckModel(val id: String, val label: String)

object DuckModels {

    const val DEFAULT_MODEL_ID = "gpt-4o-mini"

    /**
     * Modelos conhecidos do duck.ai. Como a API não é oficial, a lista pode
     * ficar desatualizada; novos modelos podem ser adicionados nas
     * configurações do aplicativo sem precisar recompilar.
     */
    val DEFAULTS: List<DuckModel> = listOf(
        DuckModel("gpt-4o-mini", "GPT-4o mini"),
        DuckModel("claude-3-haiku-20240307", "Claude 3 Haiku"),
        DuckModel("meta-llama/Llama-3.3-70B-Instruct-Turbo", "Llama 3.3 70B"),
        DuckModel("mistralai/Mistral-Small-24B-Instruct-2501", "Mistral Small 3"),
        DuckModel("o3-mini", "o3-mini")
    )

    /**
     * Une os modelos padrão aos modelos personalizados definidos pelo
     * usuário nas configurações (lista separada por vírgula, ponto e
     * vírgula ou quebras de linha).
     */
    fun mergeCustom(customCsv: String): List<DuckModel> {
        val custom = customCsv
            .split(',', ';', '\n')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .map { id -> DuckModel(id, id.substringAfterLast('/')) }
            .filter { c -> DEFAULTS.none { it.id == c.id } }
        return DEFAULTS + custom
    }
}
