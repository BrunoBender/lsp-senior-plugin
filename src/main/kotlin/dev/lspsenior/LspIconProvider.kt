package dev.lspsenior

import com.intellij.ide.IconProvider
import com.intellij.openapi.util.IconLoader
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import javax.swing.Icon

class LspIconProvider : IconProvider() {
    private val icon: Icon by lazy { IconLoader.getIcon("/icons/lsp_file.svg", javaClass) }

    override fun getIcon(element: PsiElement, flags: Int): Icon? {
        if (element is PsiFile) {
            val ext = element.virtualFile?.extension?.lowercase()
            if (ext == "lsp" || ext == "lspt") return icon
        }
        return null
    }
}
