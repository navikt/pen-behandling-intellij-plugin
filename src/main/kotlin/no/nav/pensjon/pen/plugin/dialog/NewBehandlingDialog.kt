package no.nav.pensjon.pen.plugin.dialog

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.*
import javax.swing.JComponent

class NewBehandlingDialog(project: Project) : DialogWrapper(project) {

    private val nameField = JBTextField()
    private val discriminatorField = JBTextField()
    private val aktivitetNumberField = JBTextField("A101")
    private val aktivitetDescriptionField = JBTextField()

    private var selectedTeam = TEAMS[0]
    private var selectedPriority = PRIORITIES[0]

    private val parameterRows = mutableListOf<Pair<JBTextField, JBTextField>>()

    init {
        title = "New Behandling"
        setSize(550, 500)
        init()

        nameField.document.addDocumentListener(object : javax.swing.event.DocumentListener {
            override fun insertUpdate(e: javax.swing.event.DocumentEvent) = syncDiscriminator()
            override fun removeUpdate(e: javax.swing.event.DocumentEvent) = syncDiscriminator()
            override fun changedUpdate(e: javax.swing.event.DocumentEvent) = syncDiscriminator()
        })
    }

    private fun syncDiscriminator() {
        val name = nameField.text.trim()
        discriminatorField.text = name.removeSuffix("Behandling")
    }

    override fun createCenterPanel(): JComponent = panel {
        row("Name:") {
            cell(nameField)
                .columns(COLUMNS_MEDIUM)
                .comment("Class will be named {Name}Behandling")
                .focused()
        }
        row("Discriminator value:") {
            cell(discriminatorField)
                .columns(COLUMNS_MEDIUM)
                .comment("Should NOT end with 'Behandling'")
        }
        row("Team:") {
            comboBox(TEAMS)
                .onChanged { selectedTeam = it.item }
        }
        row("Priority:") {
            comboBox(PRIORITIES)
                .onChanged { selectedPriority = it.item }
                .comment("ONLINE = user-facing, ONLINE_BATCH = automatic, BATCH = nightly")
        }

        group("Input Parameters") {
            row {
                button("Add parameter") {
                    parameterRows.add(Pair(JBTextField(), JBTextField()))
                    // Rebuild dialog to show new parameter row
                    initValidation()
                }
            }
            row {
                comment("Parameters will be serialized as JSON in the INPUT column")
            }
        }

        separator()

        group("Initial Aktivitet") {
            row("Number:") {
                cell(aktivitetNumberField)
                    .columns(COLUMNS_SHORT)
                    .comment("e.g. A101")
            }
            row("Description:") {
                cell(aktivitetDescriptionField)
                    .columns(COLUMNS_MEDIUM)
                    .comment("Functional name, e.g. 'SjekkOmIdentErFalsk'")
            }
        }
    }

    override fun doValidate(): ValidationInfo? {
        val name = nameField.text.trim()
        if (name.isEmpty()) {
            return ValidationInfo("Name is required", nameField)
        }
        if (!name[0].isUpperCase()) {
            return ValidationInfo("Name must start with an uppercase letter", nameField)
        }
        if (name.contains(" ")) {
            return ValidationInfo("Name must not contain spaces", nameField)
        }

        val discriminator = discriminatorField.text.trim()
        if (discriminator.isEmpty()) {
            return ValidationInfo("Discriminator value is required", discriminatorField)
        }
        if (discriminator.endsWith("Behandling")) {
            return ValidationInfo("Discriminator value should not end with 'Behandling'", discriminatorField)
        }

        val aktNum = aktivitetNumberField.text.trim()
        if (!aktNum.matches(Regex("A\\d{3}"))) {
            return ValidationInfo("Aktivitet number must match pattern A followed by 3 digits (e.g. A101)", aktivitetNumberField)
        }

        val aktDesc = aktivitetDescriptionField.text.trim()
        if (aktDesc.isEmpty()) {
            return ValidationInfo("Aktivitet description is required", aktivitetDescriptionField)
        }
        if (!aktDesc[0].isUpperCase()) {
            return ValidationInfo("Aktivitet description must start with an uppercase letter", aktivitetDescriptionField)
        }

        return null
    }

    fun getModel(): BehandlingModel = BehandlingModel(
        name = nameField.text.trim().removeSuffix("Behandling"),
        discriminatorValue = discriminatorField.text.trim(),
        team = selectedTeam,
        priority = selectedPriority,
        parameters = parameterRows.map { (nameField, typeField) ->
            ParameterModel(nameField.text.trim(), typeField.text.trim())
        }.filter { it.name.isNotEmpty() && it.type.isNotEmpty() },
        initialAktivitetNumber = aktivitetNumberField.text.trim(),
        initialAktivitetDescription = aktivitetDescriptionField.text.trim(),
    )

    companion object {
        val TEAMS = listOf("PESYS_FELLES", "PESYS_ALDER", "PESYS_UFORE")
        val PRIORITIES = listOf("ONLINE_BATCH", "ONLINE", "BATCH")
    }
}
