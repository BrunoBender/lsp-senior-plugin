package dev.lspsenior

import com.intellij.lang.ASTNode
import com.intellij.lang.folding.FoldingBuilderEx
import com.intellij.lang.folding.FoldingDescriptor
import com.intellij.openapi.editor.Document
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement

/**
 * Code folding para arquivos .lsp / .lspt.
 *
 * O suporte a TextMate da IDE não fornece folding, então implementamos aqui.
 * Registrado para a linguagem "textmate" mas restrito às nossas extensões via
 * checagem do virtualFile — não afeta outros arquivos TextMate.
 *
 * Dobra blocos delimitados por { } e por Inicio ... Fim (case-insensitive),
 * ignorando ocorrências dentro de strings e comentários.
 */
class LspFoldingBuilder : FoldingBuilderEx() {

    override fun buildFoldRegions(root: PsiElement, document: Document, quick: Boolean): Array<FoldingDescriptor> {
        val ext = root.containingFile?.virtualFile?.extension?.lowercase()
        if (ext != "lsp" && ext != "lspt") return FoldingDescriptor.EMPTY_ARRAY

        val text = document.charsSequence
        val node = root.node ?: return FoldingDescriptor.EMPTY_ARRAY
        val descriptors = mutableListOf<FoldingDescriptor>()

        val braceStack = ArrayDeque<Int>()   // offsets de '{'
        val wordStack = ArrayDeque<Int>()    // offsets de 'inicio'

        var i = 0
        val n = text.length
        while (i < n) {
            val c = text[i]
            when {
                // string "..."
                c == '"' -> {
                    i++
                    while (i < n && text[i] != '"') {
                        if (text[i] == '\\') i++
                        i++
                    }
                    i++
                }
                // comentário de linha @...
                c == '@' -> {
                    while (i < n && text[i] != '\n') i++
                }
                // comentário de bloco /* ... */
                c == '/' && i + 1 < n && text[i + 1] == '*' -> {
                    i += 2
                    while (i + 1 < n && !(text[i] == '*' && text[i + 1] == '/')) i++
                    i += 2
                }
                c == '{' -> { braceStack.addLast(i); i++ }
                c == '}' -> {
                    val open = braceStack.removeLastOrNull()
                    if (open != null) addRegion(descriptors, node, document, open, i + 1)
                    i++
                }
                // palavras inicio / fim (delimitadas por não-letra)
                c.isLetter() -> {
                    val start = i
                    while (i < n && (text[i].isLetterOrDigit() || text[i] == '_')) i++
                    when (text.subSequence(start, i).toString().lowercase()) {
                        "inicio" -> wordStack.addLast(start)
                        "fim" -> {
                            val open = wordStack.removeLastOrNull()
                            if (open != null) addRegion(descriptors, node, document, open, i)
                        }
                    }
                }
                else -> i++
            }
        }
        addBannerRegions(descriptors, node, document, text)
        return descriptors.toTypedArray()
    }

    /**
     * Dobra blocos de comentário de linha `@...` consecutivos (os banners
     * `@****Titulo****@`) numa linha só, com o título como placeholder.
     */
    private fun addBannerRegions(
        out: MutableList<FoldingDescriptor>,
        node: ASTNode,
        document: Document,
        text: CharSequence,
    ) {
        val lineCount = document.lineCount
        var line = 0
        while (line < lineCount) {
            if (!isCommentLine(text, document, line)) { line++; continue }
            val first = line
            while (line + 1 < lineCount && isCommentLine(text, document, line + 1)) line++
            if (line > first) {
                val start = document.getLineStartOffset(first)
                val end = document.getLineEndOffset(line)
                out.add(FoldingDescriptor(node, TextRange(start, end), null, bannerTitle(text, document, first, line)))
            }
            line++
        }
    }

    private fun isCommentLine(text: CharSequence, document: Document, line: Int): Boolean {
        val s = document.getLineStartOffset(line)
        val e = document.getLineEndOffset(line)
        var i = s
        while (i < e && text[i].isWhitespace()) i++
        return i < e && text[i] == '@'
    }

    /** Melhor título do banner: a linha com mais letras, sem @ e *. */
    private fun bannerTitle(text: CharSequence, document: Document, first: Int, last: Int): String {
        var best = ""
        for (line in first..last) {
            val raw = text.subSequence(document.getLineStartOffset(line), document.getLineEndOffset(line)).toString()
            val clean = raw.trim().trim('@').replace('*', ' ').trim()
            if (clean.count { it.isLetter() } > best.count { it.isLetter() }) best = clean
        }
        return if (best.isEmpty()) "@ ... @" else "@ $best @"
    }

    private fun addRegion(
        out: MutableList<FoldingDescriptor>,
        node: ASTNode,
        document: Document,
        startOffset: Int,
        endOffset: Int,
    ) {
        if (endOffset <= startOffset || endOffset > document.textLength) return
        // só dobra se o bloco abrange mais de uma linha
        if (document.getLineNumber(startOffset) == document.getLineNumber(endOffset - 1)) return
        out.add(FoldingDescriptor(node, TextRange(startOffset, endOffset)))
    }

    override fun getPlaceholderText(node: ASTNode): String = "..."

    override fun isCollapsedByDefault(node: ASTNode): Boolean = false
}
