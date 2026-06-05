import java.time.LocalDate

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

val currentYear = LocalDate.now().year

android {
    namespace = "com.rama.mako"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.rama.mako"
        minSdk = 21
        targetSdk = 36
        versionCode = 42
        versionName = "$currentYear.$versionCode"
    }

    flavorDimensions += "version"

    productFlavors {
        create("base") {
            dimension = "version"
        }
        create("ext") {
            dimension = "version"
            applicationIdSuffix = ".ext"
            versionNameSuffix = "-ext"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            vcsInfo.include = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("debug")
        }
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-dev"
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }

    applicationVariants.all {
        outputs.all {
            this as com.android.build.gradle.internal.api.BaseVariantOutputImpl
            outputFileName = "mako_${versionName}.apk"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

    androidResources {
        generateLocaleConfig = true
    }

    packaging {
        resources.excludes += "META-INF/*.version"
        resources.excludes += "META-INF/com/android/build/gradle/app-metadata.properties"
        jniLibs.useLegacyPackaging = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation("androidx.activity:activity-ktx:1.9.3")
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
}
