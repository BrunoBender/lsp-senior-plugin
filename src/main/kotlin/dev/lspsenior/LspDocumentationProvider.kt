package dev.lspsenior

import com.intellij.lang.documentation.AbstractDocumentationProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.impl.FakePsiElement
import com.intellij.xml.util.XmlStringUtil

/**
 * Documentação (Ctrl+Q / preview do autocomplete) para as funções nativas da Linguagem
 * Senior. Mostra a assinatura completa (com os tipos de cada parâmetro) e, quando houver,
 * a descrição oficial — ajudando o usuário a saber quais variáveis informar.
 *
 * Como o TextMate não tem PSI real, a doc é resolvida a partir do item do autocomplete
 * (o objeto do lookup é o nome da função), envolvido num [FakePsiElement] leve.
 */
class LspDocumentationProvider : AbstractDocumentationProvider() {

    override fun getDocumentationElementForLookupItem(
        psiManager: PsiManager,
        obj: Any?,
        element: PsiElement?,
    ): PsiElement? {
        val name = obj as? String ?: return null
        val fn = LspNativeFunctions.byName(name) ?: return null
        val context = element ?: return null
        return NativeFnDocElement(context, fn)
    }

    override fun getQuickNavigateInfo(element: PsiElement?, originalElement: PsiElement?): String? =
        (element as? NativeFnDocElement)?.fn?.let { signature(it) }

    override fun generateDoc(element: PsiElement?, originalElement: PsiElement?): String? {
        val fn = (element as? NativeFnDocElement)?.fn ?: return null
        val sig = XmlStringUtil.escapeString(signature(fn))
        return buildString {
            append("<pre>").append(sig).append("</pre>")
            if (!fn.description.isNullOrBlank()) {
                append("<p>").append(XmlStringUtil.escapeString(fn.description)).append("</p>")
            }
            append("<p><small>Função nativa da Linguagem Senior</small></p>")
        }
    }

    private fun signature(fn: LspNativeFunctions.NativeFn): String = "${fn.name}(${fn.params})"
}

/** Elemento leve que carrega a função nativa para a geração da documentação. */
private class NativeFnDocElement(
    private val context: PsiElement,
    val fn: LspNativeFunctions.NativeFn,
) : FakePsiElement() {
    override fun getParent(): PsiElement = context
    override fun getName(): String = fn.name
    override fun getPresentableText(): String = "${fn.name}(${fn.params})"
}
