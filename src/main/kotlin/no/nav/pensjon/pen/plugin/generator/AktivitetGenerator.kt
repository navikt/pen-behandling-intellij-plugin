package no.nav.pensjon.pen.plugin.generator

object AktivitetGenerator {

    fun generate(
        packageName: String,
        behandlingName: String,
        aktivitetNumber: String,
        aktivitetDescription: String,
        isLastAktivitet: Boolean,
    ): String {
        val aktivitetClass = "${behandlingName}${aktivitetNumber}${aktivitetDescription}Aktivitet"
        val processorClass = "${behandlingName}${aktivitetNumber}${aktivitetDescription}AktivitetProcessor"
        val behandlingClass = "${behandlingName}Behandling"
        val discriminatorValue = "${behandlingName}_$aktivitetDescription"

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
            appendLine("import jakarta.persistence.DiscriminatorValue")
            appendLine("import jakarta.persistence.Entity")
            appendLine("import no.nav.pensjon.pen_app.domain.behandling.Aktivitet")
            appendLine("import no.nav.pensjon.pen_app.domain.behandling.AktivitetProcessor")
            appendLine("import no.nav.pensjon.pen_app.domain.behandling.AktivitetResponse")
            appendLine(responseImports)
            appendLine("import org.springframework.stereotype.Component")
            appendLine()
            appendLine("@Entity")
            appendLine("@DiscriminatorValue(\"$discriminatorValue\")")
            appendLine("class $aktivitetClass : Aktivitet()")
            appendLine()
            appendLine("@Component")
            appendLine("class $processorClass(")
            appendLine("    // TODO: Inject required services")
            appendLine(") : AktivitetProcessor<$behandlingClass, $aktivitetClass>(")
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
