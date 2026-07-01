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
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.components.breadcrumbs.Crumb
import com.intellij.ui.components.breadcrumbs.StickyLineInfo
import javax.swing.Icon
import kotlin.math.min

/**
 * Coletor de breadcrumbs + sticky lines para arquivos .lsp / .lspt.
 *
 * Por que um FileBreadcrumbsCollector e não apenas um [LspBreadcrumbsProvider]:
 * o parser PSI do TextMate (`TextMateParserDefinition`) usa um `EmptyLexer`, então o
 * arquivo inteiro vira **um único leaf PSI** (offset 0 até o fim). Os coletores padrão da
 * IDE (breadcrumbs e sticky lines) navegam por PSI e, com um só elemento, nunca enxergam
 * o aninhamento de blocos. Aqui reconstruímos os blocos `{ }` / `Inicio`...`Fim` direto do
 * texto, ignorando strings e comentários — mesma técnica de [LspFoldingBuilder].
 *
 * - [computeCrumbs]: caminho de blocos da posição do cursor (barra de breadcrumbs).
 * - [computeStickyLineInfos]: escopos de bloco (linhas fixas / sticky scroll). É este que
 *   produz o comportamento de "cabeçalhos grudando no topo" conforme se rola o arquivo.
 *   `StickyLinesLanguageSupport` habilita a linguagem via [LspBreadcrumbsProvider].
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
            val labels = LspBlocks.enclosingBlocks(text, offset.coerceIn(0, text.length))
            labels.map { Crumb.Impl(null as Icon?, it, null as String?) }
        } catch (e: Exception) {
            log.warn("LSP breadcrumbs: computeCrumbs falhou", e)
            emptyList()
        }
    }

    /**
     * A IDE chama este método uma vez POR LINHA do arquivo. Varrer o texto inteiro a cada
     * chamada seria O(N²) e trava/cancela o pass em arquivos grandes (era a causa de as
     * sticky lines não aparecerem). Cacheamos a lista completa de escopos por versão do
     * documento: a varredura O(N) roda uma vez e as demais chamadas são O(1).
     */
    override fun computeStickyLineInfos(
        file: VirtualFile,
        document: Document,
        offset: Int,
    ): List<StickyLineInfo> {
        return try {
            val stamp = document.modificationStamp
            document.getUserData(STICKY_CACHE)?.let { if (it.stamp == stamp) return it.infos }
            val text = document.immutableCharSequence
            val infos = LspBlocks.allScopes(text, text.length)
                .map { StickyLineInfo(it.headerStart, it.endOffset, it.label) }
            document.putUserData(STICKY_CACHE, StickyCache(stamp, infos))
            infos
        } catch (e: Exception) {
            log.warn("LSP sticky lines: computeStickyLineInfos falhou", e)
            emptyList()
        }
    }

    private class StickyCache(val stamp: Long, val infos: List<StickyLineInfo>)

    private companion object {
        val STICKY_CACHE = Key.create<StickyCache>("lsp.sticky.lines.cache")
    }
}

/** Um escopo de bloco: linha do cabeçalho (`se ... {`) e onde ele fecha. */
private data class LspScope(val headerStart: Int, val endOffset: Int, val label: String)

/**
 * Varredura textual dos blocos `{ }` / `Inicio`...`Fim`, ignorando strings e comentários.
 */
private object LspBlocks {

    private const val MAX_LABEL_LENGTH = 60

    /** Pilha de rótulos dos blocos que envolvem [offset] (mais externo -> mais interno). */
    fun enclosingBlocks(text: CharSequence, offset: Int): List<String> {
        val stack = ArrayList<String>()
        scan(text, offset, onOpen = { _, label -> stack.add(label) }, onClose = { stack.removeLastOrNull() })
        return stack
    }

    /** Todos os escopos de bloco cujo fechamento ocorre dentro de [limit] (para sticky lines). */
    fun allScopes(text: CharSequence, limit: Int): List<LspScope> {
        val open = ArrayList<Pair<Int, String>>() // (headerStart, label)
        val result = ArrayList<LspScope>()
        val end = min(limit, text.length)
        scan(
            text, end,
            onOpen = { headerStart, label -> open.add(headerStart to label) },
            onClose = { closeOffset ->
                open.removeLastOrNull()?.let { (headerStart, label) ->
                    result.add(LspScope(headerStart, closeOffset, label))
                }
            },
        )
        // Blocos ainda abertos no fim da varredura: fecham no limite.
        while (open.isNotEmpty()) {
            val (headerStart, label) = open.removeLast()
            result.add(LspScope(headerStart, end, label))
        }
        return result
    }

    /**
     * Varredura única de [text] até [n]. Chama [onOpen] (headerStart, label) ao abrir um
     * bloco e [onClose] (offset logo após o fechamento) ao fechar.
     */
    private inline fun scan(
        text: CharSequence,
        n: Int,
        onOpen: (headerStart: Int, label: String) -> Unit,
        onClose: (closeOffset: Int) -> Unit,
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
                    onOpen(firstNonWs(text, lastBoundary, i), label(text, lastBoundary, i, "Bloco"))
                    i++
                    lastBoundary = i
                }
                c == '}' -> {
                    i++
                    onClose(i)
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
                            onOpen(firstNonWs(text, lastBoundary, start), label(text, lastBoundary, start, "inicio"))
                            lastBoundary = i
                        }
                        "fim" -> {
                            onClose(i)
                            lastBoundary = i
                        }
                    }
                }
                else -> i++
            }
        }
    }

    /** Primeiro caractere não-branco em [from, to) — início visível da instrução. */
    private fun firstNonWs(text: CharSequence, from: Int, to: Int): Int {
        var j = from.coerceAtLeast(0)
        while (j < to && text[j].isWhitespace()) j++
        return if (j < to) j else from.coerceIn(0, text.length)
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
