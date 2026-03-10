package no.nav.pensjon.pen.plugin.generator

import no.nav.pensjon.pen.plugin.dialog.BehandlingModel

object IntegrationTestGenerator {

    fun generate(packageName: String, model: BehandlingModel): String {
        val behandlingClass = "${model.name}Behandling"
        val constructorArgs = model.parameters.joinToString(",\n            ") {
            "${it.name} = TODO(\"sett testverdi\")"
        }
        val createBehandling = if (model.parameters.isNotEmpty()) {
            "$behandlingClass(\n            $constructorArgs\n        )"
        } else {
            "$behandlingClass()"
        }

        return buildString {
            appendLine("package $packageName")
            appendLine()
            appendLine("import jakarta.persistence.EntityManager")
            appendLine("import no.nav.pensjon.pen_app.domain.behandling.BehandlingProcessorService")
            appendLine("import no.nav.pensjon.pen_app.domain.behandling.kjørTilFerdigEllerUtsatt")
            appendLine("import no.nav.pensjon.pen_app.support.integration_tests.PenApplicationIntegrationTest")
            appendLine("import org.junit.jupiter.api.Test")
            appendLine("import org.springframework.beans.factory.annotation.Autowired")
            appendLine("import org.springframework.transaction.annotation.Transactional")
            appendLine()
            appendLine("@PenApplicationIntegrationTest")
            appendLine("@Transactional")
            appendLine("class ${behandlingClass}IT @Autowired constructor(")
            appendLine("    private val entityManager: EntityManager,")
            appendLine("    private val behandlingProcessorService: BehandlingProcessorService,")
            appendLine(") {")
            appendLine()
            appendLine("    @Test")
            appendLine("    fun `skal kjøre behandling til ferdig`() {")
            appendLine("        val behandling = $createBehandling")
            appendLine("        entityManager.persist(behandling)")
            appendLine("        entityManager.flush()")
            appendLine()
            appendLine("        behandlingProcessorService.kjørTilFerdigEllerUtsatt(")
            appendLine("            entityManager = entityManager,")
            appendLine("            behandlingId = behandling.behandlingId!!,")
            appendLine("        )")
            appendLine("    }")
            appendLine("}")
            appendLine()
        }
    }
}
