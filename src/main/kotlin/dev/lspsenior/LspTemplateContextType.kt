package dev.lspsenior

import com.intellij.codeInsight.template.TemplateActionContext
import com.intellij.codeInsight.template.TemplateContextType

/**
 * Contexto dos Live Templates da Linguagem Senior: ativa os templates (cursor, perm,
 * audit, etc.) apenas em arquivos .lsp / .lspt. O id "LSP" casa com o `<option name="LSP">`
 * de cada template em liveTemplates/LSP.xml.
 */
class LspTemplateContextType : TemplateContextType("LSP") {

    override fun isInContext(context: TemplateActionContext): Boolean {
        // Usa o NOME do arquivo (PsiFile.getName), não o virtualFile: na checagem de
        // contexto o platform passa uma cópia não-física do PsiFile cujo getVirtualFile()
        // é null — o que fazia isInContext retornar false e os templates nunca aparecerem.
        val name = context.file.name.lowercase()
        return name.endsWith(".lsp") || name.endsWith(".lspt")
    }
}
