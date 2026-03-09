package no.nav.pensjon.pen.plugin.dialog

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.*
import java.awt.BorderLayout
import java.awt.FlowLayout
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import javax.swing.*

class NewBehandlingDialog(project: Project) : DialogWrapper(project) {

    private val nameField = JBTextField()
    private val discriminatorField = JBTextField()
    private val aktivitetNumberField = JBTextField("A101")
    private val aktivitetDescriptionField = JBTextField()

    private var selectedTeam = TEAMS[0]
    private var selectedPriority = PRIORITIES[0]

    private data class ParameterRow(
        val nameField: JBTextField,
        val typeField: JBTextField,
        val panel: JPanel,
    )

    private val parameterRows = mutableListOf<ParameterRow>()
    private lateinit var parametersContainer: JPanel

    init {
        title = "New Behandling"
        setSize(550, 550)
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

    override fun createCenterPanel(): JComponent {
        val mainPanel = JPanel(BorderLayout())

        val topPanel = panel {
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
        }

        parametersContainer = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
        }

        val parametersSection = JPanel(BorderLayout()).apply {
            border = BorderFactory.createTitledBorder("Input Parameters")

            val headerPanel = JPanel(FlowLayout(FlowLayout.LEFT)).apply {
                add(JButton("Add parameter").apply {
                    addActionListener { addParameterRow() }
                })
                add(JBLabel("Parameters will be serialized as JSON in the INPUT column").apply {
                    foreground = java.awt.Color.GRAY
                })
            }
            add(headerPanel, BorderLayout.NORTH)
            add(parametersContainer, BorderLayout.CENTER)
        }

        val bottomPanel = panel {
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

        val verticalBox = Box.createVerticalBox().apply {
            add(topPanel)
            add(parametersSection)
            add(bottomPanel)
            add(Box.createVerticalGlue())
        }
        mainPanel.add(verticalBox, BorderLayout.NORTH)
        return mainPanel
    }

    private fun addParameterRow() {
        val nameField = JBTextField()
        val typeField = JBTextField()

        val rowPanel = JPanel(GridBagLayout())
        val gbc = GridBagConstraints().apply { insets = Insets(2, 4, 2, 4) }

        gbc.gridx = 0; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0.0
        rowPanel.add(JBLabel("Name:"), gbc)

        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0
        rowPanel.add(nameField, gbc)

        gbc.gridx = 2; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0.0
        rowPanel.add(JBLabel("Type:"), gbc)

        gbc.gridx = 3; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0
        rowPanel.add(typeField, gbc)

        val row = ParameterRow(nameField, typeField, rowPanel)

        val removeButton = JButton("✕").apply {
            addActionListener {
                parameterRows.remove(row)
                parametersContainer.remove(rowPanel)
                parametersContainer.revalidate()
                parametersContainer.repaint()
                pack()
            }
        }
        gbc.gridx = 4; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0.0
        rowPanel.add(removeButton, gbc)

        parameterRows.add(row)
        parametersContainer.add(rowPanel)
        parametersContainer.revalidate()
        parametersContainer.repaint()
        pack()
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
        parameters = parameterRows.map { row ->
            ParameterModel(row.nameField.text.trim(), row.typeField.text.trim())
        }.filter { it.name.isNotEmpty() && it.type.isNotEmpty() },
        initialAktivitetNumber = aktivitetNumberField.text.trim(),
        initialAktivitetDescription = aktivitetDescriptionField.text.trim(),
    )

    companion object {
        val TEAMS = listOf("PESYS_FELLES", "PESYS_ALDER", "PESYS_UFORE")
        val PRIORITIES = listOf("ONLINE_BATCH", "ONLINE", "BATCH")
    }
}
