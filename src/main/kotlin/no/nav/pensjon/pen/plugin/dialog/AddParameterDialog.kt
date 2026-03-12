package no.nav.pensjon.pen.plugin.dialog

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.COLUMNS_MEDIUM
import com.intellij.ui.dsl.builder.columns
import com.intellij.ui.dsl.builder.panel
import javax.swing.JComponent

class AddParameterDialog(
    project: Project,
    dialogTitle: String,
) : DialogWrapper(project) {

    private val nameField = JBTextField()
    private val typeField = JBTextField()

    init {
        title = dialogTitle
        init()
        initValidation()
    }

    override fun createCenterPanel(): JComponent = panel {
        row("Navn:") {
            cell(nameField)
                .columns(COLUMNS_MEDIUM)
                .focused()
                .comment("Parameternavn i camelCase, f.eks. 'sakId'")
        }
        row("Type:") {
            cell(typeField)
                .columns(COLUMNS_MEDIUM)
                .comment("Kotlin-type, f.eks. 'Long', 'String', 'List<Long>'")
        }
    }

    override fun doValidate(): ValidationInfo? {
        val name = nameField.text.trim()
        if (name.isEmpty()) return ValidationInfo("Navn er påkrevd", nameField)
        if (!name[0].isLowerCase()) return ValidationInfo("Navn må starte med liten bokstav", nameField)

        val type = typeField.text.trim()
        if (type.isEmpty()) return ValidationInfo("Type er påkrevd", typeField)

        return null
    }

    val paramName: String get() = nameField.text.trim()
    val paramType: String get() = typeField.text.trim()
}
