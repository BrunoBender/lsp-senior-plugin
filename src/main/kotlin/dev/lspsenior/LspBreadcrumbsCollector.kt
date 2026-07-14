package dev.lspsenior

import com.intellij.codeInsight.breadcrumbs.FileBreadcrumbsCollector
import com.intellij.openapi.Disposable
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.event.CaretEvent
import com.intellij.openapi.editor.event.CaretListener
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.components.breadcrumbs.Crumb
import javax.swing.Icon
import kotlin.math.min

/**
 * Coletor de breadcrumbs (caminho de blocos no topo do editor) para arquivos .lsp / .lspt.
 *
 * Por que um FileBreadcrumbsCollector: o parser PSI do TextMate (`TextMateParserDefinition`)
 * usa um `EmptyLexer`, então o arquivo inteiro vira **um único leaf PSI** (offset 0 até o
 * fim). Os coletores padrão da IDE navegam por PSI e, com um só elemento, nunca enxergam o
 * aninhamento de blocos. Aqui reconstruímos os blocos `{ }` / `Inicio`...`Fim` direto do
 * texto, ignorando strings e comentários — mesma técnica de [LspFoldingBuilder].
 *
 * Usa apenas API pública ([computeCrumbs]). O método interno `computeStickyLineInfos`
 * (sticky lines) NÃO é sobrescrito, pois dependia de API `@ApiStatus.Internal` que o
 * Marketplace da JetBrains proíbe. Os breadcrumbs suprem a mesma necessidade.
 */
class LspBreadcrumbsCollector : FileBreadcrumbsCollector() {

    private val log = logger<LspBreadcrumbsCollector>()

    override fun handlesFile(file: VirtualFile): Boolean {
        val ext = file.extension?.lowercase()
        return ext == "lsp" || ext == "lspt"
    }

    override fun watchForChanges(file: VirtualFile, editor: Editor, disposable: Disposable, changesHandler: Runnable) {
        editor.caretModel.addCaretListener(object : CaretListener {
            override fun caretPositionChanged(event: CaretEvent) {
                changesHandler.run()
            }
        }, disposable)
        editor.document.addDocumentListener(object : DocumentListener {
            override fun documentChanged(event: DocumentEvent) {
                changesHandler.run()
            }
        }, disposable)
    }

    override fun computeCrumbs(
        file: VirtualFile,
        document: Document,
        offset: Int,
        forcedShown: Boolean?,
    ): Iterable<Crumb> {
        return try {
            val text = document.immutableCharSequence
            LspBlocks.enclosingBlocks(text, offset.coerceIn(0, text.length))
                .map { Crumb.Impl(null as Icon?, it, null as String?) }
        } catch (e: Exception) {
            log.warn("LSP breadcrumbs: computeCrumbs falhou", e)
            emptyList()
        }
    }
}

/**
 * Varredura textual dos blocos `{ }` / `Inicio`...`Fim`, ignorando strings e comentários.
 */
private object LspBlocks {

    private const val MAX_LABEL_LENGTH = 60

    /** Pilha de rótulos dos blocos que envolvem [offset] (mais externo -> mais interno). */
    fun enclosingBlocks(text: CharSequence, offset: Int): List<String> {
        val stack = ArrayList<String>()
        scan(text, offset, onOpen = { label -> stack.add(label) }, onClose = { stack.removeLastOrNull() })
        return stack
    }

    /**
     * Varredura única de [text] até [n]. Chama [onOpen] (rótulo) ao abrir um bloco e
     * [onClose] ao fechar.
     */
    private inline fun scan(
        text: CharSequence,
        n: Int,
        onOpen: (label: String) -> Unit,
        onClose: () -> Unit,
    ) {
        val end = min(n, text.length)
        var lastBoundary = 0
        var i = 0
        while (i < end) {
            val c = text[i]
            when {
                c == '"' -> {
                    i++
                    while (i < end && text[i] != '"') {
                        if (text[i] == '\\') i++
                        i++
                    }
                    i++
                }
                c == '@' -> {
                    while (i < end && text[i] != '\n') i++
                    lastBoundary = i
                }
                c == '/' && i + 1 < end && text[i + 1] == '*' -> {
                    i += 2
                    while (i + 1 < end && !(text[i] == '*' && text[i + 1] == '/')) i++
                    i += 2
                    lastBoundary = i
                }
                c == '{' -> {
                    onOpen(label(text, lastBoundary, i, "Bloco"))
                    i++
                    lastBoundary = i
                }
                c == '}' -> {
                    i++
                    onClose()
                    lastBoundary = i
                }
                c == ';' -> {
                    i++
                    lastBoundary = i
                }
                c.isLetter() -> {
                    val start = i
                    while (i < end && (text[i].isLetterOrDigit() || text[i] == '_')) i++
                    // Só `inicio`/`fim` são limites de instrução; identificadores comuns NÃO
                    // movem o lastBoundary, senão o rótulo perderia o começo da instrução.
                    when (text.subSequence(start, i).toString().lowercase()) {
                        "inicio", "início" -> {
                            onOpen(label(text, lastBoundary, start, "inicio"))
                            lastBoundary = i
                        }
                        "fim" -> {
                            onClose()
                            lastBoundary = i
                        }
                    }
                }
                else -> i++
            }
        }
    }

    private fun label(text: CharSequence, from: Int, to: Int, fallback: String): String {
        val raw = if (from in 0 until to) text.subSequence(from, to) else ""
        return normalize(raw).ifEmpty { fallback }
    }

    private fun normalize(raw: CharSequence): String {
        val collapsed = raw.toString().replace(Regex("\\s+"), " ").trim()
        if (collapsed.isEmpty()) return ""
        return if (collapsed.length > MAX_LABEL_LENGTH) collapsed.take(MAX_LABEL_LENGTH - 1) + "…" else collapsed
    }
}
