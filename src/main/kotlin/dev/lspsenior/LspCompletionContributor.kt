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
 * funções do projeto (via [LspFunctionIndex]), as funções nativas com assinatura
 * (via [LspNativeFunctions]), as variáveis visíveis no escopo com o seu tipo
 * (via [LspSymbols.visibleVariablesWithType]) e as variáveis reservadas
 * (via [LspReservedVariables]).
 *
 * Funções inserem "()" e, quando têm parâmetros, o cursor entra entre eles para que o
 * usuário informe os argumentos; a assinatura aparece ao lado do nome na lista.
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
                        .withInsertHandler(PARENS_INSIDE),
                )
            }
        } catch (_: Exception) {
            // índice indisponível (ex.: durante indexação) — segue sem funções
        }

        // Variáveis declaradas no escopo, exibindo o tipo (Alfa, Numero, Data...).
        for (variable in LspSymbols.visibleVariablesWithType(text, offset)) {
            rs.addElement(
                LookupElementBuilder.create(variable.name)
                    .withIcon(AllIcons.Nodes.Variable)
                    .withTypeText(variable.type ?: "variável"),
            )
        }

        // Variáveis reservadas da linguagem (NomEmp, DatSis, Web_HTML...).
        for (reserved in LspReservedVariables.names) {
            rs.addElement(
                LookupElementBuilder.create(reserved)
                    .withIcon(AllIcons.Nodes.Constant)
                    .withTypeText("reservada"),
            )
        }

        // Funções nativas: assinatura ao lado do nome; cursor entra nos parênteses.
        for (native in LspNativeFunctions.all) {
            var element = LookupElementBuilder.create(native.name)
                .withIcon(AllIcons.Nodes.Function)
                .withTypeText("nativa")
                .withInsertHandler(if (native.hasParams) PARENS_INSIDE else PARENS_AFTER)
            if (native.hasParams) element = element.withTailText("(${native.params})", true)
            rs.addElement(element)
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

        /** Insere "()" e posiciona o cursor ENTRE os parênteses (funções com parâmetros). */
        val PARENS_INSIDE = InsertHandler<LookupElement> { context: InsertionContext, _ ->
            insertParens(context, caretInside = true)
        }

        /** Insere "()" e posiciona o cursor APÓS os parênteses (funções sem parâmetros). */
        val PARENS_AFTER = InsertHandler<LookupElement> { context: InsertionContext, _ ->
            insertParens(context, caretInside = false)
        }

        private fun insertParens(context: InsertionContext, caretInside: Boolean) {
            val tail = context.document.charsSequence
            val already = context.tailOffset < tail.length && tail[context.tailOffset] == '('
            if (!already) context.document.insertString(context.tailOffset, "()")
            val target = if (caretInside) context.tailOffset + 1 else context.tailOffset + 2
            context.editor.caretModel.moveToOffset(target)
        }
    }
}
