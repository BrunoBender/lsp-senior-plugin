package dev.lspsenior

/**
 * Variáveis reservadas da Linguagem Senior (NomEmp, DatSis, CodUsu, Web_HTML, etc.),
 * embutidas em `dev/lspsenior/reserved-variables.txt`. Usadas pelo autocomplete
 * (tipo "reservada"). A coloração é feita na gramática TextMate (variable.language.lsp).
 */
object LspReservedVariables {

    val names: List<String> by lazy { load() }

    private fun load(): List<String> {
        val stream = javaClass.getResourceAsStream("/dev/lspsenior/reserved-variables.txt")
            ?: return emptyList()
        return stream.bufferedReader().useLines { lines ->
            lines.map { it.trim() }
                .filter { it.isNotEmpty() && !it.startsWith("#") }
                .distinct()
                .toList()
        }
    }
}
