package dev.lspsenior

import com.intellij.util.indexing.DataIndexer
import com.intellij.util.indexing.FileBasedIndex
import com.intellij.util.indexing.FileBasedIndexExtension
import com.intellij.util.indexing.FileContent
import com.intellij.util.indexing.ID
import com.intellij.util.io.DataExternalizer
import com.intellij.util.io.EnumeratorStringDescriptor
import com.intellij.util.io.KeyDescriptor
import java.io.DataInput
import java.io.DataOutput

/**
 * Indexa as implementações de funções da Linguagem Senior nos arquivos .lsp/.lspt.
 *
 * Mapeia: nome da função -> offset do nome no arquivo (para navegação).
 *
 * Considera apenas a IMPLEMENTAÇÃO ("Funcao nome() { ... }"), ignorando os
 * protótipos ("Definir Funcao nome();"), que começam com Definir. Assim o
 * "Go to Declaration" cai direto no corpo da função.
 */
class LspFunctionIndex : FileBasedIndexExtension<String, Int>() {

    override fun getName(): ID<String, Int> = NAME

    override fun getIndexer(): DataIndexer<String, Int, FileContent> = DataIndexer { inputData ->
        val result = HashMap<String, Int>()
        val text = inputData.contentAsText
        DEFINITION.findAll(text).forEach { match ->
            val group = match.groups[1] ?: return@forEach
            result[group.value] = group.range.first
        }
        result
    }

    override fun getKeyDescriptor(): KeyDescriptor<String> = EnumeratorStringDescriptor.INSTANCE

    override fun getValueExternalizer(): DataExternalizer<Int> = object : DataExternalizer<Int> {
        override fun save(out: DataOutput, value: Int) = out.writeInt(value)
        override fun read(input: DataInput): Int = input.readInt()
    }

    override fun getVersion(): Int = 1

    override fun getInputFilter(): FileBasedIndex.InputFilter =
        FileBasedIndex.InputFilter { file ->
            val ext = file.extension?.lowercase()
            ext == "lsp" || ext == "lspt"
        }

    override fun dependsOnFileContent(): Boolean = true

    companion object {
        val NAME: ID<String, Int> = ID.create("dev.lspsenior.functionIndex")

        // Implementação no início da linha: "Funcao nome(" — protótipos começam com "Definir".
        private val DEFINITION = Regex("(?m)^[ \\t]*Funcao[ \\t]+([A-Za-z_][A-Za-z0-9_]*)")
    }
}
