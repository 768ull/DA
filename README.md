# DuckAssist (Android Nativo)

Cliente Android nativo para o [duck.ai](https://duck.ai) — o chat de IA anônimo do DuckDuckGo. Este projeto é uma reescrita completa, em Kotlin e Jetpack Compose, do DuckAssist original (que era um aplicativo WebView com páginas HTML). Não há mais WebView: a conversa, o histórico e as configurações são telas nativas do Android.

## Principais características

- **100% nativo**: interface construída com Jetpack Compose e Material 3. Nenhuma WebView, nenhum HTML embutido, nenhuma injeção de JavaScript.
- **Zoom bloqueado / interface estática**: sem pinch-to-zoom (inexistente em Compose), escala de fonte travada em 100% (mesmo se o usuário aumentar a fonte do sistema) e orientação fixa em retrato.
- **Chat direto com a API do duck.ai**: o aplicativo fala diretamente com o endpoint `duckchat` (token `x-vqd-4` com renovação automática), sem carregar o site.
- **Histórico local**: conversas salvas em banco Room (`duck_chats.db`), com exclusão individual ou em massa.
- **Seletor de modelos**: GPT-4o mini, Claude 3 Haiku, Llama 3.3 70B, Mistral Small 3 e o3-mini, além de modelos personalizados configuráveis.
- **Integração com o sistema**:
  - Assistente padrão (`ACTION_ASSIST`) — pode ser definido como o assistente do aparelho;
  - Texto selecionado (`ACTION_PROCESS_TEXT`);
  - Compartilhamento de texto (`ACTION_SEND`).
- **Configurações persistentes** (DataStore): tema (automático/claro/escuro), modelo padrão, prompt do sistema, sufixo de perguntas, envio automático, modelos personalizados e URL da API.
- **Privacidade**: sem contas, sem rastreio, histórico apenas no dispositivo.

## Estrutura do projeto

```
app/src/main/java/org/duckassist/app/
├── DuckAssistApp.kt        # Application: inicializa Room, DataStore e repositório
├── DuckViewModel.kt        # Estado de UI (Compose + fluxos do Room/DataStore)
├── MainActivity.kt         # Tela principal, trava da escala de fonte (zoom)
├── AskActivity.kt          # Assistente / texto selecionado / compartilhar
├── data/
│   ├── DuckApi.kt          # Cliente HTTP nativo da API do duck.ai (x-vqd-4)
│   ├── ChatRepository.kt   # Orquestração: token, Room e chamadas à API
│   ├── ChatDatabase.kt    # Entidades, DAO e banco Room (duck_chats.db)
│   ├── SettingsStore.kt    # Preferências em DataStore (duck_assist_prefs)
│   └── Models.kt          # Modelos de IA padrão e personalizados
└── ui/
    ├── AppRoot.kt          # Navegação entre telas
    ├── ChatScreen.kt       # Tela de conversa (bolhas, entrada, seletor de modelo)
    ├── HistoryScreen.kt    # Histórico de conversas
    ├── SettingsScreen.kt   # Configurações
    └── theme/Theme.kt      # Tema Material 3 (laranja DuckDuckGo)
```

## Como compilar

### Android Studio
1. Clone o repositório: `git clone https://github.com/768ull/DA.git`
2. Abra a pasta no Android Studio (Hedgehog ou mais recente, com JDK 17).
3. Aguarde a sincronização do Gradle e execute `app` em um dispositivo ou emulador (Android 8.0 / API 26 ou mais recente).

### Linha de comando
```bash
# Na raiz do projeto (requer Gradle 8.7+ e JDK 17)
gradle wrapper          # gera o wrapper, se necessário
./gradlew assembleDebug
# APK gerado em app/build/outputs/apk/debug/app-debug.apk
```

### GitHub Actions
O repositório inclui o workflow `.github/workflows/android.yml`, que compila o APK de debug a cada push e publica como artefato — basta abrir a aba **Actions**, baixar o artefato `duckassist-debug-apk` e instalar no aparelho.

## Sobre a API do duck.ai

O duck.ai não possui API pública oficial. Este cliente usa o mesmo protocolo interno do site:

1. `GET {base}/duckchat/v1/status` com o cabeçalho `x-vqd-accept: 1` → devolve o token `x-vqd-4`;
2. `POST {base}/duckchat/v1/chat` com o token `x-vqd-4` e o corpo `{"model": "...", "messages": [{"role": "...", "content": "..."}]}`;
3. A resposta traz o texto em `message` (ou em fluxo SSE) e um novo token para a mensagem seguinte.

Como o protocolo é não oficial, ele pode mudar. Nesse caso:
- Ajuste a **URL base da API** e os **IDs de modelos personalizados** em Configurações (sem recompilar);
- Se necessário, atualize `DuckApi.kt`.

O uso está sujeito aos Termos de Serviço do DuckDuckGo; o aplicativo não contorna limites de taxa.

## Diferenças em relação ao DuckAssist original (WebView)

| Recurso | Original (WebView) | Esta versão (nativa) |
|---|---|---|
| Interface | Site duck.ai + scripts injetados | Compose/Material 3 puro |
| Zoom | Controlado pelo WebView | Bloqueado por construção |
| Histórico | Dump de IndexedDB do site | Room no dispositivo |
| Configurações | HTML em WebView | Tela nativa DataStore |
| Conversor de documentos | WASM em WebView | *Planejado (roadmap)* |
| Assistente/voz | VoiceInteractionService | Assistente por intent *(voz no roadmap)* |
| Gerenciador de scripts | Userscripts no WebView | Não aplicável (sem WebView) |

## Roadmap

- [ ] Resposta em streaming (SSE) com efeito de digitação
- [ ] Conversor de documentos (anexos) em Kotlin
- [ ] Entrada por voz (SpeechRecognizer) e resposta falada (TTS)
- [ ] Widget de busca rápida

## Créditos e licença

- Reescrita nativa mantida por [768ull](https://github.com/768ull).
- Projeto original DuckAssist (diekjiu) — wrapper WebView do duck.ai.
- Projeto licenciado sob a [MIT License](LICENSE). O nome e os serviços do DuckDuckGo pertencem à DuckDuckGo LLC; este projeto não possui vínculo oficial com a DuckDuckGo.
