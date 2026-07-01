package dev.lspsenior

import com.intellij.lang.Language
import com.intellij.psi.PsiElement
import com.intellij.ui.breadcrumbs.BreadcrumbsProvider

/**
 * Breadcrumbs (o caminho de blocos exibido no topo do editor) para arquivos .lsp / .lspt.
 *
 * O suporte a TextMate da IDE não constrói uma árvore PSI aninhada (cada token é filho
 * direto do arquivo), então breadcrumbs não funcionam "de graça" como em linguagens com
 * parser real (ex.: JSON, cujo exemplo motivou este recurso). Por isso reconstruímos o
 * aninhamento via varredura textual dos blocos `{ }` e `Inicio`/`Fim` em [LspBlocks],
 * igual à técnica já usada em [LspFoldingBuilder] e [LspSymbols].
 *
 * Registrado para a linguagem "textmate" mas restrito às nossas extensões via checagem
 * do virtualFile — não afeta outros arquivos TextMate.
 */
class LspBreadcrumbsProvider : BreadcrumbsProvider {

    override fun getLanguages(): Array<Language> =
        Language.findLanguageByID("textmate")?.let { arrayOf(it) } ?: emptyArray()

    override fun acceptElement(e: PsiElement): Boolean = isLspFile(e) && LspBlocks.isBlockOpener(e)

    override fun getElementInfo(e: PsiElement): String = LspBlocks.headerLabel(e)

    override fun getParent(e: PsiElement): PsiElement? = if (isLspFile(e)) LspBlocks.enclosingOpener(e) else null

    private fun isLspFile(e: PsiElement): Boolean {
        val ext = e.containingFile?.virtualFile?.extension?.lowercase()
        return ext == "lsp" || ext == "lspt"
    }
}

/**
 * Reconstrói o aninhamento de blocos `{ }` / `Inicio`...`Fim` a partir do texto puro,
 * ignorando strings e comentários. Usado apenas pelos breadcrumbs.
 */
private object LspBlocks {

    private const val MAX_LABEL_LENGTH = 60

    /** True se o caractere/palavra no início de [e] abre um bloco (`{` ou `Inicio`/`Início`). */
    fun isBlockOpener(e: PsiElement): Boolean {
        val text = e.containingFile?.viewProvider?.contents ?: return false
        val offset = e.textRange.startOffset
        if (offset >= text.length) return false
        if (text[offset] == '{') return true
        return startsWithWord(text, offset, "inicio") || startsWithWord(text, offset, "início")
    }

    /** Elemento de abertura do bloco que envolve [e] (o próximo nível acima), ou null na raiz. */
    fun enclosingOpener(e: PsiElement): PsiElement? {
        val file = e.containingFile ?: return null
        val text = file.viewProvider.contents
        val parentOffset = scan(text, e.textRange.startOffset).opens.lastOrNull() ?: return null
        return file.findElementAt(parentOffset)
    }

    /** Rótulo do breadcrumb: o texto (normalizado) entre o limite de instrução anterior e [e]. */
    fun headerLabel(e: PsiElement): String {
        val file = e.containingFile ?: return "Bloco"
        val text = file.viewProvider.contents
        val offset = e.textRange.startOffset
        val boundary = scan(text, offset).lastBoundary
        val raw = if (boundary < offset) text.subSequence(boundary, offset) else ""
        return normalize(raw).ifEmpty { "Bloco" }
    }

    private class ScanState(val opens: List<Int>, val lastBoundary: Int)

    /**
     * Varredura única de [text] do início até [offset] (exclusive), mantendo:
     * - a pilha de aberturas (`{` / `Inicio`) ainda não fechadas nessa posição;
     * - o offset logo após o último limite de instrução (`;`, `{`, `}`, `Inicio`, `Fim`
     *   ou comentário), usado para extrair o texto do cabeçalho de um bloco.
     * Ignora conteúdo de strings e comentários.
     */
    private fun scan(text: CharSequence, offset: Int): ScanState {
        val opens = ArrayList<Int>()
        var lastBoundary = 0
        var i = 0
        val n = minOf(offset, text.length)
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
                c == '{' -> { opens.add(i); i++; lastBoundary = i }
                c == '}' -> { opens.removeLastOrNull(); i++; lastBoundary = i }
                c == ';' -> { i++; lastBoundary = i }
                c.isLetter() -> {
                    val start = i
                    while (i < n && (text[i].isLetterOrDigit() || text[i] == '_')) i++
                    when (text.subSequence(start, i).toString().lowercase()) {
                        "inicio", "início" -> { opens.add(start); lastBoundary = i }
                        "fim" -> { opens.removeLastOrNull(); lastBoundary = i }
                    }
                }
                else -> i++
            }
        }
        return ScanState(opens, lastBoundary)
    }

    private fun isIdentChar(c: Char) = c.isLetterOrDigit() || c == '_'

    private fun startsWithWord(text: CharSequence, offset: Int, word: String): Boolean {
        val end = offset + word.length
        if (end > text.length) return false
        if (!text.subSequence(offset, end).toString().equals(word, ignoreCase = true)) return false
        if (offset > 0 && isIdentChar(text[offset - 1])) return false
        if (end < text.length && isIdentChar(text[end])) return false
        return true
    }

    private fun normalize(raw: CharSequence): String {
        val collapsed = raw.toString().replace(Regex("\\s+"), " ").trim()
        if (collapsed.isEmpty()) return ""
        return if (collapsed.length > MAX_LABEL_LENGTH) collapsed.take(MAX_LABEL_LENGTH - 1) + "…" else collapsed
    }
}
