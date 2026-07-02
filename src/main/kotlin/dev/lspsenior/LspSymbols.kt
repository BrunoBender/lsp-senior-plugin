package dev.lspsenior

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiManager
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.indexing.FileBasedIndex

/**
 * Helpers de resolução de símbolos da Linguagem Senior, compartilhados pela
 * navegação (Go to Declaration) e pela busca de usos (Find Usages).
 *
 * Tudo é baseado em texto (a linguagem usa TextMate, sem parser semântico):
 * varreduras ignoram strings ("..."), comentários de linha (@) e de bloco (/* */).
 */
object LspSymbols {

    /** Identificador sob o offset informado, ou null. */
    fun wordAt(text: CharSequence, offset: Int): String? {
        if (offset < 0 || offset > text.length) return null
        var start = offset
        var end = offset
        while (start > 0 && isIdent(text[start - 1])) start--
        while (end < text.length && isIdent(text[end])) end++
        if (start == end) return null
        return text.subSequence(start, end).toString()
    }

    /** True se [name] é uma função conhecida (tem implementação indexada no projeto). */
    fun isFunctionName(project: Project, name: String): Boolean =
        FileBasedIndex.getInstance()
            .getContainingFiles(LspFunctionIndex.NAME, name, GlobalSearchScope.allScope(project))
            .isNotEmpty()

    /** Alvos de navegação para a IMPLEMENTAÇÃO da função [name] (em qualquer arquivo do projeto). */
    fun functionTargets(project: Project, name: String): List<LspNavTarget> {
        val targets = ArrayList<LspNavTarget>()
        val index = FileBasedIndex.getInstance()
        val scope = GlobalSearchScope.allScope(project)
        val psiManager = PsiManager.getInstance(project)
        for (file in index.getContainingFiles(LspFunctionIndex.NAME, name, scope)) {
            val offset = index.getFileData(LspFunctionIndex.NAME, file, project)[name] ?: continue
            val psiFile = psiManager.findFile(file) ?: continue
            targets.add(LspNavTarget(psiFile, offset, name))
        }
        return targets
    }

    /**
     * Declaração de variável visível para um uso: "Definir [Tipo] nome".
     * Visível = declarada antes do uso e dentro de um bloco { } que ainda o contém.
     * Retorna (offset do nome, offset do "Definir") da declaração mais próxima, ou null.
     */
    fun resolveVariableDecl(text: CharSequence, name: String, usageOffset: Int): Pair<Int, Int>? {
        val pattern = Regex(
            "(?i)\\bDefinir[ \\t]+(?:[A-Za-z_][A-Za-z0-9_.]*[ \\t]+)?(" +
                Regex.escape(name) + ")\\b(?![ \\t]*\\()",
        )
        var bestName = -1
        var bestStart = -1
        for (match in pattern.findAll(text)) {
            val group = match.groups[1] ?: continue
            val declNameOffset = group.range.first
            if (declNameOffset > usageOffset) continue
            val scopeEnd = enclosingBlockEnd(text, match.range.first)
            if (usageOffset < scopeEnd && declNameOffset > bestName) {
                bestName = declNameOffset
                bestStart = match.range.first
            }
        }
        return if (bestName >= 0) bestName to bestStart else null
    }

    /** Offset do '}' que fecha o bloco que contém [from], ou o fim do texto (escopo de arquivo). */
    fun enclosingBlockEnd(text: CharSequence, from: Int): Int {
        var depth = 0
        var i = from
        val n = text.length
        while (i < n) {
            val c = text[i]
            when {
                c == '"' -> { i++; while (i < n && text[i] != '"') { if (text[i] == '\\') i++; i++ }; i++ }
                c == '@' -> { while (i < n && text[i] != '\n') i++ }
                c == '/' && i + 1 < n && text[i + 1] == '*' -> {
                    i += 2; while (i + 1 < n && !(text[i] == '*' && text[i + 1] == '/')) i++; i += 2
                }
                c == '{' -> { depth++; i++ }
                c == '}' -> { if (depth == 0) return i; depth--; i++ }
                else -> i++
            }
        }
        return n
    }

    /**
     * Offsets de cada ocorrência do identificador [name] (igualdade exata, sem '.' antes),
     * ignorando strings e comentários.
     */
    fun occurrences(text: CharSequence, name: String): List<Int> {
        val result = ArrayList<Int>()
        var i = 0
        val n = text.length
        while (i < n) {
            val c = text[i]
            when {
                c == '"' -> { i++; while (i < n && text[i] != '"') { if (text[i] == '\\') i++; i++ }; i++ }
                c == '@' -> { while (i < n && text[i] != '\n') i++ }
                c == '/' && i + 1 < n && text[i + 1] == '*' -> {
                    i += 2; while (i + 1 < n && !(text[i] == '*' && text[i + 1] == '/')) i++; i += 2
                }
                c.isLetter() || c == '_' -> {
                    val start = i
                    while (i < n && isIdent(text[i])) i++
                    if (text.subSequence(start, i).toString() == name && !(start > 0 && text[start - 1] == '.')) {
                        result.add(start)
                    }
                }
                else -> i++
            }
        }
        return result
    }

    /**
     * Nomes de variáveis declaradas ("Definir [Tipo] nome") visíveis em [offset]:
     * declaradas antes e dentro de um bloco que ainda o contém. Ordem de declaração.
     */
    fun visibleVariables(text: CharSequence, offset: Int): List<String> =
        visibleVariablesWithType(text, offset).map { it.name }

    /** Variável declarada: nome e tipo ("Alfa", "Numero"...) quando informado no Definir. */
    data class VarDecl(val name: String, val type: String?)

    /**
     * Como [visibleVariables], mas também captura o tipo do "Definir [Tipo] nome".
     * A primeira declaração vista de cada nome prevalece (ordem de declaração).
     */
    fun visibleVariablesWithType(text: CharSequence, offset: Int): List<VarDecl> {
        val result = LinkedHashMap<String, VarDecl>()
        for (match in DECL_ANY.findAll(text)) {
            val nameGroup = match.groups[2] ?: continue
            if (nameGroup.range.first > offset) continue
            if (offset < enclosingBlockEnd(text, match.range.first)) {
                val type = match.groups[1]?.value?.takeIf { it.isNotBlank() }
                result.putIfAbsent(nameGroup.value, VarDecl(nameGroup.value, type))
            }
        }
        return result.values.toList()
    }

    /** Intervalo [início..fim] de busca para um nome: escopo da variável, ou todo o arquivo. */
    fun variableScope(text: CharSequence, name: String, offset: Int): IntRange {
        val decl = resolveVariableDecl(text, name, offset) ?: return 0..text.length
        return decl.second..enclosingBlockEnd(text, decl.second)
    }

    /** True se [name] é um identificador válido da linguagem. */
    fun isIdentifier(name: String): Boolean =
        name.isNotEmpty() && (name[0].isLetter() || name[0] == '_') && name.all { isIdent(it) }

    // Grupo 1 = tipo (opcional, ex.: "Alfa"); grupo 2 = nome da variável.
    private val DECL_ANY = Regex(
        "(?i)\\bDefinir[ \\t]+(?:([A-Za-z_][A-Za-z0-9_.]*)[ \\t]+)?([A-Za-z_][A-Za-z0-9_]*)\\b(?![ \\t]*\\()",
    )

    private fun isIdent(c: Char) = c.isLetterOrDigit() || c == '_'
}
