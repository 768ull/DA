package com.duckai.nativeapp

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.webkit.CookieManager
import android.webkit.GeolocationPermissions
import android.webkit.PermissionRequest
import android.webkit.URLUtil
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import kotlin.math.abs

class MainActivity : AppCompatActivity() {

    companion object {
        private const val HOME_URL = "https://duck.ai/"

        /** Dominios mantidos dentro do aplicativo. */
        private val INTERNAL_HOSTS = setOf("duck.ai", "duckduckgo.com")
    }

    private lateinit var webView: WebView
    private var filePathCallback: ValueCallback<Array<Uri>>? = null

    /** Seletor de arquivos para uploads (anexos de imagem no chat). */
    private val fileChooserLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val results = WebChromeClient.FileChooserParams.parseResult(
            result.resultCode, result.data
        )
        filePathCallback?.onReceiveValue(results)
        filePathCallback = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = FrameLayout(this)

        webView = NoZoomWebView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }

        /* SEM TELA DE CARREGAMENTO: o WebView ocupa a tela inteira desde
         * o primeiro quadro. O fundo escuro do tema coincide com o fundo
         * do Duck.ai, portanto nao existe rotacao de progresso nem
         * piscada branca durante a abertura. */
        root.addView(webView)
        setContentView(root)

        configureWebView()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (webView.canGoBack()) webView.goBack() else finish()
            }
        })

        if (savedInstanceState != null) {
            webView.restoreState(savedInstanceState)
        } else {
            val deepLink = intent?.dataString
            webView.loadUrl(deepLink ?: HOME_URL)
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun configureWebView() {
        webView.webViewClient = DuckWebViewClient()
        webView.webChromeClient = DuckChromeClient()

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            allowFileAccess = false
            allowContentAccess = false

            /* ---- BLOQUEIO TOTAL DO ZOOM ----
             * Camada 1: configuracoes nativas do WebView.
             * Sem suporte a zoom, sem controles nativos e sem
             * botoes de zoom na tela. Desativa tambem o zoom
             * por duplo toque. */
            setSupportZoom(false)
            builtInZoomControls = false
            displayZoomControls = false
            useWideViewPort = true
            loadWithOverviewMode = true
            textZoom = 100

            // Apenas HTTPS; conteudo misto e bloqueado.
            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            mediaPlaybackRequiresUserGesture = false

            /* ---- ABERTURA QUASE INSTANTANEA ----
             * O cache tem prioridade sobre a rede: apos o primeiro
             * uso, o Duck.ai e desenhado imediatamente a partir do
             * cache, sem aguardar novas respostas de rede. */
            cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK
        }

        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)

        webView.setDownloadListener { url, _, contentDisposition, mimeType, _ ->
            downloadFile(url, contentDisposition, mimeType)
        }
    }

    /**
     * WebView totalmente rigido. Cinco camadas independentes e
     * complementares, conforme as tecnicas consagradas de bloqueio de
     * rolagem lateral em WebView:
     *
     *  1. OVER_SCROLL_NEVER + barras ocultas: elimina o efeito
     *     elastico de esticamento (overscroll stretch) do Android;
     *  2. Trava fisica: computeHorizontalScrollRange declarado igual
     *     a area visivel - nao existe espaco lateral para rolar;
     *  3. Interceptacao de gesto: arrastes dominados pela horizontal
     *     sao consumidos antes de chegar ao motor de rolagem;
     *  4. Multitoche e duplo toque consumidos - zoom impossivel;
     *  5. Trava absoluta de posicao: qualquer deslocamento horizontal
     *     aplicado ao componente e forcado de volta a zero dentro do
     *     mesmo quadro, venha ele do dedo, do fling ou do proprio
     *     motor do Chromium. A posicao horizontal permanece 0 de
     *     forma permanente.
     *
     * A rolagem vertical permanece perfeitamente intacta e a pagina
     * do Duck.ai nao e modificada em nenhum byte.
     */
    private class NoZoomWebView(context: Context) : WebView(context) {

        private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
        private var lastX = 0f
        private var lastY = 0f
        private var lateralDrag = false
        private var correctingPosition = false

        init {
            /* Camada 1: sem efeito elastico, sem barras de rolagem. */
            overScrollMode = View.OVER_SCROLL_NEVER
            isHorizontalScrollBarEnabled = false
            isVerticalScrollBarEnabled = false
            setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY)
        }

        /* Camada 2: trava fisica da rolagem horizontal. A area rolavel
         * e declarada exatamente igual a area visivel. */
        override fun computeHorizontalScrollRange(): Int =
            computeHorizontalScrollExtent()

        /* Camada 5: trava absoluta de posicao horizontal. Toda
         * solicitacao de rolagem - do dedo, do fling ou do proprio
         * motor do Chromium - e forcada a zero antes de ser aplicada.
         * Nenhuma forca do sistema consegue mover o componente
         * lateralmente. */
        override fun scrollTo(x: Int, y: Int) {
            super.scrollTo(0, y)
        }

        override fun onOverScrolled(
            scrollX: Int,
            scrollY: Int,
            clampedX: Boolean,
            clampedY: Boolean
        ) {
            super.onOverScrolled(0, scrollY, clampedX, clampedY)
        }

        override fun onScrollChanged(l: Int, t: Int, oldl: Int, oldt: Int) {
            if (l != 0 && !correctingPosition) {
                /* Correcao de emergencia: se algum deslocamento
                 * horizontal escapar das camadas anteriores, ele e
                 * revertido dentro do proprio quadro. */
                correctingPosition = true
                try {
                    super.scrollTo(0, t)
                } finally {
                    correctingPosition = false
                }
                return
            }
            super.onScrollChanged(l, t, oldl, oldt)
        }

        private val doubleTapBlocker = GestureDetector(
            context,
            object : GestureDetector.SimpleOnGestureListener() {
                /** Camada 4: consome o duplo toque (zoom por toque duplo). */
                override fun onDoubleTap(event: MotionEvent): Boolean = true
            }
        )

        override fun onTouchEvent(event: MotionEvent): Boolean {
            doubleTapBlocker.onTouchEvent(event)

            /* Camada 4: qualquer evento com dois ou mais ponteiros
             * (pinch zoom) e descartado antes de chegar ao WebView. */
            if (event.pointerCount > 1) return true

            /* Camada 3: interceptacao de arraste lateral. */
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    lastX = event.x
                    lastY = event.y
                    lateralDrag = false
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = abs(event.x - lastX)
                    val dy = abs(event.y - lastY)
                    lastX = event.x
                    lastY = event.y
                    if (lateralDrag || (dx > touchSlop && dx > dy)) {
                        /* Movimento dominado pela horizontal: consumido.
                         * Nao ha deslocamento, nao ha esticamento, nao
                         * ha tremor. Um arraste que se inicie vertical
                         * continua rolando normalmente. */
                        lateralDrag = true
                        return true
                    }
                }
                MotionEvent.ACTION_UP,
                MotionEvent.ACTION_CANCEL -> lateralDrag = false
            }
            return super.onTouchEvent(event)
        }
    }

    /** Mantem duck.ai dentro do aplicativo; demais dominios abrem no navegador. */
    private inner class DuckWebViewClient : WebViewClient() {

        override fun shouldOverrideUrlLoading(
            view: WebView,
            request: WebResourceRequest
        ): Boolean {
            val url = request.url

            // Forca HTTPS em qualquer link http.
            if (url.scheme == "http") {
                view.loadUrl(url.toString().replaceFirst("http://", "https://"))
                return true
            }

            if (url.scheme !in listOf("https", "about", "data", "blob", "javascript")) {
                openExternally(url.toString())
                return true
            }

            val host = url.host ?: return false
            return if (host in INTERNAL_HOSTS ||
                host.endsWith(".duck.ai") ||
                host.endsWith(".duckduckgo.com")
            ) {
                false // Carrega no WebView do aplicativo.
            } else {
                openExternally(url.toString())
                true
            }
        }

        override fun onReceivedError(
            view: WebView,
            request: WebResourceRequest,
            error: WebResourceError
        ) {
            if (request.isForMainFrame) {
                val html = """
                    <html><head><meta name="viewport" content=
                    "width=device-width, initial-scale=1, maximum-scale=1, user-scalable=no">
                    </head><body style="background:#161616;color:#e8e8e8;
                    font-family:sans-serif;text-align:center;padding-top:40vh">
                    <h2>Sem conex&atilde;o</h2>
                    <p>N&atilde;o foi poss&iacute;vel carregar o Duck.ai.</p>
                    <p><a style="color:#adc2fc" href="https://duck.ai/">Tentar novamente</a></p>
                    </body></html>
                """.trimIndent()
                view.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
            }
        }
    }

    private inner class DuckChromeClient : WebChromeClient() {

        override fun onShowFileChooser(
            webView: WebView,
            callback: ValueCallback<Array<Uri>>,
            params: FileChooserParams
        ): Boolean {
            filePathCallback?.onReceiveValue(null)
            filePathCallback = callback
            return try {
                fileChooserLauncher.launch(params.createIntent())
                true
            } catch (e: ActivityNotFoundException) {
                filePathCallback = null
                false
            }
        }

        /** Privacidade por padrao: geolocalizacao negada. */
        override fun onGeolocationPermissionsShowPrompt(
            origin: String,
            callback: GeolocationPermissions.Callback
        ) {
            callback.invoke(origin, false, false)
        }

        /** Camera e microfone negados por padrao (app de privacidade). */
        override fun onPermissionRequest(request: PermissionRequest) {
            request.deny()
        }
    }

    /** Downloads enviados para o gerenciador nativo do Android. */
    private fun downloadFile(url: String, contentDisposition: String?, mimeType: String?) {
        try {
            val name = URLUtil.guessFileName(url, contentDisposition, mimeType)
            val request = DownloadManager.Request(Uri.parse(url)).apply {
                setTitle(name)
                setMimeType(mimeType)
                setNotificationVisibility(
                    DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED
                )
            }
            (getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager)
                .enqueue(request)
            Toast.makeText(this, "Download iniciado", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Falha no download", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openExternally(url: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, "Nenhum aplicativo pode abrir este link", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onPause() {
        webView.onPause()
        super.onPause()
    }

    override fun onResume() {
        webView.onResume()
        super.onResume()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        webView.saveState(outState)
    }

    override fun onDestroy() {
        webView.destroy()
        super.onDestroy()
    }
}
