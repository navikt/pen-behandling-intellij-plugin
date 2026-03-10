package no.nav.pensjon.pen.plugin.generator

import no.nav.pensjon.pen.plugin.dialog.BehandlingModel

object BehandlingGenerator {

    fun generate(packageName: String, model: BehandlingModel): String {
        val hasInput = model.parameters.isNotEmpty()
        val hasOutput = model.outputParameters.isNotEmpty()
        val constructorParams = model.parameters.joinToString(",\n    ") { "${it.name}: ${it.type}" }
        val serializableFields = model.parameters.joinToString(",\n        ") { "val ${it.name}: ${it.type}" }
        val outputSerializableFields = model.outputParameters.joinToString(",\n        ") { "val ${it.name}: ${it.type}? = null" }
        val inputGetters = model.parameters.joinToString("\n\n") { param ->
            """    val ${param.name}: ${param.type}
        get() = Json.decodeFromString<Parametere>(input).${param.name}"""
        }
        val jsonEncode = if (hasInput) {
            "Json.encodeToString(Parametere(${model.parameters.joinToString(", ") { it.name }}))"
        } else {
            """"{}""""
        }
        val initialAktivitetClass = "${model.name}${model.initialAktivitetNumber}${model.initialAktivitetDescription}Aktivitet"

        return buildString {
            appendLine("package $packageName")
            appendLine()
            appendLine("import jakarta.persistence.Column")
            appendLine("import jakarta.persistence.DiscriminatorValue")
            appendLine("import jakarta.persistence.Entity")
            if (hasInput || hasOutput) {
                appendLine("import jakarta.persistence.Lob")
                appendLine("import kotlinx.serialization.Serializable")
                appendLine("import kotlinx.serialization.encodeToString")
                appendLine("import kotlinx.serialization.json.Json")
            }
            appendLine("import no.nav.domain.pensjon.common.vedtak.Prioritet")
            appendLine("import no.nav.pensjon.pen_app.domain.behandling.Aktivitet")
            appendLine("import no.nav.pensjon.pen_app.domain.behandling.Behandling")
            appendLine("import no.nav.pensjon.pen_app.domain.behandling.ForvalgtAnsvarligTeam")
            appendLine("import no.nav.pensjon.pen_app.domain.behandling.Team.${model.team}")
            appendLine()
            appendLine("@Entity")
            appendLine("@DiscriminatorValue(value = \"${model.discriminatorValue}\")")
            appendLine("@ForvalgtAnsvarligTeam(${model.team})")

            if (hasInput) {
                appendLine("class ${model.name}Behandling(")
                appendLine("    $constructorParams")
                appendLine(") : Behandling(prioritet = Prioritet.${model.priority}) {")
                appendLine()
                appendLine("    @Lob")
                appendLine("    @Column(name = \"INPUT\")")
                appendLine("    private var input: String = $jsonEncode")
                appendLine()
                appendLine(inputGetters)
            } else {
                appendLine("class ${model.name}Behandling : Behandling(prioritet = Prioritet.${model.priority}) {")
            }

            if (hasOutput) {
                appendLine()
                appendLine("    @Lob")
                appendLine("    @Column(name = \"OUTPUT\")")
                appendLine("    private var output: String? = null")
                appendLine()
                appendLine("    fun getOutput(): Output = Json.decodeFromString(output!!)")
                appendLine()
                appendLine("    fun saveOutput(output: Output) {")
                appendLine("        this.output = Json.encodeToString(output)")
                appendLine("    }")
            }

            appendLine()
            appendLine("    override fun opprettInitiellAktivitet(): Aktivitet = $initialAktivitetClass()")

            if (hasInput) {
                appendLine()
                appendLine("    @Serializable")
                appendLine("    data class Parametere(")
                appendLine("        $serializableFields")
                appendLine("    )")
            }

            if (hasOutput) {
                appendLine()
                appendLine("    @Serializable")
                appendLine("    data class Output(")
                appendLine("        $outputSerializableFields")
                appendLine("    )")
            }

            appendLine("}")
            appendLine()
        }
    }
}
