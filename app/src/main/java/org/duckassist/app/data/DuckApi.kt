package org.duckassist.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/** Uma mensagem da conversa enviada à API (role: "system", "user" ou "assistant"). */
data class ChatTurn(val role: String, val content: String)

class DuckApiException(message: String, val code: Int = 0) : Exception(message)

object DuckApiDefaults {
    const val BASE_URL = "https://duck.ai"
    const val USER_AGENT =
        "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/130.0.0.0 Mobile Safari/537.36"
}

/**
 * Cliente nativo da API de chat do duck.ai (não oficial).
 *
 * Protocolo:
 *  1. GET  {base}/duckchat/v1/status  com o cabeçalho "x-vqd-accept: 1"
 *     devolve o token de sessão no cabeçalho "x-vqd-4".
 *  2. POST {base}/duckchat/v1/chat    com o cabeçalho "x-vqd-4" e o corpo
 *     {"model": ..., "messages": [{"role": ..., "content": ...}]}.
 *     A resposta traz a resposta do modelo em "message" (JSON) ou em
 *     fragmentos SSE, e um novo token "x-vqd-4" para a próxima mensagem.
 */
class DuckApi(private val baseUrl: String = DuckApiDefaults.BASE_URL) {

    companion object {
        private const val JSON_TYPE = "application/json; charset=utf-8"

        private val client: OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    /** Obtém um novo token de sessão (x-vqd-4). */
    suspend fun fetchToken(): String = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("$baseUrl/duckchat/v1/status")
            .header("x-vqd-accept", "1")
            .header("User-Agent", DuckApiDefaults.USER_AGENT)
            .header("Referer", "$baseUrl/")
            .header("Accept", "*/*")
            .get()
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw DuckApiException(
                    "Falha ao obter token de sessão (HTTP ${response.code})",
                    response.code
                )
            }
            response.header("x-vqd-4")
                ?: throw DuckApiException(
                    "Resposta sem token x-vqd-4 (o protocolo do duck.ai pode ter mudado)."
                )
        }
    }

    /**
     * Envia o histórico da conversa e devolve o par
     * (resposta do modelo, novo token x-vqd-4).
     */
    suspend fun chat(model: String, messages: List<ChatTurn>, token: String): Pair<String, String> =
        withContext(Dispatchers.IO) {
            val body = JSONObject().apply {
                put("model", model)
                put("messages", JSONArray().apply {
                    messages.forEach { m ->
                        put(JSONObject().put("role", m.role).put("content", m.content))
                    }
                })
            }

            val request = Request.Builder()
                .url("$baseUrl/duckchat/v1/chat")
                .header("Content-Type", JSON_TYPE)
                .header("Accept", "*/*")
                .header("x-vqd-4", token)
                .header("User-Agent", DuckApiDefaults.USER_AGENT)
                .header("Referer", "$baseUrl/")
                .header("Origin", baseUrl)
                .header("x-requested-with", "XMLHttpRequest")
                .post(body.toString().toRequestBody(JSON_TYPE.toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                val newToken = response.header("x-vqd-4") ?: token
                val raw = response.body?.string().orEmpty()

                if (response.code == 429) {
                    val type = errorType(raw)
                    throw DuckApiException(
                        if (type.isNullOrBlank()) {
                            "Limite de uso atingido. Aguarde alguns minutos."
                        } else {
                            "Limite de uso atingido ($type). Aguarde alguns minutos."
                        },
                        429
                    )
                }
                if (!response.isSuccessful) {
                    val type = errorType(raw)
                    throw DuckApiException(
                        "Erro HTTP ${response.code}" + (if (type.isNullOrBlank()) "" else " ($type)"),
                        response.code
                    )
                }

                val contentType = response.header("Content-Type").orEmpty()
                val answer = if (contentType.contains("text/event-stream")) {
                    parseSse(raw)
                } else {
                    JSONObject(raw).optString("message", "")
                }
                if (answer.isBlank()) throw DuckApiException("Resposta vazia do duck.ai")
                answer to newToken
            }
        }

    private fun errorType(raw: String): String? = try {
        if (raw.isBlank()) null
        else JSONObject(raw).optString("type").takeIf { it.isNotBlank() }
    } catch (_: Exception) {
        null
    }

    /** Acumula os campos "message" dos fragmentos SSE. */
    private fun parseSse(raw: String): String {
        val builder = StringBuilder()
        raw.lineSequence().forEach { line ->
            if (line.startsWith("data:")) {
                val payload = line.removePrefix("data:").trim()
                if (payload == "[DONE]") return@forEach
                try {
                    builder.append(JSONObject(payload).optString("message"))
                } catch (_: Exception) {
                    // Ignora fragmentos que não sejam JSON válido.
                }
            }
        }
        return builder.toString()
    }
}
