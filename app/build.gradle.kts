import com.android.build.api.artifact.ArtifactTransformationRequest
import com.android.build.api.artifact.SingleArtifact
import com.android.build.gradle.internal.PropertiesValueSource
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.io.StringReader
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.about.libraries.android)
    alias(libs.plugins.android.junit5)
}

android {
    namespace = "com.flxrs.dankchat"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.flxrs.dankchat"
        minSdk = 30
        targetSdk = 35
        versionCode = 31111
        versionName = "3.11.11"
    }

    androidResources {
        generateLocaleConfig = true
    }

    val localProperties = gradleLocalProperties(rootDir, providers)
    signingConfigs {
        create("release") {
            storeFile = file("keystore/DankChat.jks").takeIf { it.exists() } ?: File(System.getProperty("user.home") + "/dankchat/DankChat.jks")
            storePassword = localProperties.getProperty("SIGNING_STORE_PASSWORD") ?: System.getenv("SIGNING_STORE_PASSWORD")
            keyAlias = localProperties.getProperty("SIGNING_KEY_ALIAS") ?: System.getenv("SIGNING_KEY_ALIAS")
            keyPassword = localProperties.getProperty("SIGNING_KEY_PASSWORD") ?: System.getenv("SIGNING_KEY_PASSWORD")
        }
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "META-INF/versions/9/previous-compilation-data.bin"
        }
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            manifestPlaceholders["applicationLabel"] = "@string/app_name"
            signingConfig = signingConfigs.getByName("release")
        }
        getByName("debug") {
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            manifestPlaceholders["applicationLabel"] = "@string/app_name"
        }
        create("dank") {
            initWith(getByName("release"))
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            manifestPlaceholders["applicationLabel"] = "@string/app_name_dank"
            applicationIdSuffix = ".dank"
            isDefault = true
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    androidComponents.onVariants { variant ->
        val renameTask = tasks.register<RenameApkTask>("renameApk${variant.name.replaceFirstChar { it.uppercase() }}") {
            apkName.set("DankChat-${variant.name}.apk")
        }
        val transformationRequest = variant.artifacts.use(renameTask)
            .wiredWithDirectories(RenameApkTask::inputDirs, RenameApkTask::outputDirs)
            .toTransformMany(SingleArtifact.APK)
        renameTask.configure {
            this.transformationRequest = transformationRequest
        }
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    lint {
        disable += "RestrictedApi"
        disable += "UnusedResources"
        disable += "ObsoleteSdkInt"
    }
}

ksp {
    arg("room.schemaLocation", "${layout.projectDirectory}/schemas")
    arg("KOIN_CONFIG_CHECK", "true")
    arg("KOIN_DEFAULT_MODULE", "false")
    arg("KOIN_USE_COMPOSE_VIEWMODEL", "true")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(jdkVersion = 21)
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
        freeCompilerArgs.addAll(
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
            "-opt-in=kotlinx.coroutines.FlowPreview",
            "-opt-in=kotlinx.serialization.ExperimentalSerializationApi",
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3ExpressiveApi",
            "-opt-in=androidx.compose.foundation.ExperimentalFoundationApi",
            "-opt-in=androidx.compose.foundation.layout.ExperimentalLayoutApi",
            "-opt-in=kotlin.uuid.ExperimentalUuidApi",
            "-opt-in=kotlin.time.ExperimentalTime",
            "-Xnon-local-break-continue",
            "-Xwhen-guards",
        )
    }
}

dependencies {
// D8 desugaring
    coreLibraryDesugaring(libs.android.desugar.libs)

// Kotlin
    implementation(libs.kotlin.stdlib)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.serialization.json.okio)
    implementation(libs.kotlinx.datetime)
    implementation(libs.kotlinx.immutable.collections)

// AndroidX
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.browser)
    implementation(libs.androidx.emoji2)
    implementation(libs.androidx.exifinterface)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.media)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.webkit)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.datastore.core)
    implementation(libs.androidx.datastore.android)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

// Compose
    implementation(libs.compose.animation)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.runtime)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)
    "dankImplementation"(libs.compose.ui.tooling)
    implementation(libs.compose.icons.core)
    implementation(libs.compose.icons.extended)
    implementation(libs.compose.unstyled)
    implementation(libs.compose.material3.adaptive)

// Material
    implementation(libs.android.material)
    implementation(libs.android.flexbox)

// Dependency injection
    implementation(platform(libs.koin.bom))
    implementation(libs.koin.core)
    implementation(libs.koin.android)
    implementation(libs.koin.compose)
    implementation(libs.koin.compose.viewmodel)
    implementation(libs.koin.annotations)
    implementation(libs.koin.ksp.compiler)
    ksp(libs.koin.ksp.compiler)

// Image loading
    implementation(libs.coil)
    implementation(libs.coil.gif)
    implementation(libs.coil.ktor)
    implementation(libs.coil.cache.control)
    implementation(libs.coil.compose)

// HTTP clients
    implementation(libs.okhttp)
    implementation(libs.okhttp.sse)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.logging)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.client.websockets)
    implementation(libs.ktor.serialization.kotlinx.json)

// Other
    implementation(libs.colorpicker.android)
    implementation(libs.process.phoenix)
    implementation(libs.autolinktext)
    implementation(libs.aboutlibraries.compose.m3)
    implementation(libs.reorderable)

// Test
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.ktor.client.okhttp)
    testImplementation(libs.ktor.client.websockets)
    testImplementation(libs.okhttp.mockwebserver)
}

junitPlatform {
    filters {
        if (!project.hasProperty("includeIntegration")) {
            excludeTags("integration")
        }
    }
}

fun gradleLocalProperties(projectRootDir: File, providers: ProviderFactory): Properties {
    val properties = Properties()
    val propertiesContent =
        providers.of(PropertiesValueSource::class.java) {
            parameters.projectRoot.set(projectRootDir)
        }.get()

    StringReader(propertiesContent).use { reader ->
        properties.load(reader)
    }

    return properties
}

abstract class RenameApkTask : DefaultTask() {
    @get:InputDirectory
    abstract val inputDirs: DirectoryProperty

    @get:OutputDirectory
    abstract val outputDirs: DirectoryProperty

    @get:Input
    abstract val apkName: Property<String>

    @get:Internal
    lateinit var transformationRequest: ArtifactTransformationRequest<RenameApkTask>

    @TaskAction
    fun taskAction() {
        transformationRequest.submit(this) { builtArtifact ->
            val inputFile = File(builtArtifact.outputFile)
            val outputFile = File(outputDirs.get().asFile, apkName.get())
            inputFile.copyTo(outputFile, overwrite = true)
            outputFile
        }
    }
}
