package dev.lspsenior

import com.intellij.openapi.util.TextRange
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceBase
import com.intellij.psi.PsiReferenceContributor
import com.intellij.psi.PsiReferenceProvider
import com.intellij.psi.PsiReferenceRegistrar
import com.intellij.util.ProcessingContext

/**
 * Cria referências (Ctrl/Cmd+Click, Cmd+B) para identificadores da Linguagem Senior.
 *
 * Diferente de um GotoDeclarationHandler, cada referência carrega o RANGE exato do
 * identificador — então só a palavra vira link, não o nó inteiro do TextMate (que
 * pode abranger grandes trechos sem token).
 *
 * Resolução:
 * - Função: implementação "Funcao nome()" em qualquer arquivo (índice global).
 * - Variável: declaração "Definir [Tipo] nome" local ao arquivo e no escopo do bloco.
 */
class LspReferenceContributor : PsiReferenceContributor() {
    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        registrar.registerReferenceProvider(PlatformPatterns.psiElement(), LspReferenceProvider())
    }
}

private class LspReferenceProvider : PsiReferenceProvider() {
    private val identifier = Regex("[A-Za-z_][A-Za-z0-9_]*")

    override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<PsiReference> {
        val ext = element.containingFile?.virtualFile?.extension?.lowercase()
        if (ext != "lsp" && ext != "lspt") return PsiReference.EMPTY_ARRAY

        val text = element.text
        if (text.isEmpty()) return PsiReference.EMPTY_ARRAY

        val refs = ArrayList<PsiReference>()
        for (match in identifier.findAll(text)) {
            val start = match.range.first
            if (start > 0 && text[start - 1] == '.') continue // acesso a membro: logws.metodo()
            refs.add(LspReference(element, TextRange(start, match.range.last + 1)))
        }
        return if (refs.isEmpty()) PsiReference.EMPTY_ARRAY else refs.toTypedArray()
    }
}

private class LspReference(element: PsiElement, range: TextRange) :
    PsiReferenceBase<PsiElement>(element, range) {

    override fun resolve(): PsiElement? {
        val name = value
        val project = element.project
        val file = element.containingFile ?: return null
        val usageOffset = element.textRange.startOffset + rangeInElement.startOffset

        // 1) Função: implementação global. Se já estou na própria implementação,
        // não resolve (evita "navegar para si mesmo"); para ver usos, use o menu.
        val fnTargets = LspSymbols.functionTargets(project, name)
        if (fnTargets.isNotEmpty()) {
            val onImpl = fnTargets.any {
                it.targetFile.virtualFile == file.virtualFile && it.targetOffset == usageOffset
            }
            return if (onImpl) null else fnTargets.first()
        }

        // 2) Variável: local + escopo de bloco. Na própria declaração, não resolve.
        val text = file.viewProvider.contents
        val declOffset = LspSymbols.resolveVariableDecl(text, name, usageOffset)?.first ?: return null
        return if (declOffset == usageOffset) null else LspNavTarget(file, declOffset, name)
    }

    override fun getVariants(): Array<Any> = emptyArray()
}
