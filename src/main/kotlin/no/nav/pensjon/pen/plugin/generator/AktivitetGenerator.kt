package no.nav.pensjon.pen.plugin.generator

import no.nav.pensjon.pen.plugin.dialog.AktivitetModel

object AktivitetGenerator {

    fun generate(
        packageName: String,
        behandlingName: String,
        aktivitetNumber: String,
        aktivitetDescription: String,
        isLastAktivitet: Boolean,
        model: AktivitetModel? = null,
    ): String {
        val aktivitetClass = "${behandlingName}${aktivitetNumber}${aktivitetDescription}Aktivitet"
        val processorClass = "${behandlingName}${aktivitetNumber}${aktivitetDescription}AktivitetProcessor"
        val behandlingClass = "${behandlingName}Behandling"
        val discriminatorValue = "${behandlingName}_$aktivitetDescription"

        val hasInput = model?.inputParameters?.isNotEmpty() == true
        val hasOutput = model?.outputParameters?.isNotEmpty() == true

        val responseImports: String
        val responseBody: String

        if (isLastAktivitet) {
            responseImports = "import no.nav.pensjon.pen_app.domain.behandling.aktivitetFullfort"
            responseBody = "return aktivitetFullfort()"
        } else {
            responseImports = buildString {
                appendLine("import no.nav.pensjon.pen_app.domain.behandling.aktivitetFullfort")
                append("import no.nav.pensjon.pen_app.domain.behandling.nesteAktivitet")
            }
            responseBody = "TODO(\"Return nesteAktivitet(...) or aktivitetFullfort()\")"
        }

        return buildString {
            appendLine("package $packageName")
            appendLine()
            if (hasInput || hasOutput) {
                appendLine("import jakarta.persistence.Column")
            }
            appendLine("import jakarta.persistence.DiscriminatorValue")
            appendLine("import jakarta.persistence.Entity")
            if (hasInput || hasOutput) {
                appendLine("import jakarta.persistence.Lob")
                appendLine("import kotlinx.serialization.Serializable")
                appendLine("import kotlinx.serialization.encodeToString")
                appendLine("import kotlinx.serialization.json.Json")
            }
            appendLine("import no.nav.pensjon.pen_app.domain.behandling.Aktivitet")
            appendLine("import no.nav.pensjon.pen_app.domain.behandling.AktivitetProcessor")
            appendLine("import no.nav.pensjon.pen_app.domain.behandling.AktivitetResponse")
            appendLine(responseImports)
            appendLine("import org.springframework.stereotype.Component")
            appendLine()
            appendLine("@Entity")
            appendLine("@DiscriminatorValue(\"$discriminatorValue\")")

            if (hasInput) {
                val m = model!!
                val constructorParams = m.inputParameters.joinToString(",\n    ") { "${it.name}: ${it.type}" }
                appendLine("class $aktivitetClass(")
                appendLine("    $constructorParams")
                appendLine(") : Aktivitet() {")
                appendLine()
                appendLine("    @Lob")
                appendLine("    @Column(name = \"INPUT\")")
                appendLine("    private var input: String = Json.encodeToString(Input(${m.inputParameters.joinToString(", ") { it.name }}))")
                appendLine()
                for (param in m.inputParameters) {
                    appendLine("    val ${param.name}: ${param.type}")
                    appendLine("        get() = Json.decodeFromString<Input>(input).${param.name}")
                    appendLine()
                }
            } else {
                appendLine("class $aktivitetClass : Aktivitet() {")
            }

            if (hasOutput) {
                val m = model!!
                if (!hasInput) appendLine()
                appendLine("    @Lob")
                appendLine("    @Column(name = \"OUTPUT\")")
                appendLine("    private var output: String? = null")
                appendLine()
                appendLine("    fun setOutput(${m.outputParameters.joinToString(", ") { "${it.name}: ${it.type}" }}) {")
                appendLine("        output = Json.encodeToString(Output(${m.outputParameters.joinToString(", ") { it.name }}))")
                appendLine("    }")
                appendLine()
                for (param in m.outputParameters) {
                    appendLine("    val ${param.name}: ${param.type}?")
                    appendLine("        get() = output?.let { Json.decodeFromString<Output>(it).${param.name} }")
                    appendLine()
                }
            }

            if (hasInput) {
                val m = model!!
                appendLine("    @Serializable")
                appendLine("    data class Input(")
                appendLine("        ${m.inputParameters.joinToString(",\n        ") { "val ${it.name}: ${it.type}" }}")
                appendLine("    )")
                appendLine()
            }

            if (hasOutput) {
                val m = model!!
                appendLine("    @Serializable")
                appendLine("    data class Output(")
                appendLine("        ${m.outputParameters.joinToString(",\n        ") { "val ${it.name}: ${it.type}? = null" }}")
                appendLine("    )")
                appendLine()
            }

            if (!hasInput && !hasOutput) {
                // empty body, close right away
            }

            appendLine("}")
            appendLine()
            appendLine("@Component")
            appendLine("class $processorClass : AktivitetProcessor<$behandlingClass, $aktivitetClass>(")
            appendLine("    $behandlingClass::class,")
            appendLine("    $aktivitetClass::class,")
            appendLine(") {")
            appendLine("    override fun doProcess(behandling: $behandlingClass, aktivitet: $aktivitetClass): AktivitetResponse {")
            appendLine("        $responseBody")
            appendLine("    }")
            appendLine("}")
            appendLine()
        }
    }
}
