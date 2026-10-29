package org.duckassist.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity

/**
 * Ponto de entrada rápida do sistema: intercepta o assistente padrão
 * (ACTION_ASSIST), texto selecionado (ACTION_PROCESS_TEXT) e
 * compartilhamento de texto (ACTION_SEND), encaminhando o conteúdo ao
 * chat nativo.
 */
class AskActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prompt = extractPrompt(intent)
        startActivity(
            Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                putExtra(MainActivity.EXTRA_PROMPT, prompt)
            }
        )
        finish()
    }

    private fun extractPrompt(intent: Intent): String {
        intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)?.let {
            if (it.isNotBlank()) return it.toString()
        }
        intent.getCharSequenceExtra(Intent.EXTRA_TEXT)?.let {
            if (it.isNotBlank()) return it.toString()
        }
        val subject = intent.getCharSequenceExtra(Intent.EXTRA_SUBJECT)
        if (!subject.isNullOrBlank()) return subject.toString()
        // Para ACTION_ASSIST sem entrada estruturada, o usuário digita no chat.
        return ""
    }
}
