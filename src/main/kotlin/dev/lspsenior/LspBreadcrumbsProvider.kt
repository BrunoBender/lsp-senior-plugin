package dev.lspsenior

import com.intellij.lang.Language
import com.intellij.psi.PsiElement
import com.intellij.ui.breadcrumbs.BreadcrumbsProvider

/**
 * Stub de BreadcrumbsProvider para a linguagem "textmate".
 *
 * O cálculo real dos breadcrumbs dos arquivos .lsp / .lspt é feito por
 * [LspBreadcrumbsCollector] (baseado em texto), porque o PSI do TextMate é um único
 * leaf e não permite navegação por elementos. Este provider existe apenas para que a
 * IDE liste "TextMate" nas configurações de breadcrumbs e o recurso apareça habilitado
 * por padrão; seus métodos de navegação não são usados (o collector tem
 * `requiresProvider = false`).
 */
class LspBreadcrumbsProvider : BreadcrumbsProvider {

    override fun getLanguages(): Array<Language> =
        Language.findLanguageByID("textmate")?.let { arrayOf(it) } ?: emptyArray()

    override fun acceptElement(e: PsiElement): Boolean = false

    override fun getElementInfo(e: PsiElement): String = ""
}
