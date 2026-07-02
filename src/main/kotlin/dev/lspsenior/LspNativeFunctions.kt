package dev.lspsenior

/**
 * Catálogo das funções nativas da Linguagem Senior (SQL_*, WCheckVal*, addKeyAnd*InJSON,
 * Concatena, etc.), usado pelo autocomplete.
 *
 * A lista é gerada da própria gramática TextMate (entity.name.function/method) e embutida
 * em `dev/lspsenior/native-functions.txt`. Carregada uma vez, sob demanda.
 */
object LspNativeFunctions {

    val names: List<String> by lazy { load() }

    private fun load(): List<String> {
        val stream = javaClass.getResourceAsStream("/dev/lspsenior/native-functions.txt")
            ?: return emptyList()
        return stream.bufferedReader().useLines { lines ->
            lines.map { it.trim() }
                .filter { it.isNotEmpty() && !it.startsWith("#") }
                .toList()
        }
    }
}
