package dev.lspsenior

import com.intellij.icons.AllIcons
import com.intellij.ide.structureView.StructureViewBuilder
import com.intellij.ide.structureView.StructureViewModel
import com.intellij.ide.structureView.StructureViewModelBase
import com.intellij.ide.structureView.StructureViewTreeElement
import com.intellij.ide.structureView.TreeBasedStructureViewBuilder
import com.intellij.ide.util.treeView.smartTree.TreeElement
import com.intellij.lang.PsiStructureViewFactory
import com.intellij.navigation.ItemPresentation
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiFile
import javax.swing.Icon

/**
 * Structure View (Ctrl+F12 / painel Structure) para arquivos .lsp / .lspt.
 *
 * O TextMate não fornece árvore PSI, então construímos o outline varrendo o texto e
 * montando a hierarquia de blocos `{ }` / `Inicio`...`Fim` (funções, se, enquanto, etc.),
 * com rótulos derivados do texto de cada bloco. Navegação via [LspNavTarget].
 */
class LspStructureViewFactory : PsiStructureViewFactory {
    override fun getStructureViewBuilder(psiFile: PsiFile): StructureViewBuilder? {
        val ext = psiFile.virtualFile?.extension?.lowercase()
        if (ext != "lsp" && ext != "lspt") return null
        return object : TreeBasedStructureViewBuilder() {
            override fun createStructureViewModel(editor: Editor?): StructureViewModel =
                LspStructureViewModel(psiFile)

            override fun isRootNodeShown(): Boolean = false
        }
    }
}

private class LspStructureViewModel(psiFile: PsiFile) :
    StructureViewModelBase(psiFile, LspStructureElement(psiFile, null)),
    StructureViewModel.ElementInfoProvider {

    override fun isAlwaysShowsPlus(element: StructureViewTreeElement?): Boolean = false
    override fun isAlwaysLeaf(element: StructureViewTreeElement?): Boolean =
        (element as? LspStructureElement)?.node?.children?.isEmpty() ?: false
}

/** Nó do outline: rótulo do bloco, offset do cabeçalho e filhos aninhados. */
private class LspBlockNode(val label: String, val headerStart: Int, val children: MutableList<LspBlockNode> = ArrayList())

/** Elemento da árvore. [node] nulo = raiz (o arquivo). */
private class LspStructureElement(
    private val psiFile: PsiFile,
    val node: LspBlockNode?,
) : StructureViewTreeElement, ItemPresentation {

    override fun getValue(): Any =
        if (node == null) psiFile else LspNavTarget(psiFile, node.headerStart, node.label)

    override fun getPresentation(): ItemPresentation = this

    override fun getChildren(): Array<TreeElement> {
        val nodes = node?.children ?: LspOutline.parse(psiFile.viewProvider.contents)
        return nodes.map { LspStructureElement(psiFile, it) }.toTypedArray()
    }

    override fun getPresentableText(): String = node?.label ?: psiFile.name

    override fun getIcon(unused: Boolean): Icon =
        if (node == null) AllIcons.FileTypes.Text else AllIcons.Nodes.Method
}

/** Varredura textual que monta a árvore de blocos, ignorando strings e comentários. */
private object LspOutline {

    private const val MAX_LABEL_LENGTH = 60

    fun parse(text: CharSequence): List<LspBlockNode> {
        val roots = ArrayList<LspBlockNode>()
        val stack = ArrayList<LspBlockNode>()
        var lastBoundary = 0
        var i = 0
        val n = text.length

        fun open(headerStart: Int, label: String) {
            val node = LspBlockNode(label, headerStart)
            (stack.lastOrNull()?.children ?: roots).add(node)
            stack.add(node)
        }

        while (i < n) {
            val c = text[i]
            when {
                c == '"' -> { i++; while (i < n && text[i] != '"') { if (text[i] == '\\') i++; i++ }; i++ }
                c == '@' -> { while (i < n && text[i] != '\n') i++; lastBoundary = i }
                c == '/' && i + 1 < n && text[i + 1] == '*' -> {
                    i += 2; while (i + 1 < n && !(text[i] == '*' && text[i + 1] == '/')) i++; i += 2; lastBoundary = i
                }
                c == '{' -> { open(firstNonWs(text, lastBoundary, i), label(text, lastBoundary, i, "Bloco")); i++; lastBoundary = i }
                c == '}' -> { stack.removeLastOrNull(); i++; lastBoundary = i }
                c == ';' -> { i++; lastBoundary = i }
                c.isLetter() -> {
                    val start = i
                    while (i < n && (text[i].isLetterOrDigit() || text[i] == '_')) i++
                    when (text.subSequence(start, i).toString().lowercase()) {
                        "inicio", "início" -> { open(firstNonWs(text, lastBoundary, start), label(text, lastBoundary, start, "inicio")); lastBoundary = i }
                        "fim" -> { stack.removeLastOrNull(); lastBoundary = i }
                    }
                }
                else -> i++
            }
        }
        return roots
    }

    private fun firstNonWs(text: CharSequence, from: Int, to: Int): Int {
        var j = from.coerceAtLeast(0)
        while (j < to && text[j].isWhitespace()) j++
        return if (j < to) j else from.coerceIn(0, text.length)
    }

    private fun label(text: CharSequence, from: Int, to: Int, fallback: String): String {
        val raw = if (from in 0 until to) text.subSequence(from, to) else ""
        val collapsed = raw.toString().replace(Regex("\\s+"), " ").trim()
        if (collapsed.isEmpty()) return fallback
        return if (collapsed.length > MAX_LABEL_LENGTH) collapsed.take(MAX_LABEL_LENGTH - 1) + "…" else collapsed
    }
}
