import java.io.ByteArrayOutputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.gms.google.services)
}

// local.properties 파일 읽기
val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(localPropertiesFile.inputStream())
}

android {
    namespace = "com.example.promptly"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.promptly"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // BuildConfig에 API Key 추가
        // buildConfigField("String", "OPENAI_API_KEY", "\"${localProperties["OPENAI_API_KEY"] ?: ""}\"")

        // Gemini API Key
        buildConfigField("String", "GEMINI_API_KEY", "\"${localProperties["GEMINI_API_KEY"] ?: ""}\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    // BuildConfig 기능 활성화
    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}


dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.firebase.firestore)
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}


// SHA-1 지문을 로그에 출력하는 커스텀 태스크 (keytool 오류 우회용)
tasks.register("getDebugSha") {
    // Android Studio의 keytool 에러를 우회하여 SHA1 지문을 추출하는 기능
    val debugSigningConfig = android.signingConfigs.getByName("debug")

    doLast {
        println("==========================================================")
        println("Key Alias: ${debugSigningConfig.keyAlias}")

        // 키 저장소 파일 객체 가져오기
        val storeFile = debugSigningConfig.storeFile

        if (storeFile != null && storeFile.exists()) {

            // keytool 실행 및 출력 캡처
            val stdout = ByteArrayOutputStream()
            exec {
                commandLine("keytool",
                    "-list",
                    "-v",
                    "-keystore", storeFile.absolutePath,
                    "-alias", debugSigningConfig.keyAlias,
                    "-storepass", debugSigningConfig.storePassword,
                    "-keypass", debugSigningConfig.keyPassword
                )
                standardOutput = stdout
                isIgnoreExitValue = true // keytool이 오류 코드를 뱉어도 Gradle은 무시하고 진행
            }

            // 출력된 내용에서 SHA1 값만 추출 (Regex 사용)
            val output = stdout.toString()
            val sha1Matcher = "SHA1: ([0-9A-F:]+)".toRegex().find(output)

            val sha1 = sha1Matcher?.groups?.get(1)?.value ?: "NOT FOUND"

            println("✅ SHA1 FINGERPRINT (DEBUG): $sha1")
            println("==========================================================")
        } else {
            println("Error: debug.keystore not found at ${storeFile?.absolutePath ?: "UNKNOWN PATH"}")
        }
    }
}