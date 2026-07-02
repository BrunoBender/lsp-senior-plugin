package dev.lspsenior

import com.intellij.codeInsight.highlighting.HighlightUsagesHandlerBase
import com.intellij.codeInsight.highlighting.HighlightUsagesHandlerFactory
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.util.Consumer

/**
 * Realça as ocorrências do identificador sob o cursor em arquivos .lsp / .lspt
 * (o realce automático quando o cursor para sobre um símbolo).
 *
 * - Função: ocorrências no arquivo atual.
 * - Variável: ocorrências dentro do escopo do bloco da declaração.
 */
class LspHighlightUsagesHandlerFactory : HighlightUsagesHandlerFactory {

    override fun createHighlightUsagesHandler(editor: Editor, file: PsiFile): HighlightUsagesHandlerBase<*>? {
        val ext = file.virtualFile?.extension?.lowercase()
        if (ext != "lsp" && ext != "lspt") return null

        val text = editor.document.charsSequence
        val offset = editor.caretModel.offset
        val name = LspSymbols.wordAt(text, offset) ?: return null

        val scope =
            if (LspSymbols.isFunctionName(file.project, name)) 0..text.length
            else LspSymbols.variableScope(text, name, offset)

        return LspHighlightUsagesHandler(editor, file, name, scope)
    }
}

private class LspHighlightUsagesHandler(
    editor: Editor,
    file: PsiFile,
    private val name: String,
    private val scope: IntRange,
) : HighlightUsagesHandlerBase<PsiElement>(editor, file) {

    override fun getTargets(): List<PsiElement> = listOf(myFile)

    override fun selectTargets(targets: List<PsiElement>, selectionConsumer: Consumer<in List<PsiElement>>) {
        selectionConsumer.consume(targets)
    }

    override fun computeUsages(targets: List<PsiElement>) {
        val text = myEditor.document.charsSequence
        for (off in LspSymbols.occurrences(text, name)) {
            if (off in scope) myReadUsages.add(TextRange(off, off + name.length))
        }
    }
}
