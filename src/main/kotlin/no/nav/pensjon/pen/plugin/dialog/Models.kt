package no.nav.pensjon.pen.plugin.dialog

data class BehandlingModel(
    val name: String,
    val discriminatorValue: String,
    val team: String,
    val priority: String,
    val parameters: List<ParameterModel>,
    val outputParameters: List<ParameterModel>,
    val initialAktivitetNumber: String,
    val initialAktivitetDescription: String,
    val requestContextUserId: String? = null,
    val createIntegrationTest: Boolean = false,
    val generateGetInputParametere: Boolean = false,
)

data class ParameterModel(
    val name: String,
    val type: String,
)

data class AktivitetModel(
    val behandlingName: String,
    val aktivitetNumber: String,
    val aktivitetDescription: String,
    val isLastAktivitet: Boolean,
    val inputParameters: List<ParameterModel>,
    val outputParameters: List<ParameterModel>,
)
