# Duck.ai Nativo — Interface 100% Nativa (Jetpack Compose)

Este ramo (`native-compose`) contém o segundo aplicativo da dupla:

| Ramo | Aplicativo | Interface |
|---|---|---|
| `main` | Duck.ai (WebView, v1.3.1) | Página original do Duck.ai no WebView do sistema |
| `native-compose` | Duck.ai Nativo (este) | Interface construída em Jetpack Compose, sem página web visível |

## Arquitetura

1. **Interface nativa total**: Material 3 com o tema oficial do Duck.ai
   (`#161616` de fundo, `#DE5833` de destaque), balões de conversa,
   seletor de modelos e transmissão token a token em tempo real. Como a
   interface é nativa, não existe movimento horizontal por construção;
2. **Sessão invisível**: o Duck.ai protege o serviço com um desafio de
   prova de trabalho (`X-Vqd-Hash-1`) resolvido por JavaScript de
   navegador. O aplicativo carrega o duck.ai em uma WebView invisível de
   1 pixel apenas durante a inicialização da sessão; um gancho
   JavaScript observa os cabeçalhos das requisições internas e captura o
   token gerado pela própria página. A WebView é destruída em seguida;
3. **Conversa nativa**: as mensagens trafegam por OkHttp diretamente ao
   endpoint `/duckchat/v1/chat`, com respostas em streaming SSE
   renderizadas token a token pela interface Compose;
4. **Renovação automática**: ao receber o erro `ERR_CHALLENGE` (418),
   o aplicativo renova a sessão invisível e repete a pergunta.

## Correções da v1.1

- Importação ausente de `TextButton` que impedia a compilação do
  aplicativo (erro localizado em auditoria do código);
- Versão explícita de `material-icons-core` (1.7.8), eliminando
  dependência da tabela do BOM;
- Robustez da sessão invisível: falhas de criação e anexação da WebView
  retornam mensagens claras em vez de fechamentos inesperados;
- **Diagnóstico de falhas integrado**: qualquer fechamento inesperado é
  registrado internamente. Ao reabrir o aplicativo, um painel exibe o
  motivo exato (rastreamento completo) com o botão "Copiar log", para
  envio ao desenvolvedor e correção precisa.

## Modelos disponíveis

- GPT-5.4 Mini (`gpt-5.4-mini`)
- GPT-5.4 Nano (`gpt-5.4-nano`)
- Claude Haiku 4.5 (`claude-haiku-4-5`)
- Mistral Small (`mistral-small-2603`)
- GPT-OSS 120B (`tinfoil/gpt-oss-120b`)

## Avisos importantes

- O protocolo é **não oficial** e pode quebrar quando o Duck.ai mudar
  sua proteção. O aplicativo do ramo `main` (WebView) permanece a
  versão estável da dupla;
- Funções avançadas da página (geração de imagens, busca web, upload de
  arquivos, tema claro) não estão nesta versão;
- Conversas não são sincronizadas com a conta do navegador: são
  locais e voláteis.

## Como obter o APK

1. Acesse a aba **Actions** e selecione o fluxo **Build APK** da ramificação
   `native-compose`;
2. Abra a execução mais recente **com marcação verde** (concluída com sucesso);
3. Baixe o artefato **duckai-native-apk** na seção *Artifacts*;
4. O pacote contém `app-debug.apk` (instalação direta) e
   `app-release-unsigned.apk` (para assinatura e publicação).

## Como compilar localmente (Android Studio)

1. Faça o checkout da ramificação `native-compose`;
2. Abra a pasta no Android Studio e aguarde a sincronização do Gradle;
3. *Build > Build App Bundle(s) / APK(s) > Build APK(s)*.

## Notas de privacidade

- Nenhum dado é armazenado além da conversa em memória;
- Apenas HTTPS; sem rastreadores; sem permissões além de INTERNET;
- Ícone oficial do DuckDuckGo em vetor (adaptive icon).

## Estrutura

```
app/src/main/java/com/duckai/nativecompose/MainActivity.kt     (interface Compose e diagnóstico)
app/src/main/java/com/duckai/nativecompose/DuckChatClient.kt   (protocolo de conversa)
app/src/main/java/com/duckai/nativecompose/SessionHarvester.kt  (sessão invisível)
app/src/main/AndroidManifest.xml
app/src/main/res/...
.github/workflows/build-apk.yml
```
