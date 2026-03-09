package no.nav.pensjon.pen.plugin.dialog

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.*
import javax.swing.JComponent

class NewAktivitetDialog(
    project: Project,
    suggestedBehandlingName: String = "",
    suggestedNumber: String = "A101",
) : DialogWrapper(project) {

    private val behandlingNameField = JBTextField(suggestedBehandlingName)
    private val aktivitetNumberField = JBTextField(suggestedNumber)
    private val aktivitetDescriptionField = JBTextField()
    private var isLastAktivitet = true

    init {
        title = "New Aktivitet"
        setSize(500, 350)
        init()
    }

    override fun createCenterPanel(): JComponent = panel {
        row("Behandling name:") {
            cell(behandlingNameField)
                .columns(COLUMNS_MEDIUM)
                .focused()
                .comment("Name without 'Behandling' suffix, e.g. 'FalskId'")
        }
        row("Aktivitet number:") {
            cell(aktivitetNumberField)
                .columns(COLUMNS_SHORT)
                .comment("e.g. A101, A102, A201")
        }
        row("Description:") {
            cell(aktivitetDescriptionField)
                .columns(COLUMNS_MEDIUM)
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
        val name = behandlingNameField.text.trim()
        if (name.isEmpty()) {
            return ValidationInfo("Behandling name is required", behandlingNameField)
        }
        if (!name[0].isUpperCase()) {
            return ValidationInfo("Behandling name must start with an uppercase letter", behandlingNameField)
        }

        val aktNum = aktivitetNumberField.text.trim()
        if (!aktNum.matches(Regex("A\\d{3}"))) {
            return ValidationInfo("Number must match pattern A followed by 3 digits (e.g. A102)", aktivitetNumberField)
        }

        val desc = aktivitetDescriptionField.text.trim()
        if (desc.isEmpty()) {
            return ValidationInfo("Description is required", aktivitetDescriptionField)
        }
        if (!desc[0].isUpperCase()) {
            return ValidationInfo("Description must start with an uppercase letter", aktivitetDescriptionField)
        }

        return null
    }

    fun getModel(): AktivitetModel = AktivitetModel(
        behandlingName = behandlingNameField.text.trim().removeSuffix("Behandling"),
        aktivitetNumber = aktivitetNumberField.text.trim(),
        aktivitetDescription = aktivitetDescriptionField.text.trim(),
        isLastAktivitet = isLastAktivitet,
    )
}
