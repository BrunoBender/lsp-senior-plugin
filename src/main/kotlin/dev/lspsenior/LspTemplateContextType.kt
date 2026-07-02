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
        val ext = context.file.virtualFile?.extension?.lowercase()
        return ext == "lsp" || ext == "lspt"
    }
}
