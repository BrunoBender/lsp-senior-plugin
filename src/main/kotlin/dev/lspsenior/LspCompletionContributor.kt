package dev.lspsenior

import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.InsertHandler
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.icons.AllIcons
import com.intellij.util.indexing.FileBasedIndex

/**
 * Autocompletar para arquivos .lsp / .lspt.
 *
 * Como o TextMate não tem PSI estrutural, controlamos tudo manualmente: extraímos o
 * prefixo do texto antes do cursor e oferecemos palavras-chave, tipos, booleanos, as
 * funções do projeto (via [LspFunctionIndex]) e as variáveis visíveis no escopo
 * (via [LspSymbols.visibleVariables]).
 */
class LspCompletionContributor : CompletionContributor() {

    override fun fillCompletionVariants(parameters: CompletionParameters, result: CompletionResultSet) {
        val file = parameters.originalFile
        val ext = file.virtualFile?.extension?.lowercase()
        if (ext != "lsp" && ext != "lspt") return

        val text = parameters.editor.document.charsSequence
        val offset = parameters.offset

        var start = offset
        while (start > 0 && isIdent(text[start - 1])) start--
        // Acesso a membro (logws.metodo) — não completar nomes globais.
        if (start > 0 && text[start - 1] == '.') return

        val prefix = text.subSequence(start, offset).toString()
        val rs = result.withPrefixMatcher(prefix)
        // Evita a duplicata "(texto)": suprime a word-completion embutida que a IDE
        // aplica em arquivos "plain-text-like" (TextMate), sugerindo palavras do arquivo.
        result.stopHere()

        for (kw in KEYWORDS) {
            rs.addElement(LookupElementBuilder.create(kw).bold().withIcon(AllIcons.Nodes.Favorite))
        }
        for (type in TYPES) {
            rs.addElement(LookupElementBuilder.create(type).withIcon(AllIcons.Nodes.Type).withTypeText("tipo"))
        }
        for (b in BOOLEANS) {
            rs.addElement(LookupElementBuilder.create(b).withIcon(AllIcons.Nodes.Constant).withTypeText("booleano"))
        }

        val project = file.project
        try {
            for (fn in FileBasedIndex.getInstance().getAllKeys(LspFunctionIndex.NAME, project)) {
                rs.addElement(
                    LookupElementBuilder.create(fn)
                        .withIcon(AllIcons.Nodes.Function)
                        .withTypeText("função")
                        .withInsertHandler(PARENS),
                )
            }
        } catch (_: Exception) {
            // índice indisponível (ex.: durante indexação) — segue sem funções
        }

        for (variable in LspSymbols.visibleVariables(text, offset)) {
            rs.addElement(LookupElementBuilder.create(variable).withIcon(AllIcons.Nodes.Variable).withTypeText("variável"))
        }

        for (native in LspNativeFunctions.names) {
            rs.addElement(
                LookupElementBuilder.create(native)
                    .withIcon(AllIcons.Nodes.Function)
                    .withTypeText("nativa")
                    .withInsertHandler(PARENS),
            )
        }
    }

    private fun isIdent(c: Char) = c.isLetterOrDigit() || c == '_'

    private companion object {
        val KEYWORDS = listOf(
            "Se", "Senao", "Enquanto", "Para", "Regra", "VaPara", "Continue", "Pare",
            "Retorna", "Retornar", "Cancelar", "Inicio", "Fim", "Definir", "Funcao",
            "IniciarTransacao", "FinalizarTransacao", "DesfazerTransacao", "e", "ou", "nao",
        )
        val TYPES = listOf(
            "Alfa", "Numero", "Data", "Lista", "Cursor", "Tabela", "Booleano",
            "Flutuante", "Inteiro", "Decimal", "Blob", "Logico",
        )
        val BOOLEANS = listOf("cVerdadeiro", "cFalso")

        /** Insere "()" após a função e posiciona o cursor entre os parênteses. */
        val PARENS = InsertHandler<LookupElement> { context: InsertionContext, _ ->
            val editor = context.editor
            val tail = context.document.charsSequence
            val already = context.tailOffset < tail.length && tail[context.tailOffset] == '('
            if (!already) {
                context.document.insertString(context.tailOffset, "()")
            }
            editor.caretModel.moveToOffset(context.tailOffset + 1)
        }
    }
}
