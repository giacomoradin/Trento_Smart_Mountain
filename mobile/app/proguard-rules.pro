# ───────────────────────────────────────────────────────────────────────────
# TSM — regole R8/ProGuard
#
# NB: in `build.gradle.kts` la minificazione release è attualmente DISATTIVA
# (vedi commento lì): questi keep sono comunque mantenuti e completi così che
# riattivare R8 non rompa di nuovo la serializzazione Gson (regressione sync/
# picker del 2026-06, causata da DTO senza @SerializedName offuscati da R8).
# ───────────────────────────────────────────────────────────────────────────

# ── Attributi necessari a Gson (riflessione su tipi generici/annotazioni) ──
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes EnclosingMethod
-keepattributes InnerClasses

# ── OkHttp / Okio / Retrofit ──
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**
-dontwarn javax.annotation.**
# Mantieni le interfacce Retrofit e le firme dei metodi (Response<T>, @Query, …).
-keep,allowobfuscation interface it.trentosmartmountain.app.data.remote.TsmApiService
-keepclassmembers,allowshrinking,allowobfuscation interface it.trentosmartmountain.app.data.remote.** {
  @retrofit2.http.* <methods>;
}

# ── Gson ──
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken
-keep public class * implements java.lang.reflect.Type
# Campi annotati con @SerializedName: mantieni nome ESATTO (no rinomina/rimozione).
-keepclassmembers,allowobfuscation class * {
  @com.google.gson.annotations.SerializedName <fields>;
}

# ── DTO / modelli serializzati con Gson ──
# I nomi dei campi DEVONO restare invariati: Gson li usa via riflessione.
# Teniamo l'intero package dto + le classi serializzate fuori da esso.
-keep class it.trentosmartmountain.app.data.remote.dto.** { *; }
-keepclassmembers class it.trentosmartmountain.app.data.remote.dto.** { *; }
# Payload JWT, refresh/login, snapshot checklist (serializzati altrove).
-keep class it.trentosmartmountain.app.data.remote.JwtDecoder$* { *; }
-keep class it.trentosmartmountain.app.data.checklist.ChecklistPersonalStore$* { *; }

# ── Kotlin metadata / data class ──
-keep class kotlin.Metadata { *; }
-keepclassmembers class kotlin.Metadata { *; }
