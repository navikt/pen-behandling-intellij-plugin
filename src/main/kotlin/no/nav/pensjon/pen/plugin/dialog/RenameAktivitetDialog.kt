package no.nav.pensjon.pen.plugin.dialog

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.COLUMNS_MEDIUM
import com.intellij.ui.dsl.builder.columns
import com.intellij.ui.dsl.builder.panel
import javax.swing.JCheckBox
import javax.swing.JComponent
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

class RenameAktivitetDialog(
    project: Project,
    private val numStr: String,
    private val currentDescription: String,
) : DialogWrapper(project) {

    private val descriptionField = JBTextField(currentDescription)
    private val changeDiscriminatorCheckbox = JCheckBox("Endre diskriminatorverdi også", false)

    init {
        title = "Gi nytt navn til aktivitet $numStr"
        init()
        initValidation()
        descriptionField.document.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent) = initValidation()
            override fun removeUpdate(e: DocumentEvent) = initValidation()
            override fun changedUpdate(e: DocumentEvent) = initValidation()
        })
    }

    override fun createCenterPanel(): JComponent = panel {
        row("Ny beskrivelse:") {
            cell(descriptionField)
                .columns(COLUMNS_MEDIUM)
                .focused()
                .comment("PascalCase, f.eks. 'OpprettOppgave'")
        }
        row {
            cell(changeDiscriminatorCheckbox)
                .comment("OBS: Endring av diskriminator brekker bakoverkompatibilitet")
        }
    }

    override fun doValidate(): ValidationInfo? {
        val desc = descriptionField.text.trim()
        if (desc.isEmpty()) return ValidationInfo("Beskrivelse er påkrevd", descriptionField)
        if (!desc[0].isUpperCase()) return ValidationInfo("Beskrivelse må starte med stor bokstav", descriptionField)
        if (desc.contains(' ')) return ValidationInfo("Beskrivelse kan ikke inneholde mellomrom", descriptionField)
        return null
    }

    val newDescription: String get() = descriptionField.text.trim()
    val shouldChangeDiscriminator: Boolean get() = changeDiscriminatorCheckbox.isSelected
}
