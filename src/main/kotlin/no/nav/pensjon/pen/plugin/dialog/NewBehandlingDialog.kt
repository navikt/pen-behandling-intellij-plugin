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
    private val requestContextUserIdField = JBTextField()
    private val aktivitetDescriptionField = JBTextField()
    private val integrationTestCheckbox = JCheckBox("Opprett integrasjonstest", false)

    private var selectedTeam = TEAMS[0]
    private var selectedPriority = PRIORITIES[0]

    private data class ParameterRow(
        val nameField: JBTextField,
        val typeField: JBTextField,
        val panel: JPanel,
    )

    private val parameterRows = mutableListOf<ParameterRow>()
    private lateinit var parametersContainer: JPanel

    private val outputParameterRows = mutableListOf<ParameterRow>()
    private lateinit var outputParametersContainer: JPanel

    init {
        title = "Ny Behandling"
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
            row("Navn:") {
                cell(nameField)
                    .columns(COLUMNS_MEDIUM)
                    .comment("Klassen vil bli kalt {Navn}Behandling")
                    .focused()
            }
            row("Diskriminatorverdi:") {
                cell(discriminatorField)
                    .columns(COLUMNS_MEDIUM)
                    .comment("Skal IKKE slutte med 'Behandling'")
            }
            row("Team:") {
                comboBox(TEAMS)
                    .onChanged { selectedTeam = it.item }
            }
            row("Prioritet:") {
                comboBox(PRIORITIES)
                    .onChanged { selectedPriority = it.item }
                    .comment("ONLINE = brukervendt, ONLINE_BATCH = automatisk, BATCH = nattlig")
            }
            row("RequestContextUserId:") {
                cell(requestContextUserIdField)
                    .columns(COLUMNS_MEDIUM)
                    .comment("Valgfritt. Overstyrer getRequestContextUserId(), f.eks. 'Aldersovergang'")
            }
        }

        parametersContainer = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
        }

        val parametersSection = createParameterSection(
            "Input-parametere",
            "Legg til input-parameter",
            "Parametere serialiseres som JSON i INPUT-kolonnen",
            parameterRows,
            parametersContainer,
        )

        outputParametersContainer = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
        }

        val outputParametersSection = createParameterSection(
            "Output-parametere",
            "Legg til output-parameter",
            "Parametere serialiseres som JSON i OUTPUT-kolonnen",
            outputParameterRows,
            outputParametersContainer,
        )

        val bottomPanel = panel {
            separator()
            group("Første aktivitet") {
                row("Beskrivelse:") {
                    cell(aktivitetDescriptionField)
                        .columns(COLUMNS_MEDIUM)
                        .comment("Funksjonelt navn, f.eks. 'SjekkOmIdentErFalsk'")
                }
            }
            separator()
            row {
                cell(integrationTestCheckbox)
                    .comment("Oppretter {Navn}BehandlingIT.kt i test-mappen")
            }
        }

        val verticalBox = Box.createVerticalBox().apply {
            add(topPanel)
            add(parametersSection)
            add(outputParametersSection)
            add(bottomPanel)
            add(Box.createVerticalGlue())
        }
        mainPanel.add(verticalBox, BorderLayout.NORTH)
        return mainPanel
    }

    private fun createParameterSection(
        title: String,
        buttonText: String,
        description: String,
        rows: MutableList<ParameterRow>,
        container: JPanel,
    ): JPanel = JPanel(BorderLayout()).apply {
        border = BorderFactory.createTitledBorder(title)

        val headerPanel = JPanel(FlowLayout(FlowLayout.LEFT)).apply {
            add(JButton(buttonText).apply {
                addActionListener { addParameterRow(rows, container) }
            })
            add(JBLabel(description).apply {
                foreground = java.awt.Color.GRAY
            })
        }
        add(headerPanel, BorderLayout.NORTH)
        add(container, BorderLayout.CENTER)
    }

    private fun addParameterRow(rows: MutableList<ParameterRow> = parameterRows, container: JPanel = parametersContainer) {
        val nameField = JBTextField()
        val typeField = JBTextField()

        val rowPanel = JPanel(GridBagLayout())
        val gbc = GridBagConstraints().apply { insets = Insets(2, 4, 2, 4) }

        gbc.gridx = 0; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0.0
        rowPanel.add(JBLabel("Navn:"), gbc)

        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0
        rowPanel.add(nameField, gbc)

        gbc.gridx = 2; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0.0
        rowPanel.add(JBLabel("Type:"), gbc)

        gbc.gridx = 3; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0
        rowPanel.add(typeField, gbc)

        val row = ParameterRow(nameField, typeField, rowPanel)

        val removeButton = JButton("✕").apply {
            addActionListener {
                rows.remove(row)
                container.remove(rowPanel)
                container.revalidate()
                container.repaint()
                pack()
            }
        }
        gbc.gridx = 4; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0.0
        rowPanel.add(removeButton, gbc)

        rows.add(row)
        container.add(rowPanel)
        container.revalidate()
        container.repaint()
        pack()
    }

    override fun doValidate(): ValidationInfo? {
        val name = nameField.text.trim()
        if (name.isEmpty()) {
            return ValidationInfo("Navn er påkrevd", nameField)
        }
        if (!name[0].isUpperCase()) {
            return ValidationInfo("Navn må starte med stor bokstav", nameField)
        }
        if (name.contains(" ")) {
            return ValidationInfo("Navn kan ikke inneholde mellomrom", nameField)
        }

        val discriminator = discriminatorField.text.trim()
        if (discriminator.isEmpty()) {
            return ValidationInfo("Diskriminatorverdi er påkrevd", discriminatorField)
        }
        if (discriminator.endsWith("Behandling")) {
            return ValidationInfo("Diskriminatorverdi skal ikke slutte med 'Behandling'", discriminatorField)
        }

        val aktDesc = aktivitetDescriptionField.text.trim()
        if (aktDesc.isEmpty()) {
            return ValidationInfo("Beskrivelse er påkrevd", aktivitetDescriptionField)
        }
        if (!aktDesc[0].isUpperCase()) {
            return ValidationInfo("Beskrivelse må starte med stor bokstav", aktivitetDescriptionField)
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
        outputParameters = outputParameterRows.map { row ->
            ParameterModel(row.nameField.text.trim(), row.typeField.text.trim())
        }.filter { it.name.isNotEmpty() && it.type.isNotEmpty() },
        initialAktivitetNumber = "A101",
        initialAktivitetDescription = aktivitetDescriptionField.text.trim(),
        requestContextUserId = requestContextUserIdField.text.trim().ifEmpty { null },
        createIntegrationTest = integrationTestCheckbox.isSelected,
    )

    companion object {
        val TEAMS = listOf("PESYS_FELLES", "PESYS_ALDER", "PESYS_UFORE")
        val PRIORITIES = listOf("ONLINE_BATCH", "ONLINE", "BATCH")
    }
}
