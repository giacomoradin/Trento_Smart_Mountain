import java.util.Properties

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.android)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.ksp)
}

val localProperties = Properties().apply {
  val f = rootProject.file("local.properties")
  if (f.exists()) f.inputStream().use { load(it) }
}

android {
  namespace = "it.trentosmartmountain.app"
  compileSdk = libs.versions.compileSdk.get().toInt()

  defaultConfig {
    applicationId = "it.trentosmartmountain.app"
    minSdk = libs.versions.minSdk.get().toInt()
    targetSdk = libs.versions.targetSdk.get().toInt()
    versionCode = 1
    versionName = "1.0"
    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

    // Priorità:
    //   1) gradle -PTSM_API_BASE_URL=https://...  (CI / build script)
    //   2) mobile/local.properties → tsm.api.baseUrl=https://...  (dev locale)
    //   3) default produzione su Render
    // Per puntare al server locale durante dev, aggiungi a `mobile/local.properties`:
    //   tsm.api.baseUrl=http://10.0.2.2:3000/
    val baseUrl =
      (project.findProperty("TSM_API_BASE_URL") as String?)
        ?: localProperties.getProperty("tsm.api.baseUrl")
        ?: "https://trento-smart-mountain-xz7u.onrender.com/"
    buildConfigField("String", "BASE_URL", "\"${baseUrl.trimEnd('/')}/\"")
  }

  buildTypes {
    release {
      // R8/minify + shrink risorse ATTIVI. Storia (regressione sync/picker 2026-06):
      // con R8 i DTO Gson PRIVI di @SerializedName venivano offuscati (campi a/b/c…)
      // → `CreateActivityRequest` rifiutata dal backend (422 → attività mai sincro)
      // e parsing sentieri null ("Errore imprevisto" nel picker). Ora `proguard-
      // rules.pro` tiene l'INTERO package `data.remote.dto.**` (+ Gson/Retrofit/
      // @SerializedName/TypeToken), quindi la serializzazione è preservata.
      // ⚠️ Da validare on-device con lo smoke-test (sync attività + picker percorsi
      // + scan SOS i flussi a più alto rischio): se uno fallisce in release ma non
      // in debug, manca una keep-rule per quel DTO → aggiungerla qui o rimettere OFF.
      isMinifyEnabled = true
      isShrinkResources = true
      // Firma con la debug keystore così la release minificata è installabile su
      // device per il test on-device di R8. ⚠️ Prima di una pubblicazione reale
      // (Play Store / distribuzione esterna) sostituire con una upload-key dedicata.
      signingConfig = signingConfigs.getByName("debug")
      proguardFiles(
        getDefaultProguardFile("proguard-android-optimize.txt"),
        "proguard-rules.pro",
      )
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }
  kotlinOptions {
    jvmTarget = "17"
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
}

dependencies {
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.activity.compose)
  implementation(platform(libs.androidx.compose.bom))
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.compose.material3)
  implementation("androidx.compose.material:material-icons-extended")
  implementation(libs.androidx.navigation.compose)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.retrofit)
  implementation(libs.retrofit.gson)
  implementation(libs.okhttp.logging)
  implementation(libs.gson)
  implementation(libs.androidx.security.crypto)
  implementation(libs.androidx.room.runtime)
  implementation(libs.androidx.room.ktx)
  ksp(libs.androidx.room.compiler)
  implementation(libs.zxing.core)
  implementation(libs.osmdroid.android)
  implementation(libs.play.services.location)
  implementation(libs.reorderable)
  implementation(libs.androidx.exifinterface)
  implementation(libs.androidx.work.runtime.ktx)

  // ── Unit testing (JVM, src/test) ─────────────────────────────────────────
  // Primo step della test strategy mobile (vedi piano Fase 0): partiamo dalla
  // logica pura (util di sampling, formattazione tempo). I test su ViewModel
  // con coroutine/mock arriveranno nell'iterazione successiva.
  testImplementation("junit:junit:4.13.2")

  debugImplementation("androidx.compose.ui:ui-tooling")
  debugImplementation("androidx.compose.ui:ui-test-manifest")
}
