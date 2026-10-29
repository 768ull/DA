# Duck.ai — Aplicativo Nativo Android

Aplicativo que encapsula https://duck.ai/ utilizando o WebView nativo do
próprio Android (motor instalado no sistema), com zoom completamente
bloqueado e estabilidade lateral absoluta.

## Estabilidade lateral (v1.4) — quatro camadas independentes

O bloqueio da tremedeira horizontal atua em quatro camadas distintas e
complementares, conforme as técnicas consagradas de controle de
rolagem em WebView:

1. **Overscroll desativado** (`OVER_SCROLL_NEVER`) e barras de
   rolagem ocultas — elimina o efeito elástico de esticamento do
   Android 12+;
2. **Trava física** — `computeHorizontalScrollRange` é declarado
   igual a `computeHorizontalScrollExtent`: a área rolável horizontal
   é exatamente a área visível, portanto não existe espaço lateral
   para rolar;
3. **Interceptação de gesto** — arrastes dominados pela horizontal são
   consumidos no nível do componente e nunca chegam ao motor de
   rolagem; um arraste que se inicie vertical continua rolando
   normalmente;
4. **Zoom impossível** — multitoque e duplo toque são consumidos.

A página do Duck.ai não é modificada em nenhum byte; a rolagem
vertical permanece perfeitamente normal.

## Ícone oficial e desempenho (v1.1)

- **Ícone oficial do DuckDuckGo**: o logotipo vetorial original (pato branco,
  bico amarelo `#FDD209`, gravata verde `#65BC46` e olhos azuis `#2D4F8E`
  sobre o disco laranja `#DE5833`) foi convertido em *adaptive icon*,
  permanecendo nítido em qualquer densidade de tela;
- **Sem tela de carregamento**: o indicador circular de progresso foi removido
  por completo; o aplicativo abre diretamente no WebView e o fundo escuro do
  tema (`#161616`) coincide com o tema do Duck.ai, eliminando qualquer
  piscada ou rotacao de espera;
- **Abertura rápida**: o modo de cache `LOAD_CACHE_ELSE_NETWORK` prioriza o
  conteúdo já armazenado, tornando as aberturas seguintes praticamente
  instantâneas.

## Como obter o APK (sem instalar nada)

1. Acesse a aba **Actions** deste repositório;
2. Abra a execução mais recente do fluxo **Build APK** da ramificação `main`;
3. Baixe o artefato **duckai-apk** na seção *Artifacts*;
4. O pacote contém `app-debug.apk` (instalação direta) e
   `app-release-unsigned.apk` (para assinatura e publicação).

## Como compilar localmente (Android Studio)

1. Clone o repositório e abra a pasta no Android Studio;
2. Aguarde a sincronização do Gradle (o wrapper baixa o Gradle 8.11.1);
3. *Build > Build App Bundle(s) / APK(s) > Build APK(s)*;
4. O APK fica em `app/build/outputs/apk/`.

## Como assinar o APK de release

```bash
gradle assembleRelease
keytool -genkeypair -v -keystore duckai.keystore -alias duckai \
    -keyalg RSA -keysize 2048 -validity 10000
zipalign -p 4 app/build/outputs/apk/release/app-release-unsigned.apk \
    app-release-aligned.apk
apksigner sign --ks duckai.keystore --out duckai.apk \
    app-release-aligned.apk
```

## Notas de privacidade

- Geolocalização, câmera e microfone são negados por padrão;
- Apenas HTTPS; tráfego HTTP é automaticamente convertido ou bloqueado;
- Links externos ao domínio `duck.ai` abrem no navegador, fora do app.

## Estrutura

```
app/src/main/java/com/duckai/nativeapp/MainActivity.kt
app/src/main/AndroidManifest.xml
app/src/main/res/...
.github/workflows/build-apk.yml
```
