# Duck.ai — Aplicativo Nativo Android

Aplicativo que encapsula https://duck.ai/ utilizando o WebView nativo do
próprio Android (motor instalado no sistema), com zoom completamente
bloqueado por meio de três camadas:

1. **WebSettings**: `setSupportZoom(false)`, `builtInZoomControls(false)` e
   `displayZoomControls(false)` — desativa pinch, duplo toque e controles;
2. **Interceptação de multitoque**: eventos com dois ou mais ponteiros são
   descartados antes de chegarem ao WebView;
3. **GestureDetector**: o duplo toque é consumido e não produz zoom.

## Como obter o APK (sem instalar nada)

1. Acesse a aba **Actions** deste repositório;
2. Abra a execução mais recente do fluxo **Build APK**;
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
