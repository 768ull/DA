# Duck.ai — Aplicativo Nativo Android

Aplicativo que encapsula https://duck.ai/ utilizando o WebView nativo do
próprio Android (motor instalado no sistema), com zoom completamente
bloqueado e estabilidade lateral absoluta.

## Menu de versões

Todas as versões do aplicativo estão publicadas na página
[Lançamentos](https://github.com/768ull/DA/releases) do repositório
(o menu de versões). Cada versão traz o próprio APK de instalação
direta, nomeado sem a palavra debug. A versão nativa em Jetpack Compose
(ramificação `native-compose`) não é publicada no menu de versões; ela
permanece disponível apenas como artefato de compilação na aba Actions.

| Versão | Descrição | Download |
| --- | --- | --- |
| v1.0 | Versão inicial: WebView nativo com zoom totalmente bloqueado em três camadas | [duckai-v1.0.apk](https://github.com/768ull/DA/releases/download/v1.0/duckai-v1.0.apk) |
| v1.1 | Ícone oficial do DuckDuckGo, sem tela de carregamento e abertura mais rápida | [duckai-v1.1.apk](https://github.com/768ull/DA/releases/download/v1.1/duckai-v1.1.apk) |
| v1.2 | Primeira barreira de estabilidade lateral (overscroll desativado) | [duckai-v1.2.apk](https://github.com/768ull/DA/releases/download/v1.2/duckai-v1.2.apk) |
| v1.3 | Trava física da rolagem horizontal; página do Duck.ai intocada | [duckai-v1.3.apk](https://github.com/768ull/DA/releases/download/v1.3/duckai-v1.3.apk) |
| v1.4 | Interceptação de gestos horizontais no nível do componente | [duckai-v1.4.apk](https://github.com/768ull/DA/releases/download/v1.4/duckai-v1.4.apk) |
| v1.5 | Trava absoluta de posição horizontal (versão atual) | [duckai-v1.5.apk](https://github.com/768ull/DA/releases/download/v1.5/duckai-v1.5.apk) |

Observação: para instalar uma versão anterior à já instalada, é
necessário desinstalar o aplicativo atual antes, pois o Android não
permite retornar a um código de versão menor.

## Estabilidade lateral (v1.5) — cinco camadas independentes

O bloqueio da tremedeira horizontal atua em cinco camadas distintas e
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
4. **Zoom impossível** — multitoque e duplo toque são consumidos;
5. **Trava absoluta de posição** — qualquer deslocamento horizontal
   aplicado ao componente é forçado de volta a zero dentro do mesmo
   quadro, venha ele do dedo, do fling ou do próprio motor do
   Chromium (`scrollTo`, `onOverScrolled` e `onScrollChanged`
   rejeitam qualquer coordenada X diferente de zero). A posição
   horizontal permanece zero de forma permanente.

A página do Duck.ai não é modificada em nenhum byte; a rolagem vertical
permanece perfeitamente normal.

## Ícone oficial e desempenho (v1.1)

- **Ícone oficial do DuckDuckGo**: o logotipo vetorial original (pato branco,
  bico amarelo `#FDD209`, gravata verde `#65BC46` e olhos azuis `#2D4F8E`
  sobre o disco laranja `#DE5833`) foi convertido em *adaptive icon*,
  permanecendo nítido em qualquer densidade de tela;
- **Sem tela de carregamento**: o indicador circular de progresso foi removido
  por completo; o aplicativo abre diretamente no WebView e o fundo escuro do
  tema (`#161616`) coincide com o tema do Duck.ai, eliminando qualquer
  piscada ou rotação de espera;
- **Abertura rápida**: o modo de cache `LOAD_CACHE_ELSE_NETWORK` prioriza o
  conteúdo já armazenado, tornando as aberturas seguintes praticamente
  instantâneas.

## Como obter o APK

1. Acesse a página [Lançamentos](https://github.com/768ull/DA/releases)
   (menu de versões) — não é necessário estar autenticado no GitHub;
2. Abra a versão desejada e baixe o arquivo `duckai-vX.Y.apk`;
3. Instale diretamente no aparelho.

Alternativa: na aba **Actions**, abra a execução mais recente do fluxo
**Build APK** da ramificação `main` e baixe o artefato **duckai-apk**.

## Como compilar localmente (Android Studio)

1. Clone o repositório e abra a pasta no Android Studio;
2. Aguarde a sincronização do Gradle (o wrapper baixa o Gradle 8.11.1);
3. *Build > Build App Bundle(s) / APK(s) > Build APK(s)*;
4. O APK fica em `app/build/outputs/apk/`.

## Como assinar o APK de release (compilação local)

O menu de versões distribui o APK de instalação direta. Para gerar uma
versão de produção assinada, compile localmente:

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
