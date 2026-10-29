package com.duckai.nativecompose

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Cliente do protocolo de conversa do Duck.ai.
 *
 * Endpoint: POST https://duckduckgo.com/duckchat/v1/chat
 * Formato: SSE com linhas "data: {json}" contendo {"message": "..."},
 * encerradas por "data: [DONE]".
 *
 * O token de sessao (X-Vqd-Hash-1) e obtido pela SessionHarvester e
 * renovado a cada resposta pelo cabecalho de retorno.
 */
class DuckChatClient {

    /** Desafio de sessao expirado: a SessionHarvester deve renovar. */
    class ChallengeException(message: String) : Exception(message)

    companion object {
        private const val CHAT_URL = "https://duckduckgo.com/duckchat/v1/chat"
        private const val USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"
    }

    private val http = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(180, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    @Volatile
    private var token: String = ""

    fun setToken(value: String) {
        token = value
    }

    /**
     * Envia o historico ao modelo selecionado e devolve a resposta em
     * tempo real atraves de [onToken]. Retorna a resposta completa.
     */
    suspend fun chatStream(
        model: String,
        history: List<ChatMessage>,
        onToken: (String) -> Unit
    ): String = withContext(Dispatchers.IO) {
        if (token.isEmpty()) throw ChallengeException("Token de sessão ausente")

        val jsonMessages = JSONArray()
        history.forEach { m ->
            jsonMessages.put(
                JSONObject().put("role", m.role).put("content", m.content)
            )
        }
        val payload = JSONObject()
            .put("model", model)
            .put("messages", jsonMessages)

        val request = Request.Builder()
            .url(CHAT_URL)
            .header("Content-Type", "application/json")
            .header("Accept", "text/event-stream")
            .header("x-vqd-hash-1", token)
            .header("x-vqd-4", token)
            .header("User-Agent", USER_AGENT)
            .header("Referer", "https://duckduckgo.com/")
            .header("Origin", "https://duckduckgo.com")
            .header("DNT", "1")
            .post(payload.toString().toRequestBody("application/json".toMediaType()))
            .build()

        http.newCall(request).execute().use { response ->
            // Renova o token para a proxima pergunta.
            response.header("x-vqd-hash-1")?.let { token = it }
            response.header("x-vqd-4")?.let { token = it }

            when {
                response.code == 429 ->
                    throw Exception("Limite de requisições atingido. Aguarde alguns instantes.")
                response.code == 418 ->
                    throw ChallengeException("Desafio de sessão expirado")
                !response.isSuccessful ->
                    throw Exception("Erro HTTP " + response.code)
            }

            val source = response.body?.source()
                ?: throw Exception("Resposta vazia do servidor")

            val full = StringBuilder()
            while (true) {
                val line = source.readUtf8Line() ?: break
                if (!line.startsWith("data: ")) continue
                val data = line.removePrefix("data: ").trim()
                if (data == "[DONE]") break
                val obj = try {
                    JSONObject(data)
                } catch (e: Exception) {
                    continue
                }
                if (obj.optString("action") == "error") {
                    val type = obj.optString("type", "ERR_UNKNOWN")
                    if (type == "ERR_CONVERSATION_LIMIT") {
                        throw Exception(
                            "Limite da conversação atingido. " +
                            "Toque no ícone de nova conversa."
                        )
                    }
                    if (type.contains("CHALLENGE") || type.contains("VQD")) {
                        throw ChallengeException(type)
                    }
                    throw Exception(type)
                }
                val piece = obj.optString("message", "")
                if (piece.isNotEmpty()) {
                    full.append(piece)
                    onToken(piece)
                }
            }
            full.toString()
        }
    }
}
