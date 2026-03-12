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

class ChangeAktivitetNumberDialog(
    project: Project,
    private val currentNumber: String,
) : DialogWrapper(project) {

    private val numberField = JBTextField(currentNumber)
    private val renumberSubsequentCheckbox = JCheckBox("Renummerer etterfølgende aktiviteter", false)

    init {
        title = "Endre aktivitetsnummer"
        init()
        initValidation()
        numberField.document.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent) = initValidation()
            override fun removeUpdate(e: DocumentEvent) = initValidation()
            override fun changedUpdate(e: DocumentEvent) = initValidation()
        })
    }

    override fun createCenterPanel(): JComponent = panel {
        row("Nåværende nummer:") {
            label(currentNumber).bold()
        }
        row("Nytt nummer:") {
            cell(numberField)
                .columns(COLUMNS_MEDIUM)
                .focused()
                .comment("F.eks. 'A201'")
        }
        row {
            cell(renumberSubsequentCheckbox)
                .comment("Aktiviteter etter den nye posisjonen renummereres fortløpende")
        }
    }

    override fun doValidate(): ValidationInfo? {
        val num = numberField.text.trim()
        if (!num.matches(Regex("A\\d{3}"))) {
            return ValidationInfo("Aktivitetsnummer må være på formen A###, f.eks. 'A201'", numberField)
        }
        if (num == currentNumber) {
            return ValidationInfo("Nytt nummer er det samme som nåværende", numberField)
        }
        return null
    }

    val newNumber: String get() = numberField.text.trim()
    val shouldRenumberSubsequent: Boolean get() = renumberSubsequentCheckbox.isSelected
}
