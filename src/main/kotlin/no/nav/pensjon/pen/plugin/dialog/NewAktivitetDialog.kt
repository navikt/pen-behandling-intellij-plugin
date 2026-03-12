package no.nav.pensjon.pen.plugin.dialog

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.COLUMNS_MEDIUM
import com.intellij.ui.dsl.builder.columns
import com.intellij.ui.dsl.builder.panel
import java.awt.BorderLayout
import java.awt.FlowLayout
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import javax.swing.*

class NewAktivitetDialog(
    project: Project,
    private val behandlingName: String,
    suggestedNumber: String = "A101",
) : DialogWrapper(project) {

    private val aktivitetNumberField = JBTextField(suggestedNumber)
    private val aktivitetDescriptionField = JBTextField()
    private var isLastAktivitet = true

    private data class ParameterRow(
        val nameField: JBTextField,
        val typeField: JBTextField,
        val panel: JPanel,
    )

    private val inputParameterRows = mutableListOf<ParameterRow>()
    private lateinit var inputParametersContainer: JPanel

    private val outputParameterRows = mutableListOf<ParameterRow>()
    private lateinit var outputParametersContainer: JPanel

    init {
        title = "Ny Aktivitet"
        setSize(550, 500)
        init()
        initValidation()
    }

    override fun createCenterPanel(): JComponent {
        val mainPanel = JPanel(BorderLayout())

        val topPanel = panel {
            row("Behandling:") {
                label(behandlingName.ifEmpty { "(ukjent)" })
                    .bold()
            }
            row("Aktivitetsnummer:") {
                cell(aktivitetNumberField)
                    .columns(COLUMNS_MEDIUM)
                    .comment("F.eks. 'A101', 'A201'")
            }
            row("Beskrivelse:") {
                cell(aktivitetDescriptionField)
                    .columns(COLUMNS_MEDIUM)
                    .focused()
                    .comment("Funksjonelt navn, f.eks. 'OpprettOppgave'")
            }
            separator()
            row {
                checkBox("Dette er siste aktivitet (returnerer aktivitetFullfort())")
                    .onChanged { isLastAktivitet = it.isSelected }
                    .applyToComponent { isSelected = true }
            }
        }

        inputParametersContainer = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
        }

        val inputSection = createParameterSection(
            "Input-parametere",
            "Legg til input-parameter",
            "Parametere som aktiviteten mottar",
            inputParameterRows,
            inputParametersContainer,
        )

        outputParametersContainer = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
        }

        val outputSection = createParameterSection(
            "Output-parametere",
            "Legg til output-parameter",
            "Parametere som aktiviteten produserer",
            outputParameterRows,
            outputParametersContainer,
        )

        val verticalBox = Box.createVerticalBox().apply {
            add(topPanel)
            add(inputSection)
            add(outputSection)
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

    private fun addParameterRow(rows: MutableList<ParameterRow>, container: JPanel) {
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
        val num = aktivitetNumberField.text.trim()
        if (!num.matches(Regex("A\\d{3}"))) {
            return ValidationInfo("Aktivitetsnummer må være på formen A###, f.eks. 'A101'", aktivitetNumberField)
        }

        val desc = aktivitetDescriptionField.text.trim()
        if (desc.isEmpty()) {
            return ValidationInfo("Beskrivelse er påkrevd", aktivitetDescriptionField)
        }
        if (!desc[0].isUpperCase()) {
            return ValidationInfo("Beskrivelse må starte med stor bokstav", aktivitetDescriptionField)
        }

        return null
    }

    fun getModel(): AktivitetModel = AktivitetModel(
        behandlingName = behandlingName.removeSuffix("Behandling"),
        aktivitetNumber = aktivitetNumberField.text.trim(),
        aktivitetDescription = aktivitetDescriptionField.text.trim(),
        isLastAktivitet = isLastAktivitet,
        inputParameters = inputParameterRows.map { row ->
            ParameterModel(row.nameField.text.trim(), row.typeField.text.trim())
        }.filter { it.name.isNotEmpty() && it.type.isNotEmpty() },
        outputParameters = outputParameterRows.map { row ->
            ParameterModel(row.nameField.text.trim(), row.typeField.text.trim())
        }.filter { it.name.isNotEmpty() && it.type.isNotEmpty() },
    )
}
