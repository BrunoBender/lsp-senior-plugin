package dev.lspsenior

/**
 * Catálogo das funções nativas da Linguagem Senior (SQL_*, WCheckVal*, addKeyAnd*InJSON,
 * Concatena, etc.), usado pelo autocomplete e pela documentação (Ctrl+Q).
 *
 * Três fontes, embutidas em `dev/lspsenior/`:
 * - `native-functions.txt`   — nomes (derivados da gramática TextMate);
 * - `native-signatures.txt`  — assinaturas `nome(parametros)` (documentação oficial);
 * - `native-descriptions.txt`— `nome<TAB>descrição` (documentação oficial).
 *
 * O conjunto final é a união dos nomes das três fontes; quando disponível, cada função
 * recebe os parâmetros e a descrição. Carregado uma vez, sob demanda.
 */
object LspNativeFunctions {

    /** Uma função nativa: nome, lista de parâmetros (texto entre parênteses) e descrição. */
    data class NativeFn(val name: String, val params: String, val description: String?) {
        /** True se a função declara pelo menos um parâmetro. */
        val hasParams: Boolean get() = params.isNotBlank()
    }

    val all: List<NativeFn> by lazy { load() }

    private val byLower: Map<String, NativeFn> by lazy { all.associateBy { it.name.lowercase() } }

    /** Nomes de todas as funções nativas conhecidas. */
    val names: List<String> by lazy { all.map { it.name } }

    /** Função nativa pelo nome (case-insensitive), ou null. */
    fun byName(name: String): NativeFn? = byLower[name.lowercase()]

    private fun load(): List<NativeFn> {
        val signatures = loadSignatures()          // nomeLower -> (nome, params)
        val descriptions = loadDescriptions()      // nomeLower -> descrição
        val plainNames = loadLines("native-functions.txt")

        // União: nomes das assinaturas + nomes simples da gramática.
        val order = LinkedHashMap<String, String>() // nomeLower -> nome (exibição)
        for (sig in signatures.values) order.putIfAbsent(sig.first.lowercase(), sig.first)
        for (name in plainNames) order.putIfAbsent(name.lowercase(), name)

        return order.entries
            .map { (lower, display) ->
                NativeFn(
                    name = signatures[lower]?.first ?: display,
                    params = signatures[lower]?.second ?: "",
                    description = descriptions[lower],
                )
            }
            .sortedBy { it.name.lowercase() }
    }

    /** nomeLower -> (nome, params) a partir de linhas `nome(parametros)`. */
    private fun loadSignatures(): Map<String, Pair<String, String>> {
        val result = LinkedHashMap<String, Pair<String, String>>()
        for (line in loadLines("native-signatures.txt")) {
            val open = line.indexOf('(')
            val close = line.lastIndexOf(')')
            if (open <= 0 || close < open) continue
            val name = line.substring(0, open).trim()
            val params = line.substring(open + 1, close).trim()
            if (name.isNotEmpty()) result.putIfAbsent(name.lowercase(), name to params)
        }
        return result
    }

    /** nomeLower -> descrição a partir de linhas `nome<TAB>descrição`. */
    private fun loadDescriptions(): Map<String, String> {
        val result = LinkedHashMap<String, String>()
        for (line in loadLines("native-descriptions.txt")) {
            val tab = line.indexOf('\t')
            if (tab <= 0) continue
            val name = line.substring(0, tab).trim()
            val desc = line.substring(tab + 1).trim()
            if (name.isNotEmpty() && desc.isNotEmpty()) result.putIfAbsent(name.lowercase(), desc)
        }
        return result
    }

    private fun loadLines(resource: String): List<String> {
        val stream = javaClass.getResourceAsStream("/dev/lspsenior/$resource") ?: return emptyList()
        return stream.bufferedReader().useLines { lines ->
            lines.map { it.trim() }
                .filter { it.isNotEmpty() && !it.startsWith("#") }
                .toList()
        }
    }
}
