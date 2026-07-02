package dev.lspsenior

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.ui.JBColor
import java.awt.Color
import java.awt.Font

/**
 * Colore de VERMELHO as variáveis reservadas da Linguagem Senior (NomEmp, DatSis,
 * Web_HTML...) em arquivos .lsp / .lspt.
 *
 * Por que um annotator em vez da gramática TextMate: o scope `variable.language` não
 * recebe cor no tema padrão do IntelliJ, então as reservadas ficavam sem destaque.
 * Aqui usamos `enforcedTextAttributes`, que aplica a cor independentemente do tema.
 *
 * O reconhecimento é case-insensitive (AnoHoj == anohoj): comparamos cada identificador
 * em minúsculas contra [LspReservedVariables.lowerSet]. Strings e comentários são
 * ignorados, e acessos a membro (`obj.NomEmp`) não são coloridos.
 */
class LspReservedVariableAnnotator : Annotator {

    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        // Varre o arquivo inteiro uma única vez, a partir do elemento raiz (PsiFile).
        if (element !is PsiFile) return
        val ext = element.virtualFile?.extension?.lowercase()
        if (ext != "lsp" && ext != "lspt") return

        val text = element.text
        val reserved = LspReservedVariables.lowerSet
        if (reserved.isEmpty()) return

        var i = 0
        val n = text.length
        while (i < n) {
            val c = text[i]
            when {
                c == '"' -> { i++; while (i < n && text[i] != '"') { if (text[i] == '\\') i++; i++ }; i++ }
                c == '@' -> { while (i < n && text[i] != '\n') i++ }
                c == '/' && i + 1 < n && text[i + 1] == '*' -> {
                    i += 2; while (i + 1 < n && !(text[i] == '*' && text[i + 1] == '/')) i++; i += 2
                }
                c.isLetter() || c == '_' -> {
                    val start = i
                    while (i < n && (text[i].isLetterOrDigit() || text[i] == '_')) i++
                    val memberAccess = start > 0 && text[start - 1] == '.'
                    if (!memberAccess && text.subSequence(start, i).toString().lowercase() in reserved) {
                        holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
                            .range(TextRange(start, i))
                            .enforcedTextAttributes(RESERVED_ATTRS)
                            .create()
                    }
                }
                else -> i++
            }
        }
    }

    private companion object {
        // Vermelho legível em temas claros e escuros.
        val RESERVED_COLOR = JBColor(Color(0xCC, 0x00, 0x00), Color(0xFF, 0x6B, 0x68))
        val RESERVED_ATTRS = TextAttributes(RESERVED_COLOR, null, null, null, Font.PLAIN)
    }
}
