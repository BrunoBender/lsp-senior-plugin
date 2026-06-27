package dev.lspsenior

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.usageView.UsageInfo
import com.intellij.usages.Usage
import com.intellij.usages.UsageInfo2UsageAdapter
import com.intellij.usages.UsageTarget
import com.intellij.usages.UsageViewManager
import com.intellij.usages.UsageViewPresentation

/**
 * "Localizar Usos" próprio da Linguagem Senior (menu de contexto do editor).
 *
 * Substitui o Find Usages padrão, que não habilita em arquivos TextMate por não
 * encontrar um símbolo no cursor. Aqui obtemos a palavra direto do editor e
 * exibimos os resultados na janela de Usos.
 *
 * - Função: usos em todo o projeto (.lsp/.lspt).
 * - Variável: usos no arquivo atual, dentro do escopo de bloco da declaração.
 */
class LspFindUsagesAction : AnAction() {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR)
        val ext = e.getData(CommonDataKeys.PSI_FILE)?.virtualFile?.extension?.lowercase()
        e.presentation.isEnabledAndVisible = editor != null && (ext == "lsp" || ext == "lspt")
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val psiFile = e.getData(CommonDataKeys.PSI_FILE) ?: return

        val text = editor.document.charsSequence
        val offset = editor.caretModel.offset
        val name = LspSymbols.wordAt(text, offset) ?: return

        val infos =
            if (LspSymbols.isFunctionName(project, name)) functionUsages(project, name)
            else variableUsages(psiFile, text, name, offset)

        if (infos.isEmpty()) return

        val usages: Array<Usage> = infos.map { UsageInfo2UsageAdapter(it) }.toTypedArray()
        val presentation = UsageViewPresentation().apply {
            tabText = "Usos de '$name'"
            searchString = name
            isOpenInNewTab = true
        }
        UsageViewManager.getInstance(project).showUsages(UsageTarget.EMPTY_ARRAY, usages, presentation)
    }

    private fun functionUsages(project: Project, name: String): List<UsageInfo> {
        val infos = ArrayList<UsageInfo>()
        val psiManager = PsiManager.getInstance(project)
        ProjectRootManager.getInstance(project).fileIndex.iterateContent { vf ->
            val ext = vf.extension?.lowercase()
            if (ext == "lsp" || ext == "lspt") {
                psiManager.findFile(vf)?.let { pf ->
                    val content = pf.viewProvider.contents
                    for (off in LspSymbols.occurrences(content, name)) {
                        infos.add(UsageInfo(pf, off, off + name.length, false))
                    }
                }
            }
            true
        }
        return infos
    }

    private fun variableUsages(file: PsiFile, text: CharSequence, name: String, offset: Int): List<UsageInfo> {
        val decl = LspSymbols.resolveVariableDecl(text, name, offset)
        val start = decl?.second ?: 0
        val end = if (decl != null) LspSymbols.enclosingBlockEnd(text, decl.second) else text.length
        val infos = ArrayList<UsageInfo>()
        for (off in LspSymbols.occurrences(text, name)) {
            if (off in start..end) infos.add(UsageInfo(file, off, off + name.length, false))
        }
        return infos
    }
}
