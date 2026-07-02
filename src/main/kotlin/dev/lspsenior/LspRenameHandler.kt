package dev.lspsenior

import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.ui.InputValidator
import com.intellij.openapi.ui.Messages
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.refactoring.rename.RenameHandler

/**
 * Renomear (Shift+F6) para símbolos da Linguagem Senior.
 *
 * O rename padrão não funciona em arquivos TextMate (sem PsiNamedElement). Aqui
 * pedimos o novo nome e substituímos as ocorrências direto no texto, reaproveitando a
 * mesma resolução de escopo do "Localizar Usos":
 * - Função: renomeia em todos os arquivos .lsp/.lspt do projeto.
 * - Variável: renomeia apenas dentro do escopo do bloco da declaração, no arquivo atual.
 */
class LspRenameHandler : RenameHandler {

    override fun isAvailableOnDataContext(dataContext: DataContext): Boolean {
        val editor = CommonDataKeys.EDITOR.getData(dataContext) ?: return false
        val file = CommonDataKeys.PSI_FILE.getData(dataContext) ?: return false
        val ext = file.virtualFile?.extension?.lowercase()
        if (ext != "lsp" && ext != "lspt") return false
        return LspSymbols.wordAt(editor.document.charsSequence, editor.caretModel.offset) != null
    }

    override fun isRenaming(dataContext: DataContext): Boolean = isAvailableOnDataContext(dataContext)

    override fun invoke(project: Project, editor: Editor?, file: PsiFile?, dataContext: DataContext?) {
        editor ?: return
        file ?: return
        val text = editor.document.charsSequence
        val offset = editor.caretModel.offset
        val name = LspSymbols.wordAt(text, offset) ?: return

        val isFunction = LspSymbols.isFunctionName(project, name)
        val newName = promptNewName(project, name) ?: return
        if (newName == name) return

        if (isFunction) renameFunction(project, name, newName)
        else renameVariable(project, file, name, offset, newName)
    }

    override fun invoke(project: Project, elements: Array<out PsiElement>, dataContext: DataContext?) {
        // Só suportamos rename a partir do editor.
    }

    private fun promptNewName(project: Project, current: String): String? {
        val validator = object : InputValidator {
            override fun checkInput(input: String): Boolean = LspSymbols.isIdentifier(input.trim())
            override fun canClose(input: String): Boolean = checkInput(input)
        }
        return Messages.showInputDialog(
            project, "Novo nome para '$current':", "Renomear", null, current, validator,
        )?.trim()
    }

    private fun renameVariable(project: Project, file: PsiFile, name: String, offset: Int, newName: String) {
        val document = file.viewProvider.document ?: return
        val scope = LspSymbols.variableScope(document.charsSequence, name, offset)
        val offsets = LspSymbols.occurrences(document.charsSequence, name).filter { it in scope }
        if (offsets.isEmpty()) return
        WriteCommandAction.runWriteCommandAction(project, "Renomear '$name'", null, {
            replaceDescending(document, offsets, name.length, newName)
        }, file)
    }

    private fun renameFunction(project: Project, name: String, newName: String) {
        val fdm = FileDocumentManager.getInstance()
        val perDocument = LinkedHashMap<com.intellij.openapi.editor.Document, List<Int>>()
        ProjectRootManager.getInstance(project).fileIndex.iterateContent { vf ->
            val ext = vf.extension?.lowercase()
            if (ext == "lsp" || ext == "lspt") {
                fdm.getDocument(vf)?.let { doc ->
                    val offs = LspSymbols.occurrences(doc.charsSequence, name)
                    if (offs.isNotEmpty()) perDocument[doc] = offs
                }
            }
            true
        }
        if (perDocument.isEmpty()) return
        WriteCommandAction.runWriteCommandAction(project, "Renomear '$name'", null, {
            for ((doc, offs) in perDocument) replaceDescending(doc, offs, name.length, newName)
        })
    }

    /** Substitui do fim para o início para não invalidar os offsets anteriores. */
    private fun replaceDescending(
        document: com.intellij.openapi.editor.Document,
        offsets: List<Int>,
        oldLength: Int,
        newName: String,
    ) {
        for (off in offsets.sortedDescending()) {
            document.replaceString(off, off + oldLength, newName)
        }
    }
}
