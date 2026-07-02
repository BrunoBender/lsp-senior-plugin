plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.1.0"
    id("org.jetbrains.intellij.platform") version "2.17.0"
}

group = "dev.lspsenior"
version = "1.4.0"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        intellijIdeaCommunity("2025.3") {
            useInstaller = false
        }
        bundledPlugin("org.jetbrains.plugins.textmate")
        bundledModule("intellij.spellchecker")
        pluginVerifier()
        zipSigner()
    }
}

intellijPlatform {
    instrumentCode = false
    pluginConfiguration {
        ideaVersion {
            sinceBuild = "253"
            untilBuild = provider { null }
        }
    }

    // Assinatura do plugin (opcional). Lê de variáveis de ambiente para não
    // versionar segredos. Veja signing/ (gitignored).
    signing {
        certificateChainFile = file("signing/chain.crt")
        privateKeyFile = file("signing/private.pem")
        password = providers.environmentVariable("PRIVATE_KEY_PASSWORD")
    }

    // Publicação no JetBrains Marketplace via ./gradlew publishPlugin
    publishing {
        token = providers.environmentVariable("PUBLISH_TOKEN")
    }
}

kotlin {
    jvmToolchain(21)
}
