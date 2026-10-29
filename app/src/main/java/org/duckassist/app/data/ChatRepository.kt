package org.duckassist.app.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Orquestra a persistência (Room) e a comunicação com a API do duck.ai,
 * incluindo o gerenciamento do token de sessão (x-vqd-4) com renovação
 * automática em caso de expiração.
 */
class ChatRepository(
    private val dao: ChatDao,
    private val settingsStore: SettingsStore
) {

    private var cachedToken: String? = null
    private val tokenMutex = Mutex()

    fun observeConversations(): Flow<List<ConversationEntity>> =
        dao.observeConversations()

    fun observeMessages(conversationId: Long): Flow<List<MessageEntity>> =
        dao.observeMessages(conversationId)

    private suspend fun ensureToken(baseUrl: String): String = tokenMutex.withLock {
        cachedToken ?: DuckApi(baseUrl).fetchToken().also { cachedToken = it }
    }

    /**
     * Envia [text] ao modelo [model]. Cria a conversa quando necessário
     * ([conversationId] nulo) e devolve o identificador da conversa.
     */
    suspend fun send(
        conversationId: Long?,
        title: String,
        text: String,
        model: String
    ): Long {
        val s = settingsStore.settings.first()
        val baseUrl = s.apiBaseUrl.ifBlank { DuckApiDefaults.BASE_URL }
        val api = DuckApi(baseUrl)

        val convId = conversationId ?: dao.insertConversation(
            ConversationEntity(
                title = title.take(48),
                model = model,
                updatedAt = System.currentTimeMillis()
            )
        )

        dao.insertMessage(
            MessageEntity(
                conversationId = convId,
                role = "user",
                content = text,
                timestamp = System.currentTimeMillis()
            )
        )

        val history = dao.getMessagesOnce(convId)
        val payload = ArrayList<ChatTurn>(history.size + 1)
        if (s.systemPrompt.isNotBlank()) {
            payload.add(ChatTurn("system", s.systemPrompt))
        }
        history.forEach { payload.add(ChatTurn(it.role, it.content)) }

        var token = ensureToken(baseUrl)
        val (answer, newToken) = try {
            api.chat(model, payload, token)
        } catch (e: DuckApiException) {
            if (e.code == 401 || e.code == 403) {
                // Token expirado: renova e tenta uma única vez.
                tokenMutex.withLock { cachedToken = null }
                token = ensureToken(baseUrl)
                api.chat(model, payload, token)
            } else {
                throw e
            }
        }
        tokenMutex.withLock { cachedToken = newToken }

        dao.insertMessage(
            MessageEntity(
                conversationId = convId,
                role = "assistant",
                content = answer,
                timestamp = System.currentTimeMillis()
            )
        )
        dao.touchConversation(convId, System.currentTimeMillis())
        return convId
    }

    suspend fun deleteConversation(id: Long) = dao.deleteConversation(id)

    suspend fun clearAll() = dao.deleteAll()
}
