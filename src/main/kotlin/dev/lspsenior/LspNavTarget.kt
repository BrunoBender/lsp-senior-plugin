package dev.lspsenior

import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.impl.FakePsiElement

/**
 * Alvo de navegação para a Linguagem Senior.
 *
 * O PSI do TextMate não fornece um elemento navegável no offset desejado
 * (a navegação acaba no topo do arquivo). Este alvo abre o arquivo no offset
 * exato via [OpenFileDescriptor].
 */
class LspNavTarget(
    private val file: PsiFile,
    private val offset: Int,
    private val symbolName: String,
) : FakePsiElement() {

    val targetFile: PsiFile get() = file
    val targetOffset: Int get() = offset

    override fun getParent(): PsiElement = file
    override fun getContainingFile(): PsiFile = file
    override fun getProject(): Project = file.project
    override fun getName(): String = symbolName
    override fun getTextOffset(): Int = offset

    override fun canNavigate(): Boolean = file.virtualFile != null
    override fun canNavigateToSource(): Boolean = canNavigate()

    override fun navigate(requestFocus: Boolean) {
        val vf = file.virtualFile ?: return
        OpenFileDescriptor(file.project, vf, offset).navigate(requestFocus)
    }
}
