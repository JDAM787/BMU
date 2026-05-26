plugins {
    alias(libs.plugins.android.application)
}

val natives by configurations.creating
val gdxVersion: String by project

android {
    namespace = "com.example.bmu"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.bmu"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    // LibGDX core y Box2D
    implementation("com.badlogicgames.gdx:gdx:$gdxVersion")
    implementation("com.badlogicgames.gdx:gdx-box2d:$gdxVersion")

    // Backend Android
    implementation("com.badlogicgames.gdx:gdx-backend-android:$gdxVersion")

    // Natives
    natives("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-armeabi-v7a")
    natives("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-arm64-v8a")
    natives("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-x86")
    natives("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-x86_64")

    // Box2D Natives
    natives("com.badlogicgames.gdx:gdx-box2d-platform:$gdxVersion:natives-armeabi-v7a")
    natives("com.badlogicgames.gdx:gdx-box2d-platform:$gdxVersion:natives-arm64-v8a")
    natives("com.badlogicgames.gdx:gdx-box2d-platform:$gdxVersion:natives-x86")
    natives("com.badlogicgames.gdx:gdx-box2d-platform:$gdxVersion:natives-x86_64")

    implementation(libs.activity.ktx)
    implementation(libs.appcompat)
    implementation(libs.constraintlayout)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.ext.junit)
}

tasks.register<Copy>("copyAndroidNatives") {
    doFirst {
        delete("src/main/jniLibs/armeabi-v7a")
        delete("src/main/jniLibs/arm64-v8a")
        delete("src/main/jniLibs/x86")
        delete("src/main/jniLibs/x86_64")
    }
    configurations.getByName("natives").files.forEach { jar ->
        val outputDir = when {
            jar.name.contains("armeabi-v7a") -> file("src/main/jniLibs/armeabi-v7a")
            jar.name.contains("arm64-v8a") -> file("src/main/jniLibs/arm64-v8a")
            jar.name.contains("x86_64") -> file("src/main/jniLibs/x86_64")
            jar.name.contains("x86") -> file("src/main/jniLibs/x86")
            else -> null
        }
        outputDir?.let {
            copy {
                from(zipTree(jar))
                into(it)
                include("*.so")
            }
        }
    }
}

tasks.named("preBuild") {
    dependsOn("copyAndroidNatives")
}
