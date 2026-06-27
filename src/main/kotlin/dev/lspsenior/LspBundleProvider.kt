package dev.lspsenior

import com.intellij.openapi.application.PathManager
import org.jetbrains.plugins.textmate.api.TextMateBundleProvider
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

/**
 * Registra o bundle TextMate (estilo VSCode) embutido no plugin, sem necessidade de
 * importação manual. Fornece colorização (grammar), brackets/comentários
 * (language-configuration) e snippets para arquivos .lsp / .lspt.
 *
 * O reader do TextMate exige um diretório real no disco, então extraímos os
 * recursos do classpath para o diretório de sistema da IDE na primeira chamada.
 */
class LspBundleProvider : TextMateBundleProvider {

    override fun getBundles(): List<TextMateBundleProvider.PluginBundle> {
        val dir = extractBundle()
        return listOf(TextMateBundleProvider.PluginBundle("LSP Senior", dir))
    }

    private fun extractBundle(): Path {
        val target = PathManager.getSystemDir().resolve("lsp-senior-textmate-bundle")
        BUNDLE_FILES.forEach { rel ->
            val out = target.resolve(rel)
            Files.createDirectories(out.parent)
            javaClass.getResourceAsStream("/$BUNDLE_ROOT/$rel")?.use { input ->
                Files.copy(input, out, StandardCopyOption.REPLACE_EXISTING)
            } ?: error("Recurso do bundle não encontrado: /$BUNDLE_ROOT/$rel")
        }
        return target
    }

    private companion object {
        const val BUNDLE_ROOT = "textmate-bundle"
        val BUNDLE_FILES = listOf(
            "package.json",
            "language-configuration.json",
            "syntaxes/lsp.tmLanguage.json",
            "snippets/lsp.json",
        )
    }
}
