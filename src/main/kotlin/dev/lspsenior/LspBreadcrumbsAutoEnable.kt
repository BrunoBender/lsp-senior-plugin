package dev.lspsenior

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.editor.ex.EditorSettingsExternalizable
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity

/**
 * Liga os breadcrumbs para a linguagem TextMate (usada pelos arquivos .lsp / .lspt) na
 * PRIMEIRA execução após instalar o plugin — o IntelliJ vem com os breadcrumbs desligados
 * por padrão para TextMate, então sem isto o usuário precisa habilitar na mão em
 * Settings → Editor → General → Breadcrumbs.
 *
 * Roda uma única vez: guarda um flag em [PropertiesComponent] (nível aplicação). Se o
 * usuário depois desligar os breadcrumbs manualmente, o plugin NÃO religa — a escolha dele
 * é respeitada. Usa apenas API pública (`EditorSettingsExternalizable.setBreadcrumbsShownFor`).
 */
class LspBreadcrumbsAutoEnable : ProjectActivity {

    override suspend fun execute(project: Project) {
        val props = PropertiesComponent.getInstance()
        if (props.getBoolean(FLAG, false)) return
        props.setValue(FLAG, true)

        val settings = EditorSettingsExternalizable.getInstance()
        if (!settings.isBreadcrumbsShownFor(TEXTMATE_LANGUAGE_ID)) {
            settings.setBreadcrumbsShownFor(TEXTMATE_LANGUAGE_ID, true)
        }
    }

    private companion object {
        const val FLAG = "dev.lspsenior.breadcrumbs.textmate.autoEnabled"
        const val TEXTMATE_LANGUAGE_ID = "textmate"
    }
}
