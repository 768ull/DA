package com.duckai.nativecompose

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

data class ChatMessage(val role: String, val content: String)

data class DuckModel(val id: String, val label: String)

/** Modelos atuais do Duck.ai (identificadores oficiais do servico). */
val MODELS = listOf(
    DuckModel("gpt-5.4-mini", "GPT-5.4 Mini"),
    DuckModel("gpt-5.4-nano", "GPT-5.4 Nano"),
    DuckModel("claude-haiku-4-5", "Claude Haiku 4.5"),
    DuckModel("mistral-small-2603", "Mistral Small"),
    DuckModel("tinfoil/gpt-oss-120b", "GPT-OSS 120B")
)

/**
 * Registro de falhas: captura o motivo exato de qualquer fechamento
 * inesperado e o preserva internamente. Na proxima abertura, o
 * aplicativo exibe o diagnostico completo, com opcao de copia-lo.
 */
object CrashLog {
    private const val PREFS = "duck_crash"
    private const val KEY = "last_crash"

    fun install(context: Context) {
        val appContext = context.applicationContext
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                appContext
                    .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                    .edit()
                    .putString(KEY, throwable.stackTraceToString())
                    .commit()
            } catch (e: Exception) {
                // Registro e melhor-esforco: nunca deve agravar a falha.
            }
            previous?.uncaughtException(thread, throwable)
        }
    }

    fun read(context: Context): String? =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY, null)

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().remove(KEY).commit()
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CrashLog.install(this)
        setContent {
            DuckTheme {
                ChatScreen(this@MainActivity)
            }
        }
    }
}

@Composable
fun DuckTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color(0xFFDE5833),
            onPrimary = Color(0xFFFFFFFF),
            background = Color(0xFF161616),
            onBackground = Color(0xFFEDEDED),
            surface = Color(0xFF212121),
            onSurface = Color(0xFFEDEDED),
            surfaceVariant = Color(0xFF2B2B2B),
            onSurfaceVariant = Color(0xFFCFCFCF),
            outline = Color(0xFF3F3F3F)
        ),
        content = content
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(activity: ComponentActivity) {
    val scope = rememberCoroutineScope()
    val harvester = remember { SessionHarvester(activity) }
    val client = remember { DuckChatClient() }
    val listState = rememberLazyListState()

    var sessionReady by remember { mutableStateOf(false) }
    var sessionStatus by remember { mutableStateOf("Iniciando sessão...") }
    val messages = remember { mutableStateListOf<ChatMessage>() }
    var input by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var model by remember { mutableStateOf(MODELS.first()) }
    var menuOpen by remember { mutableStateOf(false) }
    var crashReport by remember { mutableStateOf(CrashLog.read(activity)) }

    fun startSession() {
        sessionReady = false
        sessionStatus = "Iniciando sessão..."
        scope.launch {
            try {
                client.setToken(harvester.bootstrap())
                sessionReady = true
            } catch (e: Exception) {
                sessionStatus = e.message ?: "Falha ao iniciar a sessão"
            }
        }
    }

    fun newConversation() {
        messages.clear()
        startSession()
    }

    fun send() {
        val text = input.trim()
        if (text.isEmpty() || busy || !sessionReady) return
        input = ""
        messages.add(ChatMessage("user", text))
        messages.add(ChatMessage("assistant", ""))
        busy = true
        scope.launch {
            var attempt = 0
            var done = false
            while (!done) {
                attempt++
                try {
                    val history = messages.subList(0, messages.size - 1).toList()
                    client.chatStream(model.id, history) { delta ->
                        val last = messages.last()
                        messages[messages.size - 1] =
                            last.copy(content = last.content + delta)
                    }
                    done = true
                } catch (e: DuckChatClient.ChallengeException) {
                    if (attempt < 3) {
                        sessionReady = false
                        sessionStatus = "Renovando sessão..."
                        try {
                            client.setToken(harvester.bootstrap())
                            sessionReady = true
                        } catch (ex: Exception) {
                            messages[messages.size - 1] = ChatMessage(
                                "assistant",
                                "Falha ao renovar a sessão: " +
                                    (ex.message ?: "erro desconhecido")
                            )
                            done = true
                        }
                    } else {
                        messages[messages.size - 1] = ChatMessage(
                            "assistant",
                            "Sessão expirada. Toque no ícone de nova conversa."
                        )
                        done = true
                    }
                } catch (e: Exception) {
                    messages[messages.size - 1] = ChatMessage(
                        "assistant",
                        "Erro: " + (e.message ?: "desconhecido")
                    )
                    done = true
                }
            }
            busy = false
        }
    }

    DisposableEffect(Unit) {
        startSession()
        onDispose { harvester.close() }
    }

    LaunchedEffect(messages.size, messages.lastOrNull()?.content?.length) {
        if (messages.isNotEmpty()) listState.scrollToItem(messages.size - 1)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        TopAppBar(
            title = { Text("Duck.ai Nativo", fontWeight = FontWeight.Bold) },
            actions = {
                TextButton(onClick = { menuOpen = true }) {
                    Text(
                        model.label,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                DropdownMenu(
                    expanded = menuOpen,
                    onDismissRequest = { menuOpen = false }
                ) {
                    MODELS.forEach { m ->
                        DropdownMenuItem(
                            text = { Text(m.label) },
                            onClick = {
                                model = m
                                menuOpen = false
                            }
                        )
                    }
                }
                IconButton(onClick = { if (!busy) newConversation() }) {
                    Icon(
                        Icons.Filled.Refresh,
                        contentDescription = "Nova conversa"
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background,
                titleContentColor = MaterialTheme.colorScheme.onBackground
            )
        )

        crashReport?.let { report ->
            CrashBanner(report) {
                CrashLog.clear(activity)
                crashReport = null
            }
        }

        if (!sessionReady) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.width(16.dp),
                    strokeWidth = 2.dp
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    sessionStatus,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (messages.isEmpty()) {
                item {
                    Text(
                        "Como posso ajudar?",
                        modifier = Modifier.padding(top = 120.dp),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            items(messages) { msg ->
                MessageBubble(msg)
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .navigationBarsPadding()
                .padding(12.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Enviar mensagem ao Duck.ai") },
                maxLines = 5,
                shape = RoundedCornerShape(24.dp)
            )
            Spacer(Modifier.width(8.dp))
            IconButton(
                onClick = { send() },
                enabled = sessionReady && !busy && input.isNotBlank(),
                modifier = Modifier.background(
                    MaterialTheme.colorScheme.primary,
                    RoundedCornerShape(50)
                )
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Enviar",
                    tint = Color.White
                )
            }
        }
    }
}

@Composable
fun MessageBubble(msg: ChatMessage) {
    val isUser = msg.role == "user"
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            color = if (isUser) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp
            ),
            modifier = Modifier.widthIn(max = 320.dp)
        ) {
            Text(
                if (msg.content.isEmpty()) " " else msg.content,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                color = if (isUser) Color.White
                        else MaterialTheme.colorScheme.onSurface,
                fontSize = 15.sp,
                lineHeight = 21.sp
            )
        }
    }
}

@Composable
fun CrashBanner(text: String, onDismiss: () -> Unit) {
    val context = LocalContext.current
    Surface(
        color = Color(0xFF7A1F14),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                "O aplicativo fechou na última execução. Diagnóstico:",
                color = Color.White,
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text,
                color = Color(0xFFFFD9D0),
                fontSize = 11.sp,
                lineHeight = 15.sp,
                modifier = Modifier
                    .heightIn(max = 160.dp)
                    .verticalScroll(rememberScrollState())
            )
            Spacer(Modifier.height(8.dp))
            Row {
                TextButton(onClick = {
                    val clipboard =
                        context.getSystemService(Context.CLIPBOARD_SERVICE)
                            as ClipboardManager
                    clipboard.setPrimaryClip(
                        ClipData.newPlainText("diagnóstico", text)
                    )
                }) { Text("Copiar log", color = Color.White) }
                TextButton(onClick = onDismiss) {
                    Text("Fechar", color = Color.White)
                }
            }
        }
    }
}
