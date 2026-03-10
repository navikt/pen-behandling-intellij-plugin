package no.nav.pensjon.pen.plugin.dialog

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.COLUMNS_MEDIUM
import com.intellij.ui.dsl.builder.columns
import com.intellij.ui.dsl.builder.panel
import javax.swing.JComponent

class NewAktivitetDialog(
    project: Project,
    private val behandlingName: String,
) : DialogWrapper(project) {

    private val aktivitetDescriptionField = JBTextField()
    private var isLastAktivitet = true

    init {
        title = "New Aktivitet"
        setSize(500, 300)
        init()
    }

    override fun createCenterPanel(): JComponent = panel {
        row("Behandling:") {
            label(behandlingName.ifEmpty { "(unknown)" })
                .bold()
        }
        row("Description:") {
            cell(aktivitetDescriptionField)
                .columns(COLUMNS_MEDIUM)
                .focused()
                .comment("Functional name, e.g. 'OpprettOppgave'")
        }

        separator()

        row {
            checkBox("This is the last aktivitet (returns aktivitetFullfort())")
                .onChanged { isLastAktivitet = it.isSelected }
                .applyToComponent { isSelected = true }
        }
    }

    override fun doValidate(): ValidationInfo? {
        val desc = aktivitetDescriptionField.text.trim()
        if (desc.isEmpty()) {
            return ValidationInfo("Description is required", aktivitetDescriptionField)
        }
        if (!desc[0].isUpperCase()) {
            return ValidationInfo("Description must start with an uppercase letter", aktivitetDescriptionField)
        }

        return null
    }

    fun getModel(aktivitetNumber: String): AktivitetModel = AktivitetModel(
        behandlingName = behandlingName.removeSuffix("Behandling"),
        aktivitetNumber = aktivitetNumber,
        aktivitetDescription = aktivitetDescriptionField.text.trim(),
        isLastAktivitet = isLastAktivitet,
    )
}
