package com.duckai.nativecompose

import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Inicia a sessao com o Duck.ai.
 *
 * O servico protege a conversa com um desafio de prova de trabalho
 * (X-Vqd-Hash-1) cuja resolucao exige JavaScript de navegador. Em vez
 * de reengenheirar o desafio, este colhedor carrega o duck.ai em uma
 * WebView invisivel de 1 pixel: a propria pagina resolve o desafio e
 * um gancho JavaScript observa os cabecalhos das requisicoes internas,
 * capturando o token gerado. A WebView e destruida em seguida.
 */
class SessionHarvester(private val activity: ComponentActivity) {

    private val main = Handler(Looper.getMainLooper())
    private var webView: WebView? = null

    suspend fun bootstrap(): String {
        destroy()

        val deferred = CompletableDeferred<String>()

        withContext(Dispatchers.Main) {
            val wv = try {
                WebView(activity).also {
                    it.alpha = 0f
                    it.settings.javaScriptEnabled = true
                    it.settings.domStorageEnabled = true
                    it.settings.databaseEnabled = true
                    it.addJavascriptInterface(object {
                        @JavascriptInterface
                        fun onToken(value: String?) {
                            if (!value.isNullOrEmpty()) {
                                runCatching { deferred.complete(value) }
                            }
                        }
                    }, "NativeBridge")
                    it.webViewClient = object : WebViewClient() {
                        override fun onPageStarted(
                            view: WebView,
                            url: String,
                            favicon: Bitmap?
                        ) {
                            view.evaluateJavascript(HOOK_JS, null)
                        }

                        override fun onPageFinished(view: WebView, url: String) {
                            view.evaluateJavascript(HOOK_JS, null)
                        }
                    }
                }
            } catch (e: Exception) {
                throw Exception(
                    "Falha ao preparar a sessão: " +
                        (e.message ?: e.javaClass.simpleName)
                )
            }

            try {
                activity.addContentView(wv, ViewGroup.LayoutParams(1, 1))
                wv.loadUrl("https://duck.ai/")
                webView = wv
            } catch (e: Exception) {
                runCatching { wv.destroy() }
                throw Exception(
                    "Falha ao anexar a sessão: " +
                        (e.message ?: e.javaClass.simpleName)
                )
            }
        }

        val token = withTimeoutOrNull(45_000L) { deferred.await() }
        destroy()
        return token ?: throw Exception(
            "Não foi possível iniciar a sessão com o Duck.ai. " +
            "Verifique a conexão e toque no ícone de nova conversa."
        )
    }

    fun close() {
        main.post { destroy() }
    }

    private fun destroy() {
        webView?.let { wv ->
            runCatching {
                (wv.parent as? ViewGroup)?.removeView(wv)
                wv.destroy()
            }
        }
        webView = null
    }

    companion object {
        private const val HOOK_JS = """
        (function () {
            if (window.__duckHookInstalled) return;
            window.__duckHookInstalled = true;

            var report = function (value) {
                try {
                    if (value && String(value).indexOf('eyJ') === 0) {
                        NativeBridge.onToken(String(value));
                    }
                } catch (e) {}
            };

            try {
                var rawFetch = window.fetch;
                if (rawFetch) {
                    window.fetch = function (input, init) {
                        try {
                            var url = (typeof input === 'string') ? input : (input && input.url) || '';
                            if (url.indexOf('duckchat') >= 0) {
                                var h = (init && init.headers) || (input && input.headers) || null;
                                var v = '';
                                if (h) {
                                    if (typeof h.get === 'function') {
                                        v = h.get('x-vqd-hash-1') || h.get('x-vqd-4') || '';
                                    } else {
                                        v = h['x-vqd-hash-1'] || h['X-Vqd-Hash-1'] || h['x-vqd-4'] || '';
                                    }
                                }
                                report(v);
                            }
                        } catch (e) {}
                        return rawFetch.apply(this, arguments);
                    };
                }
            } catch (e) {}

            try {
                var rawOpen = XMLHttpRequest.prototype.open;
                XMLHttpRequest.prototype.open = function (method, url) {
                    this.__duckUrl = url;
                    return rawOpen.apply(this, arguments);
                };
                var rawSetHeader = XMLHttpRequest.prototype.setRequestHeader;
                XMLHttpRequest.prototype.setRequestHeader = function (name, value) {
                    try {
                        var u = this.__duckUrl || '';
                        if (u.indexOf('duckchat') >= 0) { report(value); }
                    } catch (e) {}
                    return rawSetHeader.apply(this, arguments);
                };
            } catch (e) {}

            var scan = function () {
                try {
                    var stores = [window.localStorage, window.sessionStorage];
                    for (var s = 0; s < stores.length; s++) {
                        var store = stores[s];
                        for (var i = 0; i < store.length; i++) {
                            var k = store.key(i) || '';
                            var v = store.getItem(k) || '';
                            if (/vqd|hash|chall|pow|token/i.test(k)) { report(v); }
                        }
                    }
                } catch (e) {}
            };
            scan();
            setTimeout(scan, 1500);
            setTimeout(scan, 4000);
        })();
        """
    }
}
