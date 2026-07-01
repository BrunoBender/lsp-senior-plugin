package dev.lspsenior

import com.intellij.codeInsight.breadcrumbs.FileBreadcrumbsCollector
import com.intellij.openapi.Disposable
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
 * Por que um FileBreadcrumbsCollector e não apenas um [LspBreadcrumbsProvider]:
 * o parser PSI do TextMate (`TextMateParserDefinition`) usa um `EmptyLexer`, então o
 * arquivo inteiro vira **um único leaf PSI** (offset 0 até o fim). O coletor padrão da
 * IDE navega por PSI (`findElementAt` + `getParent`) e, com um só elemento, nunca enxerga
 * o aninhamento de blocos — por isso o provider baseado em PSI não exibia nada.
 *
 * Aqui reconstruímos o caminho de blocos `{ }` / `Inicio`...`Fim` direto do texto,
 * ignorando strings e comentários — mesma técnica já usada em [LspFoldingBuilder].
 * A visibilidade do painel é satisfeita pelo stub [LspBreadcrumbsProvider] (mantemos o
 * `requiresProvider` padrão para não tocar em API interna da IDE).
 */
class LspBreadcrumbsCollector : FileBreadcrumbsCollector() {

    override fun handlesFile(file: VirtualFile): Boolean {
        val ext = file.extension?.lowercase()
        return ext == "lsp" || ext == "lspt"
    }

    override fun watchForChanges(file: VirtualFile, editor: Editor, disposable: Disposable, changesHandler: Runnable) {
        editor.caretModel.addCaretListener(object : CaretListener {
            override fun caretPositionChanged(event: CaretEvent) = changesHandler.run()
        }, disposable)
        editor.document.addDocumentListener(object : DocumentListener {
            override fun documentChanged(event: DocumentEvent) = changesHandler.run()
        }, disposable)
    }

    override fun computeCrumbs(
        file: VirtualFile,
        document: Document,
        offset: Int,
        forcedShown: Boolean?,
    ): Iterable<Crumb> {
        val text = document.immutableCharSequence
        val labels = LspBlockPath.enclosingBlocks(text, offset.coerceIn(0, text.length))
        return labels.map { Crumb.Impl(null as Icon?, it, null as String?) }
    }
}

/**
 * Reconstrói, a partir do texto puro, a pilha de blocos `{ }` / `Inicio`...`Fim` que
 * envolve um dado [offset], ignorando conteúdo de strings e comentários.
 *
 * Retorna os rótulos do bloco mais externo ao mais interno. O rótulo de um bloco é o
 * texto (normalizado) entre o limite de instrução anterior e a abertura do bloco — por
 * exemplo, `se salarioBase < 3000` para o `{` correspondente, ou `funcao Nome(params)`.
 */
private object LspBlockPath {

    private const val MAX_LABEL_LENGTH = 60

    fun enclosingBlocks(text: CharSequence, offset: Int): List<String> {
        val stack = ArrayList<String>()
        var lastBoundary = 0
        var i = 0
        val n = min(offset, text.length)
        while (i < n) {
            val c = text[i]
            when {
                c == '"' -> {
                    i++
                    while (i < n && text[i] != '"') {
                        if (text[i] == '\\') i++
                        i++
                    }
                    i++
                }
                c == '@' -> {
                    while (i < n && text[i] != '\n') i++
                    lastBoundary = i
                }
                c == '/' && i + 1 < n && text[i + 1] == '*' -> {
                    i += 2
                    while (i + 1 < n && !(text[i] == '*' && text[i + 1] == '/')) i++
                    i += 2
                    lastBoundary = i
                }
                c == '{' -> {
                    stack.add(label(text, lastBoundary, i, "Bloco"))
                    i++
                    lastBoundary = i
                }
                c == '}' -> {
                    stack.removeLastOrNull()
                    i++
                    lastBoundary = i
                }
                c == ';' -> {
                    i++
                    lastBoundary = i
                }
                c.isLetter() -> {
                    val start = i
                    while (i < n && (text[i].isLetterOrDigit() || text[i] == '_')) i++
                    when (text.subSequence(start, i).toString().lowercase()) {
                        "inicio", "início" -> stack.add(label(text, lastBoundary, start, "inicio"))
                        "fim" -> stack.removeLastOrNull()
                    }
                    lastBoundary = i
                }
                else -> i++
            }
        }
        return stack
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
