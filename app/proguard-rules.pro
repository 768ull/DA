# Regras do DuckAssist (nativo).

# Entidades e DAOs do Room (persistência de conversas).
-keep class org.duckassist.app.data.** { *; }

# OkHttp / Okio
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
