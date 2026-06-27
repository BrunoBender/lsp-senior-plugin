package dev.lspsenior

import com.intellij.psi.PsiElement
import com.intellij.spellchecker.tokenizer.SpellcheckingStrategy
import com.intellij.spellchecker.tokenizer.Tokenizer

class LspSpellcheckingStrategy : SpellcheckingStrategy() {
    override fun getTokenizer(element: PsiElement): Tokenizer<*> {
        val ext = element.containingFile?.virtualFile?.extension?.lowercase()
        if (ext == "lsp" || ext == "lspt") return EMPTY_TOKENIZER
        return super.getTokenizer(element)
    }
}
